package cz.machalik.bcthesis.dencesty.events;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lukáš Machalík
 */
public class EventQueue {

    private List<Event> queue;

    public EventQueue() {
        this.queue = new ArrayList<>();
    }

    public void add(Event event) {
        queue.add(event);
    }

    public void remove(int[] ids) {
        for (int id : ids) {
            Event founded = null;
            for (Event e : queue) {
                if (e.getEventId() == id) {
                    founded = e;
                    break;
                }
            }
            if (founded != null) {
                queue.remove(founded);
            }
        }
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
