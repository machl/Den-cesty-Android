package cz.machalik.bcthesis.dencesty.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.model.RaceModel;

/**
 * Race activity.
 *
 * Lukáš Machalík
 */
public class RaceActivity extends Activity {

    protected static final String TAG = "RaceActivity";

    public static final String EXTRA_RACE_ID = "cz.machalik.bcthesis.dencesty.extra.EXTRA_RACE_ID";

    private RaceInitTask mRaceInitTask = null;

    private RaceModel raceModel;

    private BroadcastReceiver mUnsentCounterReceiver;
    private BroadcastReceiver mRaceInfoChangedReceiver;

    // UI references.
    private View mProgressView;
    private View mMainLayout;
    private Button mEndraceButton;
    private Button mWalkersButton;
    private TextView mDistanceTextView;
    private TextView mAvgSpeedTextView;
    private TextView mUnsentCounter;
    private TextView mLocationUpdatesCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        mProgressView = findViewById(R.id.race_progress);
        mMainLayout = findViewById(R.id.race_main_layout);
        mEndraceButton = (Button) findViewById(R.id.endrace_button);
        mWalkersButton = (Button) findViewById(R.id.walkers_button);
        mDistanceTextView = (TextView) findViewById(R.id.distance_textview);
        mAvgSpeedTextView = (TextView) findViewById(R.id.avgspeed_textview);
        mUnsentCounter = (TextView) findViewById(R.id.unsent_textview);
        mLocationUpdatesCounter = (TextView) findViewById(R.id.loccounter_textview);

        int raceId = getIntent().getIntExtra(EXTRA_RACE_ID, 0);
        this.raceModel = new RaceModel(raceId);

        // Show a progress spinner, and kick off a background task to
        // perform the race init attempt.
        showProgress(true);
        mRaceInitTask = new RaceInitTask(this);
        mRaceInitTask.execute();
    }

    private class RaceInitTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;

        public RaceInitTask(Context context) {
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return raceModel.init();
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mRaceInitTask = null;
            showProgress(false);

            if (success) {
                Log.i(TAG, "Successful RaceInit");
                onSuccessfulRaceInit();
            } else {
                Log.i(TAG, "Failed RaceInit");
                finish();
            }
        }

        @Override
        protected void onCancelled() {
            mRaceInitTask = null;
            showProgress(false);
            Log.i(TAG, "Cancelled RaceInit");
            finish();
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mMainLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            mMainLayout.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMainLayout.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mMainLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void onSuccessfulRaceInit() {
        initRaceUI();
    }

    private void initRaceUI() {

        this.raceModel.startRace(this); // TODO: move to some button touch

        mEndraceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogToEndRace();
            }
        });

        mWalkersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: temporary:
                Intent intent = new Intent(getBaseContext(), WalkersListActivity.class);
                intent.putExtra(RaceActivity.EXTRA_RACE_ID, raceModel.getRaceId());
                startActivity(intent);
            }
        });

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

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUnsentCounterReceiver); // TODO: if not null?
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

    protected void updateRaceInfoUI() {
        this.mDistanceTextView.setText(String.format("%d m", this.raceModel.getRaceDistance()));
        this.mAvgSpeedTextView.setText(String.format("%.2f km/h", this.raceModel.getRaceAvgSpeed()));

        this.mLocationUpdatesCounter.setText(""+this.raceModel.getLocationUpdatesCounter());
    }

    protected void setUnsentCounter(int numOfUnsentMessages) {
        mUnsentCounter.setText(""+numOfUnsentMessages);
        if (numOfUnsentMessages > 0)
            mUnsentCounter.setTextColor(getResources().getColor(R.color.counter_highlighted));
        else
            mUnsentCounter.setTextColor(getResources().getColor(R.color.counter_default));
    }

}
