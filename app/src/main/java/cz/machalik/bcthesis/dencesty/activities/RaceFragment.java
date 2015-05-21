package cz.machalik.bcthesis.dencesty.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import cz.machalik.bcthesis.dencesty.MyApplication;
import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.model.RaceModel;

/**
 * A race progress screen with user's progress in race and start/stop race buttons.
 *
 * <p>
 * A simple {@link Fragment} subclass.
 * Use the {@link RaceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RaceFragment extends Fragment {

    protected static final String TAG = "RaceFragment";

    private OnRaceFragmentInteractionListener mListener;

    /**
     * Receiver of Event queue size updates.
     */
    private BroadcastReceiver mUnsentCounterReceiver;

    /**
     * Receiver of Race Model data changes.
     */
    private BroadcastReceiver mRaceInfoChangedReceiver;

    // UI references.
    private Button mStartraceButton;
    private Button mEndraceButton;
    private TextView mDistanceTextView;
    private TextView mAvgSpeedTextView;
    private TextView mUnsentCounter;
    private TextView mLocationUpdatesCounter;

    /**
     * Use this factory method to create a new instance.
     *
     * @return A new instance of fragment RaceFragment.
     */
    public static RaceFragment newInstance() {
        Log.d(TAG, "newInstance called");
        RaceFragment fragment = new RaceFragment();
        return fragment;
    }

    /**
     * Required empty public constructor. Do not use it to create new instances.
     */
    public RaceFragment() {
    }

    /**
     * Race Model reference getter.
     * @return current Race Model
     */
    private RaceModel getRaceModel() {
        return MyApplication.get().getRaceModel();
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(Activity)} and before
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        // http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
        setRetainInstance(true);
    }

    /**
     * Called when a fragment is first attached to its activity.
     * {@link #onCreate(Bundle)} will be called after this.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnRaceFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnRaceFragmentInteractionListener");
        }
    }

    /**
     * Called when the fragment is no longer attached to its activity.  This
     * is called after {@link #onDestroy()}.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }

    /**
     * Creates view of fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_race, container, false);

        mStartraceButton = (Button) view.findViewById(R.id.startrace_button);
        mEndraceButton = (Button) view.findViewById(R.id.endrace_button);
        mDistanceTextView = (TextView) view.findViewById(R.id.distance_textview);
        mAvgSpeedTextView = (TextView) view.findViewById(R.id.avgspeed_textview);
        mUnsentCounter = (TextView) view.findViewById(R.id.unsent_textview);
        mLocationUpdatesCounter = (TextView) view.findViewById(R.id.loccounter_textview);

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

        return view;
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();

        registerBroadcastReceivers();

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
        // Stop race if race is over.
        getRaceModel().checkFinishFromActivity(getActivity());

        refreshRaceInfoValues();
        setUnsentCounter(EventUploaderService.getEventQueueSize());
        updateVisibilityOfButtons();
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onPause() {
        super.onPause();

        unregisterBroadcastReceivers();
    }

    /**
     * Registration of all broadcast receivers.
     */
    private void registerBroadcastReceivers() {
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
        LocalBroadcastManager.getInstance(getActivity())
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
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mRaceInfoChangedReceiver,
                        new IntentFilter(RaceModel.ACTION_RACE_INFO_CHANGED));
    }

    /**
     * Removing registration of all broadcast receivers.
     */
    private void unregisterBroadcastReceivers() {
        // unregister broadcast receivers
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mUnsentCounterReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRaceInfoChangedReceiver);
    }

    /**
     * Called when "Start Race" button is pressed.
     */
    private void startButtonPressed() {
        getRaceModel().startRace(getActivity());
        updateVisibilityOfButtons();
    }

    /**
     * Called when "Stop Race" button is pressed.
     */
    private void showDialogToEndRace() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        Log.d(TAG, "User manually stopped race");
                        endRace();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.are_you_sure_end_race))
                .setPositiveButton(getString(R.string.end_race), dialogClickListener)
                .setNegativeButton(getString(R.string.cancel), dialogClickListener)
                .show();
    }

    /**
     * Called when "Stop Race" confirmation button is pressed.
     */
    private void endRace() {
        Log.d(TAG, "EndRace called");
        getRaceModel().stopRace(getActivity());
        updateVisibilityOfButtons();
    }

    /**
     * Updates values in view.
     */
    private void refreshRaceInfoValues() {
        this.mDistanceTextView.setText(String.format("%,d m", getRaceModel().getRaceDistance()));
        this.mAvgSpeedTextView.setText(String.format("%.2f km/h", getRaceModel().getRaceAvgSpeed()));
        this.mLocationUpdatesCounter.setText(""+getRaceModel().getLocationUpdatesCounter());
    }

    /**
     * Updates value in "Unsent messages counter" label with new value.
     * @param numOfUnsentMessages new value of unsent counter
     */
    private void setUnsentCounter(int numOfUnsentMessages) {
        mUnsentCounter.setText(""+numOfUnsentMessages);
        if (numOfUnsentMessages > 0)
            mUnsentCounter.setTextColor(getResources().getColor(R.color.counter_highlighted));
        else
            mUnsentCounter.setTextColor(getResources().getColor(R.color.counter_default));
    }

    /**
     * Updates visibility of Start/Stop race button based on current race status.
     */
    private void updateVisibilityOfButtons() {
        if (this.mListener != null) {
            this.mListener.onRaceFragmentUpdateVisibilityOfButtons();
        }

        if (getRaceModel().isStarted()) {
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnRaceFragmentInteractionListener {
        public void onRaceFragmentUpdateVisibilityOfButtons();
    }

}
