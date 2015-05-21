package cz.machalik.bcthesis.dencesty.events;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Event represents a message which is sended by {@link cz.machalik.bcthesis.dencesty.events.EventUploaderService} to a server.
 *
 * @author Lukáš Machalík
 */
public class Event implements Serializable {

    /**
     * Type of Event which contains data about success login.
     */
    public static final String EVENTTYPE_LOGIN = "LoginSuccess";
    /**
     * Type of Event which represents start race information given to a server.
     */
    public static final String EVENTTYPE_STARTRACE = "StartRace";
    /**
     * Type of Event which represents stop race information given to a server.
     */
    public static final String EVENTTYPE_STOPRACE = "StopRace";
    /**
     * Type of Event which holds location update data whose will be sended to a server.
     */
    public static final String EVENTTYPE_LOCATIONUPDATE = "LocationUpdate";
    /**
     * Type of Event which contains data about error.
     */
    public static final String EVENTTYPE_ERROR = "Error";
    /**
     * Type of Event which contains data about warning.
     */
    public static final String EVENTTYPE_WARNING = "Warning";
    /**
     * Type of Event which contains log information.
     */
    public static final String EVENTTYPE_LOG = "Log";

    /**
     * Default Race ID which is used for Events that occurs outside the duration of a race.
     */
    private static final int UNKNOWN_RACE_ID = 0;

    /**
     * Used as key in SharedPreferences to remember current Event ID available.
     */
    private static final String EVENT_ID_COUNTER_KEY = "cz.machalik.bcthesis.dencesty.Event.eventIdCounter";

    // Event data:
    private final int eventId;
    private final int walkerId;
    private final int raceId;
    private final String type;
    private final int batteryLevel;
    private final int batteryState;
    private final String timestamp;
    private final Map extras;

    /**
     * Creates Event with unknown Race ID. Use it only when NO race is selected yet.
     *
     * @param context A {@link Context} that will be used to construct the
     *      Event. The Context will not be held past the lifetime of this
     *      Builder object.
     * @param walkerId Walker ID representing an owner of Event.
     * @param type Type of Event. Use one of Event.EVENTTYPE_* constants.
     */
    public Event(Context context, int walkerId, String type) {
        this(context, walkerId, UNKNOWN_RACE_ID, type);
    }

    /**
     * Creates Event, which happened during a race.
     *
     * @param context A {@link Context} that will be used to construct the
     *      Event. The Context will not be held past the lifetime of this
     *      Builder object.
     * @param walkerId Walker ID representing an owner of Event.
     * @param raceId Race ID representing current race.
     * @param type Type of Event. Use one of Event.EVENTTYPE_* constants.
     */
    public Event(Context context, int walkerId, int raceId, String type) {
        this.type = type;
        this.walkerId = walkerId;
        this.raceId = raceId;
        this.extras = new HashMap();

        // Obtain battery info:
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        this.batteryLevel = (int) (((float)level / (float)scale) * 100.f);
        this.batteryState = WebAPI.convertBatteryStatus(status, plugged);

        // Add unique timestamp
        this.timestamp = WebAPI.DATE_FORMAT_UPLOAD.format(new Date());

        // Obtain proper event id from shared preferences:
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        this.eventId = sharedPreferences.getInt(EVENT_ID_COUNTER_KEY, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(EVENT_ID_COUNTER_KEY, this.eventId + 1);
        editor.commit();
    }

    /**
     * Converts Event to JSON representations.
     *
     * @return JSON representation of Event object.
     */
    public JSONObject toJSONObject() {
        JSONObject ret = new JSONObject();

        try {
            ret.put("eventId", eventId);
            ret.put("walkerId", walkerId);
            ret.put("raceId", raceId);
            ret.put("type", type);
            ret.put("data", new JSONObject(extras));
            ret.put("batL", batteryLevel);
            ret.put("batS", batteryState);
            ret.put("time", timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return ret;
    }

    /**
     * Return text representation of Event.
     * @return string representation
     */
    @Override
    public String toString() {
        return "Event[" + walkerId + "," + raceId + "," + eventId + "] " + type + " " + timestamp + " " +
                batteryLevel + " " + batteryState + " " + extras.toString();
    }

    /**
     * Returns Event ID of an Event.
     * @return Event ID
     */
    public int getEventId() {
        return eventId;
    }

    /**
     * Returns Map object with Event extras. Use it for storing additional data to Event
     * based on Event type.
     * @return Event extras data structure.
     */
    public Map getExtras() {
        return extras;
    }
}
