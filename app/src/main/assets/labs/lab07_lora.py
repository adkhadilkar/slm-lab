"""Lab 7: LoRA, learning a small change instead of a whole matrix.

Shows low-rank adaptation. A large pretrained matrix W stays frozen.
We train two thin matrices, B (d x r) and A (r x d), so that B @ A is
a low-rank change that moves the layer toward a target. Full
fine-tuning would train all d*d numbers; LoRA trains only 2*d*r.

TRY: set RANK to 1 or 2. The true change has rank 4, so loss stalls.
TRY: set LR to 0.05. Training slows down; does more STEPS fix it?
TRY: set DIM to 128. How do the two parameter counts compare now?
"""
import numpy as np

import slmlab

SEED = 0           # fixes W, the target, the inputs and A
DIM = 64           # W is DIM x DIM
TRUE_RANK = 4      # rank of the change the target needs
RANK = 4           # rank of our adapter (the thing we are choosing)
N = 256            # number of random input rows
STEPS = 400        # gradient descent steps
LR = 0.2           # step size
LOG_EVERY = 10     # send a chart point this often
PRINT_EVERY = 100  # print a progress line this often


def make_problem(rng):
    """Return frozen W, the target W_star, and random inputs X.

    The target is W plus a change of rank TRUE_RANK, built from two
    thin matrices. An adapter with RANK < TRUE_RANK cannot match it.
    """
    w = rng.normal(0.0, 1.0 / np.sqrt(DIM), (DIM, DIM))
    left = rng.normal(0.0, 1.0, (DIM, TRUE_RANK))
    right = rng.normal(0.0, 1.0, (TRUE_RANK, DIM))
    change = left @ right / np.sqrt(TRUE_RANK * DIM)
    x = rng.normal(0.0, 1.0, (N, DIM))
    return w, w + change, x


def loss_and_grads(x, a, b, delta):
    """Mean squared error and its gradients for A and B.

    The model is W + B @ A and the target is W + delta, so the error is
    err = X @ (B @ A - delta).T. W cancels out, which is why it can
    stay frozen. loss = mean(err^2). By hand, with g the gradient of
    the loss with respect to the change M = B @ A:
      g = (2 / (N*DIM)) * err.T @ X
      d loss / d B = g @ A.T     (chain rule through M = B @ A)
      d loss / d A = B.T @ g
    """
    m = b @ a                                  # the low-rank change
    err = x @ (m - delta).T                    # (N, DIM) error
    loss = float(np.mean(err * err))
    g = (2.0 / (N * DIM)) * (err.T @ x)        # (DIM, DIM)
    return loss, b.T @ g, g @ a.T              # grad_a, grad_b


def train_step(a, b, x, delta):
    """One gradient descent step. Returns new A, new B, and the loss."""
    loss, grad_a, grad_b = loss_and_grads(x, a, b, delta)
    return a - LR * grad_a, b - LR * grad_b, loss


def main():
    rng = np.random.default_rng(SEED)
    w, w_star, x = make_problem(rng)
    delta = w_star - w                         # the change to learn
    a = rng.normal(0.0, 0.1, (RANK, DIM))      # small random start
    b = np.zeros((DIM, RANK))                  # zero start: B @ A = 0
    print("full fine-tune params:", DIM * DIM)
    print("LoRA params:", 2 * DIM * RANK)
    print("W stays frozen; only A and B move")
    start = None
    for step in range(STEPS):
        if slmlab.should_stop():
            break
        a, b, loss = train_step(a, b, x, delta)
        if start is None:
            start = loss                       # loss before any update
        if step % LOG_EVERY == 0:
            slmlab.metric("lora", step, loss)
        if step % PRINT_EVERY == 0:
            print("step %3d  loss %.5f" % (step, loss))
    final, _, _ = loss_and_grads(x, a, b, delta)
    slmlab.metric("lora", STEPS, final)
    print("start loss: %.5f" % start)
    print("final loss: %.5f" % final)
    print("reduced by: %.0f x" % (start / max(final, 1e-12)))


main()
