package cz.machalik.bcthesis.dencesty.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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

    private RaceModel raceModel;

    private BroadcastReceiver mUnsentCounterReceiver;
    private BroadcastReceiver mRaceInfoChangedReceiver;

    // UI references.
    private Button mStartraceButton;
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

        mStartraceButton = (Button) findViewById(R.id.startrace_button);
        mEndraceButton = (Button) findViewById(R.id.endrace_button);
        mWalkersButton = (Button) findViewById(R.id.walkers_button);
        mDistanceTextView = (TextView) findViewById(R.id.distance_textview);
        mAvgSpeedTextView = (TextView) findViewById(R.id.avgspeed_textview);
        mUnsentCounter = (TextView) findViewById(R.id.unsent_textview);
        mLocationUpdatesCounter = (TextView) findViewById(R.id.loccounter_textview);

        this.raceModel = RacesListActivity.preparedRaceModel;

        mStartraceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButtonPressed();
            }
        });

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
                    refreshRaceInfoValues();
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mRaceInfoChangedReceiver,
                        new IntentFilter(RaceModel.ACTION_RACE_INFO_CHANGED));

        // TODO: if raceMobdel.isStarted() then start location updates (maybe directly in raceModel?)
    }

    private void startButtonPressed() {
        this.raceModel.startRace(this);
        updateVisibilityOfButtons();
    }

    private void showDialogToEndRace() {
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

    private void endRace() {
        this.raceModel.stopRace(this);
        updateVisibilityOfButtons();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        this.raceModel.stopRace(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUnsentCounterReceiver); // TODO: if not null?
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRaceInfoChangedReceiver);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Stop race if race is over.
        this.raceModel.checkFinish(this);

        refreshRaceInfoValues();
        setUnsentCounter(EventUploaderService.getEventQueueSize());
        updateVisibilityOfButtons();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (this.raceModel.isStarted()) {
                new AlertDialog.Builder(this)
                        .setTitle("Race in progress")
                        .setMessage("Before returning to the race list, please, end the race!")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            } else {
                // Dismiss Activity
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void refreshRaceInfoValues() {
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

    private void updateVisibilityOfButtons() {
        if (this.raceModel.isStarted()) {
            // TODO: animation?
            mStartraceButton.setVisibility(View.GONE);
            mEndraceButton.setVisibility(View.VISIBLE);
            // mBackButton setEnabled false
        } else {
            mEndraceButton.setVisibility(View.GONE);
            mStartraceButton.setVisibility(View.VISIBLE);
            // mBackButton setEnabled true
        }
    }

}
