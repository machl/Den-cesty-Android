package cz.machalik.bcthesis.dencesty.events;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import cz.machalik.bcthesis.dencesty.model.RaceModel;

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
        this.batteryLevel = (level / scale) * 100;
        this.batteryState = convertBatteryStatusToWebApi(status, plugged);

        this.timestamp = RaceModel.dateFormat.format(new Date());
    }

    @Override
    public String toString() {
        return "Event[" + eventId + "] " + type + " " + timestamp + " " + batteryLevel + " " +
                batteryState + " " + data.toString();
    }

    // TODO: move to WebAPI
    private static int convertBatteryStatusToWebApi (int status, int plugged) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_UNKNOWN: return 0;
            case BatteryManager.BATTERY_STATUS_CHARGING: return 2;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return 1;
            case BatteryManager.BATTERY_STATUS_FULL: return 3;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                    return 4;
                } else {
                    return 1;
                }
            default: return 0;
        }
    }
}
