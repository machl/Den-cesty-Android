package cz.machalik.bcthesis.dencesty.events;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Queue that holds Events for upload to a server.
 *
 * @author Lukáš Machalík
 */
public class EventQueue {

    /**
     * Data structure.
     */
    private List<Event> queue;

    /**
     * Creates new empty EventQueue.
     */
    public EventQueue() {
        this.queue = new ArrayList<>();
    }

    /**
     * Adds Event at the end of this EventQueue.
     * @param event the Event to add.
     */
    public void add(Event event) {
        queue.add(event);
    }

    /**
     * Removes the first occurrences of the specified Event IDs.
     * @param ids array of Event IDs for remove
     */
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

    /**
     * Returns the number of elements in this EventQueue.
     * @return number of elements
     */
    public int size() {
        return queue.size();
    }

    /**
     * Converts EventQueue to JSON representation.
     * @return JSON representation of EventQueue with Events in JSON
     */
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
