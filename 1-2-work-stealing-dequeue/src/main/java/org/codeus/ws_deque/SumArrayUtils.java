package org.codeus.ws_deque;

public class SumArrayUtils {
    public static int[] getFilledArr(int arrSize) {
        int[] sumArray = new int[arrSize];
        fillArr(sumArray);
        return sumArray;
    }

    public static void fillArr(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i + 1;
        }
    }

    public static long sum(int[] arr) {
        long sum = 0;
        for (int j : arr) {
            sum = sumThrottle(sum, j);
        }
        System.out.println(sum);
        return sum;
    }

    // Used to simulate some computing operation (in this case to slow down summing a bit)
    public static long sumThrottle(long num1, long num2) {
        int sleepMillis = 1;
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return num1 + num2;
    }
}
