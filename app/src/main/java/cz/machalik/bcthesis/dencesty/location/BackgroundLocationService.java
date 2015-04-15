package cz.machalik.bcthesis.dencesty.location;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import cz.machalik.bcthesis.dencesty.R;


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

    
    /****************************** Public constants: ******************************/

    public static final String ACTION_LOCATION_CHANGED = "cz.machalik.bcthesis.dencesty.action.ACTION_LOCATION_CHANGED";

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
    //public static final int LOCATION_UPDATES_PRIORITY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    public static final int LOCATION_UPDATES_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;


    /****************************** Public API: ******************************/

    /**
     * Starts this service.
     *
     * @see Service
     */
    public static void start(Context context) {
        Log.i(TAG, "Starting location updates");
        Intent intent = new Intent(context, BackgroundLocationService.class);
        context.startService(intent);
    }

    /**
     * Stops this service.
     *
     * @see Service
     */
    public static boolean stop(Context context) {
        Log.i(TAG, "Stopping location updates");
        Intent intent = new Intent(context, BackgroundLocationService.class);
        return context.stopService(intent);
    }

    public static boolean isLocationProviderEnabled(final Activity activity) {

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS != resultCode) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, activity, 0);
            if (dialog != null) {
                //This dialog will help the user update to the latest GooglePlayServices
                dialog.show();
            }

            return false;
        }

        // Check GPS is enabled
        LocationManager service = (LocationManager) activity.getSystemService(LOCATION_SERVICE);
        if (!service.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            // Yes button clicked
                            // Show location settings to user
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            activity.startActivity(intent);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            // No button clicked
                            // Do nothing
                            break;
                    }

                }
            };

            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setMessage(activity.getString(R.string.gps_network_not_enabled))
                    .setPositiveButton(activity.getString(R.string.open_location_settings), dialogClickListener)
                    .setNegativeButton(activity.getString(R.string.cancel), dialogClickListener)
                    .show();
            return false;
        }

        return true;
    }

    public static Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    
    /****************************** Private: ******************************/

    private static Location lastKnownLocation = null;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    @Override
    public void onCreate() {
        super.onCreate();

        if (!servicesConnected()) {
            // TODO ?
        }

        // Build GoogleApiClient
        //Log.i(TAG, "Building GoogleApiClient");
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

        return Service.START_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "Connected to GoogleApiClient");
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.e(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.e(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
        // if (result.hasResolution()) {}
        // TODO: pokusit se o restart a vyhodit event (jako u iOS)
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
    public void onLocationChanged(Location location) {
        lastKnownLocation = location;

        Intent intent = new Intent(ACTION_LOCATION_CHANGED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    /****************************** Service staff: ******************************/

    IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public BackgroundLocationService getServerInstance() {
            return BackgroundLocationService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service ?
        return mBinder;
    }
}
