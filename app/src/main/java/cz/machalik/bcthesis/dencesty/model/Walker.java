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
            onFetchedRaceInfo(context, jsonResponse);
            return true;
        }

        return false;
    }

    public static int getWalkerDistance() {
        return walkerDistance;
    }

    public static double getWalkerAvgSpeed() {
        return walkerAvgSpeed;
    }

    public static JSONArray getWalkersAhead() {
        return walkersAhead;
    }

    public static JSONArray getWalkersBehind() {
        return walkersBehind;
    }

    /****************************** Private: ******************************/

    /**
     * Race info entries
     */
    // TODO: add separate distance and avgSpeed? (for walkers table)
    private static int walkerDistance; // TODO: temp (delete it)
    private static double walkerAvgSpeed; // TODO: temp (delete it)
    private static int numWalkersAhead; // TODO: change name to Count?
    private static int numWalkersBehind;
    private static int numWalkersEnded;
    private static JSONArray walkersAhead; // TODO: change to Walker[]
    private static JSONArray walkersBehind; // TODO: change to Walker[]

    private static void onFetchedRaceInfo(Context context, JSONObject jsonData) {
        if (!jsonData.has("distance") || !jsonData.has("speed") || !jsonData.has("numWalkersAhead") ||
                !jsonData.has("numWalkersBehind") || !jsonData.has("numWalkersEnded") ||
                !jsonData.has("walkersAhead") || !jsonData.has("walkersBehind")) {
            String message = "Response RaceInfo missing some entries";
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            return;
        }

        walkerDistance = jsonData.optInt("distance");
        walkerAvgSpeed = jsonData.optDouble("speed");
        numWalkersAhead = jsonData.optInt("numWalkersAhead");
        numWalkersBehind = jsonData.optInt("numWalkersBehind");
        numWalkersEnded = jsonData.optInt("numWalkersEnded");
        walkersAhead = jsonData.optJSONArray("walkersAhead");
        walkersBehind = jsonData.optJSONArray("walkersBehind");
    }
}
