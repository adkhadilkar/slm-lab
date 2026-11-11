"""Runs the CORE labs (03, 04, 05, 08 and the playground) end to end."""
import os
import re
import runpy

PLAYGROUND_INIT = {
    "PROMPT": "ROMEO:",
    "TEMPERATURE": 0.8,
    "TOP_K": 20,
    "MAX_TOKENS": 30,
    "SEED": 1,
}


def train_small(work_dir, lab_path, steps=120, eval_every=60):
    """lab04 with STEPS and EVAL_EVERY replaced, so the test is fast."""
    path = lab_path("lab04_train_gpt.py")
    with open(path, encoding="utf-8") as f:
        src = f.read()
    src, n1 = re.subn(r"^STEPS = .*$", f"STEPS = {steps}", src, flags=re.M)
    src, n2 = re.subn(r"^EVAL_EVERY = .*$", f"EVAL_EVERY = {eval_every}",
                      src, flags=re.M)
    assert n1 == 1 and n2 == 1
    exec(compile(src, path, "exec"), {"__name__": "__main__"})


def test_lab03_heat_map_and_checks(work_dir, lab_path, capsys):
    runpy.run_path(lab_path("lab03_attention.py"))
    out = capsys.readouterr().out
    assert "row sums ok: True" in out
    assert "future weight: 0.0" in out
    assert "output shape: (6, 8)" in out
    assert "cat" in out and "mat" in out


def test_lab04_small_run_saves_model(work_dir, lab_path, capsys):
    train_small(work_dir, lab_path)
    out = capsys.readouterr().out
    assert os.path.exists(os.path.join(work_dir, "tiny_gpt.npz"))
    assert "step    60" in out
    assert "step   120" in out
    assert "model saved" in out


def test_labs_05_08_and_playground_after_training(work_dir, lab_path,
                                                  capsys):
    train_small(work_dir, lab_path)
    capsys.readouterr()

    runpy.run_path(lab_path("lab05_sampling.py"))
    out = capsys.readouterr().out
    for header in ("temp 0.3:", "temp 0.8:", "temp 1.5:",
                   "top-k 1:", "top-k 5:", "top-k none:"):
        assert header in out
    assert "next char, temp 0.5:" in out
    assert "No saved model" not in out

    runpy.run_path(lab_path("lab08_quantize.py"))
    out = capsys.readouterr().out
    lines = {line.split()[0]: line.split() for line in out.splitlines()
             if line.strip()}
    for name in ("float32", "int8", "int4"):
        assert name in lines, out
        assert float(lines[name][1]) > 0
        assert float(lines[name][2]) > 0
    assert float(lines["int4"][1]) < float(lines["float32"][1])

    runpy.run_path(lab_path("playground.py"), init_globals=PLAYGROUND_INIT)
    out = capsys.readouterr().out
    assert out.startswith("ROMEO:")
    assert out.endswith("\n")
    assert len(out) == len("ROMEO:") + 30 + 1


def test_lab05_without_model_prints_hint(work_dir, lab_path, capsys):
    runpy.run_path(lab_path("lab05_sampling.py"))
    out = capsys.readouterr().out
    assert "No saved model" in out
    assert "lab04" in out


def test_lab08_without_model_reports_reconstruction_error(
        work_dir, lab_path, capsys):
    runpy.run_path(lab_path("lab08_quantize.py"))
    out = capsys.readouterr().out
    assert "reconstruction" in out.lower()
    assert "int8" in out and "int4" in out


def test_playground_without_model_prints_exactly_no_model(
        work_dir, lab_path, capsys):
    runpy.run_path(lab_path("playground.py"), init_globals=PLAYGROUND_INIT)
    assert capsys.readouterr().out == "NO_MODEL\n"


def test_playground_defaults_without_globals(work_dir, lab_path, capsys):
    runpy.run_path(lab_path("playground.py"))
    assert capsys.readouterr().out == "NO_MODEL\n"


def test_lab_source_rules(lab_path):
    names = ["lab03_attention.py", "lab04_train_gpt.py",
             "lab05_sampling.py", "lab08_quantize.py", "playground.py"]
    for name in names:
        with open(lab_path(name), encoding="utf-8") as f:
            lines = f.read().splitlines()
        assert max(len(line) for line in lines) <= 72, name
        assert lines[0].startswith('"""'), name
        assert "TRY:" in "\n".join(lines[:20]), name
        assert "\nmain()\n" in "\n".join(lines) or lines[-1] == "main()", name
        if name != "playground.py":
            assert 60 <= len(lines) <= (90 if "lab04" in name else 120), name
