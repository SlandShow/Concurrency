import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Example for implementation contention-free lock.
 */
public class BackoffLock implements Lock {

    private AtomicBoolean state = new AtomicBoolean(false);
    private static final int MIN = ...; 
    private static final int MAX = ...;

    @Override
    public void lock() {
        Backoff backoff = new Backoff(MIN, MAX);
        while (true) {
            while (state.get()) ;

            if (!state.getAndSet(true)) {
                return;
            } else {
                try {
                    backoff.backoff();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void unlock() {
        state.set(false);
    }

    static class Backoff {
        private final int minDelay;
        private final int maxDelay;
        private int limit;
        private final Random random;

        public Backoff(int min, int max) {
            this.minDelay = min;
            this.maxDelay = max;
            limit = minDelay;
            random = new Random();
        }

        public void backoff() throws InterruptedException {
            int delay = random.nextInt(limit);
            limit = Math.min(maxDelay, 2 * limit);
            Thread.sleep(delay);
        }
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
