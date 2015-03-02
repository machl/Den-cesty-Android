package cz.machalik.bcthesis.dencesty.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

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

    private BroadcastReceiver mLocationChangedReceiver;


    public void startRace(Context context) {
        // Create new start race event
        Map dataMap = new HashMap(1);
        dataMap.put("updateInterval", BackgroundLocationService.UPDATE_INTERVAL_IN_MILLISECONDS);
        Event event = new Event(context, Event.EVENTTYPE_STARTRACE, dataMap);
        EventUploaderService.addEvent(context, event);

        // Register broadcast receiver on location updates
        mLocationChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BackgroundLocationService.ACTION_LOCATION_CHANGED)) {
                    onLocationChanged(context, BackgroundLocationService.getLastKnownLocation());
                }
            }
        };
        LocalBroadcastManager.getInstance(context)
                .registerReceiver(mLocationChangedReceiver,
                        new IntentFilter(BackgroundLocationService.ACTION_LOCATION_CHANGED));

        // Start background location service
        BackgroundLocationService.start(context);
    }

    public void stopRace(Context context) {
        // Stop background location service
        boolean wasRunning = BackgroundLocationService.stop(context);

        // Create new stop race event
        if (wasRunning) {
            Event event = new Event(context, Event.EVENTTYPE_STOPRACE, new HashMap(0));
            EventUploaderService.addEvent(context, event);
            EventUploaderService.performUpload(context);
        }

        LocalBroadcastManager.getInstance(context).unregisterReceiver(mLocationChangedReceiver);
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

        EventUploaderService.addEvent(context, event);
        EventUploaderService.performUpload(context);
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

    public static int getNumOfLocationUpdates() {
        return numOfLocationUpdates;
    }
}
