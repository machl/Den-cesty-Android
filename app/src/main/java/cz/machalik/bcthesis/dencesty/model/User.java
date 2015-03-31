package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONObject;

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
                saveCreditials(context, email, password);
                return true;
            } else {
                Log.i(TAG, "Login: wrong email or password");
                removeCreditials(context);
                return false;
            }
        }

        return false;
    }

    public static void logout(Context context) {
        isLogged = false;
        removeCreditials(context);
    }

    public static boolean isLogged() {
        return isLogged;
    }

    public static int getWalkerId() {
        return walkerId;
    }

    public static String getWalkerFullName() {
        return walkerName + " " + walkerSurname;
    }

    public static boolean hasSavedCreditials(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return sharedPreferences.contains(SHAREDPREFERENCES_EMAIL_KEY)
                && sharedPreferences.contains(SHAREDPREFERENCES_PASSWORD_KEY);
    }

    /**
     * Obtain email for login from shared preferences
     */
    public static String getSavedCreditialsEmail(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return sharedPreferences.getString(SHAREDPREFERENCES_EMAIL_KEY, null);
    }

    /**
     * Obtain password for login from shared preferences
     */
    public static String getSavedCreditialsPassword(Context context) { // TODO: change to token (http://stackoverflow.com/questions/1925486/android-storing-username-and-password)
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return sharedPreferences.getString(SHAREDPREFERENCES_PASSWORD_KEY, null);
    }


    /****************************** Private: ******************************/

    private static final String SHAREDPREFERENCES_EMAIL_KEY = "cz.machalik.bcthesis.dencesty.User.email";
    private static final String SHAREDPREFERENCES_PASSWORD_KEY = "cz.machalik.bcthesis.dencesty.User.password";

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

    private static void onSuccessfulLogin(Context context, JSONObject jsonData) {
        if (!jsonData.has("id") || !jsonData.has("name") || !jsonData.has("surname")) {
            String message = "Response login missing info";
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            return;
        }

        walkerId = jsonData.optInt("id");
        walkerName = jsonData.optString("name");
        walkerSurname = jsonData.optString("surname");
        isLogged = true;

        Event event = new Event(context, walkerId, Event.EVENTTYPE_LOGIN);
        event.getExtras().put("systemName", Build.VERSION.RELEASE + " " + Build.VERSION.CODENAME);
        event.getExtras().put("sdk", Integer.valueOf(Build.VERSION.SDK_INT));
        event.getExtras().put("model", Build.MODEL);
        // TODO: info o povolených polohových službách, internetu ...?

        EventUploaderService.addEvent(context, event);
        EventUploaderService.performUpload(context);
    }

    private static void saveCreditials(Context context, String email, String password) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SHAREDPREFERENCES_EMAIL_KEY, email);
        editor.putString(SHAREDPREFERENCES_PASSWORD_KEY, password);
        editor.commit();
    }

    private static void removeCreditials(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(SHAREDPREFERENCES_EMAIL_KEY);
        editor.remove(SHAREDPREFERENCES_PASSWORD_KEY);
        editor.commit();
    }
}
