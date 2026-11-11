"""Tests for slm_runner.py, driven by a fake listener."""
import os
import sys
import threading
import time

import pytest

import slm_runner

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
PYLIB = os.path.join(ASSETS, "pylib")
DATA = os.path.join(ASSETS, "data")

LOOP_CODE = """
def tick(i):
    return i

i = 0
while True:
    tick(i)
    i += 1
"""


class FakeListener:
    def __init__(self):
        self.output = []
        self.metrics = []
        self.finished = []

    def onOutput(self, text):
        self.output.append(text)

    def onMetric(self, name, step, value):
        self.metrics.append((name, step, value))

    def onFinished(self, ok, error):
        self.finished.append((ok, error))

    def text(self):
        return "".join(self.output)


@pytest.fixture(autouse=True)
def configured(tmp_path, monkeypatch):
    monkeypatch.setenv("SLMLAB_WORK_DIR", str(tmp_path / "work"))
    slm_runner.configure(PYLIB, DATA, str(tmp_path / "work"))
    yield


def start_in_thread(code, listener):
    thread = threading.Thread(target=slm_runner.run, args=(code, listener))
    thread.start()
    return thread


def wait_until_running(timeout=5.0):
    end = time.time() + timeout
    while time.time() < end:
        if slm_runner.is_running():
            return True
        time.sleep(0.01)
    return False


def test_normal_run_prints_and_finishes_ok():
    lst = FakeListener()
    slm_runner.run("print('hello lab')\nx = 1 + 1\n", lst)
    assert "hello lab\n" in lst.text()
    assert lst.finished == [(True, "")]
    assert not slm_runner.is_running()


def test_exception_reports_error_and_clean_traceback():
    lst = FakeListener()
    code = "def go():\n    raise ValueError('boom')\n\ngo()\n"
    slm_runner.run(code, lst)
    assert lst.finished == [(False, "ValueError: boom")]
    text = lst.text()
    assert "Traceback" in text
    assert "<lab>" in text
    assert "ValueError: boom" in text
    assert "slm_runner.py" not in text


def test_syntax_error_is_reported_as_failure():
    lst = FakeListener()
    slm_runner.run("def broken(:\n", lst)
    assert len(lst.finished) == 1
    ok, error = lst.finished[0]
    assert ok is False
    assert error.startswith("SyntaxError")


def test_metric_is_forwarded_with_int_step_and_float_value():
    lst = FakeListener()
    code = "import slmlab\nslmlab.metric('train', 3, 1.5)\n"
    slm_runner.run(code, lst)
    assert lst.finished == [(True, "")]
    assert lst.metrics == [("train", 3, 1.5)]
    name, step, value = lst.metrics[0]
    assert type(step) is int
    assert type(value) is float


def test_stop_interrupts_loop_within_three_seconds():
    lst = FakeListener()
    thread = start_in_thread(LOOP_CODE, lst)
    assert wait_until_running()
    time.sleep(0.2)
    began = time.time()
    slm_runner.stop()
    thread.join(timeout=3.0)
    elapsed = time.time() - began
    assert not thread.is_alive()
    assert elapsed < 3.0
    assert "[stopped]" in lst.text()
    assert lst.finished == [(True, "")]


def test_stdout_is_restored_after_normal_and_error_runs():
    before_out, before_err = sys.stdout, sys.stderr
    slm_runner.run("print('a')\n", FakeListener())
    assert sys.stdout is before_out and sys.stderr is before_err
    slm_runner.run("raise RuntimeError('x')\n", FakeListener())
    assert sys.stdout is before_out and sys.stderr is before_err


def test_exactly_one_finish_per_run():
    cases = ["print('ok')\n", "raise KeyError('k')\n", "import sys\nsys.exit(0)\n"]
    for code in cases:
        lst = FakeListener()
        slm_runner.run(code, lst)
        assert len(lst.finished) == 1, code
    lst = FakeListener()
    thread = start_in_thread(LOOP_CODE, lst)
    assert wait_until_running()
    slm_runner.stop()
    thread.join(timeout=3.0)
    assert len(lst.finished) == 1


def test_system_exit_counts_as_normal_end():
    lst = FakeListener()
    slm_runner.run("import sys\nsys.exit(3)\n", lst)
    assert lst.finished == [(True, "")]


def test_second_run_while_active_reports_already_running():
    first = FakeListener()
    thread = start_in_thread(LOOP_CODE, first)
    assert wait_until_running()
    second = FakeListener()
    slm_runner.run("print('nope')\n", second)
    assert second.finished == [(False, "Already running")]
    assert second.output == []
    slm_runner.stop()
    thread.join(timeout=3.0)
    assert not thread.is_alive()
    assert first.finished == [(True, "")]
