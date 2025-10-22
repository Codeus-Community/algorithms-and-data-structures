package org.codeus.ws_deque.queue;

import org.codeus.ws_deque.entity.Golfer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author yelyzavetalubenets
 **/
@Component
public class WriteBehindQueue {
    private final Queue<Golfer> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(Golfer golfer) {
        queue.add(golfer);
    }

    public List<Golfer> drain() {
        List<Golfer> drained = new ArrayList<>();
        Golfer g;
        while ((g = queue.poll()) != null) drained.add(g);
        return drained;
    }

    public int size(){
        return queue.size();
    }
}

