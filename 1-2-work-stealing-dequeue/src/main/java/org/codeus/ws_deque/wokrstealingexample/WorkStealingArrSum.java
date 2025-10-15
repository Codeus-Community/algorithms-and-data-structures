package org.codeus.ws_deque.wokrstealingexample;

import java.util.concurrent.ForkJoinPool;

import static org.codeus.ws_deque.SumArrayUtils.getFilledArr;

public class WorkStealingArrSum {

    public long returnSumUsingWorkStealing(int arraySize) {
        long before = System.currentTimeMillis();

        ForkJoinPool pool = ForkJoinPool.commonPool();
        int[] arrToSum = getFilledArr(arraySize);

        //TODO Create a RecursiveTask and implement it to return a sum. Return sum in this method.
//        Long sum = pool.invoke(someTask);
        pool.shutdown();

        long after = System.currentTimeMillis();
        int secondsTook = (int) (after - before) / 1000;
        System.out.println("Seconds took: " + secondsTook);

        //TODO Need to return the actual sum here
        return 123L;
    }

}
