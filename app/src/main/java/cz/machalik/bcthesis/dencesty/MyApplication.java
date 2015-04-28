package cz.machalik.bcthesis.dencesty;

import android.app.Application;
import android.content.SharedPreferences;

import com.securepreferences.SecurePreferences;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

/**
 * https://github.com/scottyab/secure-preferences/blob/master/sample/src/com/securepreferences/sample/App.java
 * https://github.com/ACRA/acra/wiki/BasicSetup
 * Lukáš Machalík
 */
@ReportsCrashes(
        formUri = "http://www.machalik.cz/dencesty/report.php"
)
public class MyApplication extends Application {

    protected static MyApplication instance;
    private SecurePreferences mSecurePrefs;

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
}
