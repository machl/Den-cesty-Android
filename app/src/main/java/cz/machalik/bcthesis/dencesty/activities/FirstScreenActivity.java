package cz.machalik.bcthesis.dencesty.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.location.BackgroundLocationService;

public class FirstScreenActivity extends Activity {

    protected static final String TAG = "FirstScreenActivity";

    /**
     * Handles the Start Updates button and requests start of location updates.
     */
    protected void onStartRaceButtonHandler(View view) {
        if (servicesConnected() && locationEnabled()) { // TODO: move to RaceActivity
            // Requests location updates from the BackgroundLocationService.
        }
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {

            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                //This dialog will help the user update to the latest GooglePlayServices
                dialog.show();
            }

            return false;
        }
    }

    private boolean locationEnabled() {
        boolean enabled = BackgroundLocationService.isLocationProviderEnabled(this);

        if (!enabled) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            // Yes button clicked
                            // Show location settings to user
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            // No button clicked
                            // Do nothing
                            break;
                    }

                }
            };

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(getString(R.string.gps_network_not_enabled))
                    .setPositiveButton(getString(R.string.open_location_settings), dialogClickListener)
                    .setNegativeButton(getString(R.string.cancel), dialogClickListener)
                    .show();
        }

        return enabled;
    }

}
