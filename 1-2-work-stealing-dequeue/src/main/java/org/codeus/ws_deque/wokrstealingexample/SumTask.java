package org.codeus.ws_deque.wokrstealingexample;

import java.util.concurrent.RecursiveTask;

public class SumTask extends RecursiveTask<Long> {
    private static final int TASK_DIVISION_THRESHOLD = 100;
    private final int[] array;
    private final int start;
    private final int end;

    public SumTask(int[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
        System.out.printf("Created Task with Start: %s, End: %s%n", start, end);
    }

    @Override
    protected Long compute() {
        if (end - start <= TASK_DIVISION_THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        } else {
            int mid = (start + end) / 2;
            SumTask left = new SumTask(array, start, mid);
            SumTask right = new SumTask(array, mid, end);

            left.fork();
            return right.compute() + left.join();
        }
    }
}
