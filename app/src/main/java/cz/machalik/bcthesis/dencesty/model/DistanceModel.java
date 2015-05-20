package cz.machalik.bcthesis.dencesty.model;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Lukáš Machalík
 */
public class DistanceModel {

    protected static final String TAG = "DistanceModel";

    public static final int MIN_DISTANCE_TO_MARK_AS_OFF_THE_ROUTE_UPDATE = 300;
    public static final int MIN_NUMBER_OF_OFF_THE_ROUTE_UPDATES_TO_NOTIFY = 3;

    public static final String ACTION_DISTANCE_CHANGED = "cz.machalik.bcthesis.dencesty.action.ACTION_DISTANCE_CHANGED";

    private int distance = 0;
    private double avgSpeed = 0.0;
    private int lastCheckpoint = 0;
    private Date startTime;
    private Checkpoint[] checkpoints;

    private static Location lastKnownLocation = null;

    // off the route detection
    private float lastDistanceToNextCheckpoint = 0;
    private int offRouteUpdatesCounter = 0;
    private int offRouteNotificationCounter = 0;

    public DistanceModel() {
        lastKnownLocation = null;
    }

    public void init(JSONObject data) {
        final JSONObject raceInfo = data.optJSONObject("race");
        this.startTime = null;
        try {
            this.startTime = WebAPI.DATE_FORMAT_DOWNLOAD.parse(raceInfo.optString("start_time"));
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        final JSONObject scoreboard = data.optJSONObject("scoreboard");
        if (scoreboard != null) {
            this.distance = scoreboard.optInt("distance");
            this.avgSpeed = scoreboard.optDouble("avgSpeed");
            this.lastCheckpoint = scoreboard.optInt("lastCheckpoint");
        }

        // Process race_checkpoints:
        final JSONArray raceCheckpoints = data.optJSONArray("checkpoints");
        this.checkpoints = new Checkpoint[raceCheckpoints.length()];
        for (int i = 0; i < raceCheckpoints.length(); i++) {
            final JSONObject o = raceCheckpoints.optJSONObject(i);
            checkpoints[i] = new Checkpoint(o.optInt("checkid"),
                    o.optInt("meters"),
                    o.optDouble("latitude"),
                    o.optDouble("longitude"));
        }

        Arrays.sort(this.checkpoints, new Comparator<Checkpoint>() {
            @Override
            public int compare(Checkpoint lhs, Checkpoint rhs) {
                return lhs.id - rhs.id;
            }
        });
    }

    public void onLocationChanged(Context context, Location location) {
        if (!location.hasAccuracy() || location.getAccuracy() > 200 ) {
            // inaccurate location updates filter
            return;
        }

        int newDistance = calculateDistance(location);

        if (newDistance > this.distance) {
            this.distance = newDistance;
            this.avgSpeed = calculateAvgSpeed(location);
            notifyDistanceChanged(context, location);

            // on the route, reset off the route detection
            onOnRouteLocationUpdate(location);
        } else {
            // off the route
            onOffRouteLocationUpdate(context, location);
        }
    }

    private int calculateDistance(Location location) {

        int count = checkpoints.length - 1;
        int halfForward = lastCheckpoint + (count / 2);
        int limit = Math.min(count, halfForward);

        for (int i = lastCheckpoint; i < limit; i++) {
            Checkpoint last = checkpoints[i];
            Checkpoint next = checkpoints[i+1];

            float distanceLast = distanceBetween(last.latitude, last.longitude, location.getLatitude(), location.getLongitude());
            float distanceNext = distanceBetween(location.getLatitude(), location.getLongitude(), next.latitude, next.longitude);
            float distanceBetween = distanceBetween(last.latitude, last.longitude, next.latitude, next.longitude);

            if (distanceNext < distanceBetween && distanceLast < distanceBetween) {
                double progressBetween = ((double)distanceLast) / (double)(distanceLast + distanceNext);
                int realDistanceBetween = next.meters - last.meters;
                int newDistance = ((int)(progressBetween * realDistanceBetween)) + last.meters;

                this.lastCheckpoint = i;
                return newDistance;
            }
        }

        return 0;
    }

    private double calculateAvgSpeed(Location location) {
        long secondsSinceStart = (location.getTime() - startTime.getTime()) / 1000;
        return ((double)this.distance / secondsSinceStart) * 3.6;
    }

    private void notifyDistanceChanged(Context context, Location location) {
        lastKnownLocation = location;

        Intent intent = new Intent(ACTION_DISTANCE_CHANGED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public int getDistance() {
        return distance;
    }

    public double getAvgSpeed() {
        return avgSpeed;
    }

    public int getLastCheckpoint() {
        return lastCheckpoint;
    }

    private static float distanceBetween(double startLatitude, double startLongitude,
                                         double endLatitude, double endLongitude) {
        float[] results = new float[1];
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
        return results[0];
    }

    public Checkpoint[] getCheckpoints() {
        return checkpoints;
    }

    public static Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public int getOffRouteUpdatesCounter() {
        return offRouteUpdatesCounter;
    }

    private void onOnRouteLocationUpdate(Location location) {
        this.lastDistanceToNextCheckpoint = 0;
        this.offRouteUpdatesCounter = 0;
    }

    private void onOffRouteLocationUpdate(Context context, Location location) {
        Checkpoint nextCheckpoint = checkpoints[this.lastCheckpoint + 1];
        float distanceToNextCheckpoint = distanceBetween(location.getLatitude(), location.getLongitude(),
                                                        nextCheckpoint.latitude, nextCheckpoint.longitude);
        float distanceDelta = distanceToNextCheckpoint - this.lastDistanceToNextCheckpoint;

        if (distanceDelta > MIN_DISTANCE_TO_MARK_AS_OFF_THE_ROUTE_UPDATE) {
            this.lastDistanceToNextCheckpoint = distanceToNextCheckpoint;
            this.offRouteUpdatesCounter++;

            Log.d(TAG, "OFF THE ROUTE: lastDistanceToNextCheckpoint = " + this.lastDistanceToNextCheckpoint);
            Log.d(TAG, "OFF THE ROUTE: offRouteUpdatesCounter = " + this.offRouteUpdatesCounter);

            if (this.offRouteUpdatesCounter >= MIN_NUMBER_OF_OFF_THE_ROUTE_UPDATES_TO_NOTIFY) {
                sendOffTheRouteLocalNotification(context);
                this.offRouteUpdatesCounter = 0;
            }
        }
    }

    private void sendOffTheRouteLocalNotification(Context context) {

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(context.getString(R.string.notification_lost_title))
                        .setContentText(context.getString(R.string.notification_lost_text))
                        .setSound(alarmSound);

        // Increase an ID for the notification
        this.offRouteNotificationCounter++;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(this.offRouteNotificationCounter, mBuilder.build());
    }
}
