package cz.machalik.bcthesis.dencesty.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Locale;

import cz.machalik.bcthesis.dencesty.MyApplication;
import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.RaceModel;
import cz.machalik.bcthesis.dencesty.model.WalkersModel;

/**
 * A race detail screen that contains 3 tabs (fragments) with race detail, race scoreboard and map.
 *
 * @author Lukáš Machalík
 */
public class RaceTabbedActivity extends Activity implements ActionBar.TabListener,
        RaceFragment.OnRaceFragmentInteractionListener {

    protected static final String TAG = "RaceTabbedActivity";

    protected static final String EXTRA_RACEID = "cz.machalik.bcthesis.dencesty.extra.RACEID";

    /**
     * Background download task for race initialization (Race Model init).
     */
    private LoadRaceTask mLoadRaceTask = null;
    private RaceModel preparedRaceModel = null;

    /**
     * Current Race ID.
     */
    private int raceId;

    /**
     * The SectionsPagerAdapter that will provide
     * fragments for each of the sections.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    /**
     * Race Model reference getter.
     * @return current Race Model
     */
    private RaceModel getRaceModel() {
        return MyApplication.get().getRaceModel();
    }

    /**
     * Walkers Model reference getter.
     * @return current Walkers Model
     */
    private WalkersModel getWalkersModel() {
        return MyApplication.get().getWalkersModel();
    }

    /**
     * Called when the activity is starting.  This is where most initialization
     * should go.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race_tabbed);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            this.raceId = extras.getInt(EXTRA_RACEID);
        } else {
            this.raceId = savedInstanceState.getInt(EXTRA_RACEID);
        }

        if (getRaceModel() == null || getRaceModel().getRaceId() != this.raceId) {
            attemptLoadRace(this.raceId);
        } else {
            if (MyApplication.get().getWalkersModel() == null) {
                MyApplication.get().setWalkersModel(new WalkersModel(this.raceId));
            }

            setUpViewPager();
        }
    }

    /**
     * Start background task to load race data.
     * @param raceId
     */
    private void attemptLoadRace(int raceId) {
        mLoadRaceTask = new LoadRaceTask(this, raceId);
        mLoadRaceTask.execute();
    }

    /**
     * Represents an asynchronous task used to load race data for Race Model initialization.
     */
    private class LoadRaceTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final int mRaceId;
        private ProgressDialog dialog;

        public LoadRaceTask(Context context, int raceId) {
            mContext = context;
            mRaceId = raceId;
            dialog = new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage(getString(R.string.downloading_race_info));
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            preparedRaceModel = new RaceModel();
            return preparedRaceModel.init(mContext, mRaceId);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mLoadRaceTask = null;
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (success) {
                Log.i(TAG, "Successful LoadRace");
                onSuccessfulLoadRace();
            } else {
                Log.i(TAG, "Failed LoadRace");
                finish();
            }
        }

        @Override
        protected void onCancelled() {
            mLoadRaceTask = null;
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            Log.i(TAG, "Cancelled RaceInit");
            //finish();
        }
    }

    /**
     * Called on successful Race Model init.
     */
    private void onSuccessfulLoadRace() {
        MyApplication.get().setRaceModel(this.preparedRaceModel);
        this.preparedRaceModel = null;

        // Init shared WalkersModel
        int raceId = getRaceModel().getRaceId();
        MyApplication.get().setWalkersModel(new WalkersModel(raceId));

        setUpViewPager();

        getRaceModel().checkFinishFromActivity(this);
    }

    /**
     * Setups view pager with tabs.
     */
    private void setUpViewPager() {
        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }

    /**
     * Called after {@link #onRestoreInstanceState}, {@link #onRestart}, or
     * {@link #onPause}, for your activity to start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Stop race if race time is over.
        if (getRaceModel() != null) {
            getRaceModel().checkFinishFromActivity(this);
        }
    }

    /**
     * Save state for recreation.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_RACEID, this.raceId);
    }

    /**
     * Called when activity is being destroyed. It's important to stop capturing location,
     * when this screen disappear, because user has no other chance to stop it.
     */
    @Override
    protected void onDestroy() {
        Log.d("RaceTabbedActivity", "onDestroy called");
        if (isFinishing()) {

            if (this.mLoadRaceTask != null) {
                this.mLoadRaceTask.cancel(true);
            }

            // to be sure that race in not left in progress when activity is finished
            if (getRaceModel() != null) {
                getRaceModel().stopRace(this);
            }
        }
        super.onDestroy();
    }

    /**
     * Prevents activity dismiss when hardware back button is pressed and race is in progress (it's started in app).
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (getRaceModel() != null && getRaceModel().isStarted()) {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.back_key_warning_title))
                        .setMessage(getString(R.string.back_key_warning_message))
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

    /**
     * Updates visibility of software back button based on race status.
     */
    @Override
    public void onRaceFragmentUpdateVisibilityOfButtons() {
        if (getRaceModel().isStarted()) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Tab switching handle.
     */
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0:
                    return RaceFragment.newInstance();
                case 1:
                    return WalkersListFragment.newInstance();
                case 2:
                    return RaceMapFragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }
}
