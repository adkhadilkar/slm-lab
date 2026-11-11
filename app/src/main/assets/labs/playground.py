"""Generate text from the trained tiny GPT.

This is the Playground screen, not a lesson lab. The app puts values
for PROMPT, TEMPERATURE, TOP_K, MAX_TOKENS and SEED before this code.
When they are missing, the defaults below are used. The text streams
in small pieces. With no saved model, it prints NO_MODEL.

TRY:
  TEMPERATURE = 0.5 (more predictable text)
  TOP_K = None (sample from every character)
  MAX_TOKENS = 400 (longer text)
"""
import os

import numpy as np

import slmlab
from tinygpt import TinyGPT

MODEL_FILE = "tiny_gpt.npz"
PROMPT = globals().get("PROMPT", "ROMEO:")
TEMPERATURE = globals().get("TEMPERATURE", 0.8)
TOP_K = globals().get("TOP_K", 20)
MAX_TOKENS = globals().get("MAX_TOKENS", 200)
SEED = globals().get("SEED", 0)
FLUSH_EVERY = 8


def main():
    path = slmlab.work_path(MODEL_FILE)
    if not os.path.exists(path):
        print("NO_MODEL")
        return
    model, tok = TinyGPT.load(path)
    rng = np.random.default_rng(SEED)
    ids = tok.encode(PROMPT)
    if len(ids) == 0:
        ids = np.zeros(1, dtype=np.int64)
    idx = ids[None, :]
    pending = PROMPT
    for _ in range(MAX_TOKENS):
        idx = model.generate(idx, 1, temperature=TEMPERATURE,
                             top_k=TOP_K, rng=rng)
        pending += tok.decode(idx[0, -1:])
        if len(pending) >= FLUSH_EVERY:
            print(pending, end="", flush=True)
            pending = ""
    print(pending, end="", flush=True)
    print()


main()
