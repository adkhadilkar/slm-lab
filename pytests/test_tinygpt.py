"""Tests for tinygpt.py: gradients, training, save/load, generation."""
import numpy as np

import slmlab
from tinygpt import Adam, CharTokenizer, Config, TinyGPT, get_batch


def tiny_model(seed=0):
    cfg = Config(vocab_size=11, block_size=6, n_layer=2, n_head=2,
                 d_model=8, dtype=np.float64)
    return TinyGPT(cfg, seed=seed)


def test_gradient_check_every_parameter():
    model = tiny_model()
    rng = np.random.default_rng(1)
    x = rng.integers(0, 11, size=(3, 6))
    y = rng.integers(0, 11, size=(3, 6))
    model.forward(x, y)
    grads = model.backward()
    assert set(grads) == set(model.params)
    eps = 1e-5
    worst = 0.0
    for name, p in model.params.items():
        picks = rng.choice(p.size, size=min(6, p.size), replace=False)
        for flat_i in picks:
            pos = np.unravel_index(flat_i, p.shape)
            orig = p[pos]
            p[pos] = orig + eps
            _, loss_plus = model.forward(x, y)
            p[pos] = orig - eps
            _, loss_minus = model.forward(x, y)
            p[pos] = orig
            numeric = (loss_plus - loss_minus) / (2 * eps)
            analytic = grads[name][pos]
            denom = max(1e-8, abs(analytic) + abs(numeric))
            rel = abs(analytic - numeric) / denom
            worst = max(worst, rel)
            assert rel < 1e-4, (name, pos, analytic, numeric, rel)
    print("worst relative error", worst)


def test_training_lowers_validation_loss():
    text = slmlab.corpus()
    tok = CharTokenizer(text)
    data = tok.encode(text)
    split = int(0.9 * len(data))
    train, val = data[:split], data[split:]
    cfg = Config(vocab_size=tok.vocab_size)
    model = TinyGPT(cfg, seed=0)
    opt = Adam(model.params, lr=3e-3)
    rng = np.random.default_rng(0)
    for _ in range(300):
        x, y = get_batch(train, cfg.block_size, 16, rng)
        model.forward(x, y)
        opt.step(model.params, model.backward())
    vx, vy = get_batch(val, cfg.block_size, 64, np.random.default_rng(5))
    _, val_loss = model.forward(vx, vy)
    print("validation loss", val_loss)
    assert val_loss < 2.7


def test_save_then_load_gives_identical_logits(tmp_path):
    text = "abcdefghijklmnopqrstuvwxyz \n"
    tok = CharTokenizer(text)
    model = TinyGPT(Config(vocab_size=tok.vocab_size))
    x = np.array([tok.encode("hello world")])
    logits_before, _ = model.forward(x)
    path = str(tmp_path / "model.npz")
    model.save(path, tok.chars)
    loaded, tok2 = TinyGPT.load(path)
    logits_after, _ = loaded.forward(x)
    assert tok2.chars == tok.chars
    assert np.array_equal(logits_before, logits_after)
    assert loaded.num_params() == model.num_params()


def test_generate_shape_and_valid_ids():
    tok = CharTokenizer("abcde fgh")
    model = TinyGPT(Config(vocab_size=tok.vocab_size, block_size=8))
    idx = tok.encode("abc")[None, :]
    out = model.generate(idx, 20, temperature=0.8, top_k=5,
                         rng=np.random.default_rng(3))
    assert out.shape == (1, 3 + 20)
    assert out.min() >= 0 and out.max() < tok.vocab_size
    assert np.array_equal(out[:, :3], idx)


def test_generate_top_k_one_is_deterministic():
    tok = CharTokenizer("abcde fgh")
    model = TinyGPT(Config(vocab_size=tok.vocab_size, block_size=8))
    idx = tok.encode("ab")[None, :]
    a = model.generate(idx, 15, top_k=1, rng=np.random.default_rng(1))
    b = model.generate(idx, 15, top_k=1, rng=np.random.default_rng(99))
    assert np.array_equal(a, b)


def test_tokenizer_round_trip_and_unknown_skipped():
    tok = CharTokenizer("hello")
    assert tok.chars == sorted(set("hello"))
    ids = tok.encode("hel?lo")
    assert ids.dtype == np.int64
    assert tok.decode(ids) == "hello"
