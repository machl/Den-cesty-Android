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

    public static User get() {
        return MyApplication.get().getUserModel();
    }

    public enum LoginResult {
        SUCCESS, FAILED, CONNECTION_ERROR
    }

    public LoginResult attemptLogin(Context context, String email, String password) {
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

    public void logout(Context context) {
        setLogged(context, false);
        removeCreditials(context);
    }

    public boolean isLogged() {
        return this.isLogged;
    }

    public int getWalkerId() {
        return this.walkerId;
    }

    public String getWalkerFullName() {
        return this.walkerName + " " + this.walkerSurname;
    }

    public boolean hasSavedCreditials(Context context) {
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
    public String getSavedCreditialsEmail(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return sharedPreferences.getString(SHAREDPREFERENCES_EMAIL_KEY, null);
    }

    /**
     * Obtain password for login from shared preferences
     */
    public String getSavedCreditialsPassword(Context context) { // TODO: change to token (http://stackoverflow.com/questions/1925486/android-storing-username-and-password)
        SharedPreferences secureSharedPreferences = MyApplication.get().getSecureSharedPreferences();
        return secureSharedPreferences.getString(SHAREDPREFERENCES_PASSWORD_KEY, null);
    }


    /****************************** Private: ******************************/

    private static final String SHAREDPREFERENCES_EMAIL_KEY = "cz.machalik.bcthesis.dencesty.User.email";
    private static final String SHAREDPREFERENCES_PASSWORD_KEY = "cz.machalik.bcthesis.dencesty.User.password";

    // Keys for saving UserModel state to shared preferences for recreation on low memory
    private static final String SHAREDPREFERENCES_STATE_ISLOGGED_KEY = "cz.machalik.bcthesis.dencesty.User.state.isLogged";
    private static final String SHAREDPREFERENCES_STATE_WALKERID_KEY = "cz.machalik.bcthesis.dencesty.User.state.walkerId";
    private static final String SHAREDPREFERENCES_STATE_WALKERNAME_KEY = "cz.machalik.bcthesis.dencesty.User.state.walkerName";
    private static final String SHAREDPREFERENCES_STATE_WALKERSURNAME_KEY = "cz.machalik.bcthesis.dencesty.User.state.walkerSurname";

    /**
     * True, if user is successfully logged.
     */
    private boolean isLogged = false;

    /**
     * Info about logged user.
     */
    private int walkerId;
    private String walkerName;
    private String walkerSurname;

    public User(Context context) {
        // recreation on low memory
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        this.isLogged = sharedPreferences.getBoolean(SHAREDPREFERENCES_STATE_ISLOGGED_KEY, false);
        if (this.isLogged) {
            this.walkerId = sharedPreferences.getInt(SHAREDPREFERENCES_STATE_WALKERID_KEY, 0);
            this.walkerName = sharedPreferences.getString(SHAREDPREFERENCES_STATE_WALKERNAME_KEY, null);
            this.walkerSurname = sharedPreferences.getString(SHAREDPREFERENCES_STATE_WALKERSURNAME_KEY, null);
        }
    }

    public void setLogged(Context context, boolean isLogged) {
        this.isLogged = isLogged;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        sharedPreferences.edit()
                .putBoolean(SHAREDPREFERENCES_STATE_ISLOGGED_KEY, isLogged)
                .commit();
    }

    private void setWalkerId(Context context, int walkerId) {
        this.walkerId = walkerId;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        sharedPreferences.edit()
                .putInt(SHAREDPREFERENCES_STATE_WALKERID_KEY, walkerId)
                .commit();
    }

    private void setWalkerName(Context context, String walkerName) {
        this.walkerName = walkerName;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        sharedPreferences.edit()
                .putString(SHAREDPREFERENCES_STATE_WALKERNAME_KEY, walkerName)
                .commit();
    }

    private void setWalkerSurname(Context context, String walkerSurname) {
        this.walkerSurname = walkerSurname;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        sharedPreferences.edit()
                .putString(SHAREDPREFERENCES_STATE_WALKERSURNAME_KEY, walkerSurname)
                .commit();
    }

    private void onSuccessfulLogin(Context context, JSONObject jsonData) {
        if (!jsonData.has("id") || !jsonData.has("name") || !jsonData.has("surname")) {
            String message = "Response login missing info";
            Log.e(TAG, message);
            FileLogger.log(TAG, message);
            return;
        }

        setWalkerId(context, jsonData.optInt("id"));
        setWalkerName(context, jsonData.optString("name"));
        setWalkerSurname(context, jsonData.optString("surname"));
        setLogged(context, true);

        Event event = new Event(context, getWalkerId(), Event.EVENTTYPE_LOGIN);
        event.getExtras().put("systemName", Build.VERSION.RELEASE + " " + Build.VERSION.CODENAME);
        event.getExtras().put("sdk", Integer.valueOf(Build.VERSION.SDK_INT));
        event.getExtras().put("model", Build.MODEL);
        event.getExtras().put("appVersion", BuildConfig.VERSION_NAME);
        // TODO: info o povolených polohových službách, internetu ...?

        EventUploaderService.addEvent(context, event);
        EventUploaderService.performUpload(context);
    }

    private void saveCreditials(Context context, String email, String password) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences secureSharedPreferences = MyApplication.get().getSecureSharedPreferences();

        sharedPreferences.edit()
                .putString(SHAREDPREFERENCES_EMAIL_KEY, email)
                .commit();

        secureSharedPreferences.edit()
                .putString(SHAREDPREFERENCES_PASSWORD_KEY, password)
                .commit();
    }

    private void removeCreditials(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences secureSharedPreferences = MyApplication.get().getSecureSharedPreferences();

        sharedPreferences.edit()
                .remove(SHAREDPREFERENCES_EMAIL_KEY)
                .remove(SHAREDPREFERENCES_STATE_WALKERID_KEY)
                .remove(SHAREDPREFERENCES_STATE_WALKERNAME_KEY)
                .remove(SHAREDPREFERENCES_STATE_WALKERSURNAME_KEY)
                .commit();

        secureSharedPreferences.edit()
                .remove(SHAREDPREFERENCES_PASSWORD_KEY)
                .commit();
    }
}
