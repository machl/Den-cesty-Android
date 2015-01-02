package cz.machalik.bcthesis.dencesty.model;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.machalik.bcthesis.dencesty.events.Event;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.location.BackgroundLocationService;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

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


    public void startRace(Context context) {
        Log.i(TAG, "Starting location updates");
        BackgroundLocationService.start(context);
    }

    public void stopRace(Context context) {
        Log.i(TAG, "Stopping location updates");
        BackgroundLocationService.stop(context);
    }

    public void onLocationChanged(Context context, Location location) {
        mCurrentLocation = location;

        Float course = location.hasBearing() ? location.getBearing() : -1;
        Double altitude = location.hasAltitude() ? location.getAltitude() : -1;
        Integer counter = numOfLocationUpdates++;
        Float speed = location.hasSpeed() ? location.getSpeed() : -1;
        Float verAcc = -1f; // Android does not provide vertical accuracy information
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();
        Float horAcc = location.hasAccuracy() ? location.getAccuracy() : -1;
        String provider = location.getProvider() != null ? location.getProvider() : "notset";

        Date date = new Date(location.getTime());
        String timestamp = WebAPI.DATE_FORMAT.format(date);

        /*String info = "Location changed: " + counter + ' ' + provider + ' ' + latitude + ' ' +
                longitude + ' ' + altitude + ' ' + speed + ' ' + course + ' ' + horAcc +
                ' ' + verAcc + ' ' + timestamp;
        //Log.i(TAG, info);
        FileLogger.log(TAG, info);*/

        Map dataMap = new HashMap(10);
        dataMap.put("latitude", latitude);
        dataMap.put("longitude", longitude);
        dataMap.put("altitude", altitude);
        dataMap.put("course", course);
        dataMap.put("speed", speed);
        dataMap.put("horAcc", horAcc);
        dataMap.put("verAcc", verAcc);
        dataMap.put("timestamp", timestamp);
        dataMap.put("counter", counter);
        dataMap.put("provider", provider);

        Event event = new Event(context, Event.EVENTTYPE_LOCATIONUPDATE, dataMap);

        EventUploaderService.startActionAddEvent(context, event);
        EventUploaderService.startActionUpload(context);
    }
}
