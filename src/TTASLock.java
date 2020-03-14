import java.util.*;

class TTASLock implements Lock {

    private AtomicBoolean state = new AtomicBoolean(false);

    @Override
    public void lock() {
        while (true) {
            while (state.get()) {
            }

            if (!state.getAndSet(true)) {
                return;
            }
        }
    }

    @Override
    public void unlock() {
        state.set(false);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long l, TimeUnit timeUnit) throws InterruptedException {
        return false;
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
