package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import cz.machalik.bcthesis.dencesty.events.Event;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.other.FileLogger;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Lukáš Machalík
 */
public class User {
    protected static final String TAG = "User";


    /****************************** Public API: ******************************/

    public static boolean attemptLogin(Context context, String email, String password) {
        JSONObject jsonResponse = WebAPI.synchronousLoginHandlerRequest(email, password);

        if (jsonResponse != null) {
            boolean success = jsonResponse.optBoolean("success");
            if (success) {
                onSuccessfulLogin(context, jsonResponse);
                return true;
            } else {
                Log.i(TAG, "Login: wrong email or password");
                return false;
            }
        }

        return false;
    }

    public static boolean isLogged() {
        return isLogged;
    }

    public static int getWalkerId() {
        return walkerId;
    }

    public static String getWalkerUsername() {
        return walkerUsername;
    }

    public static String getWalkerFullName() {
        return walkerName + " " + walkerSurname;
    }


    /****************************** Private: ******************************/

    /**
     * True, if user is successfully logged.
     */
    private static boolean isLogged = false;

    /**
     * Info about logged user.
     */
    private static int walkerId;
    private static String walkerName;
    private static String walkerSurname;
    private static String walkerUsername;

    private static void onSuccessfulLogin(Context context, JSONObject jsonData) {
        if (!jsonData.has("id") || !jsonData.has("name") || !jsonData.has("surname") || !jsonData.has("username")) {
            String message = "Response login missing info";
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            return;
        }

        walkerId = jsonData.optInt("id");
        walkerName = jsonData.optString("name");
        walkerSurname = jsonData.optString("surname");
        walkerUsername = jsonData.optString("username");
        isLogged = true;

        Map dataMap = new HashMap(10);
        dataMap.put("systemName", Build.VERSION.RELEASE + " " + Build.VERSION.CODENAME);
        dataMap.put("sdk", Integer.valueOf(Build.VERSION.SDK_INT));
        dataMap.put("model", Build.MODEL);
        // TODO: info o povolených polohových službách, internetu ...?

        Event event = new Event(context, Event.EVENTTYPE_LOGIN, dataMap);

        EventUploaderService.addEvent(context, event);
        EventUploaderService.performUpload(context);
    }


}
