package cz.machalik.bcthesis.dencesty.events;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import cz.machalik.bcthesis.dencesty.other.FileLogger;

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

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_ADD_EVENT = "cz.machalik.bcthesis.dencesty.action.ADD_EVENT";
    //private static final String ACTION_FOO = "cz.machalik.bcthesis.dencesty.action.FOO";

    private static final String EXTRA_EVENT = "cz.machalik.bcthesis.dencesty.extra.EVENT";
    //private static final String EXTRA_PARAM1 = "cz.machalik.bcthesis.dencesty.extra.PARAM1";
    //private static final String EXTRA_PARAM2 = "cz.machalik.bcthesis.dencesty.extra.PARAM2";

    /**
     * Starts this service to perform action AddEvent with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionAddEvent(Context context, String event) {
        // TODO: zmenit String parament na Event
        Intent intent = new Intent(context, EventUploaderService.class);
        intent.setAction(ACTION_ADD_EVENT);
        intent.putExtra(EXTRA_EVENT, event);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    /*public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, EventUploaderService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }*/

    public EventUploaderService() {
        super("EventUploaderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_ADD_EVENT.equals(action)) {
                final String event = intent.getStringExtra(EXTRA_EVENT);
                handleActionAddEvent(event);
            } /*else if (ACTION_FOO.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            }*/
        }
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionAddEvent(String event) {
        Log.i(TAG, "New event: " + event);
        FileLogger.log(TAG, "New event: " + event);
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    /*private void handleActionFoo(String param1, String param2) {
        throw new UnsupportedOperationException("Not yet implemented");
    }*/


}
