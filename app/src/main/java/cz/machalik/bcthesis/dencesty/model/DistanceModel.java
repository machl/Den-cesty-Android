package cz.machalik.bcthesis.dencesty.model;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Lukáš Machalík
 */
public class DistanceModel {

    protected static final String TAG = "DistanceModel";

    private Checkpoint[] checkpoints;
    private Date startTime;

    private int distance = 0;
    private double avgSpeed = 0.0;
    private int lastCheckpoint = 0;

    public DistanceModel() { }

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

    public void onLocationChanged(Location location) {
        //mCurrentLocation = location;
        int newDistance = calculateDistance(location);

        if (newDistance > this.distance) {
            this.distance = newDistance;
            this.avgSpeed = calculateAvgSpeed(location);
            // TODO: notify distance changed pro účely mapy
        } else {
            // TODO: upozorneni na sejiti z trasy (po nekolika location updatech mimo)
        }
    }

    private int calculateDistance(Location location) {
        if (!location.hasAccuracy() || location.getAccuracy() > 200 ) {
            // inaccurate location updates filter
            return 0;
        }

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

    public static class Checkpoint {

        public final int id;
        public final int meters;
        public final double latitude;
        public final double longitude;

        public Checkpoint(int id, int meters, double latitude, double longitude) {
            this.id = id;
            this.meters = meters;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
