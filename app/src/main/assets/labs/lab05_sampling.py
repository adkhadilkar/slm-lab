"""Sampling from the trained tiny GPT.

Loads the model saved by lab04. Temperature rescales the scores before
the softmax: low values pick the likeliest characters, high values take
more risks. Top-k keeps only the k likeliest characters. The tables show
the probabilities the model gives to the next character.

TRY:
  PROMPT = "To be" (text from the corpus works best)
  SAMPLE_CHARS = 200 (longer samples)
  TABLE_TEMPS = (0.5, 2.0) (other temperatures for the tables)
"""
import os

import numpy as np

import slmlab
from tinygpt import TinyGPT, softmax

PROMPT = "ROMEO:"
SAMPLE_CHARS = 80
SEED = 0
TEMPERATURES = (0.3, 0.8, 1.5)
TOP_KS = (1, 5, None)
TABLE_TEMPS = (0.5, 1.5)
TABLE_SIZE = 8
MODEL_FILE = "tiny_gpt.npz"


def load_model():
    """Returns (model, tokenizer), or None when no model is saved."""
    path = slmlab.work_path(MODEL_FILE)
    if not os.path.exists(path):
        return None
    return TinyGPT.load(path)


def print_table(model, tok, ids, temp):
    """The most likely next characters after the prompt."""
    logits, _ = model.forward(ids)
    probs = softmax(logits[0, -1].astype(np.float64) / temp)
    print(f"next char, temp {temp}:")
    for i in np.argsort(probs)[::-1][:TABLE_SIZE]:
        print(f"  {repr(tok.chars[i]):>5} {probs[i]:.3f}")


def sample(model, tok, ids, temp, top_k):
    rng = np.random.default_rng(SEED)
    out = model.generate(ids, SAMPLE_CHARS, temperature=temp,
                         top_k=top_k, rng=rng)
    return tok.decode(out[0])


def main():
    saved = load_model()
    if saved is None:
        print("No saved model yet.")
        print("Run lab04_train_gpt.py first to create it.")
        return
    model, tok = saved
    ids = tok.encode(PROMPT)
    if len(ids) == 0:
        print("The prompt has no known characters.")
        return
    ids = ids[None, :]
    print("prompt:", repr(PROMPT))
    for temp in TEMPERATURES:
        print(f"temp {temp}:")
        print(sample(model, tok, ids, temp, None))
    for top_k in TOP_KS:
        label = "none" if top_k is None else top_k
        print(f"top-k {label}:")
        print(sample(model, tok, ids, 0.8, top_k))
    for temp in TABLE_TEMPS:
        print_table(model, tok, ids, temp)


main()
