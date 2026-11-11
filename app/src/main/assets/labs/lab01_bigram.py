"""Lab 1: a character bigram model.

Shows what a language model does at its simplest. It counts which
character tends to follow which, turns the counts into probabilities,
measures how surprised it is by real text, then samples new text.

TRY: set SAMPLE_LEN to 400 and read the new sample.
TRY: set SMOOTH to 0.01. Does the loss change much?
TRY: change SEED. Only the sample should change, not the loss.
"""
import math

import numpy as np

import slmlab

SEED = 0           # same seed gives the same numbers every run
SAMPLE_LEN = 200   # characters to generate at the end
SMOOTH = 1.0       # add-one smoothing: every pair gets 1 extra count


def count_pair(counts, a, b):
    """Record one occurrence of character id a followed by b."""
    counts[a, b] += 1.0


def count_bigrams(ids, vocab):
    """Build the table counts[a, b] = how often b follows a."""
    # float64 keeps the row sums exact enough for rng.choice later.
    counts = np.zeros((vocab, vocab))
    for i in range(len(ids) - 1):
        count_pair(counts, ids[i], ids[i + 1])   # one call per pair
    return counts


def to_probs(counts):
    """Turn counts into P(next | current). Each row sums to 1.

    Smoothing adds SMOOTH to every cell first, so a pair that never
    appears in the text still gets a small, non-zero probability.
    """
    smoothed = counts + SMOOTH
    return smoothed / smoothed.sum(axis=1, keepdims=True)


def surprise(probs, a, b):
    """Surprise of seeing b after a, in nats. Lower is better."""
    return -math.log(probs[a, b])


def avg_nll(probs, ids):
    """Average surprise per character. This is the model's loss."""
    total = 0.0
    for i in range(len(ids) - 1):
        total += surprise(probs, ids[i], ids[i + 1])
    return total / (len(ids) - 1)


def next_char(probs, current, rng):
    """Draw the next character id from row `current` of the table."""
    return int(rng.choice(probs.shape[0], p=probs[current]))


def sample(probs, chars, start, rng):
    """Generate SAMPLE_LEN characters, one draw at a time."""
    out = [start]
    for _ in range(SAMPLE_LEN - 1):
        out.append(next_char(probs, out[-1], rng))
    return "".join(chars[i] for i in out)


def main():
    rng = np.random.default_rng(SEED)
    text = slmlab.corpus()
    chars = sorted(set(text))                  # the vocabulary
    index = {ch: i for i, ch in enumerate(chars)}
    ids = [index[ch] for ch in text]           # text as integers
    vocab = len(chars)
    print("characters:", len(text))
    print("vocabulary:", vocab)

    counts = count_bigrams(ids, vocab)
    probs = to_probs(counts)
    loss = avg_nll(probs, ids)
    uniform = math.log(vocab)                  # guess any char equally
    print("bigram loss: %.3f nats" % loss)
    print("uniform loss: %.3f nats" % uniform)
    print("bigram beats uniform:", loss < uniform)
    print("--- sample ---")
    print(sample(probs, chars, ids[0], rng))


main()
