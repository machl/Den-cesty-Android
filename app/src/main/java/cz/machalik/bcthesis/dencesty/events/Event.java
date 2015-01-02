package cz.machalik.bcthesis.dencesty.events;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;
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

    private static int eventIdCounter = 0;

    private int eventId;
    private String type;
    private Map data;
    private int batteryLevel;
    private int batteryState;
    private String timestamp;

    public Event(Context context, String type, Map data) {
        this.type = type;
        this.data = data;

        this.eventId = eventIdCounter++; // TODO: ukládání i mezi spuštěními aplikace

        // Obtain battery info:
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1); // TODO: co je to scale? kdy se meni? nemeni se pripojenim powerbanky?
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        this.batteryLevel = (int) (((float)level / (float)scale) * 100.f);
        this.batteryState = WebAPI.convertBatteryStatus(status, plugged);

        this.timestamp = WebAPI.DATE_FORMAT.format(new Date());
    }

    public JSONObject toJSONObject() {
        JSONObject ret = new JSONObject();

        try {
            ret.put("eventId", eventId);
            ret.put("type", type);
            ret.put("data", new JSONObject(data));
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
        return "Event[" + eventId + "] " + type + " " + timestamp + " " + batteryLevel + " " +
                batteryState + " " + data.toString();
    }

    public int getEventId() {
        return eventId;
    }
}
