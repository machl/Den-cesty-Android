package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.machalik.bcthesis.dencesty.events.Event;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.location.BackgroundLocationService;
import cz.machalik.bcthesis.dencesty.other.FileLogger;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Lukáš Machalík
 */
public class RaceModel {
    protected static final String TAG = "RaceModel";

    // It's singleton
    private static RaceModel ourInstance = new RaceModel();
    public static RaceModel getInstance() {
        return ourInstance;
    }
    private RaceModel() {} // TODO: ukládat info mezi spuštěními aplikace

    /**
     * True, if user is successfully logged.
     */
    private boolean isLogged = false;

    /**
     * Info about logged user.
     */
    private int walkerId;
    private String walkerName;
    private String walkerSurname;
    private String walkerUsername;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    /**
     * Number od location updates so far.
     */
    private static int numOfLocationUpdates = 0;


    public void startRace(Context context) {
        Log.i(TAG, "Starting location updates");
        BackgroundLocationService.start(context);
    }

    public void stopRace(Context context) {
        Log.i(TAG, "Stopping location updates");
        BackgroundLocationService.stop(context);
    }

    public void onLocationChanged(Context context, Location location) {
        mCurrentLocation = location;

        Float course = location.hasBearing() ? location.getBearing() : -1;
        Double altitude = location.hasAltitude() ? location.getAltitude() : -1;
        Integer counter = numOfLocationUpdates++;
        Float speed = location.hasSpeed() ? location.getSpeed() : -1;
        Float verAcc = -1f; // Android does not provide vertical accuracy information
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();
        Float horAcc = location.hasAccuracy() ? location.getAccuracy() : -1;
        String provider = location.getProvider() != null ? location.getProvider() : "notset";

        Date date = new Date(location.getTime());
        String timestamp = WebAPI.DATE_FORMAT.format(date);

        /*String info = "Location changed: " + counter + ' ' + provider + ' ' + latitude + ' ' +
                longitude + ' ' + altitude + ' ' + speed + ' ' + course + ' ' + horAcc +
                ' ' + verAcc + ' ' + timestamp;
        //Log.i(TAG, info);
        FileLogger.log(TAG, info);*/

        Map dataMap = new HashMap(10);
        dataMap.put("latitude", latitude);
        dataMap.put("longitude", longitude);
        dataMap.put("altitude", altitude);
        dataMap.put("course", course);
        dataMap.put("speed", speed);
        dataMap.put("horAcc", horAcc);
        dataMap.put("verAcc", verAcc);
        dataMap.put("timestamp", timestamp);
        dataMap.put("counter", counter);
        dataMap.put("provider", provider);

        Event event = new Event(context, Event.EVENTTYPE_LOCATIONUPDATE, dataMap);

        EventUploaderService.startActionAddEvent(context, event);
        EventUploaderService.startActionUpload(context);
    }

    public boolean login(Context context, String email, String password) {
        JSONObject jsonResponse = WebAPI.synchronousLoginHandlerRequest(email, password);

        if (jsonResponse != null) {
            boolean success = jsonResponse.optBoolean("success");
            if (success) {
                onSuccessfulLogin(context, jsonResponse);
                return true;
            } else {
                Log.i(TAG, "Login: wrong email or password");
                return false;
            }
        }

        return false;
    }

    private void onSuccessfulLogin(Context context, JSONObject jsonData) {
        if (!jsonData.has("id") || !jsonData.has("name") || !jsonData.has("surname") || !jsonData.has("username")) {
            String message = "Response login missing info";
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            return;
        }

        this.walkerId = jsonData.optInt("id");
        this.walkerName = jsonData.optString("name");
        this.walkerSurname = jsonData.optString("surname");
        this.walkerUsername = jsonData.optString("username");
        this.isLogged = true;

        Map dataMap = new HashMap(10);
        dataMap.put("systemName", Build.VERSION.RELEASE + " " + Build.VERSION.CODENAME);
        dataMap.put("sdk", Integer.valueOf(Build.VERSION.SDK_INT));
        dataMap.put("model", Build.MODEL);
        // TODO: info o povolených polohových službách, internetu ...?

        Event event = new Event(context, Event.EVENTTYPE_LOGIN, dataMap);

        EventUploaderService.startActionAddEvent(context, event);
        EventUploaderService.startActionUpload(context);
    }


    public int getWalkerId() {
        return walkerId;
    }

    public String getWalkerUsername() {
        return walkerUsername;
    }

    public String getWalkerFullName() {
        return walkerName + " " + walkerSurname;
    }
}