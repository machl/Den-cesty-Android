package cz.machalik.bcthesis.dencesty.activities;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.MapFragment;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.RaceModel;
import cz.machalik.bcthesis.dencesty.model.WalkersModel;

/**
 * A simple {@link MapFragment} subclass.
 * Use the {@link RaceMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RaceMapFragment extends MapFragment {

    private RaceModel raceModel;
    private WalkersModel walkersModel;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param raceModel Race model.
     * @param walkersModel Walkers model.
     * @return A new instance of fragment RaceMapFragment.
     */
    public static RaceMapFragment newInstance(RaceModel raceModel, WalkersModel walkersModel) {
        RaceMapFragment fragment = new RaceMapFragment();
        fragment.raceModel = raceModel;
        fragment.walkersModel = walkersModel;
        return fragment;
    }

    public RaceMapFragment() {
        // Required empty public constructor
    }

    /*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_race_map, container, false);
    } */


}
