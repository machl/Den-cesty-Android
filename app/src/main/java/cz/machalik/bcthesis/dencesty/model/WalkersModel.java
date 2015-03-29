package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
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


    /****************************** Public API: ******************************/

    public boolean fetchWalkersFromWeb(Context context) {
        JSONObject jsonResponse = WebAPI.synchronousWalkersListRequest(this.raceId, User.getWalkerId());

        if (jsonResponse != null) {
            initializeWalkers(jsonResponse);
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
    private Walker[] walkersAhead; // TODO: ahead/behind/present předělat na Enum?
    private Walker[] walkersBehind;

    public WalkersModel(int raceId) {
        this.raceId = raceId;

        // Basic init:
        this.presentWalker = new Walker(User.getWalkerFullName(),
                                        0,
                                        0,
                                        RaceState.NOTSTARTED,
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
            presentWalker = new Walker(User.getWalkerFullName(),
                    jsonData.optInt("distance"),
                    jsonData.optDouble("speed"),
                    jsonData.optInt("raceState"),
                    WebAPI.DATE_FORMAT_DOWNLOAD.parse(jsonData.optString("updated_at")));

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
                                              WebAPI.DATE_FORMAT_DOWNLOAD.parse(o.optString("updated_at")));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static class Walker {
        public final String name;
        public final int distance;
        public final double avgSpeed;
        public final int raceState;
        public final Date updatedAt;

        public Walker(String name, int distance, double avgSpeed, int raceState, Date updatedAt) {
            this.name = name;
            this.distance = distance;
            this.avgSpeed = avgSpeed;
            this.raceState = raceState;
            this.updatedAt = updatedAt;
        }
    }

    public static class RaceState {
        public static final int NOTSTARTED = 0;
        public static final int STARTED = 1;
        public static final int ENDED = 2;
    }
}
