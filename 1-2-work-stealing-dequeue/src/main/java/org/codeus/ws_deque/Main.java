package org.codeus.ws_deque;

import org.codeus.ws_deque.noworkstealingexample.NoWorkStealingArrSum;
import org.codeus.ws_deque.wokrstealingexample.WorkStealingArrSum;

public class Main {
    private static final int ARRAY_SIZE = 5_000;

    public static void main(String[] args) {
        // Uncomment this initial example if needed
        showNoWorkStealingExample();

        //Work Stealing Example
        showWorkStealingExample();
    }

    private static void showWorkStealingExample() {
        WorkStealingArrSum workStealingArrSum = new WorkStealingArrSum();

        long workStealingSum = workStealingArrSum.returnSumUsingWorkStealing(ARRAY_SIZE);
        System.out.println("WorkStealingSum: " + workStealingSum);
    }

    private static void showNoWorkStealingExample() {
        NoWorkStealingArrSum noWorkStealingExample = new NoWorkStealingArrSum();
        long noWorkStealingSum = noWorkStealingExample.returnSumWithoutWorkStealing(ARRAY_SIZE);
        System.out.println("NoWorkStealingSum: " + noWorkStealingSum);
    }

}
