package cz.machalik.bcthesis.dencesty.activities;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.Checkpoint;
import cz.machalik.bcthesis.dencesty.model.DistanceModel;
import cz.machalik.bcthesis.dencesty.model.RaceModel;
import cz.machalik.bcthesis.dencesty.model.WalkersModel;
import cz.machalik.bcthesis.dencesty.model.WalkersModel.Walker;

/**
 * A simple {@link MapFragment} subclass.
 * Use the {@link RaceMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RaceMapFragment extends MapFragment {

    protected static final String TAG = "RaceMapFragment";

    private RaceModel raceModel;
    private WalkersModel walkersModel;

    private BroadcastReceiver mDistanceChangedReceiver;
    private BroadcastReceiver mWalkersRefreshReceiver;

    private List<Marker> walkersMapMarkers = null;
    private Marker lastLocationUpdateMarker = null;

    private SimpleDateFormat timeFormatter = new SimpleDateFormat("H:mm");

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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (MapsInitializer.initialize(getActivity()) != ConnectionResult.SUCCESS ||
                getMap() == null) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.map_toast_missing_gplay_services),
                    Toast.LENGTH_LONG).show();
        } else {
            getMap().setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    getMap().setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    getMap().setMyLocationEnabled(true);
                    getMap().getUiSettings().setMapToolbarEnabled(false);

                    drawCheckpoints();
                    showWalkersOnMap();
                    onDistanceChangedNotification();
                }
            });
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isResumed() && getMap() != null) {
            getMap().setMyLocationEnabled(isVisibleToUser);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        registerBroadcastReceivers();
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterBroadcastReceivers();
    }

    private void registerBroadcastReceivers() {
        // Register broadcast receiver on distance updates
        mDistanceChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(DistanceModel.ACTION_DISTANCE_CHANGED)) {
                    onDistanceChangedNotification();
                }
            }
        };
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mDistanceChangedReceiver,
                        new IntentFilter(DistanceModel.ACTION_DISTANCE_CHANGED));

        // Register broadcast receiver on walkers model updates
        mWalkersRefreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(WalkersModel.ACTION_WALKERS_DID_REFRESHED)) {
                    showWalkersOnMap();
                }
            }
        };
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mWalkersRefreshReceiver,
                        new IntentFilter(WalkersModel.ACTION_WALKERS_DID_REFRESHED));
    }

    private void unregisterBroadcastReceivers() {
        // unregister broadcast receivers
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDistanceChangedReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mWalkersRefreshReceiver);
    }

    private void onDistanceChangedNotification() {
        Location location = DistanceModel.getLastKnownLocation();
        if (location == null) {
            return;
        }

        if (this.lastLocationUpdateMarker != null) {
            this.lastLocationUpdateMarker.remove();
        }

        int distance = this.raceModel.getRaceDistance();
        String markerTitle = String.format(getActivity().getString(R.string.map_marker_title_distance), distance);
        String markerSnippet = String.format(getActivity().getString(R.string.map_marker_snippet_distance),
                timeFormatter.format(new Date(location.getTime())));
        final Marker marker = getMap().addMarker(new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .draggable(false)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                .title(markerTitle)
                .snippet(markerSnippet));
        this.lastLocationUpdateMarker = marker;
    }

    private void showWalkersOnMap() {
        // Remove walkers markers from map
        if (this.walkersMapMarkers != null) {
            for (Marker m : walkersMapMarkers) {
                m.remove();
            }
        }

        Walker[] walkersAhead = this.walkersModel.getWalkersAhead();
        Walker[] walkersBehind = this.walkersModel.getWalkersBehind();
        Walker presentWalker = this.walkersModel.getPresentWalker();

        this.walkersMapMarkers = new ArrayList<>(walkersAhead.length + walkersBehind.length);

        for (Walker walker : walkersAhead) {
            // Ignore walkers without location (with default zero location)
            if (walker.latitude == 0 || walker.longitude == 0) {
                continue;
            }

            String markerTitle = walker.name;
            int distance = walker.distance - presentWalker.distance;
            String markerSnippet = String.format(getActivity().getString(R.string.map_marker_snippet_walker_ahead), distance);
            final Marker marker = getMap().addMarker(new MarkerOptions()
                .position(new LatLng(walker.latitude, walker.longitude))
                .draggable(false)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                .title(markerTitle)
                .snippet(markerSnippet));
            this.walkersMapMarkers.add(marker);
        }
        for (Walker walker : walkersBehind) {
            // Ignore walkers without location (with default zero location)
            if (walker.latitude == 0 || walker.longitude == 0) {
                continue;
            }

            String markerTitle = walker.name;
            int distance = presentWalker.distance - walker.distance;
            String markerSnippet = String.format(getActivity().getString(R.string.map_marker_snippet_walker_behind), distance);
            final Marker marker = getMap().addMarker(new MarkerOptions()
                    .position(new LatLng(walker.latitude, walker.longitude))
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                    .title(markerTitle)
                    .snippet(markerSnippet));
            this.walkersMapMarkers.add(marker);
        }

        if (this.lastLocationUpdateMarker == null && presentWalker.latitude != 0 && presentWalker.longitude != 0) {
            String markerTitle = String.format(getActivity().getString(R.string.map_marker_title_presentwalker), presentWalker.distance);
            String markerSnippet = String.format(getActivity().getString(R.string.map_marker_snippet_presentwalker),
                    timeFormatter.format(presentWalker.updatedAt));
            final Marker marker = getMap().addMarker(new MarkerOptions()
                    .position(new LatLng(presentWalker.latitude, presentWalker.longitude))
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                    .title(markerTitle)
                    .snippet(markerSnippet));
            this.lastLocationUpdateMarker = marker;
        }
    }

    private void drawCheckpoints() {
        Checkpoint[] checkpoints = this.raceModel.getCheckpoints();

        if (checkpoints == null || checkpoints.length == 0) {
            return;
        }

        List<LatLng> coordinates = new ArrayList<>(checkpoints.length);
        for (int i = 0; i < checkpoints.length; i++) {
            LatLng coordinate = new LatLng(checkpoints[i].latitude, checkpoints[i].longitude);
            coordinates.add(coordinate);
        }

        // Instantiates a new Polyline object and adds points to define a polyline
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(coordinates);
        polylineOptions.color(0x800000FF);
        polylineOptions.width(6);

        getMap().addPolyline(polylineOptions);

        // Add marker for start
        LatLng startLocation = coordinates.get(0);
        getMap().addMarker(new MarkerOptions()
                .position(startLocation)
                .draggable(false)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title(getActivity().getString(R.string.map_marker_start)));

        // Add marker for finish
        LatLng finishLocation = coordinates.get(coordinates.size()-1);
        getMap().addMarker(new MarkerOptions()
                .position(finishLocation)
                .draggable(false)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .title(getActivity().getString(R.string.map_marker_finish)));

        // Zoom to fit markers
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(startLocation);
        builder.include(finishLocation);
        LatLngBounds bounds = builder.build();
        int padding = 100; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        getMap().moveCamera(cu);
    }

}
