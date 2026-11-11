"""Lab 2: byte pair encoding (BPE) from scratch.

Shows how a tokenizer learns its vocabulary. It starts with single
characters, then repeatedly merges the most common adjacent pair into
one new token. Common words become single tokens, so the same text
turns into fewer tokens for the model to read.

TRY: set NUM_MERGES to 50, then to 400. How do ratio and speed change?
TRY: put a rare letter or an emoji into SAMPLE. It stays a single
     character token, because the merges never learned it.
TRY: set TRAIN_CHARS to 2000. Do the first merges change?
"""
import slmlab

SEED = 0               # BPE has no randomness; kept for the shared rule
TRAIN_CHARS = 20000    # characters of the corpus used to learn merges
NUM_MERGES = 200       # how many merges to learn
SHOW_MERGES = 15       # how many merges to print
SAMPLE = "To be, or not to be: that is the question."


def add_pair(counts, pair):
    """Count one occurrence of an adjacent token pair."""
    counts[pair] = counts.get(pair, 0) + 1


def count_pairs(tokens):
    """Count every adjacent pair in the token list."""
    counts = {}
    for i in range(len(tokens) - 1):
        add_pair(counts, (tokens[i], tokens[i + 1]))
    return counts


def pair_at(tokens, i, pair):
    """True if the two tokens starting at position i equal pair."""
    return i + 1 < len(tokens) and (tokens[i], tokens[i + 1]) == pair


def apply_merge(tokens, pair):
    """Replace every adjacent `pair` with one joined token."""
    out = []
    i = 0
    while i < len(tokens):
        if pair_at(tokens, i, pair):
            out.append(pair[0] + pair[1])      # the new merged token
            i += 2                             # skip both old tokens
        else:
            out.append(tokens[i])
            i += 1
    return out


def train_bpe(text, num_merges):
    """Learn merges. Each step merges the most frequent pair."""
    tokens = list(text)                        # start: one per char
    merges = []
    for _ in range(num_merges):
        counts = count_pairs(tokens)
        if not counts:
            break
        best = max(counts, key=counts.get)     # most frequent pair
        merges.append(best)
        tokens = apply_merge(tokens, best)
    return merges


def encode(text, merges):
    """Apply the learned merges in the order they were learned."""
    tokens = list(text)
    for pair in merges:
        tokens = apply_merge(tokens, pair)
    return tokens


def decode(tokens):
    """Joining the tokens gives back the original text exactly."""
    return "".join(tokens)


def main():
    text = slmlab.corpus()[:TRAIN_CHARS]
    print("training chars:", len(text))
    merges = train_bpe(text, NUM_MERGES)
    print("merges learned:", len(merges))
    print("first merges:")
    for rank, (a, b) in enumerate(merges[:SHOW_MERGES], start=1):
        print("%2d. %r + %r" % (rank, a, b))

    tokens = encode(SAMPLE, merges)
    back = decode(tokens)
    assert back == SAMPLE, "round trip failed"
    print("round trip: ok")
    print("chars before:", len(SAMPLE))
    print("tokens after:", len(tokens))
    print("ratio: %.2f" % (len(SAMPLE) / len(tokens)))
    print(" | ".join(tokens))


main()
