"""Train a tiny GPT on the bundled Shakespeare text.

The model reads blocks of characters and learns to predict the next
one. The live chart shows the training loss (train) and the loss on
held-out text (val). The trained weights are saved for the next labs,
also when you press Stop.

TRY:
  STEPS = 300 (a shorter run that still lowers the loss)
  N_LAYER = 1 (a smaller model trains faster and learns less)
  LR = 1e-2 (a larger step size; watch for instability)
"""
import numpy as np

import slmlab
from tinygpt import (Adam, CharTokenizer, Config, TinyGPT,
                     estimate_loss, get_batch)

STEPS = 1500
BATCH = 16
BLOCK = 32
N_LAYER = 2
N_HEAD = 2
D_MODEL = 64
LR = 3e-3
EVAL_EVERY = 100
SEED = 0
TRAIN_FRACTION = 0.9
SAMPLE_CHARS = 200


def load_data():
    """Returns the tokenizer and the train and validation ids."""
    text = slmlab.corpus()
    tok = CharTokenizer(text)
    ids = tok.encode(text)
    cut = int(TRAIN_FRACTION * len(ids))
    return tok, ids[:cut], ids[cut:]


def train(model, train_ids, val_ids):
    """One Adam step per loop. Returns the last training loss."""
    opt = Adam(model.params, lr=LR)
    rng = np.random.default_rng(SEED)
    val_rng = np.random.default_rng(SEED + 1)
    loss = None
    for step in range(1, STEPS + 1):
        if slmlab.should_stop():
            print("stop requested at", step)
            break
        x, y = get_batch(train_ids, BLOCK, BATCH, rng)
        _, loss = model.forward(x, y)
        opt.step(model.params, model.backward())
        if step % 10 == 0:
            slmlab.metric("train", step, loss)
        if step % EVAL_EVERY == 0:
            val = estimate_loss(model, val_ids, BLOCK, BATCH, val_rng)
            slmlab.metric("val", step, val)
            print(f"step {step:5d}  train {loss:.3f}  val {val:.3f}")
    return loss


def main():
    tok, train_ids, val_ids = load_data()
    cfg = Config(vocab_size=tok.vocab_size, block_size=BLOCK,
                 n_layer=N_LAYER, n_head=N_HEAD, d_model=D_MODEL)
    model = TinyGPT(cfg, seed=SEED)
    print("parameters:", model.num_params())
    path = slmlab.work_path("tiny_gpt.npz")
    try:
        train(model, train_ids, val_ids)
    finally:
        model.save(path, tok.chars)
    print("model saved")
    start = tok.encode("\n")[None, :]
    rng = np.random.default_rng(SEED)
    out = model.generate(start, SAMPLE_CHARS, temperature=0.8, rng=rng)
    print(tok.decode(out[0]))


main()
