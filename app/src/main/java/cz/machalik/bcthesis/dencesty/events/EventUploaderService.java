package cz.machalik.bcthesis.dencesty.events;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import cz.machalik.bcthesis.dencesty.other.FileLogger;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 *
 * Inspired by:
 * http://developer.android.com/guide/components/services.html
 * <p/>
 *
 * Lukáš Machalík
 */
public class EventUploaderService extends IntentService {
    protected static final String TAG = "EventUploaderService";

    private static final String ACTION_ADD_EVENT = "cz.machalik.bcthesis.dencesty.action.ADD_EVENT";
    private static final String ACTION_UPLOAD = "cz.machalik.bcthesis.dencesty.action.UPLOAD";
    private static final String ACTION_REMOVE_EVENTS = "cz.machalik.bcthesis.dencesty.action.REMOVE_EVENTS";

    private static final String EXTRA_EVENT = "cz.machalik.bcthesis.dencesty.extra.EVENT";
    private static final String EXTRA_IDS = "cz.machalik.bcthesis.dencesty.extra.IDS";

    private static EventQueue eventQueue = new EventQueue();

    /**
     * Starts this service to perform action AddEvent with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionAddEvent(Context context, Event event) {
        Intent intent = new Intent(context, EventUploaderService.class);
        intent.setAction(ACTION_ADD_EVENT);
        intent.putExtra(EXTRA_EVENT, event);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Upload with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpload(Context context) {
        Intent intent = new Intent(context, EventUploaderService.class);
        intent.setAction(ACTION_UPLOAD);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action RemoveEvents with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionRemoveEvents(Context context, int[] ids) {
        Intent intent = new Intent(context, EventUploaderService.class);
        intent.setAction(ACTION_REMOVE_EVENTS);
        intent.putExtra(EXTRA_IDS, ids);
        context.startService(intent);
    }

    public EventUploaderService() {
        super("EventUploaderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_ADD_EVENT.equals(action)) {
                final Event event = (Event) intent.getSerializableExtra(EXTRA_EVENT);
                handleActionAddEvent(event);
            } else if (ACTION_UPLOAD.equals(action)) {
                handleActionUpload();
            }else if (ACTION_REMOVE_EVENTS.equals(action)) {
                final int[] ids = intent.getIntArrayExtra(EXTRA_IDS);
                handleActionRemoveEvents(ids);
            }
        }
    }

    /**
     * Handle action AddEvent in the provided background thread with the provided
     * parameters.
     */
    private void handleActionAddEvent(Event event) {
        Log.i(TAG, "New event: " + event.toString());
        FileLogger.log(TAG, "New event: " + event.toString());
        eventQueue.add(event);
    }

    /**
     * Handle action Upload in the provided background thread with the provided
     * parameters.
     */
    private void handleActionUpload() {
        if (eventQueue.size() > 0) {
            JSONArray json = eventQueue.toJSONArray();
            //Log.i(TAG, "JSON Request: " + json.toString());

            JSONObject jsonResponse = WebAPI.synchronousEventHandlerRequest(json);
            if (jsonResponse != null) {
                //Log.i(TAG, "JSON Response: " + jsonResponse.toString());
                JSONArray savedIdsJsonArray = jsonResponse.optJSONArray("savedEventIds");

                if (savedIdsJsonArray != null) {
                    try {

                        int len = savedIdsJsonArray.length();
                        int[] savedIds = new int[len];
                        for (int i = 0; i < len; i++) {
                            savedIds[i] = savedIdsJsonArray.getInt(i);
                        }

                        EventUploaderService.startActionRemoveEvents(this, savedIds);

                    } catch (JSONException e) {
                        String message = "Event upload response: JSONException: " + e.getLocalizedMessage();
                        Log.e(TAG, message);
                        FileLogger.log(TAG, message);
                        e.printStackTrace();
                    }
                } else {
                    String message = "Event upload response: Wrong response: " + jsonResponse.toString();
                    Log.e(TAG, message);
                    FileLogger.log(TAG, message);
                }
            }
        } else {
            Log.i(TAG, "Event queue upload failed: Queue is empty!");
        }
    }

    /**
     * Handle action RemoveEvents in the provided background thread with the provided
     * parameters.
     */
    private void handleActionRemoveEvents(int[] ids) {
        String message = "Removing events: " + Arrays.toString(ids);
        Log.i(TAG, message);
        FileLogger.log(TAG, message);

        eventQueue.remove(ids);
    }


}
