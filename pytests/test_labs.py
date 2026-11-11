"""Tests for the four labs and data file owned by PYTHON-LABS.

Each lab runs as __main__ through runpy, and its printed output is
checked for the behaviour the lab teaches.
"""
import os
import re
import runpy

import pytest

import slmlab

LAB_FILES = [
    "lab01_bigram.py",
    "lab02_tokenizer.py",
    "lab06_rag.py",
    "lab07_lora.py",
]


def run_lab(name, lab_path, capsys):
    """Run one lab as a script and return everything it printed."""
    runpy.run_path(lab_path(name), run_name="__main__")
    return capsys.readouterr().out


def test_lab01_bigram_beats_uniform(work_dir, lab_path, capsys):
    out = run_lab("lab01_bigram.py", lab_path, capsys)
    bigram = float(re.search(r"bigram loss: ([0-9.]+)", out).group(1))
    uniform = float(re.search(r"uniform loss: ([0-9.]+)", out).group(1))
    assert bigram < uniform
    sample = out.split("--- sample ---\n")[1]
    assert len(sample) == 200 + 1          # 200 characters, then newline


def test_lab02_round_trip_and_ratio(work_dir, lab_path, capsys):
    out = run_lab("lab02_tokenizer.py", lab_path, capsys)
    assert "round trip: ok" in out
    ratio = float(re.search(r"ratio: ([0-9.]+)", out).group(1))
    assert ratio > 1.5
    assert len(re.findall(r"^\s*\d+\. ", out, re.M)) == 15


def test_lab06_top_result_is_relevant(work_dir, lab_path, capsys):
    out = run_lab("lab06_rag.py", lab_path, capsys)
    scores = [float(s) for s in
              re.findall(r"^\d+\. score ([0-9.]+)$", out, re.M)]
    assert len(scores) == 3
    assert scores == sorted(scores, reverse=True)
    top_block = out.split("1. score")[1].split("2. score")[0]
    assert "retriev" in top_block.lower()
    assert "--- prompt ---" in out
    assert "Context:" in out and "Question: How does retrieval" in out


def test_lab07_lora_loss_falls_tenfold(work_dir, lab_path, capsys):
    collected = []
    slmlab._set_hooks(lambda n, s, v: collected.append((n, s, v)),
                      lambda: False)
    try:
        out = run_lab("lab07_lora.py", lab_path, capsys)
    finally:
        slmlab._set_hooks(None, None)
    start = float(re.search(r"start loss: ([0-9.]+)", out).group(1))
    final = float(re.search(r"final loss: ([0-9.]+)", out).group(1))
    assert start / final >= 10
    assert "full fine-tune params: 4096" in out
    assert "LoRA params: 512" in out
    names = {n for n, _, _ in collected}
    assert names == {"lora"}
    steps = [s for _, s, _ in collected]
    assert steps[0] == 0 and len(steps) > 10
    assert steps == sorted(steps)


@pytest.mark.parametrize("name", LAB_FILES)
def test_lab_file_is_phone_sized(name, lab_path):
    with open(lab_path(name), encoding="utf-8") as f:
        text = f.read()
    lines = text.splitlines()
    assert 60 <= len(lines) <= 120
    assert max(len(line) for line in lines) <= 72
    assert lines[0].startswith('"""')
    assert 2 <= text.count("TRY:") <= 3
    assert "tinygpt" not in text
    assert "\nmain()\n" in text


def test_ai_notes_are_short_and_plain():
    path = os.path.join(slmlab._resolve_data_dir(), "ai_notes.txt")
    with open(path, encoding="utf-8") as f:
        notes = [line for line in f.read().splitlines() if line.strip()]
    assert 40 <= len(notes) <= 50
    assert not any(ch.isdigit() for ch in "\n".join(notes))
