package cz.machalik.bcthesis.dencesty.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.location.BackgroundLocationService;
import cz.machalik.bcthesis.dencesty.model.RaceModel;

public class FirstScreenActivity extends Activity {

    protected static final String TAG = "FirstScreenActivity";

    // UI references.
    private TextView mFullnameTextView;
    private TextView mUsernameTextView;
    private Button mStartRaceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_screen);

        mFullnameTextView = (TextView) findViewById(R.id.fullname_textview);
        mUsernameTextView = (TextView) findViewById(R.id.username_textview);

        mStartRaceButton = (Button) findViewById(R.id.start_race_button);
        mStartRaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartRaceButtonHandler(v);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mFullnameTextView.setText(RaceModel.getInstance().getWalkerFullName());
        mUsernameTextView.setText(RaceModel.getInstance().getWalkerUsername());
    }

    /**
     * Handles the Start Updates button and requests start of location updates.
     */
    protected void onStartRaceButtonHandler(View view) {
        if (servicesConnected() && locationEnabled()) {
            // Requests location updates from the BackgroundLocationService.
            RaceModel.getInstance().startRace(this);

            Intent intent = new Intent(this, RaceActivity.class);
            startActivity(intent);
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
