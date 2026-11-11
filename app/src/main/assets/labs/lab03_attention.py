"""Causal self-attention: one head, forward pass only.

Random embeddings for a short sentence go through Q, K and V
projections, a causal mask and a softmax. The heat map shows the
weights: each row is a query token and each column a token it looks
at. Darker characters mean a larger weight. Future tokens stay blank.

TRY:
  SENTENCE = "a b a c" (a repeated word gets the same embedding)
  D_HEAD = 2 (a smaller head changes the weights)
  SEED = 7 (other random projections)
"""
import math

import numpy as np

SENTENCE = "the cat sat on the mat"
SEED = 0
D_EMB = 16
D_HEAD = 8
SHADES = " .:-=+*#%@"


def make_embeddings(words, rng):
    """One random vector per distinct word. Returns (T, D_EMB)."""
    distinct = sorted(set(words))
    table = rng.normal(0.0, 1.0, size=(len(distinct), D_EMB))
    rows = [distinct.index(w) for w in words]
    return table[rows]


def softmax(scores):
    """Softmax over the last axis. Masked (-inf) entries become 0."""
    e = np.exp(scores - scores.max(axis=-1, keepdims=True))
    return e / e.sum(axis=-1, keepdims=True)


def causal_attention(x, rng):
    """x: (T, D_EMB). Returns weights (T, T) and outputs (T, D_HEAD)."""
    w_q, w_k, w_v = [rng.normal(0.0, 0.5, size=(D_EMB, D_HEAD))
                     for _ in range(3)]
    q = x @ w_q                                     # (T, D_HEAD)
    k = x @ w_k                                     # (T, D_HEAD)
    v = x @ w_v                                     # (T, D_HEAD)
    scores = q @ k.T / math.sqrt(D_HEAD)            # (T, T)
    t = len(x)
    visible = np.tril(np.ones((t, t), dtype=bool))  # key <= query
    scores = np.where(visible, scores, -np.inf)
    weights = softmax(scores)                       # (T, T)
    return weights, weights @ v                     # (T, D_HEAD)


def shade(p):
    """One character for a weight between 0 and 1."""
    level = int(round(p * (len(SHADES) - 1)))
    return SHADES[level]


def print_heat_map(words, weights):
    width = max(len(w) for w in words)
    heads = "".join(w[0] for w in words)
    print(" " * width + " |" + heads + "|")
    for i, word in enumerate(words):
        row = "".join(shade(p) for p in weights[i])
        print(f"{word:>{width}} |{row}|")


def print_checks(weights):
    sums_ok = np.allclose(weights.sum(axis=1), 1.0)
    future = np.triu(weights, k=1).sum()
    print("row sums ok:", bool(sums_ok))
    print("future weight:", float(future))


def main():
    rng = np.random.default_rng(SEED)
    words = SENTENCE.split()
    x = make_embeddings(words, rng)
    weights, out = causal_attention(x, rng)
    print("tokens:", len(words), "d_head:", D_HEAD)
    print_heat_map(words, weights)
    print_checks(weights)
    print("output shape:", out.shape)


main()
