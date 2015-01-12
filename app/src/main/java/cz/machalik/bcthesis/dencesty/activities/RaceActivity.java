package cz.machalik.bcthesis.dencesty.activities;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.RaceModel;

/**
 * Test activity.
 *
 * Lukáš Machalík
 */
public class RaceActivity extends ActionBarActivity {
    protected static final String TAG = "RaceActivity";

    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;
    protected Button refreshButton;
    protected TextView distanceTextView;
    protected TextView avgSpeedTextView;
    protected ListView walkersListView;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);

        refreshButton = (Button) findViewById(R.id.refreshButton);

        distanceTextView = (TextView) findViewById(R.id.distanceTextView);
        avgSpeedTextView = (TextView) findViewById(R.id.avgSpeedTextView);

        walkersListView = (ListView) findViewById(R.id.walkersListView);

        mRequestingLocationUpdates = false;
        setButtonsEnabledState();
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            setButtonsEnabledState();
            stopLocationUpdates();
        }
    }

    public void refreshButtonHandler(View view) {
        // TODO: zabezpečit proti několikanásobnému zmáčknutí
        new RaceInfoUpdateAsyncTask(this).execute();
    }

    /**
     * Requests location updates from the BackgroundLocationService.
     */
    protected void startLocationUpdates() {
        RaceModel.getInstance().startRace(this);
        mRequestingLocationUpdates = true;
    }

    /**
     * Removes location updates from the BackgroundLocationService.
     */
    protected void stopLocationUpdates() {
        RaceModel.getInstance().stopRace(this);
        mRequestingLocationUpdates = false;
    }

    /**
     * Ensures that only one button is enabled at any time. The Start Updates button is enabled
     * if the user is not requesting location updates. The Stop Updates button is enabled if the
     * user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mRequestingLocationUpdates)
            stopLocationUpdates();
        super.onDestroy();
    }

    private class RaceInfoUpdateAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private Context context;
        public RaceInfoUpdateAsyncTask (Context context){
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return RaceModel.getInstance().fetchRaceInfo(context);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.i(TAG, "Successful RaceInfoUpdate");
                updateRaceInfoUI();
            } else {
                Log.i(TAG, "Failed RaceInfoUpdate");
            }
        }
    }


    private void updateRaceInfoUI() {
        this.distanceTextView.setText(String.format("%d m", RaceModel.getInstance().getRaceInfoDistance()));
        this.avgSpeedTextView.setText(String.format("%d km/h", RaceModel.getInstance().getRaceInfoAvgSpeed()));

        walkersListView.setAdapter(new WalkersListAdapter(this));
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_race, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
