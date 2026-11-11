"""A tiny decoder-only transformer in NumPy, with hand-written backprop.

Shapes: B batch, T time (tokens), C d_model, H heads, hd head size,
V vocabulary size. Each *_fwd function has a *_bwd partner below it that
returns the gradient of its input and of its parameters.
"""
import math

import numpy as np

LN_EPS = 1e-5
GELU_K = math.sqrt(2.0 / math.pi)
GELU_A = 0.044715


class Config:
    """Model size. Plain attributes with defaults."""

    def __init__(self, vocab_size, block_size=32, n_layer=2, n_head=2,
                 d_model=64, dtype=np.float32):
        self.vocab_size = vocab_size
        self.block_size = block_size
        self.n_layer = n_layer
        self.n_head = n_head
        self.d_model = d_model
        self.dtype = dtype


class CharTokenizer:
    """One id per distinct character, in sorted order."""

    def __init__(self, text_or_chars):
        self.chars = sorted(set(text_or_chars))
        self.vocab_size = len(self.chars)
        self._index = {c: i for i, c in enumerate(self.chars)}

    def encode(self, s):
        """Returns int64 ids. Unknown characters are skipped."""
        ids = [self._index[c] for c in s if c in self._index]
        return np.array(ids, dtype=np.int64)

    def decode(self, ids):
        return "".join(self.chars[int(i)] for i in ids)


# ---------- primitives: each returns (output, cache) ----------

def softmax(s):
    """s: (..., N). Softmax over the last axis."""
    e = np.exp(s - s.max(axis=-1, keepdims=True))
    return e / e.sum(axis=-1, keepdims=True)


def linear_fwd(x, w, b):
    """x: (..., In), w: (In, Out), b: (Out,) -> (..., Out)."""
    return x @ w + b, (x, w)


def linear_bwd(dy, cache):
    """Returns dx (..., In), dw (In, Out), db (Out,).

    Weight and bias gradients sum over every leading axis (batch and time).
    """
    x, w = cache
    x2 = x.reshape(-1, x.shape[-1])          # (N, In), N = B*T
    dy2 = dy.reshape(-1, dy.shape[-1])       # (N, Out)
    return dy @ w.T, x2.T @ dy2, dy2.sum(axis=0)


def layer_norm_fwd(x, g, b):
    """x: (B, T, C). Normalise each vector over C, then scale and shift."""
    mu = x.mean(axis=-1, keepdims=True)                       # (B, T, 1)
    xc = x - mu                                               # (B, T, C)
    rstd = 1.0 / np.sqrt((xc * xc).mean(axis=-1, keepdims=True) + LN_EPS)
    xhat = xc * rstd                                          # (B, T, C)
    return g * xhat + b, (xhat, rstd, g)


def layer_norm_bwd(dy, cache):
    """Returns dx (B, T, C), dg (C,), db (C,).

    The mean and variance both depend on x, so their terms are kept.
    """
    xhat, rstd, g = cache
    axes = tuple(range(dy.ndim - 1))
    dxhat = dy * g                                            # (B, T, C)
    dx = rstd * (dxhat - dxhat.mean(axis=-1, keepdims=True)
                 - xhat * (dxhat * xhat).mean(axis=-1, keepdims=True))
    return dx, (dy * xhat).sum(axis=axes), dy.sum(axis=axes)


def gelu_fwd(x):
    """GELU, tanh approximation. x: (B, T, 4C)."""
    # x * x * x, not x ** 3: the power ufunc is about 100x slower in float32
    t = np.tanh(GELU_K * (x + GELU_A * x * x * x))            # (B, T, 4C)
    return 0.5 * x * (1.0 + t), (x, t)


def gelu_bwd(dy, cache):
    """Derivative of 0.5 x (1 + tanh(u)) with u = k (x + a x^3)."""
    x, t = cache
    du = GELU_K * (1.0 + 3.0 * GELU_A * x * x)                # du/dx
    dgelu = 0.5 * (1.0 + t) + 0.5 * x * (1.0 - t * t) * du    # (B, T, 4C)
    return dy * dgelu


def cross_entropy(logits, targets):
    """Mean NLL over all B*T positions. logits (B, T, V), targets (B, T)."""
    z = logits - logits.max(axis=-1, keepdims=True)           # (B, T, V)
    logp = z - np.log(np.exp(z).sum(axis=-1, keepdims=True))  # (B, T, V)
    picked = np.take_along_axis(logp, targets[..., None], axis=-1)
    return float(-picked.mean())


def cross_entropy_bwd(logits, targets):
    """d(mean loss)/d logits = (softmax - one_hot) / (B*T)."""
    B, T = targets.shape
    d = softmax(logits)                                       # (B, T, V)
    bi, ti = np.indices((B, T))
    d[bi, ti, targets] -= 1.0
    return d / (B * T)


# ---------- causal multi-head attention ----------

def attn_fwd(h, P, n_head):
    """h: (B, T, C). P holds qkv_w, qkv_b, proj_w, proj_b."""
    B, T, C = h.shape
    hd = C // n_head
    qkv, c_qkv = linear_fwd(h, P["qkv_w"], P["qkv_b"])        # (B, T, 3C)
    qkv = qkv.reshape(B, T, 3, n_head, hd).transpose(2, 0, 3, 1, 4)
    q, k, v = qkv[0], qkv[1], qkv[2]                          # (B, H, T, hd)
    scores = q @ k.transpose(0, 1, 3, 2) / math.sqrt(hd)      # (B, H, T, T)
    future = np.triu(np.ones((T, T), dtype=bool), k=1)        # j > i
    scores = np.where(future, scores.dtype.type(-1e9), scores)
    p = softmax(scores)                                       # (B, H, T, T)
    o = (p @ v).transpose(0, 2, 1, 3).reshape(B, T, C)        # (B, T, C)
    out, c_proj = linear_fwd(o, P["proj_w"], P["proj_b"])     # (B, T, C)
    return out, (q, k, v, p, c_qkv, c_proj, n_head)


def attn_bwd(dout, cache):
    """Returns dh (B, T, C) and a dict of attention parameter gradients."""
    q, k, v, p, c_qkv, c_proj, n_head = cache
    B, T, C = dout.shape
    hd = C // n_head
    do, dw_proj, db_proj = linear_bwd(dout, c_proj)           # (B, T, C)
    do = do.reshape(B, T, n_head, hd).transpose(0, 2, 1, 3)   # (B, H, T, hd)
    dp = do @ v.transpose(0, 1, 3, 2)                         # (B, H, T, T)
    dv = p.transpose(0, 1, 3, 2) @ do                         # (B, H, T, hd)
    # softmax backward; masked entries have p = 0, so they get no gradient
    ds = p * (dp - (dp * p).sum(axis=-1, keepdims=True))      # (B, H, T, T)
    scale = 1.0 / math.sqrt(hd)
    dq = ds @ k * scale                                       # (B, H, T, hd)
    dk = ds.transpose(0, 1, 3, 2) @ q * scale                 # (B, H, T, hd)
    dqkv = np.stack([dq, dk, dv])                             # (3, B, H, T, hd)
    dqkv = dqkv.transpose(1, 3, 0, 2, 4).reshape(B, T, 3 * C)  # (B, T, 3C)
    dh, dw_qkv, db_qkv = linear_bwd(dqkv, c_qkv)              # (B, T, C)
    grads = {"qkv_w": dw_qkv, "qkv_b": db_qkv,
             "proj_w": dw_proj, "proj_b": db_proj}
    return dh, grads


# ---------- one pre-norm transformer block ----------

def block_fwd(x, P, n_head):
    """x: (B, T, C). P holds the block's parameters (local names)."""
    h, c_ln1 = layer_norm_fwd(x, P["ln1_g"], P["ln1_b"])      # (B, T, C)
    a, c_att = attn_fwd(h, P, n_head)                         # (B, T, C)
    x = x + a                                                 # residual
    h, c_ln2 = layer_norm_fwd(x, P["ln2_g"], P["ln2_b"])      # (B, T, C)
    m, c_fc = linear_fwd(h, P["fc_w"], P["fc_b"])             # (B, T, 4C)
    g, c_gelu = gelu_fwd(m)                                   # (B, T, 4C)
    o, c_out = linear_fwd(g, P["out_w"], P["out_b"])          # (B, T, C)
    return x + o, (c_ln1, c_att, c_ln2, c_fc, c_gelu, c_out)


def block_bwd(dx, cache):
    """Returns dx for the block input and a dict of local gradients."""
    c_ln1, c_att, c_ln2, c_fc, c_gelu, c_out = cache
    grads = {}
    dg, grads["out_w"], grads["out_b"] = linear_bwd(dx, c_out)
    dm = gelu_bwd(dg, c_gelu)                                 # (B, T, 4C)
    dh, grads["fc_w"], grads["fc_b"] = linear_bwd(dm, c_fc)   # (B, T, C)
    dx2, grads["ln2_g"], grads["ln2_b"] = layer_norm_bwd(dh, c_ln2)
    dx = dx + dx2                                             # residual
    da, att_grads = attn_bwd(dx, c_att)
    grads.update(att_grads)
    dx1, grads["ln1_g"], grads["ln1_b"] = layer_norm_bwd(da, c_ln1)
    return dx + dx1, grads


# ---------- the model ----------

class TinyGPT:
    """Token + position embeddings, n_layer blocks, final norm, head."""

    def __init__(self, cfg, seed=0):
        self.cfg = cfg
        self.params = self._init_params(np.random.default_rng(seed))
        self._cache = None

    def _init_params(self, rng):
        cfg = self.cfg
        C, V, T = cfg.d_model, cfg.vocab_size, cfg.block_size

        def w(*shape):
            return (rng.standard_normal(shape) * 0.02).astype(cfg.dtype)

        def zeros(*shape):
            return np.zeros(shape, dtype=cfg.dtype)

        def ones(*shape):
            return np.ones(shape, dtype=cfg.dtype)

        p = {"wte": w(V, C), "wpe": w(T, C)}
        for i in range(cfg.n_layer):
            pre = f"l{i}."
            p.update({
                pre + "ln1_g": ones(C), pre + "ln1_b": zeros(C),
                pre + "qkv_w": w(C, 3 * C), pre + "qkv_b": zeros(3 * C),
                pre + "proj_w": w(C, C), pre + "proj_b": zeros(C),
                pre + "ln2_g": ones(C), pre + "ln2_b": zeros(C),
                pre + "fc_w": w(C, 4 * C), pre + "fc_b": zeros(4 * C),
                pre + "out_w": w(4 * C, C), pre + "out_b": zeros(C),
            })
        p.update({"lnf_g": ones(C), "lnf_b": zeros(C),
                  "head_w": w(C, V), "head_b": zeros(V)})
        return p

    def _layer(self, i):
        """The parameters of block i, with the 'l<i>.' prefix removed."""
        pre = f"l{i}."
        return {k[len(pre):]: v for k, v in self.params.items()
                if k.startswith(pre)}

    def forward(self, idx, targets=None):
        """idx (B, T) ints. Returns (logits (B, T, V), loss or None)."""
        P = self.params
        idx = np.asarray(idx, dtype=np.int64)
        B, T = idx.shape
        if T > self.cfg.block_size:
            raise ValueError("sequence longer than block_size")
        x = P["wte"][idx] + P["wpe"][:T]                      # (B, T, C)
        caches = []
        for i in range(self.cfg.n_layer):
            x, c = block_fwd(x, self._layer(i), self.cfg.n_head)
            caches.append(c)
        h, c_lnf = layer_norm_fwd(x, P["lnf_g"], P["lnf_b"])  # (B, T, C)
        logits, c_head = linear_fwd(h, P["head_w"], P["head_b"])  # (B, T, V)
        self._cache = {"idx": idx, "blocks": caches, "lnf": c_lnf,
                       "head": c_head, "logits": logits, "targets": None}
        if targets is None:
            self._cache = None
            return logits, None
        self._cache["targets"] = np.asarray(targets, dtype=np.int64)
        return logits, cross_entropy(logits, self._cache["targets"])

    def backward(self):
        """Gradients of the last forward's loss, same keys as params."""
        c = self._cache
        if c is None or c["targets"] is None:
            raise ValueError("call forward(idx, targets) before backward()")
        grads = {}
        dl = cross_entropy_bwd(c["logits"], c["targets"])     # (B, T, V)
        dh, grads["head_w"], grads["head_b"] = linear_bwd(dl, c["head"])
        dx, grads["lnf_g"], grads["lnf_b"] = layer_norm_bwd(dh, c["lnf"])
        for i in reversed(range(self.cfg.n_layer)):
            dx, block_grads = block_bwd(dx, c["blocks"][i])
            for k, v in block_grads.items():
                grads[f"l{i}.{k}"] = v
        # position rows get the gradient summed over the batch
        grads["wpe"] = np.zeros_like(self.params["wpe"])
        grads["wpe"][:dx.shape[1]] = dx.sum(axis=0)           # (T, C)
        grads["wte"] = np.zeros_like(self.params["wte"])
        np.add.at(grads["wte"], c["idx"], dx)                 # (V, C)
        return grads

    def generate(self, idx, max_new_tokens, temperature=1.0, top_k=None,
                 rng=None):
        """idx (1, T). Returns (1, T + max_new_tokens) int64 ids."""
        rng = rng if rng is not None else np.random.default_rng(0)
        idx = np.asarray(idx, dtype=np.int64)
        V = self.cfg.vocab_size
        for _ in range(max_new_tokens):
            logits, _ = self.forward(idx[:, -self.cfg.block_size:])
            s = logits[:, -1, :].astype(np.float64) / temperature  # (1, V)
            if top_k is not None and top_k < V:
                kth = np.sort(s, axis=-1)[:, -top_k][:, None]     # (1, 1)
                s = np.where(s < kth, -np.inf, s)
            probs = softmax(s)[0]                                 # (V,)
            nxt = rng.choice(V, p=probs / probs.sum())
            idx = np.concatenate([idx, [[nxt]]], axis=1)
        return idx

    def num_params(self):
        return int(sum(v.size for v in self.params.values()))

    def save(self, path, chars):
        """Writes weights and config with np.savez (no pickled objects)."""
        arrays = dict(self.params)
        arrays["cfg_vocab_size"] = np.array(self.cfg.vocab_size)
        arrays["cfg_block_size"] = np.array(self.cfg.block_size)
        arrays["cfg_n_layer"] = np.array(self.cfg.n_layer)
        arrays["cfg_n_head"] = np.array(self.cfg.n_head)
        arrays["cfg_d_model"] = np.array(self.cfg.d_model)
        arrays["chars"] = np.array("".join(chars))
        np.savez(path, **arrays)

    @classmethod
    def load(cls, path):
        """Returns (model, tokenizer) saved by save()."""
        with np.load(path, allow_pickle=False) as data:
            arrays = {k: data[k] for k in data.files}
        chars = arrays.pop("chars").item()
        cfg = Config(
            vocab_size=int(arrays.pop("cfg_vocab_size")),
            block_size=int(arrays.pop("cfg_block_size")),
            n_layer=int(arrays.pop("cfg_n_layer")),
            n_head=int(arrays.pop("cfg_n_head")),
            d_model=int(arrays.pop("cfg_d_model")),
            dtype=arrays["wte"].dtype.type)
        model = cls(cfg)
        model.params = arrays
        return model, CharTokenizer(chars)


# ---------- training helpers ----------

class Adam:
    """Adam with the global gradient norm clipped to 1.0 each step."""

    def __init__(self, params, lr=3e-3, betas=(0.9, 0.99), eps=1e-8):
        self.lr = lr
        self.b1, self.b2 = betas
        self.eps = eps
        self.t = 0
        self.m = {k: np.zeros_like(v) for k, v in params.items()}
        self.v = {k: np.zeros_like(v) for k, v in params.items()}

    def step(self, params, grads):
        """Updates params in place."""
        self.t += 1
        norm = math.sqrt(sum(float((g * g).sum()) for g in grads.values()))
        scale = 1.0 if norm <= 1.0 else 1.0 / norm
        bc1 = 1.0 - self.b1 ** self.t
        bc2 = 1.0 - self.b2 ** self.t
        for k, p in params.items():
            g = grads[k] * scale
            self.m[k] = self.b1 * self.m[k] + (1.0 - self.b1) * g
            self.v[k] = self.b2 * self.v[k] + (1.0 - self.b2) * g * g
            mhat = self.m[k] / bc1
            vhat = self.v[k] / bc2
            p -= (self.lr * mhat / (np.sqrt(vhat) + self.eps)).astype(p.dtype)


def get_batch(data, block_size, batch_size, rng):
    """Random windows of data. Returns inputs x and targets y, (B, T)."""
    starts = rng.integers(0, len(data) - block_size, size=batch_size)
    offsets = np.arange(block_size)                           # (T,)
    x = data[starts[:, None] + offsets]                       # (B, T)
    y = data[starts[:, None] + offsets + 1]                   # (B, T)
    return x.astype(np.int64), y.astype(np.int64)


def estimate_loss(model, data, block_size, batch_size, rng, iters=10):
    """Mean loss over `iters` random batches. No parameter update."""
    losses = []
    for _ in range(iters):
        x, y = get_batch(data, block_size, batch_size, rng)
        _, loss = model.forward(x, y)
        losses.append(loss)
    return float(np.mean(losses))
