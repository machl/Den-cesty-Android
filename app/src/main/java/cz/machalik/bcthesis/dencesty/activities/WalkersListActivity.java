package cz.machalik.bcthesis.dencesty.activities;

import android.app.ListActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Date;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.WalkersModel;
import cz.machalik.bcthesis.dencesty.model.WalkersModel.Walker;

public class WalkersListActivity extends ListActivity {

    protected static final String TAG = "WalkersListActivity";

    private static final int TIMEINTERVAL_TO_SUPPRESS_REFRESHING = 5 * 60; // in seconds

    /**
     * Keep track of the refresh task to ensure we can cancel it if requested.
     */
    private WalkersUpdateAsyncTask mRefreshTask = null;

    // UI references.
    private SwipeRefreshLayout mSwipeContainer;

    private Date lastTimeRefreshed = null;

    private WalkersModel walkersModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkers_list);

        int raceId = getIntent().getIntExtra(RaceActivity.EXTRA_RACE_ID, 0);
        this.walkersModel = new WalkersModel(raceId);

        mSwipeContainer = (SwipeRefreshLayout) findViewById(R.id.walkers_list_swipe_container);
        mSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                attemptRefresh();
            }
        });

        // assign the list adapter
        //setListAdapter(new WalkersListAdapter(this));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (this.lastTimeRefreshed == null ||
                ((new Date().getTime() - this.lastTimeRefreshed.getTime()) / 1000) > TIMEINTERVAL_TO_SUPPRESS_REFRESHING)
        {
            showProgress(true);
            attemptRefresh();
        } else {
            updateWalkersList();
        }
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

    private class WalkersUpdateAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        public WalkersUpdateAsyncTask (Context context){
            this.mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return walkersModel.fetchWalkersFromWeb(mContext);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mRefreshTask = null;
            showProgress(false);

            if (success) {
                Log.i(TAG, "Successful WalkersUpdate");
                lastTimeRefreshed = new Date();
                updateWalkersList();
            } else {
                Log.i(TAG, "Failed WalkersUpdate");
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

    protected void updateWalkersList() {
        //final WalkersListAdapter adapter = ((WalkersListAdapter)getListAdapter());
        //adapter.notifyDataSetChanged();
        setListAdapter(new WalkersListAdapter(this));
    }

    private class WalkersListAdapter extends BaseAdapter {

        private final Context context;

        public WalkersListAdapter(Context context) {
            this.context = context;
        }

        public Walker[] getWalkersAhead() {
            return walkersModel.getWalkersAhead();
        }

        public Walker[] getWalkersBehind() {
            return walkersModel.getWalkersBehind();
        }

        public Walker getMe() {
            return walkersModel.getPresentWalker();
        }

        @Override
        public int getCount() {
            return getWalkersAhead().length + 1 + getWalkersBehind().length;
        }

        @Override
        public Walker getItem(int position) {
            if (position < getWalkersAhead().length) { // Ahead
                return getWalkersAhead()[position];
            } else if (position > getWalkersAhead().length) { // Behind
                return getWalkersBehind()[position - getWalkersAhead().length - 1];
            } else { // Me
                return getMe();
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.walker_list_item, null);
            }

            TextView nameLabel = (TextView) convertView.findViewById(R.id.nameLabel);
            TextView progressLabel = (TextView) convertView.findViewById(R.id.progressLabel);
            TextView infoLabel = (TextView) convertView.findViewById(R.id.infoLabel);
            TextView timeLabel = (TextView) convertView.findViewById(R.id.timeLabel);

            Walker walker = getItem(position);

            nameLabel.setText(walker.name);
            progressLabel.setText(String.format("%d m, %.2f km/h", walker.distance, walker.avgSpeed));

            // Show "Ended" if walker ended race already
            if (walker.raceState == WalkersModel.RaceState.ENDED) {
                infoLabel.setVisibility(View.VISIBLE);
            } else {
                infoLabel.setVisibility(View.GONE);
            }

            // 'Updated at' label:
            if (walker.updatedAt != null) {
                Date now = new Date();
                long deltaSeconds = (now.getTime() - walker.updatedAt.getTime()) / 1000;
                long deltaMinutes = deltaSeconds / 60;

                if (deltaSeconds < 5) {
                    timeLabel.setText("just now");
                } else if (deltaSeconds < 60) {
                    timeLabel.setText(String.format("%d seconds ago", deltaSeconds));
                } else if (deltaSeconds < 120) {
                    timeLabel.setText("a minute ago");
                } else if (deltaMinutes < 60) {
                    timeLabel.setText(String.format("%d minutes ago", deltaMinutes));
                } else if (deltaMinutes < 120) {
                    timeLabel.setText("an hour ago");
                } else {
                    int hours = (int) Math.floor(deltaMinutes / 60);
                    timeLabel.setText(String.format("%d hours ago", hours));
                }
            } else {
                timeLabel.setText("never updated");
            }

            // Set different background color for current user
            if (position == getWalkersAhead().length) { // Me
                convertView.setBackgroundColor(context.getResources().getColor(R.color.listitem_me));
            } else {
                convertView.setBackgroundColor(0x00000000); // transparent
            }

            return convertView;
        }
    }
}
