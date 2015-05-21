package cz.machalik.bcthesis.dencesty.model;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.events.Event;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.location.BackgroundLocationService;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Represents actual race progress and race state for current user.
 *
 * It has to be initialized with init method call on background thread (not UI thread!).
 *
 * @author Lukáš Machalík
 */
public class RaceModel {

    protected static final String TAG = "RaceModel";

    /****************************** Public constants: ******************************/

    /**
     * Current user's progress changed broadcast intent action.
     */
    public static final String ACTION_RACE_INFO_CHANGED = "cz.machalik.bcthesis.dencesty.action.ACTION_RACE_INFO_CHANGED";

    /**
     * Time interval before official race start to allow user start race (capturing location).
     * In seconds.
     */
    public static final int TIMEINTERVAL_BEFORE_RACE_START_TO_ALLOW_START_LOCATION = 10 * 60;

    /****************************** Public API: ******************************/

    /**
     * Creates empty RaceModel. To proper initialization, call init from download thread.
     */
    public RaceModel() {}

    /**
     * Init RaceModel with data from a server (synchronously). You have to run this method
     * from background thread (eg. download thread with AsyncTask).
     * @param raceId Race ID of race
     */
    public boolean init(Context context, int raceId) {
        JSONObject response = WebAPI.synchronousRaceDataRequest(raceId, User.get().getWalkerId());

        if (response == null || !response.has("race") || !response.has("checkpoints")) {
            return false;
        }

        // Process race_info:
        final JSONObject raceInfo = response.optJSONObject("race");
        this.raceId = raceInfo.optInt("id");
        this.startTime = null;
        this.finishTime = null;
        try {
            this.startTime = WebAPI.DATE_FORMAT_DOWNLOAD.parse(raceInfo.optString("start_time"));
            this.finishTime = WebAPI.DATE_FORMAT_DOWNLOAD.parse(raceInfo.optString("finish_time"));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        this.distanceModel.init(response);

        // Process scoreboard:
        final JSONObject scoreboard = response.optJSONObject("scoreboard");
        if (scoreboard != null && scoreboard.length() > 0) {
            this.isStarted = (1 == scoreboard.optInt("raceState"));

            if (this.isStarted) {
                if (isRaceAbleToStart()) {
                    // Restart race (location service)
                    Log.d(TAG, "Restarting race automatically from init");

                    Event event = new Event(context, User.get().getWalkerId(), this.raceId, Event.EVENTTYPE_LOG);
                    event.getExtras().put("msg", "Restarting race automatically");
                    EventUploaderService.addEvent(context, event);
                    EventUploaderService.performUpload(context);

                    startLocationService(context);
                } else {
                    // End race manually
                    Log.d(TAG, "Ending race automatically from init");
                    checkFinishOnBackground(context);
                }
            }
        } else {
            this.isStarted = false;
        }

        return true;
    }

    /**
     * Starts race and starts capturing location.
     * May fail and show alert dialog when time is before official start time or race time is over.
     */
    public void startRace(Activity activity) {

        if (!this.isStarted && BackgroundLocationService.isLocationProviderEnabled(activity)) {

            if (isRaceAbleToStart()) {
                Context context = activity;

                // Create new start race event
                Event event = new Event(context, User.get().getWalkerId(), this.raceId, Event.EVENTTYPE_STARTRACE);
                event.getExtras().put("updateInterval", BackgroundLocationService.UPDATE_INTERVAL_IN_MILLISECONDS);
                EventUploaderService.addEvent(context, event);
                EventUploaderService.performUpload(context);

                startLocationService(context);

                if (!isTimeInRace()) {
                    new AlertDialog.Builder(activity)
                            .setTitle(activity.getString(R.string.start_race_alert_before_title))
                            .setMessage(activity.getString(R.string.start_race_alert_before_message))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }

            } else {
                Date now = new Date();
                if (now.before(this.startTime)) {

                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                    String startTimeString = dateFormat.format(this.startTime);

                    new AlertDialog.Builder(activity)
                            .setTitle(activity.getString(R.string.start_race_alert_soon_title))
                            .setMessage(String.format(activity.getString(R.string.start_race_alert_soon_message), startTimeString))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();

                } else if (now.after(this.finishTime)) {

                    new AlertDialog.Builder(activity)
                            .setTitle(activity.getString(R.string.start_race_alert_finished_title))
                            .setMessage(activity.getString(R.string.start_race_alert_finished_message))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();

                }
            }

        }
    }

    /**
     * Stops race and stops capturing location.
     */
    public void stopRace(Context context) {
        Log.d(TAG, "StopRace called");
        if (this.isStarted) {
            this.isStarted = false;

            // Stop background location service
            boolean wasRunning = BackgroundLocationService.stop(context);

            // Create new stop race event
            //if (wasRunning) {
                Event event = new Event(context, User.get().getWalkerId(), this.raceId, Event.EVENTTYPE_STOPRACE);
                EventUploaderService.addEvent(context, event);
                EventUploaderService.performUpload(context);
            //}

            LocalBroadcastManager.getInstance(context).unregisterReceiver(mLocationChangedReceiver);
        }
    }

    /**
     * Manually checks if race time is over. Call this method when app is becoming to foreground etc.
     * May show dialog that informs user about race finishing.
     */
    public void checkFinishFromActivity(Activity activity) {
        Log.d(TAG, "Checking finish from activity.");
        checkFinishOnBackground(activity);

        if (this.showEndRaceAlert) {
            new AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.auto_race_ended_title))
                    .setMessage(activity.getString(R.string.auto_race_ended_message))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            this.showEndRaceAlert = false;
        }
    }

    /**
     * Return true if race is in progress (if user starts race in this app).
     * @return true if race is in progress
     */
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Returns current Race ID.
     * @return current race id
     */
    public int getRaceId() {
        return raceId;
    }

    /**
     * Returns current user's elapsed distance in race.
     * @return elapsed distance in meters
     */
    public int getRaceDistance() {
        // May be not inited yet
        return distanceModel != null ? distanceModel.getDistance() : 0;
    }

    /**
     * Returns current user's average speed in race.
     * @return average speed in km/h
     */
    public double getRaceAvgSpeed() {
        // May be not inited yet
        return distanceModel != null ? distanceModel.getAvgSpeed() : 0;
    }

    /**
     * Checks if current time is between official race start time and finish time, or it is certain
     * time interval (10 minutes) before start time.
     * @return true, if user is allowed to start race
     */
    public boolean isRaceAbleToStart() {
        Date now = new Date();
        long deltaSeconds = (now.getTime() - this.startTime.getTime()) / 1000;
        if (deltaSeconds < -TIMEINTERVAL_BEFORE_RACE_START_TO_ALLOW_START_LOCATION) {
            return false;
        }
        if (now.after(this.finishTime)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if current time is between official race start time and finish time.
     * @return true, if real race is in progress
     */
    public boolean isTimeInRace() {
        Date now = new Date();
        if (now.before(this.startTime)) {
            return false;
        }
        if (now.after(this.finishTime)) {
            return false;
        }

        return true;
    }

    /**
     * Returns number of location updates so far.
     * @return location updates count
     */
    public int getLocationUpdatesCounter() {
        return locationUpdatesCounter;
    }

    /**
     * Returns race route.
     * @return race route
     */
    public Checkpoint[] getCheckpoints() {
        return this.distanceModel.getCheckpoints();
    }

    /****************************** Private: ******************************/

    private int raceId;
    private boolean isStarted = false;
    private Date startTime = null;
    private Date finishTime = null;
    private DistanceModel distanceModel = new DistanceModel();

    /**
     * Number od location updates so far.
     */
    private int locationUpdatesCounter = 0;

    /**
     * Receiver for location updates from BackgroundLocationService.
     */
    private BroadcastReceiver mLocationChangedReceiver;

    /**
     * If true, it will show automatic end race alert dialog when app becomes foreground.
     */
    private boolean showEndRaceAlert = false;

    /**
     * Starts capturing location updates.
     */
    private void startLocationService(Context context) {
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

    /**
     * Called when new location updates arrives.
     * @param context context of location update source
     * @param location location update
     */
    private void onLocationChanged(Context context, Location location) {
        if (isTimeInRace()) {
            distanceModel.onLocationChanged(context, location);
        }

        fireLocationUpdateEvent(context, location);

        // Stop if race is over.
        checkFinishOnBackground(context);

        notifySomeRaceInfoChanged(context);
    }

    /**
     * Creates and sends new Event of type LocationUpdate to server.
     */
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

        Event event = new Event(context, User.get().getWalkerId(), this.raceId, Event.EVENTTYPE_LOCATIONUPDATE);
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
        event.getExtras().put("offRouteCounter", distanceModel.getOffRouteUpdatesCounter());

        EventUploaderService.addEvent(context, event);
        EventUploaderService.performUpload(context);
    }

    /**
     * Checks if race time is over.
     */
    private void checkFinishOnBackground(Context context) {
        if (isStarted() && !isRaceAbleToStart()) {
            Log.d(TAG, "checkFinishOnBackground stopping race");
            stopRace(context);
            this.showEndRaceAlert = true;
        }
    }

    /**
     * Fires message to controllers that user's race progress has changed.
     */
    private void notifySomeRaceInfoChanged(Context context) {
        Intent intent = new Intent(ACTION_RACE_INFO_CHANGED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
