package cz.machalik.bcthesis.dencesty.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.RaceModel;

public class FirstScreenActivity extends Activity {

    protected static final String TAG = "FirstScreenActivity";

    // UI references.
    private TextView mFullnameTextView;
    private TextView mUsernameTextView;
    private Button mStartRaceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_screen);

        mFullnameTextView = (TextView) findViewById(R.id.fullname_textview);
        mUsernameTextView = (TextView) findViewById(R.id.username_textview);

        mStartRaceButton = (Button) findViewById(R.id.start_race_button);
        mStartRaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartRaceButtonHandler(v);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mFullnameTextView.setText(RaceModel.getInstance().getWalkerFullName());
        mUsernameTextView.setText(RaceModel.getInstance().getWalkerUsername());
    }

    /**
     * Handles the Start Updates button and requests start of location updates.
     */
    private void onStartRaceButtonHandler(View view) {
        // Requests location updates from the BackgroundLocationService.
        RaceModel.getInstance().startRace(this);

        Intent intent = new Intent(this, RaceActivity.class);
        startActivity(intent);
    }

}
