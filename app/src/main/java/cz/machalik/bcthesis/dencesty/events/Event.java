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
 * Lukáš Machalík
 */
public class Event implements Serializable {

    // Types of event:
    public static final String EVENTTYPE_LOGIN = "LoginSuccess";
    public static final String EVENTTYPE_STARTRACE = "StartRace";
    public static final String EVENTTYPE_STOPRACE = "StopRace";
    public static final String EVENTTYPE_LOCATIONUPDATE = "LocationUpdate";
    public static final String EVENTTYPE_CHECKPOINT = "Checkpoint";
    public static final String EVENTTYPE_USERREFRESH = "UserRefresh";
    public static final String EVENTTYPE_ERROR = "Error";
    public static final String EVENTTYPE_WARNING = "Warning";
    public static final String EVENTTYPE_LOG = "Log";

    private static final int UNKNOWN_RACE_ID = 0;

    private static final String EVENT_ID_COUNTER_KEY = "cz.machalik.bcthesis.dencesty.Event.eventIdCounter";

    private final int eventId;
    private final int raceId;
    private final String type;
    private final int batteryLevel;
    private final int batteryState;
    private final String timestamp;
    private final Map extras;

    public Event(Context context, String type) {
        this(context, UNKNOWN_RACE_ID, type);
    }

    public Event(Context context, int raceId, String type) {
        this.type = type;
        this.raceId = raceId;
        this.extras = new HashMap();

        // Obtain battery info:
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1); // TODO: co je to scale? kdy se meni? nemeni se pripojenim powerbanky?
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

    public JSONObject toJSONObject() {
        JSONObject ret = new JSONObject();

        try {
            ret.put("eventId", eventId);
            ret.put("raceid", raceId);
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

    @Override
    public String toString() {
        return "Event[" + eventId + "] " + raceId + " " + type + " " + timestamp + " " +
                batteryLevel + " " + batteryState + " " + extras.toString();
    }

    public int getEventId() {
        return eventId;
    }

    public Map getExtras() {
        return extras;
    }
}
