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
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.model.RaceModel;
import cz.machalik.bcthesis.dencesty.model.Walker;

/**
 * Race activity.
 *
 * Lukáš Machalík
 */
public class RaceActivity extends Activity {
    protected static final String TAG = "RaceActivity";

    public static final String EXTRA_RACE_ID = "cz.machalik.bcthesis.dencesty.extra.EXTRA_RACE_ID";

    private RaceModel raceModel;

    /**
     * Keep track of the refresh task to ensure we can cancel it if requested.
     */
    private WalkersUpdateAsyncTask mRefreshTask = null;

    private BroadcastReceiver mUnsentCounterReceiver;
    private BroadcastReceiver mRaceInfoChangedReceiver;

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

        int raceId = getIntent().getIntExtra(EXTRA_RACE_ID, 0);
        this.raceModel = new RaceModel(raceId);


        this.raceModel.startRace(this);


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
                if (intent.getAction().equals(EventUploaderService.ACTION_EVENT_QUEUE_SIZE_CHANGED)) {
                    int numOfUnsentEvents = intent.getIntExtra(EventUploaderService.EXTRA_EVENT_QUEUE_SIZE, 0);
                    setUnsentCounter(numOfUnsentEvents);
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mUnsentCounterReceiver,
                        new IntentFilter(EventUploaderService.ACTION_EVENT_QUEUE_SIZE_CHANGED));

        // Register broadcast receiver on location counter updates
        mRaceInfoChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(RaceModel.ACTION_RACE_INFO_CHANGED)) {
                    updateRaceInfoUI();
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mRaceInfoChangedReceiver,
                        new IntentFilter(RaceModel.ACTION_RACE_INFO_CHANGED));
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
        this.raceModel.stopRace(this);
    }

    public void attemptRefresh() {
        if (mRefreshTask != null) {
            return;
        }

        // Show a progress spinner, and kick off a background task to
        // perform the race info refresh attempt.
        // showProgress(true);
        mRefreshTask = new WalkersUpdateAsyncTask(this);
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
        setUnsentCounter(EventUploaderService.getEventQueueSize());
        updateRaceInfoUI();
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRaceInfoChangedReceiver);
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

    private class WalkersUpdateAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        public WalkersUpdateAsyncTask (Context context){
            this.mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return Walker.fetchWalkersFromWeb(mContext);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mRefreshTask = null;
            showProgress(false);

            if (success) {
                //Log.i(TAG, "Successful RaceInfoUpdate");
                updateWalkersUI();
            } else {
                //Log.i(TAG, "Failed RaceInfoUpdate");
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
        this.mDistanceTextView.setText(String.format("%d m", this.raceModel.getRaceDistance()));
        this.mAvgSpeedTextView.setText(String.format("%.2f km/h", this.raceModel.getRaceAvgSpeed()));

        this.mLocationUpdatesCounter.setText(""+this.raceModel.getLocationUpdatesCounter());
    }

    protected void updateWalkersUI() {
        mWalkersListView.setAdapter(new WalkersListAdapter(this));
    }

    protected void setUnsentCounter(int numOfUnsentMessages) {
        mUnsentCounter.setText(""+numOfUnsentMessages);
        if (numOfUnsentMessages > 0)
            mUnsentCounter.setTextColor(getResources().getColor(R.color.counter_highlighted));
        else
            mUnsentCounter.setTextColor(getResources().getColor(R.color.counter_default));
    }

}
