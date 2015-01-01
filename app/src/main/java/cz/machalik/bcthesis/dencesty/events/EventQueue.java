package cz.machalik.bcthesis.dencesty.events;

import android.content.Context;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Lukáš Machalík
 */
public class EventQueue {

    private Queue<Event> queue;

    public EventQueue() {
        this.queue = new ArrayDeque<>();
    }

    public void add(Event event) {
        queue.add(event);
    }

}
