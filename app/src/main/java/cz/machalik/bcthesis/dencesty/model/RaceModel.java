package cz.machalik.bcthesis.dencesty.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

import cz.machalik.bcthesis.dencesty.events.Event;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.location.BackgroundLocationService;
import cz.machalik.bcthesis.dencesty.model.DistanceModel.Checkpoint;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Lukáš Machalík
 */
public class RaceModel {

    protected static final String TAG = "RaceModel";

    /****************************** Public constants: ******************************/

    public static final String ACTION_RACE_INFO_CHANGED = "cz.machalik.bcthesis.dencesty.action.ACTION_RACE_INFO_CHANGED";


    /****************************** Public API: ******************************/

    public RaceModel() { // TODO: another info? int change to JSONData?
    }

    public boolean init(int raceId) {
        JSONObject response = WebAPI.synchronousRaceDataRequest(raceId, User.getWalkerId());

        if (response == null || !response.has("race") || !response.has("checkpoints")) {
            return false;
        }

        // Process race_info:
        final JSONObject raceInfo = response.optJSONObject("race");
        this.raceId = raceInfo.optInt("id");

        // Process scoreboard:
        final JSONObject scoreboard = response.optJSONObject("scoreboard");
        if (scoreboard != null && scoreboard.length() > 0) {
            this.isStarted = (1 == scoreboard.optInt("raceState"));
        } else {
            this.isStarted = false;
        }

        this.distanceModel.init(response);

        return true;
    }

    public void startRace(Context context) {
        if (!this.isStarted) {
            // Create new start race event
            Event event = new Event(context, User.getWalkerId(), this.raceId, Event.EVENTTYPE_STARTRACE);
            event.getExtras().put("updateInterval", BackgroundLocationService.UPDATE_INTERVAL_IN_MILLISECONDS);
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

            this.isStarted = true;
        }
    }

    public void stopRace(Context context) {
        this.isStarted = false;

        // Stop background location service
        boolean wasRunning = BackgroundLocationService.stop(context);

        // Create new stop race event
        if (wasRunning) {
            Event event = new Event(context, User.getWalkerId(), this.raceId, Event.EVENTTYPE_STOPRACE);
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
        // May be not inited yet
        return distanceModel != null ? distanceModel.getDistance() : 0;
    }

    public double getRaceAvgSpeed() {
        // May be not inited yet
        return distanceModel != null ? distanceModel.getAvgSpeed() : 0;
    }

    public int getLocationUpdatesCounter() {
        return locationUpdatesCounter;
    }


    /****************************** Private: ******************************/

    private int raceId;
    private boolean isStarted = false;
    private DistanceModel distanceModel = new DistanceModel();

    /**
     * Number od location updates so far.
     */
    private int locationUpdatesCounter = 0;

    private BroadcastReceiver mLocationChangedReceiver;


    private void onLocationChanged(Context context, Location location) {
        distanceModel.onLocationChanged(location);

        fireLocationUpdateEvent(context, location);
        notifySomeRaceInfoChanged(context);
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
        String timestamp = WebAPI.DATE_FORMAT_UPLOAD.format(date);

        /*String info = "Location changed: " + counter + ' ' + provider + ' ' + latitude + ' ' +
                longitude + ' ' + altitude + ' ' + speed + ' ' + course + ' ' + horAcc +
                ' ' + verAcc + ' ' + timestamp;
        //Log.i(TAG, info);
        FileLogger.log(TAG, info);*/

        Event event = new Event(context, User.getWalkerId(), this.raceId, Event.EVENTTYPE_LOCATIONUPDATE);
        event.getExtras().put("latitude", latitude);
        event.getExtras().put("longitude", longitude);
        event.getExtras().put("altitude", altitude);
        event.getExtras().put("course", course);
        event.getExtras().put("speed", speed);
        event.getExtras().put("horAcc", horAcc);
        event.getExtras().put("verAcc", verAcc);
        event.getExtras().put("timestamp", timestamp);
        event.getExtras().put("counter", counter);
        event.getExtras().put("provider", provider);
        event.getExtras().put("distance", getRaceDistance());
        event.getExtras().put("avgSpeed", getRaceAvgSpeed());
        event.getExtras().put("lastCheckpoint", distanceModel.getLastCheckpoint());

        EventUploaderService.addEvent(context, event);
        EventUploaderService.performUpload(context);
    }

    private void notifySomeRaceInfoChanged(Context context) {
        Intent intent = new Intent(ACTION_RACE_INFO_CHANGED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
