package cz.machalik.bcthesis.dencesty.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
    private Button mRefreshButton;
    private TextView mDistanceTextView;
    private TextView mAvgSpeedTextView;
    private ListView mWalkersListView;
    private View mProgressView;

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

        mRefreshButton = (Button) findViewById(R.id.refresh_button);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRefresh();
            }
        });

        mDistanceTextView = (TextView) findViewById(R.id.distance_textview);
        mAvgSpeedTextView = (TextView) findViewById(R.id.avgspeed_textview);

        mWalkersListView = (ListView) findViewById(R.id.walkers_listview);

        mProgressView = findViewById(R.id.refresh_progress);
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
        showProgress(true);
        mRefreshTask = new RaceInfoUpdateAsyncTask(this);
        mRefreshTask.execute();
    }

    /**
     * Shows the progress UI and hides the refresh button.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRefreshButton.setVisibility(show ? View.GONE : View.VISIBLE);
            mRefreshButton.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRefreshButton.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mRefreshButton.setVisibility(show ? View.GONE : View.VISIBLE);
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


    protected void updateRaceInfoUI() {
        this.mDistanceTextView.setText(String.format("%d m", RaceModel.getInstance().getRaceInfoDistance()));
        this.mAvgSpeedTextView.setText(String.format("%d km/h", RaceModel.getInstance().getRaceInfoAvgSpeed()));

        mWalkersListView.setAdapter(new WalkersListAdapter(this));
    }

}
