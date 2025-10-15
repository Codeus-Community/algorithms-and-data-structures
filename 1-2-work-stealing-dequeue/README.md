# Work-stealing dequeue practice

The system is a plain pure Java application that sums the array.

The problem: currently system has a class [NoWorkStealingArrSum.java](src/main/java/org/codeus/ws_deque/noworkstealingexample/NoWorkStealingArrSum.java).
It just sequentially adds each element inside array.

The practice is to make the summing of the array execute less that 1s (see `ARRAY_SIZE` constant in the Main class).
The task should be done using Java's `ForkJoinPool`. 
In order to complete the task - RESOLVE ALL TODOS.

Hint: the recursive task has to be created, which is going to divide the array by next principle:
task1 = new Task(start, mid);
task2 = new Task(mid, end)

Already provided solution is provided in branch `1-2-work-stealing-deque-completed`.

The presentation resources: https://excalidraw.com/#json=s6-LP95H3bmZ1jLkzD4l2,e6hjjIsi4UiXDLOFZEf3ew

