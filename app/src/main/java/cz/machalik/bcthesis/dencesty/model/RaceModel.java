package cz.machalik.bcthesis.dencesty.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.machalik.bcthesis.dencesty.events.Event;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.location.BackgroundLocationService;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Lukáš Machalík
 */
public class RaceModel {
    protected static final String TAG = "RaceModel";

    /****************************** Public constants: ******************************/

    public static final String ACTION_RACE_INFO_CHANGED = "cz.machalik.bcthesis.dencesty.action.ACTION_RACE_INFO_CHANGED";


    /****************************** Public API: ******************************/

    public RaceModel(int raceId) { // TODO: another info? int change to JSONData?
        this.raceId = raceId;
    }

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

    public boolean isStarted() {
        return isStarted;
    }

    public int getRaceId() {
        return raceId;
    }

    public int getRaceDistance() {
        return raceDistance;
    }

    public double getRaceAvgSpeed() {
        return raceAvgSpeed;
    }

    public int getLocationUpdatesCounter() {
        return locationUpdatesCounter;
    }


    /****************************** Private: ******************************/

    private int raceId;
    private boolean isStarted = false;
    private int raceDistance = 0;
    private double raceAvgSpeed = 0.0;

    /**
     * Number od location updates so far.
     */
    private int locationUpdatesCounter = 0;

    private BroadcastReceiver mLocationChangedReceiver;


    private void onLocationChanged(Context context, Location location) {
        //mCurrentLocation = location;
        int newDistance = calculateDistance(location);

        if (newDistance > this.raceDistance) {
            this.raceDistance = newDistance;
            this.raceAvgSpeed = calculateAvgSpeed(location);
        } else {
            // TODO: upozorneni na sejiti z trasy (po nekolika location updatech mimo)
        }

        fireLocationUpdateEvent(context, location);
        notifySomeRaceInfoChanged(context);
    }

    private int calculateDistance(Location location) {
        // TODO: proper implementation
        return raceDistance + 50;
    }

    private double calculateAvgSpeed(Location location) {
        // TODO: proper implementation
        return raceAvgSpeed + 0.5;
    }

    private void fireLocationUpdateEvent(Context context, Location location) {
        Float course = location.hasBearing() ? location.getBearing() : -1;
        Double altitude = location.hasAltitude() ? location.getAltitude() : -1;
        Integer counter = locationUpdatesCounter++;
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
        dataMap.put("distance", this.raceDistance);
        dataMap.put("avgSpeed", this.raceAvgSpeed);

        Event event = new Event(context, Event.EVENTTYPE_LOCATIONUPDATE, dataMap);

        EventUploaderService.addEvent(context, event);
        EventUploaderService.performUpload(context);
    }

    private void notifySomeRaceInfoChanged(Context context) {
        Intent intent = new Intent(ACTION_RACE_INFO_CHANGED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
