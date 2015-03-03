package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.machalik.bcthesis.dencesty.other.FileLogger;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Lukáš Machalík
 */
public class Walker {
    protected static final String TAG = "Walker";

    /****************************** Public constants: ******************************/


    /****************************** Public API: ******************************/

    public static boolean fetchWalkersFromWeb(Context context) {
        JSONObject jsonResponse = WebAPI.synchronousRaceInfoUpdateRequest();

        if (jsonResponse != null) {
            initializeWalkers(jsonResponse);
            return true;
        }

        return false;
    }

    public static Walker getPresentWalker() {
        return presentWalker;
    }

    public static Walker[] getWalkersAhead() {
        return walkersAhead;
    }

    public static Walker[] getWalkersBehind() {
        return walkersBehind;
    }

    public static int getNumWalkersAhead() {
        return numWalkersAhead;
    }

    public static int getNumWalkersBehind() {
        return numWalkersBehind;
    }

    public static int getNumWalkersEnded() {
        return numWalkersEnded;
    }

    public String getName() {
        return name;
    }

    public int getDistance() {
        return distance;
    }

    public double getAvgSpeed() {
        return avgSpeed;
    }

    /****************************** Private: ******************************/

    /**
     * Race info entries
     */
    private static Walker presentWalker;
    private static int numWalkersAhead;
    private static int numWalkersBehind;
    private static int numWalkersEnded;
    private static Walker[] walkersAhead; // TODO: ahead/behind/present předělat na Enum?
    private static Walker[] walkersBehind;

    private static void initializeWalkers(JSONObject jsonData) {
        if (!jsonData.has("distance") || !jsonData.has("speed") || !jsonData.has("numWalkersAhead") ||
                !jsonData.has("numWalkersBehind") || !jsonData.has("numWalkersEnded") ||
                !jsonData.has("walkersAhead") || !jsonData.has("walkersBehind")) {
            String message = "Response RaceInfo missing some entries";
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            return;
        }

        presentWalker = new Walker(User.getWalkerUsername(), jsonData.optInt("distance"), jsonData.optDouble("speed"));

        numWalkersAhead = jsonData.optInt("numWalkersAhead");
        numWalkersBehind = jsonData.optInt("numWalkersBehind");
        numWalkersEnded = jsonData.optInt("numWalkersEnded");

        final JSONArray walkersAheadJsonArray = jsonData.optJSONArray("walkersAhead");
        walkersAhead = new Walker[walkersAheadJsonArray.length()];
        for (int i = 0; i < walkersAheadJsonArray.length(); i++) {
            final JSONObject o = walkersAheadJsonArray.optJSONObject(i);
            walkersAhead[i] = new Walker(o.optString("name"), o.optInt("distance"), o.optDouble("speed"));
        }

        final JSONArray walkersBehindJsonArray = jsonData.optJSONArray("walkersBehind");
        walkersBehind = new Walker[walkersBehindJsonArray.length()];
        for (int i = 0; i < walkersBehindJsonArray.length(); i++) {
            final JSONObject o = walkersBehindJsonArray.optJSONObject(i);
            walkersBehind[i] = new Walker(o.optString("name"), o.optInt("distance"), o.optDouble("speed"));
        }
    }

    private final String name;
    private final int distance;
    private final double avgSpeed;

    private Walker(String name, int distance, double avgSpeed) {
        this.name = name;
        this.distance = distance;
        this.avgSpeed = avgSpeed;
    }

}
