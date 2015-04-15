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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.events.EventUploaderService;
import cz.machalik.bcthesis.dencesty.model.RaceModel;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RaceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RaceFragment extends Fragment {

    protected static final String TAG = "RaceFragment";

    private OnRaceFragmentInteractionListener mListener;

    private RaceModel raceModel;

    private BroadcastReceiver mUnsentCounterReceiver;
    private BroadcastReceiver mRaceInfoChangedReceiver;

    // UI references.
    private Button mStartraceButton;
    private Button mEndraceButton;
    private TextView mDistanceTextView;
    private TextView mAvgSpeedTextView;
    private TextView mUnsentCounter;
    private TextView mLocationUpdatesCounter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param raceModel Current RaceModel.
     * @return A new instance of fragment RaceFragment.
     */
    public static RaceFragment newInstance(RaceModel raceModel) {
        RaceFragment fragment = new RaceFragment();
        fragment.raceModel = raceModel;
        return fragment;
    }

    public RaceFragment() {
        // Required empty public constructor
    }

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

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }

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

    @Override
    public void onResume() {
        super.onResume();

        registerBroadcastReceivers();

        onDidAppear();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            onDidAppear();
        }
    }

    private void onDidAppear() {
        // Stop race if race is over.
        this.raceModel.checkFinish(getActivity());

        refreshRaceInfoValues();
        setUnsentCounter(EventUploaderService.getEventQueueSize());
        updateVisibilityOfButtons();
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterBroadcastReceivers();
    }

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

    private void unregisterBroadcastReceivers() {
        // unregister broadcast receivers
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mUnsentCounterReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRaceInfoChangedReceiver);
    }


    private void startButtonPressed() {
        this.raceModel.startRace(getActivity());
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.are_you_sure_end_race))
                .setPositiveButton(getString(R.string.end_race), dialogClickListener)
                .setNegativeButton(getString(R.string.cancel), dialogClickListener)
                .show();
    }

    private void endRace() {
        this.raceModel.stopRace(getActivity());
        updateVisibilityOfButtons();
    }

    private void refreshRaceInfoValues() {
        this.mDistanceTextView.setText(String.format("%d m", this.raceModel.getRaceDistance()));
        this.mAvgSpeedTextView.setText(String.format("%.2f km/h", this.raceModel.getRaceAvgSpeed()));
        this.mLocationUpdatesCounter.setText(""+this.raceModel.getLocationUpdatesCounter());
    }

    private void setUnsentCounter(int numOfUnsentMessages) {
        mUnsentCounter.setText(""+numOfUnsentMessages);
        if (numOfUnsentMessages > 0)
            mUnsentCounter.setTextColor(getResources().getColor(R.color.counter_highlighted));
        else
            mUnsentCounter.setTextColor(getResources().getColor(R.color.counter_default));
    }

    private void updateVisibilityOfButtons() {
        if (this.mListener != null) {
            this.mListener.onRaceFragmentUpdateVisibilityOfButtons();
        }

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
