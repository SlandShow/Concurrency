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

In boyh examples we can se incorrect result.
