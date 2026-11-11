"""Shared test setup: makes slm_runner, slmlab and tinygpt importable on a desktop."""
import os
import sys

import pytest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
PYLIB = os.path.join(ASSETS, "pylib")
DATA = os.path.join(ASSETS, "data")
LABS = os.path.join(ASSETS, "labs")
RUNNER_DIR = os.path.join(ROOT, "app", "src", "main", "python")

for p in (PYLIB, RUNNER_DIR):
    if p not in sys.path:
        sys.path.insert(0, p)

os.environ["SLMLAB_DATA_DIR"] = DATA


@pytest.fixture()
def work_dir(tmp_path, monkeypatch):
    """A fresh writable work directory, wired into slmlab."""
    import slmlab

    d = str(tmp_path / "work")
    os.makedirs(d, exist_ok=True)
    monkeypatch.setenv("SLMLAB_WORK_DIR", d)
    slmlab._configure(DATA, d)
    yield d
    slmlab._configure(None, None)
    slmlab._set_hooks(None, None)


@pytest.fixture()
def lab_path():
    """Returns a function mapping a lab file name to its absolute path."""
    return lambda name: os.path.join(LABS, name)
