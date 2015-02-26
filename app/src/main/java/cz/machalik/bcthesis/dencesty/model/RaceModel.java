package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
import android.location.Location;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.machalik.bcthesis.dencesty.activities.RaceActivity;
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
    protected boolean isLogged = false;

    /**
     * Info about logged user.
     */
    protected int walkerId;
    protected String walkerName;
    protected String walkerSurname;
    protected String walkerUsername;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    /**
     * Race info entries
     */
    protected int raceInfoDistance;
    protected double raceInfoAvgSpeed;
    protected int raceInfoNumWalkersAhead;
    protected int raceInfoNumWalkersBehind;
    protected int raceInfoNumWalkersEnded;
    protected JSONArray raceInfoWalkersAhead;
    protected JSONArray raceInfoWalkersBehind;

    /**
     * Number od location updates so far.
     */
    protected static int numOfLocationUpdates = 0;

    /**
     * Number of unsent messages so far.
     */
    protected static int numOfUnsentMessages = 0;


    public void startRace(Context context) {
        // Create new start race event
        Map dataMap = new HashMap(1);
        dataMap.put("updateInterval", BackgroundLocationService.UPDATE_INTERVAL_IN_MILLISECONDS);
        Event event = new Event(context, Event.EVENTTYPE_STARTRACE, dataMap);
        EventUploaderService.startActionAddEvent(context, event);

        // Start background location service
        //Log.i(TAG, "Starting location updates");
        BackgroundLocationService.start(context);
    }

    public void stopRace(Context context) {
        // Stop background location service
        //Log.i(TAG, "Stopping location updates");
        boolean wasRunning = BackgroundLocationService.stop(context);

        // Create new stop race event
        if (wasRunning) {
            Event event = new Event(context, Event.EVENTTYPE_STOPRACE, new HashMap(0));
            EventUploaderService.startActionAddEvent(context, event);
            EventUploaderService.startActionUpload(context);
        }
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
        RaceActivity.updateLocationCounter(context, numOfLocationUpdates);

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
                //Log.i(TAG, "Login: wrong email or password");
                return false;
            }
        }

        return false;
    }

    protected void onSuccessfulLogin(Context context, JSONObject jsonData) {
        if (!jsonData.has("id") || !jsonData.has("name") || !jsonData.has("surname") || !jsonData.has("username")) {
            String message = "Response login missing info";
            //Log.e(TAG, message);
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

    public boolean fetchRaceInfo(Context context) {
        JSONObject jsonResponse = WebAPI.synchronousRaceInfoUpdateRequest();

        if (jsonResponse != null) {
            onFetchedRaceInfo(context, jsonResponse);
            return true;
        }

        return false;
    }

    protected void onFetchedRaceInfo(Context context, JSONObject jsonData) {
        if (!jsonData.has("distance") || !jsonData.has("speed") || !jsonData.has("numWalkersAhead") ||
                !jsonData.has("numWalkersBehind") || !jsonData.has("numWalkersEnded") ||
                !jsonData.has("walkersAhead") || !jsonData.has("walkersBehind")) {
            String message = "Response RaceInfo missing some entries";
            //Log.e(TAG, message);
            FileLogger.log(TAG, message);
            return;
        }

        this.raceInfoDistance = jsonData.optInt("distance");
        this.raceInfoAvgSpeed = jsonData.optDouble("speed");
        this.raceInfoNumWalkersAhead = jsonData.optInt("numWalkersAhead");
        this.raceInfoNumWalkersBehind = jsonData.optInt("numWalkersBehind");
        this.raceInfoNumWalkersEnded = jsonData.optInt("numWalkersEnded");
        this.raceInfoWalkersAhead = jsonData.optJSONArray("walkersAhead");
        this.raceInfoWalkersBehind = jsonData.optJSONArray("walkersBehind");
    }


    /*** GETTERS & SETTERS ***/

    public boolean isLogged() {
        return isLogged;
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

    public int getRaceInfoDistance() {
        return raceInfoDistance;
    }

    public double getRaceInfoAvgSpeed() {
        return raceInfoAvgSpeed;
    }

    public int getRaceInfoNumWalkersAhead() {
        return raceInfoNumWalkersAhead;
    }

    public int getRaceInfoNumWalkersBehind() {
        return raceInfoNumWalkersBehind;
    }

    public int getRaceInfoNumWalkersEnded() {
        return raceInfoNumWalkersEnded;
    }

    public JSONArray getRaceInfoWalkersAhead() {
        return raceInfoWalkersAhead;
    }

    public JSONArray getRaceInfoWalkersBehind() {
        return raceInfoWalkersBehind;
    }

    public static void setNumOfUnsentMessages(int numOfUnsentMessages) {
        RaceModel.numOfUnsentMessages = numOfUnsentMessages;
    }

    public static int getNumOfUnsentMessages() {
        return numOfUnsentMessages;
    }

    public static int getNumOfLocationUpdates() {
        return numOfLocationUpdates;
    }
}
