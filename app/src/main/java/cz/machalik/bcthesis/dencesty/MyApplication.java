package cz.machalik.bcthesis.dencesty;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.securepreferences.SecurePreferences;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import cz.machalik.bcthesis.dencesty.model.RaceModel;
import cz.machalik.bcthesis.dencesty.model.User;
import cz.machalik.bcthesis.dencesty.model.WalkersModel;

/**
 * Custom Application object that holds references to all models, provide access to encrypted
 * SharedPreferences and configures ACRA for automatic crashes reporting.
 *
 * <p>
 * Models are hard to recreate, so Application object is the best way to hold model references,
 * because Application object is deallocated on low memory as the last object of entire app instance.
 *
 * <p>
 * About encrypted SharedPreferences:
 * https://github.com/scottyab/secure-preferences/blob/master/sample/src/com/securepreferences/sample/App.java
 *
 * <p>
 * About ACRA crash reporting tool:
 * https://github.com/ACRA/acra/wiki/BasicSetup
 *
 * @author Lukáš Machalík
 */
@ReportsCrashes(
        formUri = "http://www.machalik.cz/dencesty/report.php"
)
public class MyApplication extends Application {

    protected static final String TAG = "MyApplication";

    // Singleton instance:
    private static MyApplication instance;

    // Encrypted prefs:
    private SecurePreferences mSecurePrefs;

    // Models:
    private User userModel;
    private RaceModel raceModel;
    private WalkersModel walkersModel;

    /**
     * Required constructor called by a system.
     */
    public MyApplication(){
        super();
        instance = this;
    }

    /**
     * Obtain Application singleton instance.
     * @return current instance
     */
    public static MyApplication get() {
        return instance;
    }

    /**
     * Single point for the app to get the secure prefs object
     * @return encrypted SharedPreferences object
     */
    public SharedPreferences getSecureSharedPreferences() {
        if(mSecurePrefs==null){
            mSecurePrefs = new SecurePreferences(this, null, "encrypted_prefs.xml");
            SecurePreferences.setLoggingEnabled(true);
        }
        return mSecurePrefs;
    }

    /**
     * Called when the application is starting, before any activity, service,
     * or receiver objects (excluding content providers) have been created.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }

    /**
     * Obtain User Model. You do not need to init model manually, it's lazily initialized.
     * @return current User Model
     */
    public User getUserModel() {
        // lazy init
        if (this.userModel == null) {
            Log.d(TAG, "New UserModel init");
            this.userModel = new User(this);
        }

        return this.userModel;
    }

    /**
     * Sets new current Race Model.
     * @param raceModel new Race Model
     */
    public void setRaceModel(RaceModel raceModel) {
        if (this.raceModel != null) {
            this.raceModel.stopRace(this);
            this.raceModel = null;
        }
        this.raceModel = raceModel;
    }

    /**
     * Obtain current Race Model.
     * @return current Race Model or null if there is no Race Model yet
     */
    public RaceModel getRaceModel() {
        return raceModel;
    }

    /**
     * Sets new current Walkers Model.
     * @param walkersModel new Walkers Model
     */
    public void setWalkersModel(WalkersModel walkersModel) {
        this.walkersModel = walkersModel;
    }

    /**
     * Obtain current Walkers Model.
     * @return current Walkers Model or null if there is no Walkers Model yet
     */
    public WalkersModel getWalkersModel() {
        return walkersModel;
    }

}
