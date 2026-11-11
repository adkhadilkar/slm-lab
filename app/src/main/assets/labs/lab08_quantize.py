"""Quantize the trained weights to 8 and 4 bits.

Each weight matrix is stored as small integers plus one float scale
(symmetric, one scale per matrix). Fewer bits take less space but add
rounding error, which shows up as a higher validation loss. Vectors
(biases and norms) stay as float32. Without a saved model, the lab
quantizes a random matrix and reports the reconstruction error.

TRY:
  BIT_WIDTHS = (8, 6, 3) (other bit widths, same code)
  VAL_ITERS = 40 (a more accurate validation loss)
  SEED = 3 (a different random matrix when there is no model)
"""
import os

import numpy as np

import slmlab
from tinygpt import CharTokenizer, TinyGPT, estimate_loss

BIT_WIDTHS = (8, 4)
VAL_ITERS = 20
SEED = 0
BATCH = 16
RANDOM_SHAPE = (64, 64)
MODEL_FILE = "tiny_gpt.npz"


def quantize(w, bits):
    """Returns (integers, scale) for symmetric quantization."""
    qmax = 2 ** (bits - 1) - 1                      # 127 for 8 bits
    scale = float(np.abs(w).max()) / qmax
    if scale == 0.0:
        scale = 1.0
    q = np.clip(np.round(w / scale), -qmax, qmax).astype(np.int8)
    return q, scale


def dequantize(q, scale):
    return (q.astype(np.float32) * scale).astype(np.float32)


def quantized_params(params, bits):
    """Each matrix replaced by its quantize-then-dequantize copy."""
    out = {}
    for name, w in params.items():
        if w.ndim == 2:
            out[name] = dequantize(*quantize(w, bits))
        else:
            out[name] = w.copy()
    return out


def size_kb(params, bits):
    """Storage for matrices at `bits`; other arrays stay float32."""
    total = 0.0
    for w in params.values():
        if w.ndim == 2:
            total += w.size * bits / 8 + 4          # + the float scale
        else:
            total += w.size * 4
    return total / 1024


def load_model():
    """Returns (model, tokenizer), or None when no model is saved."""
    path = slmlab.work_path(MODEL_FILE)
    if not os.path.exists(path):
        return None
    return TinyGPT.load(path)


def val_loss(model, val_ids):
    rng = np.random.default_rng(SEED)
    return estimate_loss(model, val_ids, model.cfg.block_size, BATCH,
                         rng, iters=VAL_ITERS)


def compare_models(model, tok):
    text = slmlab.corpus()
    ids = tok.encode(text)
    val_ids = ids[int(0.9 * len(ids)):]
    print(f"{'format':<8}{'KB':>8}{'val loss':>10}")
    for bits in (32,) + BIT_WIDTHS:
        name = "float32" if bits == 32 else f"int{bits}"
        m = model
        if bits != 32:
            m = TinyGPT(model.cfg)
            m.params = quantized_params(model.params, bits)
        loss = val_loss(m, val_ids)
        print(f"{name:<8}{size_kb(model.params, bits):>8.1f}"
              f"{loss:>10.3f}")


def random_matrix_demo():
    rng = np.random.default_rng(SEED)
    w = rng.normal(0.0, 0.02, size=RANDOM_SHAPE).astype(np.float32)
    print("Reconstruction error (mean abs):")
    for bits in BIT_WIDTHS:
        err = np.abs(w - dequantize(*quantize(w, bits)))
        print(f"  int{bits}: {err.mean():.6f}")


def main():
    saved = load_model()
    if saved is None:
        print("No saved model yet. Showing a random matrix.")
        random_matrix_demo()
        return
    model, tok = saved
    compare_models(model, tok)


main()
