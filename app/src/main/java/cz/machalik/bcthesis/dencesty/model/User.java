package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONObject;

import cz.machalik.bcthesis.dencesty.BuildConfig;
import cz.machalik.bcthesis.dencesty.MyApplication;
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

    public enum LoginResult {
        SUCCESS, FAILED, CONNECTION_ERROR
    }

    public static LoginResult attemptLogin(Context context, String email, String password) {
        JSONObject jsonResponse = WebAPI.synchronousLoginHandlerRequest(email, password);

        if (jsonResponse == null) {
            return LoginResult.CONNECTION_ERROR;
        }

        boolean success = jsonResponse.optBoolean("success");
        if (success) {
            onSuccessfulLogin(context, jsonResponse);
            saveCreditials(context, email, password);
            return LoginResult.SUCCESS;
        } else {
            Log.i(TAG, "Login: wrong email or password");
            removeCreditials(context);
            return LoginResult.FAILED;
        }
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
        SharedPreferences sharedSecurePreferences = MyApplication.get().getSecureSharedPreferences();

        // Remove old sharedPreferences password in plaintext (version 3.3 to 3.4 upgrade)
        if (sharedPreferences.contains(SHAREDPREFERENCES_PASSWORD_KEY)) {
            String plainPassword = sharedPreferences.getString(SHAREDPREFERENCES_PASSWORD_KEY, null);

            // Remove from unsecure preferences
            sharedPreferences.edit()
                    .remove(SHAREDPREFERENCES_PASSWORD_KEY)
                    .commit();

            Log.i(TAG, "Removing old password creditials");

            // Add to secure preferences
            sharedSecurePreferences.edit()
                    .putString(SHAREDPREFERENCES_PASSWORD_KEY, plainPassword)
                    .commit();
        }

        return sharedPreferences.contains(SHAREDPREFERENCES_EMAIL_KEY)
                && sharedSecurePreferences.contains(SHAREDPREFERENCES_PASSWORD_KEY);
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
        SharedPreferences secureSharedPreferences = MyApplication.get().getSecureSharedPreferences();
        return secureSharedPreferences.getString(SHAREDPREFERENCES_PASSWORD_KEY, null);
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
        event.getExtras().put("appVersion", BuildConfig.VERSION_NAME);
        // TODO: info o povolených polohových službách, internetu ...?

        EventUploaderService.addEvent(context, event);
        EventUploaderService.performUpload(context);
    }

    private static void saveCreditials(Context context, String email, String password) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences secureSharedPreferences = MyApplication.get().getSecureSharedPreferences();

        sharedPreferences.edit()
                .putString(SHAREDPREFERENCES_EMAIL_KEY, email)
                .commit();

        secureSharedPreferences.edit()
                .putString(SHAREDPREFERENCES_PASSWORD_KEY, password)
                .commit();
    }

    private static void removeCreditials(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences secureSharedPreferences = MyApplication.get().getSecureSharedPreferences();

        sharedPreferences.edit()
                .remove(SHAREDPREFERENCES_EMAIL_KEY)
                .commit();

        secureSharedPreferences.edit()
                .remove(SHAREDPREFERENCES_PASSWORD_KEY)
                .commit();
    }
}
