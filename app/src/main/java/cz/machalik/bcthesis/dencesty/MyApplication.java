package cz.machalik.bcthesis.dencesty;

import android.app.Application;


import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

/**
 * https://github.com/ACRA/acra/wiki/BasicSetup
 * Lukáš Machalík
 */
@ReportsCrashes(
        formUri = "http://www.machalik.cz/dencesty/report.php"
)
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
