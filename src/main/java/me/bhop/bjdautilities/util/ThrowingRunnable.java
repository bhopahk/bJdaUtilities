package me.bhop.bjdautilities.util;

public final class ThrowingRunnable implements Runnable {
    private final Runnable delegate;

    public ThrowingRunnable(Runnable delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run() {
        try {
            delegate.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
