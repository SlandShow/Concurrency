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

For real reducing, we need to usw correct count of threads (<b>N</b>), because idle threads can waste CPU time.

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

In both examples we can se incorrect result.

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
