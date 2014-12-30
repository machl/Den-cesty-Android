package cz.machalik.bcthesis.dencesty;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Inspired by:
 * https://gist.github.com/blackcj/20efe2ac885c7297a676
 * https://github.com/googlesamples/android-play-location/tree/master/LocationUpdates
 * <p/>
 *
 * Lukáš Machalík
 */
public class BackgroundLocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
                                                                  GoogleApiClient.OnConnectionFailedListener,
                                                                  LocationListener {

    protected static final String TAG = "BackgLocService";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5 * 60 * 1000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Strong hint to the LocationClient for which location sources to use.
     * PRIORITY_BALANCED_POWER_ACCURACY: Block level accuracy is considered to be about 100 meter
     * accuracy. Using a coarse accuracy such as this often consumes less power.
     */
    public static final int LOCATION_UPDATES_PRIORITY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;

    /**
     * Starts this service.
     *
     * @see Service
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, BackgroundLocationService.class);
        context.startService(intent);
    }

    /**
     * Stops this service.
     *
     * @see Service
     */
    public static void stop(Context context) {
        Intent intent = new Intent(context, BackgroundLocationService.class);
        context.stopService(intent);
    }

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    /**
     * Number od location updates so far.
     */
    private static int numOfLocationUpdates = 0;

    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public BackgroundLocationService getServerInstance() {
            return BackgroundLocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!servicesConnected()) {
            // TODO ?
        }

        // Build GoogleApiClient
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create location request
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        // The priority of the request is a strong hint to the LocationClient for which location
        // sources to use. For example, PRIORITY_HIGH_ACCURACY is more likely to use GPS, and
        // PRIORITY_BALANCED_POWER_ACCURACY is more likely to use WIFI & Cell tower positioning,
        // but it also depends on many other factors (such as which sources are available) and
        // is implementation dependent.
        mLocationRequest.setPriority(LOCATION_UPDATES_PRIORITY);
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            /*
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                //This dialog will help the user update to the latest GooglePlayServices
                dialog.show();
            }
            */
            return false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        mGoogleApiClient.connect();

        return START_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected to GoogleApiClient");
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
        // if (result.hasResolution()) {}
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service ?
        return mBinder;
    }


    @Override
    public void onLocationChanged(Location location) {
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
        EventUploaderService.startActionAddEvent(this, info);
    }


}
