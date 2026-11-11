# SLM Lab — Design and UX Specification

An Android app for learning how small language models work by building one.
The learner reads short lessons and runs real Python (NumPy) on the phone:
first a tiny GPT from scratch, then the techniques that make a pretrained small
model useful (retrieval, LoRA, quantization).

This document is the single source of truth. Where it gives a signature, file
path, colour or string, use it exactly. If something is not specified, choose
the simplest option consistent with the rest and note it in your report.

---

## 1. Product

### 1.1 Who it is for
An experienced software engineer who is new to ML internals, learning in short
sessions on a phone (10–20 minutes), sometimes offline.

### 1.2 What the learner can do
1. Follow a 6-week path of 14 lessons in 3 phases.
2. Read a lesson, tick off its checklist, mark it complete.
3. Open the lesson's lab: edit Python, run it on the phone, watch output and a
   live loss chart, stop it, reset the code.
4. Generate text from the tiny GPT they trained (Playground).
5. Look up terms (Glossary).
6. Change theme and editor font size, reset progress (Settings).

### 1.3 Two kinds of lesson
- `ON_DEVICE`: has a lab that runs inside the app.
- `GUIDED`: the work needs a GPU (Colab or laptop). The lesson gives steps and
  copyable code. No in-app lab.

### 1.4 Non-goals
No accounts, no network calls, no analytics, no PyTorch on the phone, no
foreground service (a run may be killed if the app is backgrounded for long),
no syntax-aware editor features beyond those in section 6.

---

## 2. Curriculum

`id` values are fixed. `lab` is the asset path under `assets/`.

| Phase | id | Title | Kind | Lab | Min |
|---|---|---|---|---|---|
| 1 Build a tiny GPT (weeks 1–2) | p1-01 | What a language model does | ON_DEVICE | labs/lab01_bigram.py | 15 |
| | p1-02 | Tokens and BPE | ON_DEVICE | labs/lab02_tokenizer.py | 15 |
| | p1-03 | Self-attention | ON_DEVICE | labs/lab03_attention.py | 20 |
| | p1-04 | The transformer block | ON_DEVICE | (none: reading only, `lab` = null) | 15 |
| | p1-05 | Training a tiny GPT | ON_DEVICE | labs/lab04_train_gpt.py | 20 |
| | p1-06 | Sampling: temperature and top-k | ON_DEVICE | labs/lab05_sampling.py | 10 |
| 2 Make a small model useful (weeks 3–4) | p2-01 | Choosing a pretrained small model | GUIDED | — | 15 |
| | p2-02 | Retrieval from first principles | ON_DEVICE | labs/lab06_rag.py | 15 |
| | p2-03 | A real RAG pipeline | GUIDED | — | 20 |
| | p2-04 | Building an evaluation set | GUIDED | — | 15 |
| 3 Adapt and ship (weeks 5–6) | p3-01 | LoRA: low-rank adapters | ON_DEVICE | labs/lab07_lora.py | 15 |
| | p3-02 | QLoRA fine-tuning | GUIDED | — | 20 |
| | p3-03 | Quantization | ON_DEVICE | labs/lab08_quantize.py | 15 |
| | p3-04 | Running a model on your phone | GUIDED | — | 15 |

Phase ids: `phase-1`, `phase-2`, `phase-3`.

---

## 3. Architecture

```
Compose UI (screens)  ->  ViewModels  ->  AppContainer { repo, progress, runner }
                                             |          |           |
                                   assets (json/md/py)  DataStore   Chaquopy -> slm_runner.py
                                                        + files          exec(lab code)
                                                                         imports slmlab, tinygpt, numpy
```

- Single activity, single Gradle module `app`, Kotlin, Jetpack Compose, Material 3.
- Python is embedded with Chaquopy. Only `slm_runner.py` is compiled into the
  app as a Python source. Everything the learner can read or edit ships as
  Android assets and is copied to `filesDir` at startup:
  - `assets/pylib/*.py` -> `filesDir/pylib/` (added to `sys.path`): `slmlab.py`, `tinygpt.py`
  - `assets/data/*` -> `filesDir/data/`: `tiny_corpus.txt`, `ai_notes.txt`
  - `assets/labs/*.py` are read as text by Kotlin and shown in the editor.
  - Writable work directory: `filesDir/work/` (model weights `tiny_gpt.npz`).
- Manual dependency injection through `AppContainer` (already written, see 3.3).
- JSON parsing uses `org.json` (built into Android). No serialization plugin.

### 3.1 Versions (use exactly; they are a known-compatible set)

| Thing | Version |
|---|---|
| Gradle wrapper | 8.14.3 (already generated; do not regenerate) |
| Android Gradle Plugin | 8.13.0 |
| Kotlin + `org.jetbrains.kotlin.plugin.compose` | 2.0.21 |
| Chaquopy plugin `com.chaquo.python` | 17.2.1 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |
| Java / Kotlin JVM target | 17 |
| Compose BOM `androidx.compose:compose-bom` | 2024.12.01 |
| `androidx.activity:activity-compose` | 1.9.3 |
| `androidx.core:core-ktx` | 1.15.0 |
| `androidx.lifecycle:lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` | 2.8.7 |
| `androidx.navigation:navigation-compose` | 2.8.5 |
| `androidx.datastore:datastore-preferences` | 1.1.1 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 |
| Compose artifacts via BOM | `ui`, `ui-tooling-preview`, `material3`, `material-icons-extended`, debug `ui-tooling` |

Chaquopy block in `app/build.gradle.kts`:

```kotlin
android { defaultConfig { ndk { abiFilters += listOf("arm64-v8a", "x86_64") } } }
chaquopy {
    defaultConfig {
        version = "3.12"
        pip { install("numpy") }
    }
}
```

Application id and namespace: `com.slmlab.app`. App label: `SLM Lab`.
`versionCode 1`, `versionName "1.0"`. Release minification off.

### 3.2 Project layout and file ownership

Each file has exactly one owner. Never edit a file you do not own. If you need
a change in someone else's file, say so in your final report.

```
slm-lab/
  DESIGN.md                                        LEAD
  gradlew, gradlew.bat, gradle/wrapper/*           LEAD (done)
  settings.gradle.kts, build.gradle.kts            ANDROID-CORE
  gradle.properties, gradle/libs.versions.toml     ANDROID-CORE
  .gitignore                                       ANDROID-CORE
  app/build.gradle.kts, app/proguard-rules.pro     ANDROID-CORE
  app/src/main/AndroidManifest.xml                 ANDROID-CORE
  app/src/main/res/**                              ANDROID-CORE
  app/src/main/java/com/slmlab/app/
    data/Models.kt                                 LEAD (done, read-only)
    data/Contracts.kt                              LEAD (done, read-only)
    SlmLabApp.kt, MainActivity.kt                  ANDROID-CORE
    data/AssetCurriculumRepository.kt              ANDROID-CORE
    data/DataStoreProgressStore.kt                 ANDROID-CORE
    python/ChaquopyRunner.kt                       ANDROID-CORE
    ui/theme/Color.kt, Type.kt, Theme.kt           ANDROID-CORE
    ui/nav/AppNav.kt                               ANDROID-CORE
    ui/components/*.kt                             ANDROID-CORE
    ui/roadmap/*, ui/lesson/*, ui/lab/*            ANDROID-SCREENS
    ui/playground/*, ui/glossary/*, ui/settings/*  ANDROID-SCREENS
  app/src/main/python/slm_runner.py                PYTHON-CORE
  app/src/main/assets/
    pylib/slmlab.py                                LEAD (done, read-only)
    pylib/tinygpt.py                               PYTHON-CORE
    data/tiny_corpus.txt                           PYTHON-CORE
    data/ai_notes.txt                              PYTHON-LABS
    labs/lab03_attention.py, lab04_train_gpt.py,
         lab05_sampling.py, lab08_quantize.py,
         playground.py                             PYTHON-CORE
    labs/lab01_bigram.py, lab02_tokenizer.py,
         lab06_rag.py, lab07_lora.py               PYTHON-LABS
    curriculum.json, glossary.json, lessons/*.md   CONTENT
  pytests/conftest.py                              LEAD (done, read-only)
  pytests/test_runner.py, test_tinygpt.py,
          test_core_labs.py                        PYTHON-CORE
  pytests/test_labs.py                             PYTHON-LABS
```

### 3.3 Kotlin contracts (already written — read them)

`data/Models.kt` and `data/Contracts.kt` define every shared type:
`Curriculum`, `Phase`, `LessonMeta`, `LessonKind`, `GlossaryTerm`, `MetricPoint`,
`RunEvent`, `ThemeMode`, `AppSettings`, `CurriculumRepository`, `ProgressStore`,
`PyRunner`, `PyListener`, `AppContainer`, `AppPaths`. Build against them as written.

ViewModels get dependencies from `AppContainer` and are created like this:

```kotlin
val vm: LabViewModel = viewModel(
    key = "lab-$lessonId",
    factory = viewModelFactory {
        initializer { LabViewModel(lessonId, AppContainer.repo, AppContainer.progress, AppContainer.runner) }
    }
)
```

### 3.4 Asset formats (CONTENT writes, ANDROID-CORE parses)

`assets/curriculum.json`

```json
{
  "phases": [
    {
      "id": "phase-1",
      "title": "Build a tiny GPT",
      "subtitle": "Understand the model by writing one",
      "weeks": "Weeks 1–2",
      "lessons": [
        {
          "id": "p1-01",
          "title": "What a language model does",
          "summary": "One sentence, at most 90 characters.",
          "minutes": 15,
          "kind": "ON_DEVICE",
          "content": "lessons/p1-01.md",
          "lab": "labs/lab01_bigram.py",
          "checklist": ["Three to five short, checkable statements."]
        }
      ]
    }
  ]
}
```

`lab` is a string or JSON `null`. `kind` is `ON_DEVICE` or `GUIDED`.

`assets/glossary.json`: `[{"term": "Attention", "definition": "One to three sentences."}]`,
sorted A–Z by term, 45–60 terms.

`assets/lessons/<id>.md` uses only the Markdown subset in 6.1.

### 3.5 Startup sequence (`SlmLabApp.onCreate`)
1. `if (!Python.isStarted()) Python.start(AndroidPlatform(this))`.
2. Build `AppPaths(filesDir)`; create `pylib`, `data`, `work`, `code` directories.
3. Copy every file in `assets/pylib` and `assets/data` to the matching directory,
   overwriting (they are small; this keeps them in sync with the APK).
4. Call Python `slm_runner.configure(pylibDir, dataDir, workDir)`.
5. Set `AppContainer.paths`, `repo`, `progress`, `runner`.

Steps 2–4 run synchronously in `onCreate` (total is a few hundred KB).

---

## 4. Python side

Target: Python 3.10+ and NumPy 1.23+. Use only `numpy` and the standard library.
Do not use APIs that exist only in NumPy 2. All arrays `float32` unless stated.
Every file must run unchanged on a desktop (`python3 file.py`) and in the app.

### 4.1 `slmlab` helper module (already written: `assets/pylib/slmlab.py`)

```python
slmlab.metric(name: str, step: int, value: float)  # point on the live chart
slmlab.corpus(name="tiny_corpus.txt") -> str        # read a bundled text file
slmlab.data_path(name) -> str                       # absolute path in data dir
slmlab.work_path(name) -> str                       # absolute path in writable dir
slmlab.should_stop() -> bool                        # True after the user pressed Stop
```

On a desktop it finds data via the `SLMLAB_DATA_DIR` / `SLMLAB_WORK_DIR`
environment variables (set by `pytests/conftest.py`) or the repo layout.

### 4.2 `slm_runner.py` (PYTHON-CORE)

```python
def configure(pylib_dir: str, data_dir: str, work_dir: str) -> None
    # insert pylib_dir at sys.path[0] if absent; import slmlab and call
    # slmlab._configure(data_dir, work_dir)

def run(code: str, listener) -> None
    # Blocking. Called on a background thread created by Kotlin.
    # Executes `code` in a fresh globals dict {"__name__": "__main__"}.

def stop() -> None
    # Called from another thread. Makes the running code raise KeyboardInterrupt soon.

def is_running() -> bool
```

`listener` is a Java object with exactly these methods (see `PyListener` in
Contracts.kt). Call them with plain Python `str`, `int`, `float`, `bool`:

```
listener.onOutput(text: str)
listener.onMetric(name: str, step: int, value: float)
listener.onFinished(ok: bool, error: str)     # error == "" when ok
```

Behaviour:
- While running, `sys.stdout` and `sys.stderr` are replaced by a writer that
  buffers and calls `onOutput` when the buffer contains a newline or exceeds
  256 characters, and on `flush()`. Restore the originals in `finally`.
- `slmlab._set_hooks(metric_fn, stop_fn)` is called before the run so
  `slmlab.metric` forwards to `listener.onMetric` and `slmlab.should_stop`
  reads the stop flag. Hooks are cleared in `finally`.
- Before each run, purge `tinygpt` from `sys.modules` so edits to library files
  would be picked up, and reset the stop flag.
- Stop uses two mechanisms together:
  1. A `sys.settrace` global trace function installed for the run, which
     handles only `"call"` events: if the stop flag is set it raises
     `KeyboardInterrupt`; it always returns `None` (no per-line tracing).
  2. `stop()` also tries `ctypes.pythonapi.PyThreadState_SetAsyncExc(
     ctypes.c_ulong(thread_ident), ctypes.py_object(KeyboardInterrupt))`
     inside `try/except Exception` (it may be unavailable on Android).
- Outcomes, always ending with exactly one `onFinished` call:
  - normal end -> `onFinished(True, "")`
  - `KeyboardInterrupt` after stop -> output `"\n[stopped]\n"`, `onFinished(True, "")`
  - `SystemExit` -> treated as normal end
  - any other exception -> print a traceback to the output that omits the
    runner's own frames (the user's code appears as file `<lab>`), then
    `onFinished(False, "<ExceptionType>: <message>")`
- Compile with `compile(code, "<lab>", "exec")` so line numbers match the editor.
- A second `run` while one is active calls `onFinished(False, "Already running")`
  on the new listener and returns.

### 4.3 `tinygpt.py` (PYTHON-CORE)

A decoder-only transformer in NumPy with hand-written backpropagation.
Readable first: short functions, a comment on every shape. About 300–400 lines.

```python
class Config:            # plain attributes with defaults
    vocab_size: int
    block_size: int = 32
    n_layer: int = 2
    n_head: int = 2
    d_model: int = 64
    dtype = np.float32

class CharTokenizer:
    def __init__(self, text_or_chars)     # sorted unique characters
    vocab_size: int
    chars: list[str]
    def encode(self, s: str) -> np.ndarray   # int64; unknown chars are skipped
    def decode(self, ids) -> str

class TinyGPT:
    def __init__(self, cfg: Config, seed: int = 0)
    params: dict[str, np.ndarray]
    def forward(self, idx, targets=None) -> tuple[np.ndarray, float | None]
        # idx: (B, T) ints, T <= block_size. logits: (B, T, vocab).
        # loss = mean cross-entropy over all B*T positions when targets given.
    def backward(self) -> dict[str, np.ndarray]
        # gradients of the last forward's loss, same keys and shapes as params
    def generate(self, idx, max_new_tokens, temperature=1.0, top_k=None, rng=None) -> np.ndarray
        # idx: (1, T). Crops context to block_size. Returns (1, T + max_new_tokens).
    def num_params(self) -> int
    def save(self, path: str, chars: list[str]) -> None    # np.savez; stores cfg and chars
    @classmethod
    def load(cls, path: str) -> tuple["TinyGPT", CharTokenizer]

class Adam:
    def __init__(self, params, lr=3e-3, betas=(0.9, 0.99), eps=1e-8)
    def step(self, params, grads) -> None      # in place; clips global grad norm to 1.0

def get_batch(data, block_size, batch_size, rng) -> tuple[np.ndarray, np.ndarray]
def estimate_loss(model, data, block_size, batch_size, rng, iters=10) -> float
```

Architecture: token embedding + learned position embedding; `n_layer` pre-norm
blocks (`x = x + attn(ln1(x))`, `x = x + mlp(ln2(x))`); causal multi-head
attention with `d_model // n_head` per head; MLP `d_model -> 4*d_model -> d_model`
with GELU (tanh approximation); final LayerNorm; linear head without weight tying.
Init: normal(0, 0.02) for weights, zeros for biases, ones/zeros for LayerNorm.
`save` must not use pickled objects (store `chars` as a single joined string and
config values as numeric arrays) so `np.load(..., allow_pickle=False)` works.

Acceptance (in `pytests/test_tinygpt.py`):
- Gradient check with `dtype=np.float64`, a tiny config (`vocab 11, block 6,
  n_layer 2, n_head 2, d_model 8`, batch 3): for every parameter, compare
  analytic and central-difference gradients on 6 random entries; relative error
  below `1e-4` for all.
- Training 300 steps on the bundled corpus with the default config lowers the
  validation loss below 2.7.
- `save` then `load` reproduces identical logits.
- `generate` returns the right shape and only valid token ids; with `top_k=1`
  it is deterministic.

### 4.4 Bundled data
- `data/tiny_corpus.txt` (PYTHON-CORE): the first 300,000 characters of the
  public-domain Tiny Shakespeare file, cut at the last newline. Download from
  `https://raw.githubusercontent.com/karpathy/char-rnn/master/data/tinyshakespeare/input.txt`.
- `data/ai_notes.txt` (PYTHON-LABS): 40–50 short factual notes about language
  models, one per line, each 1–2 sentences, written in your own words. Stable
  concepts only (tokenization, attention, context window, embeddings, RAG,
  fine-tuning, LoRA, quantization, evaluation, sampling). No product names, no
  version numbers, no dates, no benchmark figures.

### 4.5 Labs
Rules for every lab file:
- Starts with a docstring: what it shows, then `TRY:` lines suggesting 2–3 edits.
- Tunable values are UPPER_CASE constants at the top.
- 60–120 lines (lab04 may reach 90; it leans on `tinygpt`). Phone-width friendly:
  lines at most 72 characters.
- Work is inside functions, with `main()` called at the bottom (plain call, no
  `if __name__` guard needed but allowed). Loops that run long call a function
  each iteration so Stop works.
- Prints progress in short lines (at most 44 characters wide where practical).
- Finishes in under 20 seconds on a desktop with default constants, except
  lab04 (under 90 seconds).
- Deterministic: seed NumPy RNG from a `SEED` constant.

| File | Owner | What it does |
|---|---|---|
| lab01_bigram.py | LABS | Count character bigrams in the corpus, turn counts into probabilities, report average negative log-likelihood, compare with the uniform baseline `ln(vocab)`, sample 200 characters. |
| lab02_tokenizer.py | LABS | Byte-pair encoding from scratch on the first 20,000 corpus characters: `NUM_MERGES` merges, print the first 15 merges, encode and decode a sample sentence (round trip must match), print the compression ratio. |
| lab03_attention.py | CORE | One causal self-attention head, forward only, on a short sentence with random embeddings: build Q, K, V, mask, softmax; print the attention weights as a text heat map (rows = query token, shading characters ` .:-=+*#%@`), and show that row sums are 1 and no token attends to the future. |
| lab04_train_gpt.py | CORE | Train `TinyGPT` on the corpus. Constants: `STEPS=1500, BATCH=16, BLOCK=32, N_LAYER=2, N_HEAD=2, D_MODEL=64, LR=3e-3, EVAL_EVERY=100`. 90/10 train/val split. Every 10 steps `slmlab.metric("train", step, loss)`; every `EVAL_EVERY` steps `slmlab.metric("val", step, val_loss)` and print one line. Print a 200-character sample at the end. Save to `slmlab.work_path("tiny_gpt.npz")`. Check `slmlab.should_stop()` each step and save before exiting when stopped. |
| lab05_sampling.py | CORE | Load the saved model (if missing, print how to create it and return). For one prompt, print samples at temperatures 0.3, 0.8, 1.5 and with top-k 1, 5, none. Print the next-character probability table (top 8) for the prompt at two temperatures. |
| lab06_rag.py | LABS | Retrieval without a neural network: split `ai_notes.txt` into notes, build TF-IDF vectors, embed `QUESTION`, rank by cosine similarity, print the top `TOP_K` notes with scores, then print the assembled prompt (context + question) that a model would receive. |
| lab07_lora.py | LABS | LoRA on one frozen matrix: a "pretrained" `W` (64×64), a target that equals `W` plus a rank-4 change, train adapters `A` (r×d) and `B` (d×r, zero-initialised) by gradient descent on mean squared error; log `slmlab.metric("lora", step, loss)`; compare trainable parameter counts (full vs LoRA) and final loss for `RANK` in the constant. |
| lab08_quantize.py | CORE | Quantize the saved model's weight matrices to 8-bit and 4-bit (symmetric, per-tensor scale), report size in KB and validation loss for float32, int8, int4. If no saved model exists, demonstrate on a random matrix: report reconstruction error. |
| playground.py | CORE | Not a lesson lab. Expects globals `PROMPT, TEMPERATURE, TOP_K, MAX_TOKENS, SEED` to be defined by a prefix that Kotlin prepends (provide defaults via `globals().get`). Loads the saved model; if missing prints exactly `NO_MODEL` and returns. Otherwise prints only the generated text (prompt included), flushing every 8 characters so text streams. |

Tests (`pytests/`): each lab is executed with `runpy.run_path` under a
temporary work dir and must exit without error; tests also assert the key
behaviours (for example lab02 round trip, lab06 top result is relevant to the
question, lab07 LoRA loss decreases by 10×). For speed, tests may override
constants by executing the source with replaced constant lines.

---

## 5. UX

### 5.1 Navigation

```
Bottom bar (4 tabs):  Path | Playground | Glossary | Settings
Path -> Lesson -> Lab            (pushed screens, bottom bar hidden)
Playground --(no model)--> Lesson p1-05
```

Routes: `roadmap`, `playground`, `glossary`, `settings`, `lesson/{id}`, `lab/{id}`
(`id` is the lesson id). Start destination `roadmap`. Tab switches use
`popUpTo(startDestination) { saveState = true }`, `launchSingleTop`, `restoreState`.
Tab icons: `Icons.Outlined.Route`, `AutoAwesome`, `MenuBook`, `Settings`.

### 5.2 Path (roadmap) screen

```
+------------------------------------------+
| SLM Lab                                  |
| [ring 36%]  5 of 14 lessons              |
|             Next: Self-attention   [>]   |
+------------------------------------------+
| WEEKS 1–2                                |
| Build a tiny GPT                 3 / 6   |
| Understand the model by writing one      |
|  (v) What a language model does   15 min |
|  (v) Tokens and BPE               15 min |
|  ( ) Self-attention     [On device] 20m  |
|  ...                                     |
| WEEKS 3–4                                |
| Make a small model useful        0 / 4   |
|  ( ) Choosing a pretrained...  [Colab]   |
+------------------------------------------+
```

- Header card: `ProgressRing`, "`n` of 14 lessons", and a "Continue" row that
  opens the first incomplete lesson. When all are complete: "Path complete".
- One section per phase: overline with `weeks` (uppercase, muted), title,
  subtitle, right-aligned "`done` / `total`".
- Lesson row (min height 64dp, whole row clickable): status circle (filled
  check in primary when complete, outlined otherwise), title (1–2 lines),
  summary (1 line, muted, ellipsis), trailing `KindChip` and "`N` min".
- No locking: any lesson can be opened in any order.
- Loading: centered `CircularProgressIndicator`. Error: message + "Retry".

### 5.3 Lesson screen

```
+------------------------------------------+
| <-  Self-attention                       |
+------------------------------------------+
| [On device]  20 min                      |
| (rendered markdown, scrolls)             |
| ...                                      |
| Checklist                                |
| [x] I can say what Q, K and V are        |
| [ ] I ran the lab and read the heat map  |
+------------------------------------------+
| [ Open lab ]            [ Mark complete ]|
+------------------------------------------+
```

- Top bar: back arrow, lesson title (1 line, ellipsis).
- Body: `KindChip` + minutes, then `MarkdownView`, then "Checklist" heading and
  one checkbox row per item (state persisted immediately).
- For `GUIDED` lessons, show an info banner above the markdown: "This lesson
  runs on Colab or a laptop. Copy the code blocks from here." (secondary colour).
- Sticky bottom bar: "Open lab" (filled button; hidden when `labAsset == null`)
  and "Mark complete" (outlined; when complete it reads "Completed" with a check
  icon and tapping it un-completes). When all checklist items become ticked and
  the lesson is not complete, mark it complete automatically and show a snackbar
  "Lesson completed".
- After the checklist: "Next: <title>" text button opening the next lesson in
  path order (omit on the last lesson).

### 5.4 Lab screen (the core experience)

```
+------------------------------------------+
| <-  Lab: Self-attention            [...] |
+------------------------------------------+
| [ Code ]  [ Output • ]  [ Chart ]        |
+------------------------------------------+
|  1  """Causal self-attention..."""       |
|  2  SEQ = "the cat sat"                  |
|  3  ...                (editor, scrolls) |
+------------------------------------------+
| Tab ( ) [ ] : = _ " # , . + - *          |  <- symbol bar, only when keyboard is open
+------------------------------------------+
| (>) Run          Idle                    |
+------------------------------------------+
```

- Top bar: back, "Lab: <lesson title>", overflow menu: "Reset code",
  "Copy code", "Clear output".
- Three tabs (Material `PrimaryTabRow`): **Code**, **Output**, **Chart**.
  - Code: `CodeEditor` filling the space; `SymbolBar` sits directly above the
    bottom bar while the keyboard is visible (`WindowInsets.isImeVisible`).
  - Output: `ConsolePane`. Empty state: "Run the code to see output here."
    A dot appears on the tab label when new output arrives while another tab is
    selected; it clears when the tab is opened.
  - Chart: `LossChart` of all metric series. Empty state: "This lab does not
    log metrics yet. Labs that train a model draw their loss here."
- Bottom bar: primary action button and a status text.
  - Idle: button "Run" (play icon). Status "Idle", or after a run "Finished in
    12.4 s", "Stopped", or "Error: <message>" in the error colour (1 line, ellipsis).
  - Running: button "Stop" (stop icon, error colour). Status "Running 0:12"
    updating every second, with a small indeterminate progress indicator.
- Pressing Run: hide the keyboard, clear previous output and metrics, switch to
  the Output tab, start the run. When the first metric of a run arrives and the
  user has not changed tabs since pressing Run, switch to Chart once.
- Editing is disabled while running (`readOnly`).
- Code autosaves 500 ms after the last edit (`ProgressStore.saveCode`). On open:
  saved code if present, else the asset source. "Reset code" asks for
  confirmation ("Reset to the original code? Your edits will be lost.") then
  restores the asset source and clears the saved copy.
- The screen stays on while running (`LocalView.current.keepScreenOn`).
- Back while running: dialog "Stop the run and leave?" with "Stop and leave" / "Stay".
- Output buffer is capped at 200,000 characters (drop from the front).
- A run continues across rotation (state lives in the ViewModel). Leaving the
  screen stops the run (`onCleared` calls `runner.stop()`).

### 5.5 Playground screen

```
+------------------------------------------+
| Playground                               |
| Prompt                                   |
| [ ROMEO:                               ] |
| Temperature            0.8  ----o------  |
| Top-k                   20  --o--------  |
| Length                 200  ----o------  |
| [ Generate ]                             |
+------------------------------------------+
| ROMEO: what light through yonder...      |
+------------------------------------------+
```

- Controls: prompt text field (default `ROMEO:`), temperature slider 0.1–2.0
  step 0.1 (default 0.8), top-k slider 0–65 where 0 means "off" (default 20),
  length slider 50–500 step 50 (default 200). Current values shown on the right.
- "Generate" runs `labs/playground.py` through `PyRunner` with a prefix that
  defines `PROMPT`, `TEMPERATURE`, `TOP_K` (`None` when 0), `MAX_TOKENS`, `SEED`
  (random int per press). Text streams into a monospace card. While generating
  the button reads "Stop".
- Build the prefix with a Kotlin function that writes a Python string literal
  safely: wrap in double quotes; escape `\`, `"`, newline (`\n`), carriage
  return, tab.
- If output is exactly `NO_MODEL` (trimmed): show the empty state instead —
  "No trained model yet. Train one in the lesson 'Training a tiny GPT'." with a
  button "Open that lesson" (`onOpenLesson("p1-05")`).
- If a lab run is in progress (`runner.isRunning`), disable Generate and show
  "A lab is running. Stop it first."

### 5.6 Glossary screen
Search field at top (filters by term or definition, case-insensitive), then an
A–Z list: term in title style, definition below. Empty result: "No terms match."

### 5.7 Settings screen
- Theme: three-option segmented control — System, Light, Dark.
- Editor font size: slider 10–18 sp (default 13), with a one-line monospace preview.
- "Reset progress": confirmation dialog, then `ProgressStore.resetAll()`;
  snackbar "Progress reset".
- About: app name, version, one line "Python runs on this device. Nothing is uploaded."

### 5.8 Strings and tone
Sentence case everywhere. Buttons say what they do ("Run", "Stop", "Open lab",
"Mark complete"). No exclamation marks, no emoji. Put all user-visible strings
in Kotlin as literals (no need for `strings.xml` beyond the app name).

### 5.9 Accessibility
Touch targets at least 48dp. Every icon-only button has a `contentDescription`.
Status is never conveyed by colour alone (icons or text accompany it). Text
uses `sp`. Body text contrast at least 4.5:1 in both themes.

---

## 6. UI components (ANDROID-CORE implements; ANDROID-SCREENS calls)

Package `com.slmlab.app.ui.components`. Signatures are fixed:

```kotlin
@Composable fun MarkdownView(markdown: String, modifier: Modifier = Modifier)
@Composable fun CodeEditor(
    value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit,
    fontSizeSp: Int, modifier: Modifier = Modifier, readOnly: Boolean = false)
@Composable fun SymbolBar(onInsert: (String) -> Unit, modifier: Modifier = Modifier)
fun TextFieldValue.insertAtCursor(text: String): TextFieldValue      // helper for SymbolBar
@Composable fun ConsolePane(text: String, fontSizeSp: Int, modifier: Modifier = Modifier,
    emptyText: String = "Run the code to see output here.")
@Composable fun LossChart(series: Map<String, List<MetricPoint>>, modifier: Modifier = Modifier)
@Composable fun ProgressRing(progress: Float, modifier: Modifier = Modifier, size: Dp = 56.dp)
@Composable fun KindChip(kind: LessonKind, modifier: Modifier = Modifier)
@Composable fun EmptyState(message: String, modifier: Modifier = Modifier,
    actionLabel: String? = null, onAction: (() -> Unit)? = null)
```

One file per component (`MarkdownView.kt`, `CodeEditor.kt` (also holds
`SymbolBar` and `insertAtCursor`), `ConsolePane.kt`, `LossChart.kt`,
`ProgressRing.kt`, `KindChip.kt`, `EmptyState.kt`).

### 6.1 MarkdownView
Hand-written renderer, no library. Does not scroll itself (the parent scrolls);
it is a `Column`. Supported subset — this is also the contract for CONTENT:
- `# `, `## `, `### ` headings
- paragraphs separated by blank lines; consecutive lines join with a space
- `- ` bullet lists and `1. ` numbered lists (single level)
- fenced code blocks with three backticks (optional language tag, ignored):
  monospace card on `surfaceVariant`, horizontally scrollable, with a copy icon
  button at the top right (copies to clipboard)
- `> ` block quotes rendered as a callout card with a 3dp primary left border
- inline `**bold**` and `` `code` `` (monospace with a subtle background)
No tables, images, links, nested lists or HTML.

### 6.2 CodeEditor
`BasicTextField`, monospace, `fontSizeSp`, line height 1.4×, no autocorrect or
capitalisation (`KeyboardOptions(capitalization = None, autoCorrect = false,
keyboardType = KeyboardType.Ascii)`), cursor in primary colour. Line-number
gutter on the left (muted, right-aligned, same font, not selectable). Lines do
not wrap: the text area scrolls horizontally and the whole editor vertically
(gutter scrolls vertically with the text). Pressing Enter keeps the previous
line's indentation, plus four spaces when that line ends with `:` (implement in
the `onValueChange` wrapper by detecting a single inserted `\n`). No syntax
highlighting.

### 6.3 SymbolBar
Horizontally scrollable row, 44dp high, on `surfaceVariant`. Keys, in order:
`Tab` (inserts four spaces), `(`, `)`, `[`, `]`, `:`, `=`, `_`, `"`, `#`, `,`,
`.`, `+`, `-`, `*`. Each key at least 40dp wide.

### 6.4 ConsolePane
Monospace selectable text on `surfaceVariant`, vertical scroll, long lines wrap.
Auto-scrolls to the bottom when text grows, unless the user has scrolled up
(resume auto-scroll when they return to the bottom).

### 6.5 LossChart
`Canvas` line chart. X = step, Y = value, both auto-ranged with 5% padding;
4 horizontal grid lines with Y labels (2 decimals), X labels at min and max.
One line per series; colours in order: primary, secondary, tertiary. Legend row
above the plot (colour dot + name + latest value to 3 decimals). Downsample any
series over 600 points by taking every k-th point. Height 240dp by default.

### 6.6 KindChip
`ON_DEVICE` -> label "On device", icon `PhoneAndroid`, primary container colours.
`GUIDED` -> label "Colab", icon `Cloud`, secondary container colours.

---

## 7. Visual design

Dark-first, calm, a "lab notebook at night" feel: near-black blue-grey surfaces,
one mint accent for anything runnable or done, amber for anything that happens
off-device. Flat surfaces separated by tone, not shadows.

### 7.1 Colour tokens

| Role | Dark | Light |
|---|---|---|
| background | #0F1115 | #FAFAF7 |
| surface | #171A21 | #FFFFFF |
| surfaceVariant | #1F2430 | #EEF0EC |
| outline | #2C3340 | #D5D9DE |
| onBackground / onSurface | #E6E9EF | #1B1F27 |
| onSurfaceVariant (muted text) | #9AA3B2 | #5B6472 |
| primary | #7CE0C3 | #0E7C66 |
| onPrimary | #06241C | #FFFFFF |
| primaryContainer | #12382F | #CFF3E8 |
| onPrimaryContainer | #A6F0DA | #06382D |
| secondary | #FFB86B | #9A5200 |
| onSecondary | #2E1A00 | #FFFFFF |
| secondaryContainer | #3D2A10 | #FFE3C2 |
| onSecondaryContainer | #FFD9A8 | #3A2000 |
| tertiary | #9DB7FF | #3454B4 |
| error | #FF6B6B | #B3261E |
| onError | #2B0707 | #FFFFFF |

No dynamic colour. Status bar and navigation bar match `background`
(edge-to-edge with `enableEdgeToEdge()`; screens handle insets through `Scaffold`).

### 7.2 Type
System sans (`FontFamily.Default`) for UI, `FontFamily.Monospace` for code and
console. Material 3 scale with these overrides: `headlineSmall` 22sp SemiBold,
`titleMedium` 16sp SemiBold, `bodyLarge` 16sp / 24sp line height,
`bodyMedium` 14sp / 20sp, `labelSmall` 11sp Medium with 0.8sp letter spacing
(used for uppercase overlines).

### 7.3 Shape and spacing
Cards 14dp corners, chips 8dp, buttons 12dp. Screen horizontal padding 16dp.
Vertical rhythm in multiples of 4dp; 12dp between list rows' content blocks,
24dp between sections.

### 7.4 Motion
Default Material transitions only. Tab content switches without animation.
The loss chart redraws without animation.

---

## 8. State and persistence

- `DataStoreProgressStore` (Preferences DataStore named `slmlab`):
  `completed` (string set of lesson ids), `checked` (string set of
  `"<lessonId>#<index>"`), `theme` (string), `editor_font_sp` (int).
- Saved lab code: plain files `filesDir/code/<lessonId>.py`.
- Trained weights: `filesDir/work/tiny_gpt.npz`. `resetAll()` clears DataStore
  and deletes everything in `code/` and `work/`.
- `ChaquopyRunner.run(code)` returns a cold `Flow<RunEvent>` (`callbackFlow`
  with an unlimited buffer). Collecting starts a daemon thread named `py-run`
  that calls `slm_runner.run(code, PyListener { trySend(it) })`, then closes the
  flow. `awaitClose { stop() }`. `isRunning` is a `StateFlow<Boolean>` set true
  before the thread starts and false when it ends. Only one run at a time
  across the whole app.
- ViewModels expose a single immutable `UiState` data class via `StateFlow`,
  collected with `collectAsStateWithLifecycle()`.

---

## 9. Quality bar for every contributor

- Kotlin: no unused imports left behind that would fail compilation, no TODOs,
  no placeholder screens. The code cannot be compiled in this environment (no
  Android SDK), so read your code back carefully: every symbol you use must be
  imported, every experimental Material 3 API must be opted in with
  `@OptIn(ExperimentalMaterial3Api::class)` (or the matching annotation), and
  every signature you call must match this document and the contract files.
- Python: run it. Every lab and test must pass with
  `cd /home/claude/slm-lab && python3 -m pytest pytests -q`.
- Stay inside your owned files. Report anything you could not finish plainly.
