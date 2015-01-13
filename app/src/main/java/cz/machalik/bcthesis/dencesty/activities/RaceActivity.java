package cz.machalik.bcthesis.dencesty.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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

    /**
     * Keep track of the refresh task to ensure we can cancel it if requested.
     */
    private RaceInfoUpdateAsyncTask mRefreshTask = null;

    // UI references.
    private Button mEndraceButton;
    private SwipeRefreshLayout mSwipeContainer;
    private TextView mDistanceTextView;
    private TextView mAvgSpeedTextView;
    private ListView mWalkersListView;

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
                new Handler().postDelayed(new Runnable() {
                    @Override public void run() {
                        attemptRefresh();
                    }
                }, 3000);
            }
        });

        mDistanceTextView = (TextView) findViewById(R.id.distance_textview);
        mAvgSpeedTextView = (TextView) findViewById(R.id.avgspeed_textview);

        mWalkersListView = (ListView) findViewById(R.id.walkers_listview);
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
        builder.setMessage("Are you sure that you want end race?")
                .setPositiveButton("End race", dialogClickListener)
                .setNegativeButton("Cancel", dialogClickListener)
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

}
