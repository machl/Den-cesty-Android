package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

import cz.machalik.bcthesis.dencesty.other.FileLogger;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Lukáš Machalík
 */
public class WalkersModel {

    protected static final String TAG = "WalkersModel";

    /****************************** Public constants: ******************************/

    public static final String ACTION_WALKERS_DID_REFRESHED = "cz.machalik.bcthesis.dencesty.action.ACTION_WALKERS_DID_REFRESHED";


    /****************************** Public API: ******************************/

    public boolean fetchWalkersFromWeb(Context context) {
        JSONObject jsonResponse = WebAPI.synchronousWalkersListRequest(this.raceId, User.get().getWalkerId());

        if (jsonResponse != null) {
            initializeWalkers(jsonResponse);
            notifyWalkersDidRefreshed(context);
            return true;
        }

        return false;
    }

    public Walker getPresentWalker() {
        return presentWalker;
    }

    public Walker[] getWalkersAhead() {
        return walkersAhead;
    }

    public Walker[] getWalkersBehind() {
        return walkersBehind;
    }

    public int getNumWalkersAhead() {
        return numWalkersAhead;
    }

    public int getNumWalkersBehind() {
        return numWalkersBehind;
    }

    public int getNumWalkersEnded() {
        return numWalkersEnded;
    }


    /****************************** Private: ******************************/

    private int raceId;

    /**
     * Race info entries
     */
    private Walker presentWalker;
    private int numWalkersAhead;
    private int numWalkersBehind;
    private int numWalkersEnded;
    private Walker[] walkersAhead;
    private Walker[] walkersBehind;

    public WalkersModel(int raceId) {
        this.raceId = raceId;

        // Basic init:
        this.presentWalker = new Walker(User.get().getWalkerFullName(),
                                        0,
                                        0,
                                        RaceState.NOTSTARTED,
                                        0,
                                        0,
                                        null);
        this.walkersAhead = new Walker[0];
        this.walkersBehind = new Walker[0];
    }

    private void initializeWalkers(JSONObject jsonData) {
        if (!jsonData.has("distance") || !jsonData.has("speed") || !jsonData.has("numWalkersAhead") ||
                !jsonData.has("numWalkersBehind") || !jsonData.has("numWalkersEnded") ||
                !jsonData.has("walkersAhead") || !jsonData.has("walkersBehind")) {
            String message = "Response RaceInfo missing some entries";
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            return;
        }

        try {
            Date presentWalkerUpdatedAt = null;
            if (jsonData.has("updated_at")) {
                presentWalkerUpdatedAt = WebAPI.DATE_FORMAT_DOWNLOAD.parse(jsonData.optString("updated_at"));
            }

            presentWalker = new Walker(User.get().getWalkerFullName(),
                    jsonData.optInt("distance"),
                    jsonData.optDouble("speed"),
                    jsonData.optInt("raceState"),
                    jsonData.optDouble("latitude", 0),
                    jsonData.optDouble("longitude", 0),
                    presentWalkerUpdatedAt);

            numWalkersAhead = jsonData.optInt("numWalkersAhead");
            numWalkersBehind = jsonData.optInt("numWalkersBehind");
            numWalkersEnded = jsonData.optInt("numWalkersEnded");

            final JSONArray walkersAheadJsonArray = jsonData.optJSONArray("walkersAhead");
            walkersAhead = new Walker[walkersAheadJsonArray.length()];
            for (int i = 0; i < walkersAheadJsonArray.length(); i++) {
                final JSONObject o = walkersAheadJsonArray.optJSONObject(i);
                walkersAhead[i] = new Walker(o.optString("name"),
                                             o.optInt("distance"),
                                             o.optDouble("speed"),
                                             o.optInt("raceState"),
                                             o.optDouble("latitude", 0),
                                             o.optDouble("longitude", 0),
                                             WebAPI.DATE_FORMAT_DOWNLOAD.parse(o.optString("updated_at")));
            }

            final JSONArray walkersBehindJsonArray = jsonData.optJSONArray("walkersBehind");
            walkersBehind = new Walker[walkersBehindJsonArray.length()];
            for (int i = 0; i < walkersBehindJsonArray.length(); i++) {
                final JSONObject o = walkersBehindJsonArray.optJSONObject(i);
                walkersBehind[i] = new Walker(o.optString("name"),
                                              o.optInt("distance"),
                                              o.optDouble("speed"),
                                              o.optInt("raceState"),
                                              o.optDouble("latitude", 0),
                                              o.optDouble("longitude", 0),
                                              WebAPI.DATE_FORMAT_DOWNLOAD.parse(o.optString("updated_at")));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void notifyWalkersDidRefreshed(Context context) {
        Intent intent = new Intent(ACTION_WALKERS_DID_REFRESHED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static class Walker {
        public final String name;
        public final int distance;
        public final double avgSpeed;
        public final int raceState;
        public final double latitude;
        public final double longitude;
        public final Date updatedAt;

        public Walker(String name, int distance, double avgSpeed, int raceState,
                      double latitude, double longitude, Date updatedAt) {
            this.name = name;
            this.distance = distance;
            this.avgSpeed = avgSpeed;
            this.raceState = raceState;
            this.latitude = latitude;
            this.longitude = longitude;
            this.updatedAt = updatedAt;
        }
    }

    public static class RaceState {
        public static final int NOTSTARTED = 0;
        public static final int STARTED = 1;
        public static final int ENDED = 2;
    }
}
