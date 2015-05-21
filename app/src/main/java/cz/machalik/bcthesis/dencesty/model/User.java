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
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * Represents logged User and provides saved login credentials.
 * It is singleton, use it with User.get().
 *
 * @author Lukáš Machalík
 */
public class User {
    protected static final String TAG = "User";


    /****************************** Public API: ******************************/

    /**
     * Obtain current User model. Never returns null.
     * @return User model
     */
    public static User get() {
        return MyApplication.get().getUserModel();
    }

    /**
     * Result of login attempt.
     */
    public enum LoginResult {
        SUCCESS, FAILED, CONNECTION_ERROR
    }

    /**
     * Attempts to log in with given credentials. Saves given credentials on success.
     * @param email user's e-mail
     * @param password user's password
     * @return {@link cz.machalik.bcthesis.dencesty.model.User.LoginResult}
     */
    public LoginResult attemptLogin(Context context, String email, String password) {
        JSONObject jsonResponse = WebAPI.synchronousLoginHandlerRequest(email, password);

        if (jsonResponse == null) {
            return LoginResult.CONNECTION_ERROR;
        }

        boolean success = jsonResponse.optBoolean("success");
        if (success) {
            onSuccessfulLogin(context, jsonResponse);
            saveCredentials(context, email, password);
            return LoginResult.SUCCESS;
        } else {
            Log.i(TAG, "Login: wrong email or password");
            removeCredentials(context);
            return LoginResult.FAILED;
        }
    }

    /**
     * Logs out current user and removes his credentials.
     */
    public void logout(Context context) {
        setLogged(context, false);
        removeCredentials(context);
    }

    /**
     * Return true if user is logged in.
     * @return true if user is logged in
     */
    public boolean isLogged() {
        return this.isLogged;
    }

    /**
     * Returns user's ID.
     * @return User ID
     */
    public int getWalkerId() {
        return this.walkerId;
    }

    /**
     * Returns user's name and surname.
     * @return name and surname connected with a space
     */
    public String getWalkerFullName() {
        return this.walkerName + " " + this.walkerSurname;
    }

    /**
     * Returns true if user has saved credentials (is not logged out).
     * @return true if credentials are found
     */
    public boolean hasSavedCredentials(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences sharedSecurePreferences = MyApplication.get().getSecureSharedPreferences();

        // Remove old sharedPreferences password in plaintext (version 3.3 to 3.4+ upgrade)
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
     * Obtain e-mail for login from shared preferences.
     * @return user's email
     */
    public String getSavedCredentialsEmail(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return sharedPreferences.getString(SHAREDPREFERENCES_EMAIL_KEY, null);
    }

    /**
     * Obtain password for login from shared preferences
     * @return user's password
     */
    public String getSavedCredentialsPassword(Context context) { // TODO: change to token (http://stackoverflow.com/questions/1925486/android-storing-username-and-password)
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

    /**
     * Used only by Application object to initialize User Model.
     */
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

    /**
     * Saves user model state to persist memory for future recreating.
     */
    private void setLogged(Context context, boolean isLogged) {
        this.isLogged = isLogged;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        sharedPreferences.edit()
                .putBoolean(SHAREDPREFERENCES_STATE_ISLOGGED_KEY, isLogged)
                .commit();
    }

    /**
     * Saves user's id to persist memory for future recreating.
     */
    private void setWalkerId(Context context, int walkerId) {
        this.walkerId = walkerId;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        sharedPreferences.edit()
                .putInt(SHAREDPREFERENCES_STATE_WALKERID_KEY, walkerId)
                .commit();
    }

    /**
     * Saves user's name to persist memory for future recreating.
     */
    private void setWalkerName(Context context, String walkerName) {
        this.walkerName = walkerName;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        sharedPreferences.edit()
                .putString(SHAREDPREFERENCES_STATE_WALKERNAME_KEY, walkerName)
                .commit();
    }

    /**
     * Saves user's surname to persist memory for future recreating.
     */
    private void setWalkerSurname(Context context, String walkerSurname) {
        this.walkerSurname = walkerSurname;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        sharedPreferences.edit()
                .putString(SHAREDPREFERENCES_STATE_WALKERSURNAME_KEY, walkerSurname)
                .commit();
    }

    /**
     * Called when user is successfully logged in.
     * @param jsonData data from server response on login attempt
     */
    private void onSuccessfulLogin(Context context, JSONObject jsonData) {
        if (!jsonData.has("id") || !jsonData.has("name") || !jsonData.has("surname")) {
            String message = "Response login missing info";
            Log.e(TAG, message);
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

        EventUploaderService.addEvent(context, event);
        EventUploaderService.performUpload(context);
    }

    /**
     * Saves user's credentials to persist memory.
     */
    private void saveCredentials(Context context, String email, String password) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences secureSharedPreferences = MyApplication.get().getSecureSharedPreferences();

        sharedPreferences.edit()
                .putString(SHAREDPREFERENCES_EMAIL_KEY, email)
                .commit();

        secureSharedPreferences.edit()
                .putString(SHAREDPREFERENCES_PASSWORD_KEY, password)
                .commit();
    }

    /**
     * Removes user's credentials from persist memory.
     */
    private void removeCredentials(Context context) {
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
