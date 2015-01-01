package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.location.BackgroundLocationService;

/**
 * Lukáš Machalík
 */
public class RaceModel {
    // It's singleton
    private static RaceModel ourInstance = new RaceModel();
    public static RaceModel getInstance() {
        return ourInstance;
    }
    private RaceModel() {}


    protected static final String TAG = "RaceModel";

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    /**
     * Number od location updates so far.
     */
    private static int numOfLocationUpdates = 0;

    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    public void startRace(Context context) {
        Log.i(TAG, "Starting location updates");
        BackgroundLocationService.start(context);
    }

    public void stopRace(Context context) {
        Log.i(TAG, "Stoping location updates");
        BackgroundLocationService.stop(context);
    }

    public void onLocationChanged(Context context, Location location) {
        mCurrentLocation = location;

        float course = location.hasBearing() ? location.getBearing() : -1;
        double altitude = location.hasAltitude() ? location.getAltitude() : -1;
        int counter = numOfLocationUpdates++;
        float speed = location.hasSpeed() ? location.getSpeed() : -1;
        float verAcc = -1; // Android does not provide vertical accuracy information
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        float horAcc = location.hasAccuracy() ? location.getAccuracy() : -1;
        String provider = location.getProvider() != null ? location.getProvider() : "notset";

        Date date = new Date(location.getTime());
        String timestamp = format.format(date);

        String info = "Location changed: " + counter + ' ' + provider + ' ' + latitude + ' ' +
                longitude + ' ' + altitude + ' ' + speed + ' ' + course + ' ' + horAcc +
                ' ' + verAcc + ' ' + timestamp;

        //Log.i(TAG, info);
        EventUploaderService.startActionAddEvent(context, info);
    }
}
