"""Lab 6: retrieval without a neural network.

Shows the retrieval step of retrieval augmented generation (RAG). Each
note becomes a vector of TF-IDF word weights, and so does the question.
Notes that point in the same direction as the question score highest.
The best notes are then pasted into the prompt the model will read.

TRY: change QUESTION to one about LoRA or quantization.
TRY: set TOP_K to 1 or 5. What does the prompt gain or lose?
TRY: remove a word from STOP_WORDS and watch the ranking move.
"""
import re
import textwrap

import numpy as np

import slmlab

SEED = 0      # nothing here is random; kept for the shared rule
TOP_K = 3     # how many notes to paste into the prompt
QUESTION = "How does retrieval help a model use facts from documents?"
STOP_WORDS = {"how", "does", "a", "the", "of", "to", "from", "is",
              "and", "it", "in", "an", "on", "with", "that"}


def load_notes():
    """One note per non-empty line of ai_notes.txt."""
    with open(slmlab.data_path("ai_notes.txt"), encoding="utf-8") as f:
        lines = f.read().splitlines()
    return [line.strip() for line in lines if line.strip()]


def tokenize(text):
    """Lower-case words, with stop words removed."""
    # Only runs of letters count as words; digits and punctuation drop.
    words = re.findall(r"[a-z]+", text.lower())
    return [w for w in words if w not in STOP_WORDS]


def build_vocab(token_lists):
    """Give every distinct word a column number, in sorted order."""
    words = set()
    for tokens in token_lists:
        words.update(tokens)
    return {w: i for i, w in enumerate(sorted(words))}


def count_matrix(token_lists, vocab):
    """Row = text, column = word, cell = how often it occurs."""
    m = np.zeros((len(token_lists), len(vocab)))
    for row, tokens in enumerate(token_lists):
        for w in tokens:
            if w in vocab:                     # unknown words ignored
                m[row, vocab[w]] += 1.0
    return m


def idf_weights(counts):
    """Inverse document frequency: rare words weigh more.

    df is the number of notes that contain the word. A word in every
    note gets the lowest weight; a word in one note gets a high one.
    """
    n = counts.shape[0]
    df = (counts > 0).sum(axis=0)
    return np.log((1.0 + n) / (1.0 + df)) + 1.0


def unit_rows(m):
    """Scale each row to length 1, so only its direction matters."""
    norms = np.sqrt((m * m).sum(axis=-1, keepdims=True))
    return m / np.maximum(norms, 1e-12)


def show_top(notes, scores, order):
    """Print the best TOP_K notes with their cosine scores."""
    for rank, idx in enumerate(order[:TOP_K], start=1):
        print("%d. score %.3f" % (rank, scores[idx]))
        body = textwrap.fill(notes[idx], width=60)
        print(textwrap.indent(body, "   "))


def build_prompt(question, notes, order):
    """The text a language model would actually receive."""
    lines = ["Answer the question using the context.", "", "Context:"]
    for idx in order[:TOP_K]:
        lines.append("- " + notes[idx])
    lines += ["", "Question: " + question, "Answer:"]
    return "\n".join(lines)


def main():
    notes = load_notes()
    token_lists = [tokenize(n) for n in notes]
    vocab = build_vocab(token_lists)
    counts = count_matrix(token_lists, vocab)
    idf = idf_weights(counts)                  # one weight per word
    docs = unit_rows(counts * idf)             # TF-IDF per note
    q_counts = count_matrix([tokenize(QUESTION)], vocab)
    q_vec = unit_rows(q_counts * idf)[0]       # TF-IDF of question
    # Both vectors have length 1, so the dot product is the cosine.
    scores = docs @ q_vec
    order = np.argsort(-scores, kind="stable") # best first
    print("notes:", len(notes), " words:", len(vocab))
    print("question:", QUESTION)
    print()
    show_top(notes, scores, order)
    print("--- prompt ---")
    print(build_prompt(QUESTION, notes, order))


main()
