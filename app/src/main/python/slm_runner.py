"""Runs lab code for the app (Chaquopy) and on a desktop.

Kotlin calls configure() once, then run(code, listener) on a background
thread, and stop() from the UI thread. The listener receives output,
metrics and exactly one final status. See DESIGN.md section 4.2.
"""
import ctypes
import linecache
import os
import sys
import threading
import traceback

_lock = threading.Lock()
_running = False
_finishing = False
_thread_ident = None
_stop_requested = False

_MAX_BUFFER = 256
_RUNNER_FILE = "slm_runner.py"


def configure(pylib_dir, data_dir, work_dir):
    """Make slmlab importable and tell it where data and work files live."""
    if pylib_dir not in sys.path:
        sys.path.insert(0, pylib_dir)
    import slmlab
    slmlab._configure(data_dir, work_dir)


def is_running():
    with _lock:
        return _running


def stop():
    """Ask the running code to stop. Safe to call from any thread."""
    global _stop_requested
    with _lock:
        if not _running or _finishing:
            return
        _set_async(_thread_ident, KeyboardInterrupt)
        _stop_requested = True


def run(code, listener):
    """Blocking. Runs `code` and ends with exactly one onFinished call."""
    global _running, _finishing, _thread_ident, _stop_requested
    with _lock:
        busy = _running
        if not busy:
            _running = True
            _finishing = False
            _stop_requested = False
            _thread_ident = threading.get_ident()
    if busy:
        listener.onFinished(False, "Already running")
        return
    try:
        ok, error = _execute(code, listener)
    except KeyboardInterrupt:
        ok, error = (True, "") if _stop_requested else (False, "KeyboardInterrupt")
    except BaseException as exc:
        ok, error = False, _describe(exc)
    with _lock:
        _running = False
        _finishing = False
        _thread_ident = None
    listener.onFinished(ok, error)


class _OutputWriter:
    """Stands in for sys.stdout and sys.stderr during a run."""

    encoding = "utf-8"

    def __init__(self, listener):
        self._listener = listener
        self._parts = []
        self._size = 0

    def write(self, text):
        text = str(text)
        if text:
            self._parts.append(text)
            self._size += len(text)
            if "\n" in text or self._size > _MAX_BUFFER:
                self.flush()
        return len(text)

    def flush(self):
        if self._parts:
            text = "".join(self._parts)
            self._parts = []
            self._size = 0
            self._listener.onOutput(text)

    def isatty(self):
        return False


def _execute(code, listener):
    """Runs the code with output, hooks and tracing set up. Returns (ok, error)."""
    import slmlab

    sys.modules.pop("tinygpt", None)  # pick up edits to the library
    writer = _OutputWriter(listener)
    old_out, old_err = sys.stdout, sys.stderr
    sys.stdout = writer
    sys.stderr = writer
    slmlab._set_hooks(lambda n, s, v: listener.onMetric(n, s, v), _stop_check)
    ok, error = True, ""
    try:
        compiled = compile(code, "<lab>", "exec")
        linecache.cache["<lab>"] = (len(code), None,
                                    code.splitlines(True), "<lab>")
        sys.settrace(_trace)
        exec(compiled, {"__name__": "__main__"})
    except KeyboardInterrupt:
        sys.settrace(None)  # the tracer must not fire inside this handler
        writer.write("\n[stopped]\n")
    except SystemExit:
        sys.settrace(None)
    except BaseException as exc:
        sys.settrace(None)
        writer.write(_format_error(exc))
        ok, error = False, _describe(exc)
    finally:
        sys.settrace(None)
        _mark_finishing()
        slmlab._set_hooks(None, None)
        writer.flush()
        sys.stdout, sys.stderr = old_out, old_err
    return ok, error


def _mark_finishing():
    """No more stop requests after this; clear any pending async stop."""
    global _finishing
    with _lock:
        _finishing = True
        ident = _thread_ident
    _set_async(ident, None)


def _trace(frame, event, arg):
    """Global trace: only 'call' events. Raises once stop was requested."""
    if event == "call" and _stop_requested:
        _set_async(threading.get_ident(), None)  # avoid a second raise
        raise KeyboardInterrupt
    return None


def _stop_check():
    return _stop_requested


def _set_async(ident, exc):
    """Raise exc (a class) in thread `ident`; with exc None, clear it.

    Best effort: ctypes may be unavailable (for example on Android).
    """
    if ident is None:
        return
    try:
        set_exc = ctypes.pythonapi.PyThreadState_SetAsyncExc
        arg = ctypes.py_object(exc) if exc is not None else None
        count = set_exc(ctypes.c_ulong(ident), arg)
        if count > 1:
            set_exc(ctypes.c_ulong(ident), None)
    except Exception:
        pass


def _is_runner_frame(frame):
    return os.path.basename(frame.filename) == _RUNNER_FILE


def _format_error(exc):
    """Traceback text without the runner's own frames."""
    frames = [f for f in traceback.extract_tb(exc.__traceback__)
              if not _is_runner_frame(f)]
    lines = ["Traceback (most recent call last):\n"]
    lines += traceback.format_list(frames)
    lines += traceback.format_exception_only(type(exc), exc)
    return "".join(lines)


def _describe(exc):
    msg = str(exc)
    name = type(exc).__name__
    return f"{name}: {msg}" if msg else name
