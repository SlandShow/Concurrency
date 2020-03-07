import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Example of spin-lock implementation, using test-and-set instructions.
 * Read more: https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/atomic/AtomicBoolean.html#getAndSet(boolean)
 */
public class TASLock implements Lock {
    private AtomicBoolean state = new AtomicBoolean(false);

    @Override
    public void lock() {
        while (state.getAndSet(true));
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
