package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Lukáš Machalík
 */
public class DistanceModel {

    protected static final String TAG = "DistanceModel";

    public static final String ACTION_DISTANCE_CHANGED = "cz.machalik.bcthesis.dencesty.action.ACTION_DISTANCE_CHANGED";

    private int distance = 0;
    private double avgSpeed = 0.0;
    private int lastCheckpoint = 0;
    private Date startTime;
    private Checkpoint[] checkpoints;

    private static Location lastKnownLocation = null;

    // off the route detection
    private int lastDistanceToNextCheckpoint = 0;
    private int offRouteUpdatesCounter = 0;

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
            onOffRouteLocationUpdate(location);
        }
    }

    private int calculateDistance(Location location) {

        for (int i = lastCheckpoint; i < checkpoints.length - 1; i++) {
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
        // TODO: off road detection
    }

    private void onOffRouteLocationUpdate(Location location) {
        // TODO: off road detection
    }
}
