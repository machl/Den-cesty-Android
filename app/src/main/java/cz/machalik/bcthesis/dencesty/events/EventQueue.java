package cz.machalik.bcthesis.dencesty.events;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Iterator;
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

    public void remove(int[] ids) {
        // TODO remove events by ids
    }

    public int size() {
        return queue.size();
    }

    public synchronized JSONArray toJSONArray() {
        JSONArray array = new JSONArray();

        Iterator<Event> it = queue.iterator();
        while (it.hasNext()) {
            Event event = it.next();
            JSONObject jsonEvent = event.toJSONObject();
            array.put(jsonEvent);
        }

        return array;
    }

}
