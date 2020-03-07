# Concurrency
## Sample repo for concurrency practice.

Some of this stuff from [Udemy course](https://www.udemy.com/course/java-multithreading-concurrency-performance-optimization "Udemy course").

## Basics
<b>Latency</b> - time for global task.
```
Latency = Task
```

We can try to reduce it using parallelism:
```
Latency = Task / N, where N - number of threads
```

<a href="https://ibb.co/r0S3qBk"><img src="https://i.ibb.co/YQMXJ52/image.png" alt="image" border="0"></a>

For real reducing, we need to use correct count of threads (<b>N</b>), because idle threads can waste CPU time.

So, thats why:
```
N = cores of CPU 
```

### 1. Painter.

Usage of painter:
```
    public static final String IMAGE_PATH = "./resources/flowers.jpg";
    public static final String RESULT_PATH = "./out/flowers.jpg";

    private static final ImagePainter painter = new ImagePainter();

    public static void main(String[] args) throws IOException {

        BufferedImage originalImage = ImageIO.read(new File(IMAGE_PATH));
        BufferedImage resultImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Recolor image API call
        painter.recolorImage(originalImage,
                resultImage,
                0,
                0,
                originalImage.getWidth(),
                originalImage.getHeight());

        File outputFile = new File(RESULT_PATH);
        ImageIO.write(resultImage, "jpg", outputFile);
    }
    ...
```
Input image:

![image not found](https://i.ibb.co/nMbBfGM/flowers.jpg)

Output image:

![image not found](https://i.ibb.co/cJy1sMY/flowers.jpg)

### Paint via threads:

<a href="https://ibb.co/xC4gDPx"><img src="https://i.ibb.co/YRrd3YH/image.png" alt="image" border="0"></a>

Usage for N threads:
```
public static void recolorMultithreaded(BufferedImage originalImage, BufferedImage resultImage, int numberOfThreads) {
        List<Thread> threads = new ArrayList<>();
        int width = originalImage.getWidth();
        int height = originalImage.getHeight() / numberOfThreads;

        for(int i = 0; i < numberOfThreads ; i++) {
            final int threadMultiplier = i;

            Thread thread = new Thread(() -> {
                int xOrigin = 0 ;
                int yOrigin = height * threadMultiplier;

                painter.recolorImage(originalImage, resultImage, xOrigin, yOrigin, width, height);
            });

            threads.add(thread);
        }

        for(Thread thread : threads) {
            thread.start();
        }

        for(Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }
```
To achieve optimality, it is necessary that the number of threads be equal to the number of cores on the computer.
The more complex the task, the more profitable it will be divided into parallel subtasks:

<a href="https://ibb.co/PGssMZL"><img src="https://i.ibb.co/zVWWFhM/image.png" alt="image" border="0"></a>

### 2. Resource sharing

Threads using the same resource, for example - object, database, connection to socket and so on.
But using this aproach, be carefull. What happend if two threads start do some dirty thing in parallel?


We have two tasks, one for incrementing, second for decrementing.
```
CountInventory countInventory = new CountInventory();

Thread first = new Thread(() -> {
        for (int i = 0; i < 10000; i++) {
             countInventory.increment();
        }
});

Thread second = new Thread(() -> {
        for (int i = 0; i < 10000; i++) {
             countInventory.decrement();
         }
});
```

If we will start them sequently - reuslt will be 0.
```
first.start();
first.join();

second.start();
second.join();

System.out.print("Result - " + countInventory.getItems());
```

If we will start them in parallel - the result will not be determined.
```
first.start();
second.start();

first.join();
second.join();

System.out.print("Result - " + countInventory.getItems());
```

`countInventory.getItems())` can return -2200, -5796, -1785 and so on. 

But why not zero?

Because operator `var++` non atomic operation and can be splitted into separate operations:
1. Get value of `var`
2. Increment it
3. Update new value for `var`

In case of latency, our timing can be:
```
FIRST CASE
----------

first task
1. currValue <- 0
2. newValue <- 0 + 1
6. currValue <- 1

second task
3. currValue <- 0
4. currValue <- 0 - 1 <- -1
5. items <- -1

RESULT IS 1

SECOND CASE
-----------
first task
1. currValue <- 0
2. newValue <- 0 + 1
5. currValue <- 1

second task
3. currValue <- 0
4. currValue <- 0 - 1 <- -1
6. items <- -1

RESULT IS -1
```

In both examples we can see incorrect result.

### 3. Atomic operations
Atomic operations:
1. Reference assigment 
```
Object obj1 = ...;
Object obj2 = ...;
obj1 = obj2; // Atomic
```
2. Assigments to primitive types (except long and double) - reads and writes:
```
int a = 123; // Atomic
```

Long & double - 64 bit, that's why java cannot guarantee atomic assigments:

<a href="https://ibb.co/bHPHwJy"><img src="https://i.ibb.co/PTNTXQb/image.png" alt="image" border="0"></a>

Let's look on next code:
```
// Which of the following operations are atomic and thread safe?
public class SharedClass {
        private String name;
 
        public void updateString(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
```
All objects taken from heap. That's why `getName()` operaton is thead safe.

### 4. Volatile.
The Java volatile keyword guarantees visibility of changes to variables across threads.

In a multithreaded application where the threads operate on non-volatile variables, each thread may copy variables from main memory into a CPU cache while working on them, for performance reasons. If your computer contains more than one CPU, each thread may run on a different CPU. That means, that each thread may copy the variables into the CPU cache of different CPUs. This is illustrated here:

<a href="https://imgbb.com/"><img src="https://i.ibb.co/PjXyBYm/image.png" alt="image" border="0"></a>

Imagine a situation in which two or more threads have access to a shared object which contains a counter variable declared like this:
```
public class SharedObject {
    public int counter = 0;
}
```

<a href="https://imgbb.com/"><img src="https://i.ibb.co/sJJSRqQ/image.png" alt="image" border="0"></a>

### Locks, Mutual exclusion
Mutual exclusion (Mutex) garantee that only one thread can execute some critical section, while another have no access to this section.

Let's check Peterson algorithm for 2-threaded apllication via C++ based pseudo-code:
```
struct Lock {
    /* Atomic int - special type, writing & reading from this variables happens as atomic operation */
    atomic_int last;
    atomic_int flag[2];
}

// Spin-lock realisation for 2-threaded application
void lock() {
    int me = threadId(); // Return id of thread, 0 or 1
    int other = 1 - me; // Id of another thread, 1, or 0
    
    // Atomic operations
    flag[me] = 1; 
    last = me;
    
    // If another thread not acquire the lock and last guy who acquire the lock is me - spin a while
    while (flag[other] && last == me) {}
}

void unlock() {
    me = threadId();
    flag[me] = 0;
}
 ```
### More Spin-locks, exponential backoff
Spinlock is a lock which causes a thread trying to acquire it to simply wait in a loop ("spin") while repeatedly checking if the lock is available.

#### Test and set locks
[Test-and-set](https://en.wikipedia.org/wiki/Test-and-set) instruction - atomic instruction used to write some value to a memory location and return its old value.

It's very easy to use tests-and-set operation in lock algorithm inplementation. Let's check the test-and-set lock (TASLock) example:
```
class TASLock implements Lock {
    private AtomicBoolean state = new AtomicBoolean(false);
    
    @Override
    public void lock() {
        while(state.getAndSet(true));
    }
    
    @Override
    public void unlock() {
        state.set(false);
    }
    
    // Other Lock methods...
}
```
Because they avoid overhead from operating system process rescheduling or context switching, spinlocks are efficient if threads are likely to be blocked for only short periods. For this reason, operating-system kernels often use spinlocks. However, spinlocks become wasteful if held for longer durations, as they may prevent other threads from running and require rescheduling. The longer a thread holds a lock, the greater the risk that the thread will be interrupted by the OS scheduler while holding the lock.

### ReentrantLock
A [reentrant mutual exclusion Lock](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/ReentrantLock.html "Docs") with the same basic behavior and semantics as the implicit monitor lock accessed using synchronized methods and statements, but with extended capabilities.

What actually "reentrant" word means? Reentrant mutex (recursive mutex, recursive lock) is a particular type of mutual exclusion (mutex) device that may be locked multiple times by the same process/thread, without causing a deadlock.

```
// No deadlocks here!
synchronized(mutexA) {
    synchronized(mutexA) {
        // Some code...
    }
}
```

Basic usage of Lock object:
```
Lock lock = new ReentrantLock();
try {
    lock.lock();
    // Critical section...
} finally {
    lock.unlock();
}
```

Versus synchronized:
```
synchronized(mutexObject) {
    // Critival section...
}    
```

Result here is the same (ecursive mutex property still exists).

When we try to `lock.lock()`, out thread will [spin](https://en.wikipedia.org/wiki/Spinlock) a while for getting access to mutual exclusion. It's like a Peterson spin-lock or like a basic synchronized construction. 

#### Contention
Let's talk about contention. A lock is said to be contented when there are multiple threads concurrently trying to acquire the lock while it’s taken. That's main property of all spin-locks.

And yes, what is a main difference between spin-lock and ReentrantLock? In ReentrantLock we have special `tryLock()` method, which Acquires the lock only if it is not held by another thread at the time of invocation.
```
Lock lock = new ReentrantLock();
...
void tryToGetMutex() {
    if (lock.tryLock()) {
        try {
            // Critical section...
        } finally {
            lock.unlock();
        }
    }
}
```

`tryLock()` can return true and acquire the lock if it is free, and can return false if lock is not free. Main difference is that `tryLock()` in case if non-acquiring don't spin a while until lock will be under this thread. Instead of this case, we can retry to call `tryToGetMutex()` manually in next steps.

#### Fairness and starvation
So, if we set flag `fair` of ReentrantLock to `true`, which means [next](http://tutorials.jenkov.com/java-concurrency/starvation-and-fairness.html "Docs"):
```
 @param fair {@code true} if this lock should use a fair ordering policy
 ```
 
 If a thread is not granted CPU time because other threads grab it all, it is called "starvation". The thread is "starved to death" because other threads are allowed the CPU time instead of it. The solution to starvation is called "fairness" - that all threads are fairly granted a chance to execute. Also, fairness may reduce throughpu of application!
 
### The Java volatile Visibility Guarantee.
The Java volatile keyword is intended to address variable visibility problems. By declaring the counter variable volatile all writes to the counter variable will be written back to main memory immediately. Also, all reads of the counter variable will be read directly from main memory.

Here is how the volatile declaration of the counter variable looks:
```
public class SharedObject {
    public volatile int counter = 0;
}
```

[Read more here](http://tutorials.jenkov.com/java-concurrency/volatile.html "Resource").

### 4. Flushing with a volatile.

Let's look on this code sample:
```
class BadTask implements Runnable {
    boolean keepRunning = true;

    @Override
    public void run() {
        while (keepRunning) {
            // Spin a while
        }

        System.out.println("Done.");
    }
}
```

Let's run it via new thread in `main()` function:
```
BadTask badTask = new BadTask();
new Thread(badTask).start();
Thread.sleep(1000);
badTask.keepRunning = false;
System.out.println("keepRunning is false");
```

The intention of this code is to let BadTask run for 1 second and after that to stop it by setting keepRunning boolean to false. As simple as it may look this code is doomed to fail - the BadTask won’t stop after 1 second and will run until you terminate the program manually.

If the program does not stop you may wonder what happend. In short - the main thread and the thread running BadTask have been executed on different cores. Each core has its own set of registers and caches. The new value of keepRunning has been written to one of these without being flushed to the main memory. Thus it is not visible to the code running on a different core.

Ok, how we can fix it? The simplest and the most correct way is to mark this variable volatile. Another approach would be to acquire a common lock when accessing it but that would be definetly an overkill.

So what we will do today? We will introduce another variable marked with a volatile keyword! In the above code it does not make much sense and is only for demonstrating some aspects of Java memory model. But think about a scenario where there are more variables of keepRunning nature. Have a look at the below code that does not have visibility problem anymore:
```
class BadTask implements Runnable {
        boolean keepRunning = true;
        volatile boolean flush = true; // !

        @Override
        public void run() {
                while(keepRunning) {
                        if(flush); // Happens before
                }
                System.out.println("BadTask is done.");
        }
}
```

And let's run it again:
```
BadTask r = new BadTask();
new Thread(r).start();
Thread.sleep(1000);
r.keepRunning = false;
r.flush = false;
System.out.println("keepRunning is false");
```

So as already mentioned we have introduced a new volatile variable “flush”. We do two things with it. First, we do a write operation in the main thread, right after modifying a non-volatile keepRunning variable. Second, in the thread running BadTask, we do a read operation on it.
Now, how come the value of keepRunning is flushed to the main memory? This is guaranteded by the current Java memory model. According to JSR133 “writing to a volatile field has the same memory effect as a monitor release, and reading from a volatile field has the same memory effect as a monitor acquire”. Thus, actions on memory done by one thread before writing to a volatile variable will be visible to another thread after reading that variable.
