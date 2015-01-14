package cz.machalik.bcthesis.dencesty.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.RaceModel;

/**
 * Race activity.
 *
 * Lukáš Machalík
 */
public class RaceActivity extends Activity {

    protected static final String TAG = "RaceActivity";

    private static final String ACTION_UPDATE_LOCATION_COUNTER = "cz.machalik.bcthesis.dencesty.action.UPDATE_LOCATION_COUNTER";
    private static final String ACTION_UPDATE_UNSENT_COUNTER = "cz.machalik.bcthesis.dencesty.action.UPDATE_UNSENT_COUNTER";

    private static final String EXTRA_NUM_OF_LOCATION_UPDATES = "cz.machalik.bcthesis.dencesty.extra.NUM_OF_LOCATION_UPDATES";
    private static final String EXTRA_NUM_OF_UNSENT_EVENTS = "cz.machalik.bcthesis.dencesty.extra.NUM_OF_UNSENT_EVENTS";

    /**
     * Keep track of the refresh task to ensure we can cancel it if requested.
     */
    private RaceInfoUpdateAsyncTask mRefreshTask = null;

    private BroadcastReceiver mUnsentCounterReceiver;
    private BroadcastReceiver mLocationUpdatesCounterReceiver;

    // UI references.
    private Button mEndraceButton;
    private SwipeRefreshLayout mSwipeContainer;
    private TextView mDistanceTextView;
    private TextView mAvgSpeedTextView;
    private ListView mWalkersListView;
    private TextView mUnsentCounter;
    private TextView mLocationUpdatesCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        mEndraceButton = (Button) findViewById(R.id.endrace_button);
        mEndraceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogToEndRace();
            }
        });

        mSwipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                attemptRefresh();
            }
        });

        mDistanceTextView = (TextView) findViewById(R.id.distance_textview);
        mAvgSpeedTextView = (TextView) findViewById(R.id.avgspeed_textview);
        mUnsentCounter = (TextView) findViewById(R.id.unsent_textview);
        mLocationUpdatesCounter = (TextView) findViewById(R.id.loccounter_textview);

        mWalkersListView = (ListView) findViewById(R.id.walkers_listview);

        // Register broadcast receiver on unsent events counter updates
        mUnsentCounterReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_UPDATE_UNSENT_COUNTER)) {
                    int numOfUnsentEvents = intent.getIntExtra(EXTRA_NUM_OF_UNSENT_EVENTS, 0);
                    mUnsentCounter.setText(""+numOfUnsentEvents);
                    if (numOfUnsentEvents > 0)
                        mUnsentCounter.setTextColor(getResources().getColor(R.color.counter_highlighted));
                    else
                        mUnsentCounter.setTextColor(getResources().getColor(R.color.counter_default));
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mUnsentCounterReceiver,
                        new IntentFilter(ACTION_UPDATE_UNSENT_COUNTER));

        // Register broadcast receiver on location counter updates
        mLocationUpdatesCounterReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_UPDATE_LOCATION_COUNTER)) {
                    int numOfLocationUpdates = intent.getIntExtra(EXTRA_NUM_OF_LOCATION_UPDATES, 0);
                    mLocationUpdatesCounter.setText(""+numOfLocationUpdates);
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mLocationUpdatesCounterReceiver,
                        new IntentFilter(ACTION_UPDATE_LOCATION_COUNTER));
    }

    protected void showDialogToEndRace() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        endRace();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.are_you_sure_end_race))
                .setPositiveButton(getString(R.string.end_race), dialogClickListener)
                .setNegativeButton(getString(R.string.cancel), dialogClickListener)
                .show();
    }

    protected void endRace() {
        stopLocationUpdates();

        // Dismiss Activity
        finish();
    }

    /**
     * Removes location updates from the BackgroundLocationService.
     */
    protected void stopLocationUpdates() {
        RaceModel.getInstance().stopRace(this);
    }

    public void attemptRefresh() {
        if (mRefreshTask != null) {
            return;
        }

        // Show a progress spinner, and kick off a background task to
        // perform the race info refresh attempt.
        // showProgress(true);
        mRefreshTask = new RaceInfoUpdateAsyncTask(this);
        mRefreshTask.execute();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        attemptRefresh();
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
        stopLocationUpdates();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUnsentCounterReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationUpdatesCounterReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showDialogToEndRace();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class RaceInfoUpdateAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        public RaceInfoUpdateAsyncTask (Context context){
            this.mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return RaceModel.getInstance().fetchRaceInfo(mContext);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mRefreshTask = null;
            showProgress(false);

            if (success) {
                Log.i(TAG, "Successful RaceInfoUpdate");
                updateRaceInfoUI();
            } else {
                Log.i(TAG, "Failed RaceInfoUpdate");
            }
        }

        @Override
        protected void onCancelled() {
            mRefreshTask = null;
            showProgress(false);
        }
    }

    private void showProgress(final boolean show) {
        mSwipeContainer.setRefreshing(show);
    }


    protected void updateRaceInfoUI() {
        this.mDistanceTextView.setText(String.format("%d m", RaceModel.getInstance().getRaceInfoDistance()));
        this.mAvgSpeedTextView.setText(String.format("%d km/h", RaceModel.getInstance().getRaceInfoAvgSpeed()));

        mWalkersListView.setAdapter(new WalkersListAdapter(this));
    }

    public static void updateLocationCounter(Context context, int numOfLocationUpdates) {
        Intent intent = new Intent(ACTION_UPDATE_LOCATION_COUNTER);
        intent.putExtra(EXTRA_NUM_OF_LOCATION_UPDATES, numOfLocationUpdates);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void updateUnsentEventsCounter(Context context, int numOfUnsentEvents) {
        Intent intent = new Intent(ACTION_UPDATE_UNSENT_COUNTER);
        intent.putExtra(EXTRA_NUM_OF_UNSENT_EVENTS, numOfUnsentEvents);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
