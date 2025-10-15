package org.codeus.ws_deque.wokrstealingexample;

import java.util.concurrent.ForkJoinPool;

import static org.codeus.ws_deque.SumArrayUtils.getFilledArr;

public class WorkStealingArrSum {

    public long returnSumUsingWorkStealing(int arraySize) {
        long before = System.currentTimeMillis();

        ForkJoinPool pool = ForkJoinPool.commonPool();
        int[] arrToSum = getFilledArr(arraySize);

        SumTask task = new SumTask(arrToSum, 0, arrToSum.length);
        Long sum = pool.invoke(task);
        pool.shutdown();

        long after = System.currentTimeMillis();
        int secondsTook = (int) (after - before) / 1000;
        System.out.println("Seconds took: " + secondsTook);

        return sum;
    }

}
