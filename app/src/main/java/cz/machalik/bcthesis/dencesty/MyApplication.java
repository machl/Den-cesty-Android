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
 * https://github.com/scottyab/secure-preferences/blob/master/sample/src/com/securepreferences/sample/App.java
 * https://github.com/ACRA/acra/wiki/BasicSetup
 * Lukáš Machalík
 */
@ReportsCrashes(
        formUri = "http://www.machalik.cz/dencesty/report.php"
)
public class MyApplication extends Application {

    protected static final String TAG = "MyApplication";

    protected static MyApplication instance;
    private SecurePreferences mSecurePrefs;

    // Models:
    private User userModel;
    private RaceModel raceModel;
    private WalkersModel walkersModel;

    public MyApplication(){
        super();
        instance = this;
    }
    public static MyApplication get() {
        return instance;
    }

    /**
     * Single point for the app to get the secure prefs object
     * @return
     */
    public SharedPreferences getSecureSharedPreferences() {
        if(mSecurePrefs==null){
            mSecurePrefs = new SecurePreferences(this, null, "encrypted_prefs.xml");
            SecurePreferences.setLoggingEnabled(true);
        }
        return mSecurePrefs;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }

    public User getUserModel() {
        // lazy init
        if (this.userModel == null) {
            Log.d(TAG, "New UserModel init");
            this.userModel = new User(this);
        }

        return this.userModel;
    }


    public void setRaceModel(RaceModel raceModel) {
        if (this.raceModel != null) {
            this.raceModel.stopRace(this);
            this.raceModel = null;
        }
        this.raceModel = raceModel;
    }

    public RaceModel getRaceModel() {
        return raceModel;
    }


    public void setWalkersModel(WalkersModel walkersModel) {
        this.walkersModel = walkersModel;
    }

    public WalkersModel getWalkersModel() {
        return walkersModel;
    }

}
