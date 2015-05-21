package cz.machalik.bcthesis.dencesty.activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.location.BackgroundLocationService;
import cz.machalik.bcthesis.dencesty.model.User;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

/**
 * A races list screen with all available races downloaded from a server.
 *
 * @author Lukáš Machalík
 */
public class RacesListActivity extends ListActivity implements SwipeRefreshLayout.OnRefreshListener {

    protected static final String TAG = "RacesListActivity";

    /**
     * Minimal time interval for automatic refreshing of list from a server.
     * It prevents refreshing too often.
     */
    private static final int TIMEINTERVAL_TO_SUPPRESS_REFRESHING = 5 * 60; // in seconds

    /**
     * Maximum interval before official race start to allow continue to race detail.
     */
    public static final int TIMEINTERVAL_BEFORE_START_TO_ALLOW_CONTINUE = 10 * 60; // in seconds

    /**
     * Keep track of the refresh task to ensure we can cancel it if requested.
     */
    private RacesUpdateAsyncTask mRefreshTask = null;

    /**
     * Race items.
     */
    private List<RaceItem> races = new ArrayList<>();

    // UI references.
    private SwipeRefreshLayout mSwipeContainer;
    private SwipeRefreshLayout mEmptySwipeContainer;

    private Date lastTimeRefreshed = null;

    /**
     * Called when the activity is starting.  This is where most initialization
     * should go.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_races_list);

        mSwipeContainer = (SwipeRefreshLayout) findViewById(R.id.races_list_swipe_container);
        mSwipeContainer.setOnRefreshListener(this);
        mEmptySwipeContainer = (SwipeRefreshLayout) findViewById(R.id.races_list_swipe_container_empty);
        mEmptySwipeContainer.setOnRefreshListener(this);

        //ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);

        // assign the list adapter
        setListAdapter(new RacesListAdapter(this));

        ListView lv = (ListView)findViewById(android.R.id.list);
        lv.setEmptyView(mEmptySwipeContainer);
    }

    /**
     * Called after {@link #onRestoreInstanceState}, {@link #onRestart}, or
     * {@link #onPause}, for your activity to start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (this.lastTimeRefreshed == null ||
                ((new Date().getTime() - this.lastTimeRefreshed.getTime()) / 1000) > TIMEINTERVAL_TO_SUPPRESS_REFRESHING)
        {
            showProgress(true);
            attemptRefresh();
        }

        // Check for Google Play services availability
        BackgroundLocationService.isLocationProviderEnabled(this);
    }

    /**
     * Creates options menu (for Logout button).
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_races_list, menu);
        return true;
    }

    /**
     * Called when options menu item is selected (pressed).
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when logout button was pressed.
     * Dismisses this activity.
     */
    private void logout() {
        User.get().logout(this);
        finish();
    }

    /**
     * Called when user do "pull to refresh" gesture.
     */
    @Override
    public void onRefresh() {
        attemptRefresh();
    }

    /**
     * Creates new background download task to update races list.
     */
    private void attemptRefresh() {
        if (mRefreshTask != null) {
            return;
        }

        // Show a progress spinner, and kick off a background task to
        // perform the race info refresh attempt.
        // showProgress(true);
        mRefreshTask = new RacesUpdateAsyncTask(this);
        mRefreshTask.execute();
    }

    /**
     * Represents an asynchronous task used to refresh list of races.
     */
    private class RacesUpdateAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        public RacesUpdateAsyncTask (Context context){
            this.mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            JSONArray response = WebAPI.synchronousRacesListUpdateRequest();
            return processResponse(response);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mRefreshTask = null;
            showProgress(false);

            if (success) {
                //Log.i(TAG, "Successful RacesUpdate");
                lastTimeRefreshed = new Date();
                updateRacesList();
            } else {
                Log.i(TAG, "Failed RacesUpdate");
            }
        }

        @Override
        protected void onCancelled() {
            mRefreshTask = null;
            showProgress(false);
        }
    }

    /**
     * Process response from server on races list update attempt.
     * @param response server response data
     * @return returns true if data were successfully parsed
     */
    private Boolean processResponse(JSONArray response) {

        if (response == null) {
            return false;
        }

        races.clear();
        for (int i = 0; i < response.length(); i++) {
            final JSONObject o = response.optJSONObject(i);

            try {
                Date startTime = WebAPI.DATE_FORMAT_DOWNLOAD.parse(o.optString("start_time"));
                Date finishTime = WebAPI.DATE_FORMAT_DOWNLOAD.parse(o.optString("finish_time"));

                RaceItem item = new RaceItem(o.optInt("id"),
                        o.optString("name_cs"),
                        o.optString("name_en"),
                        startTime,
                        finishTime);

                races.add(item);

            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    /**
     * Shows or hides loading spinning wheel.
     * @param show true will show loading, false will hide loading
     */
    private void showProgress(final boolean show) {
        mSwipeContainer.setRefreshing(show);
        mEmptySwipeContainer.setRefreshing(show);
    }

    /**
     * Force tell ListView to update it's content.
     */
    private void updateRacesList() {
        //getListView().setAdapter(new RacesListAdapter(this));
        final RacesListAdapter adapter = ((RacesListAdapter)getListAdapter());
        adapter.notifyDataSetChanged();
    }

    /**
     * Data structure for single race item.
     */
    private class RaceItem {
        int id;
        String nameCS;
        String nameEN;
        Date startTime;
        Date finishTime;

        public RaceItem(int id, String nameCS, String nameEN, Date startTime, Date finishTime) {
            this.id = id;
            this.nameCS = nameCS;
            this.nameEN = nameEN;
            this.startTime = startTime;
            this.finishTime = finishTime;
        }
    }

    /**
     * Data adapter for ListView.
     */
    private class RacesListAdapter extends BaseAdapter {

        private List<RaceItem> data;
        private final Context context;
        private SimpleDateFormat dateFormatter = new SimpleDateFormat(getString(R.string.races_list_item_dateformat));

        public RacesListAdapter(Context context) {
            this.context = context;
            loadRacesData();
        }

        @Override
        public void notifyDataSetChanged() {
            loadRacesData();
            super.notifyDataSetChanged();
        }

        private void loadRacesData() {
            // Make copy to be sure that race condition on data never happen
            this.data = new ArrayList<>(races);
        }

        @Override
        public int getCount() {
            return this.data.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public RaceItem getItem(int position) {
            return this.data.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.race_list_item, null);
            }

            TextView nameCS = (TextView) convertView.findViewById(R.id.nameCS);
            TextView nameEN = (TextView) convertView.findViewById(R.id.nameEN);
            TextView startTime = (TextView) convertView.findViewById(R.id.startTime);
            TextView finishTime = (TextView) convertView.findViewById(R.id.finishTime);

            RaceItem item = getItem(position);

            nameCS.setText(item.nameCS);
            startTime.setText(dateFormatter.format(item.startTime));
            finishTime.setText(dateFormatter.format(item.finishTime));

            if (item.nameEN.isEmpty()) {
                nameEN.setVisibility(View.GONE);
            } else {
                nameEN.setVisibility(View.VISIBLE);
                nameEN.setText(item.nameEN);
            }

            return convertView;
        }
    }

    /**
     * Called when user selects list item.
     */
    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        RaceItem selectedItem = (RaceItem) getListView().getItemAtPosition(position);

        long secondsSinceStart = (new Date().getTime() - selectedItem.startTime.getTime()) / 1000;
        if (secondsSinceStart > -TIMEINTERVAL_BEFORE_START_TO_ALLOW_CONTINUE) {
            startRaceTabbedActivity(selectedItem.id);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.start_soon_alert_title))
                    .setMessage(getString(R.string.start_soon_alert_message))
                    .setPositiveButton(android.R.string.ok, null)
                    //.setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

    }

    /**
     * Starts next activity (race detail) with specified Race ID.
     * @param raceId selected Race ID
     */
    private void startRaceTabbedActivity(int raceId) {
        Intent intent = new Intent(this, RaceTabbedActivity.class);
        intent.putExtra(RaceTabbedActivity.EXTRA_RACEID, raceId);
        startActivity(intent);
    }
}
