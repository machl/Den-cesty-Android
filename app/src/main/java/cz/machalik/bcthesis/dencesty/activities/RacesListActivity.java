package cz.machalik.bcthesis.dencesty.activities;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.webapi.WebAPI;

public class RacesListActivity extends ListActivity {

    protected static final String TAG = "RacesListActivity";

    /**
     * Keep track of the refresh task to ensure we can cancel it if requested.
     */
    private RacesUpdateAsyncTask mRefreshTask = null;

    private List<RaceItem> races = new ArrayList<>();

    // UI references.
    private SwipeRefreshLayout mSwipeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_races_list);

        mSwipeContainer = (SwipeRefreshLayout) findViewById(R.id.races_list_swipe_container);
        mSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                attemptRefresh();
            }
        });

        // assign the list adapter
        setListAdapter(new RacesListAdapter(this));
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        RaceItem selectedItem = (RaceItem) getListView().getItemAtPosition(position);

        Log.d(TAG, "Launching RaceActivity with ID "+selectedItem.id);

        Intent intent = new Intent(this, RaceActivity.class);
        intent.putExtra(RaceActivity.EXTRA_RACE_ID, selectedItem.id);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        attemptRefresh();
    }

    private void attemptRefresh() {
        if (mRefreshTask != null) {
            return;
        }

        // Show a progress spinner, and kick off a background task to
        // perform the race info refresh attempt.
        showProgress(true);
        mRefreshTask = new RacesUpdateAsyncTask(this);
        mRefreshTask.execute();
    }

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
                Log.i(TAG, "Successful RacesUpdate");
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

    private Boolean processResponse(JSONArray response) {
        //races = new ArrayList<>(response.length());
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

    private void showProgress(final boolean show) {
        mSwipeContainer.setRefreshing(show);
    }

    private void updateRacesList() {
        //getListView().setAdapter(new RacesListAdapter(this));
        final RacesListAdapter adapter = ((RacesListAdapter)getListAdapter());
        adapter.notifyDataSetChanged();
    }

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

    private class RacesListAdapter extends BaseAdapter {

        private final Context context;

        public RacesListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return races.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public RaceItem getItem(int position) {
            return races.get(position);
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
            nameEN.setText(item.nameEN);
            startTime.setText(item.startTime.toString()); // TODO: lepsi formatovani casu
            finishTime.setText(item.finishTime.toString());

            return convertView;
        }
    }
}
