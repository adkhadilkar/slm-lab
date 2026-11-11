"""Small helper API available to every lab.

    import slmlab
    text = slmlab.corpus()                    # bundled training text
    slmlab.metric("train", step, loss)        # point on the live chart
    path = slmlab.work_path("tiny_gpt.npz")   # writable file location
    if slmlab.should_stop(): ...              # user pressed Stop

Works the same inside the app and on a desktop.
"""
import os

_data_dir = None
_work_dir = None
_metric_hook = None
_stop_hook = None


def _configure(data_dir, work_dir):
    """Called by the app (slm_runner.configure) and by tests."""
    global _data_dir, _work_dir
    _data_dir = data_dir
    _work_dir = work_dir


def _set_hooks(metric_fn, stop_fn):
    """Called by slm_runner around each run. Pass None, None to clear."""
    global _metric_hook, _stop_hook
    _metric_hook = metric_fn
    _stop_hook = stop_fn


def _resolve_data_dir():
    if _data_dir:
        return _data_dir
    env = os.environ.get("SLMLAB_DATA_DIR")
    if env:
        return env
    # Desktop fallback: assets/pylib/slmlab.py -> assets/data
    here = os.path.dirname(os.path.abspath(__file__))
    return os.path.join(os.path.dirname(here), "data")


def _resolve_work_dir():
    if _work_dir:
        return _work_dir
    env = os.environ.get("SLMLAB_WORK_DIR")
    if env:
        return env
    return os.path.join(os.path.expanduser("~"), ".slmlab_work")


def data_path(name):
    """Absolute path of a bundled data file."""
    return os.path.join(_resolve_data_dir(), name)


def work_path(name):
    """Absolute path of a file in the writable work directory."""
    d = _resolve_work_dir()
    os.makedirs(d, exist_ok=True)
    return os.path.join(d, name)


def corpus(name="tiny_corpus.txt"):
    """Return a bundled text file as a string."""
    with open(data_path(name), "r", encoding="utf-8") as f:
        return f.read()


def metric(name, step, value):
    """Send one point to the live chart. Does nothing on a desktop."""
    if _metric_hook is not None:
        _metric_hook(str(name), int(step), float(value))


def should_stop():
    """True once the user has pressed Stop."""
    if _stop_hook is not None:
        return bool(_stop_hook())
    return False
