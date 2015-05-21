package cz.machalik.bcthesis.dencesty.activities;

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

import cz.machalik.bcthesis.dencesty.MyApplication;
import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.model.WalkersModel;
import cz.machalik.bcthesis.dencesty.other.SwipeRefreshListFragment;

/**
 * A race scoreboard screen with all competitors and their progress in current race.
 *
 * <p>
 * A simple {@link android.app.ListFragment} subclass.
 * Use the {@link RaceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WalkersListFragment extends SwipeRefreshListFragment {

    protected static final String TAG = "WalkersListFragment";

    /**
     * Minimal time interval for automatic refreshing of list from a server.
     * It prevents refreshing too often.
     */
    private static final int TIMEINTERVAL_TO_SUPPRESS_REFRESHING = 5 * 60; // in seconds

    private Date lastTimeRefreshed = null;

    /**
     * Keep track of the refresh task to ensure we can cancel it if requested.
     */
    private WalkersUpdateAsyncTask mRefreshTask = null;

    /**
     * Adapter for list.
     */
    private WalkersListAdapter walkersListAdapter = null;

    /**
     * Use this factory method to create a new instance.
     *
     * @return A new instance of fragment RaceFragment.
     */
    public static WalkersListFragment newInstance() {
        WalkersListFragment fragment = new WalkersListFragment();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WalkersListFragment() {
    }

    /**
     * Walkers Model reference getter.
     * @return current Walkers Model
     */
    private WalkersModel getWalkersModel() {
        return MyApplication.get().getWalkersModel();
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(android.app.Activity)} and before
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    /**
     * Attach to list view once the view hierarchy has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /**
         * Implement {@link android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener}. When users do the "swipe to
         * refresh" gesture, SwipeRefreshLayout invokes
         * {@link android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener#onRefresh onRefresh()}. In
         * {@link android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener#onRefresh onRefresh()}, call a method that
         * refreshes the content. Call the same method in response to the Refresh action from the
         * action bar.
         */
        setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(TAG, "onRefresh called from SwipeRefreshLayout");

                initiateRefresh();
            }
        });
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link android.app.Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
        onDidAppear();
    }

    /**
     * Hint about whether this fragment's UI is currently visible
     * to the user.
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            onDidAppear();
        }
    }

    /**
     * Called when fragment is probably becoming visible to user.
     */
    private void onDidAppear() {
        //Log.e(TAG, "onDidAppear");

        if (this.lastTimeRefreshed == null ||
                ((new Date().getTime() - this.lastTimeRefreshed.getTime()) / 1000) > TIMEINTERVAL_TO_SUPPRESS_REFRESHING)
        {
            // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
            if (!isRefreshing()) {
                setRefreshing(true);
            }

            // Start our refresh background task
            initiateRefresh();
        } else {
            if (this.walkersListAdapter != null) {
                this.walkersListAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Refreshes list content from a server.
     *
     * <p>
     * By abstracting the refresh process to a single method, the app allows both the
     * SwipeGestureLayout onRefresh() method and the Refresh action item to refresh the content.
     */
    private void initiateRefresh() {
        if (mRefreshTask != null) {
            return;
        }

        // Show a progress spinner, and kick off a background task to
        // perform the race info refresh attempt.
        // showProgress(true);
        mRefreshTask = new WalkersUpdateAsyncTask(getActivity());
        mRefreshTask.execute();

        // TODO: (nápad) vypustit location update s okamzitou polohou?

        // Do manually upload events
        EventUploaderService.performUpload(getActivity());
    }

    /**
     * When the AsyncTask finishes, it calls onRefreshComplete(), which updates the data in the
     * ListAdapter and turns off the progress bar.
     */
    private void onRefreshComplete(Context context, boolean success) {
        if (success) {
            //Log.i(TAG, "Successful WalkersUpdate");
            this.lastTimeRefreshed = new Date();

            this.walkersListAdapter = new WalkersListAdapter(context);
            setListAdapter(this.walkersListAdapter);
        } else {
            Log.i(TAG, "Failed WalkersUpdate");
        }

        // Stop the refreshing indicator
        setRefreshing(false);
    }

    /**
     * Represents an asynchronous task used to refresh Walkers Model content.
     */
    private class WalkersUpdateAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        public WalkersUpdateAsyncTask (Context context){
            this.mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return getWalkersModel().fetchWalkersFromWeb(mContext);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mRefreshTask = null;

            // Tell the Fragment that the refresh has completed
            onRefreshComplete(mContext, result);
        }

        @Override
        protected void onCancelled() {
            mRefreshTask = null;
            setRefreshing(false);
        }
    }

    /**
     * Data adapter for ListView.
     */
    private class WalkersListAdapter extends BaseAdapter {

        private final Context context;

        public WalkersListAdapter(Context context) {
            this.context = context;
        }

        public WalkersModel.Walker[] getWalkersAhead() {
            return getWalkersModel().getWalkersAhead();
        }

        public WalkersModel.Walker[] getWalkersBehind() {
            return getWalkersModel().getWalkersBehind();
        }

        public WalkersModel.Walker getMe() {
            return getWalkersModel().getPresentWalker();
        }

        @Override
        public int getCount() {
            return getWalkersAhead().length + 1 + getWalkersBehind().length;
        }

        @Override
        public WalkersModel.Walker getItem(int position) {
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

            WalkersModel.Walker walker = getItem(position);

            nameLabel.setText(walker.name);
            progressLabel.setText(String.format("%,d m, %.2f km/h", walker.distance, walker.avgSpeed));

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
                    timeLabel.setText(getString(R.string.time_delta_justnow));
                } else if (deltaSeconds < 60) {
                    timeLabel.setText(String.format(getString(R.string.time_delta_seconds_ago), deltaSeconds));
                } else if (deltaSeconds < 120) {
                    timeLabel.setText(getString(R.string.time_delta_minute_ago));
                } else if (deltaMinutes < 60) {
                    timeLabel.setText(String.format(getString(R.string.time_delta_minutes_ago), deltaMinutes));
                } else if (deltaMinutes < 120) {
                    timeLabel.setText(getString(R.string.time_delta_hour_ago));
                } else {
                    int hours = (int) Math.floor(deltaMinutes / 60);
                    timeLabel.setText(String.format(getString(R.string.time_delta_hours_ago), hours));
                }
            } else {
                timeLabel.setText(getString(R.string.time_delta_never_updated));
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
