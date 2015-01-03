package cz.machalik.bcthesis.dencesty.activities;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.RaceModel;

/**
 * Test activity.
 *
 * Lukáš Machalík
 */
public class RaceActivity extends ActionBarActivity {
    protected static final String TAG = "RaceActivity";

    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;
    protected TextView myLogTextView;
    protected Button loginButton;
    protected EditText emailTextField;
    protected EditText passwordTextField;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;


    private int counter = 0;
    private void addLocUpdateToLog(String lat, String lon, String time) {
        CharSequence old = myLogTextView.getText();
        myLogTextView.setText(counter + ": " + lat + ", " + lon + ", " + time + "\n" + old);
        counter++;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);
        myLogTextView = (TextView) findViewById(R.id.my_log);

        emailTextField = (EditText) findViewById(R.id.emailTextField);
        passwordTextField = (EditText) findViewById(R.id.passwordTextField);
        loginButton = (Button) findViewById(R.id.login_button);

        View.OnFocusChangeListener ofcl = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        };
        emailTextField.setOnFocusChangeListener(ofcl);
        passwordTextField.setOnFocusChangeListener(ofcl);

        mRequestingLocationUpdates = false;
        setButtonsEnabledState();
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            setButtonsEnabledState();
            stopLocationUpdates();
        }
    }

    public void loginButtonHandler(View view) {
        new LoginAsyncTask(this).execute(emailTextField.getText().toString(),
                                         passwordTextField.getText().toString());
    }

    /**
     * Requests location updates from the BackgroundLocationService.
     */
    protected void startLocationUpdates() {
        RaceModel.getInstance().startRace(this);
        mRequestingLocationUpdates = true;
    }

    /**
     * Removes location updates from the BackgroundLocationService.
     */
    protected void stopLocationUpdates() {
        RaceModel.getInstance().stopRace(this);
        mRequestingLocationUpdates = false;
    }

    /**
     * Ensures that only one button is enabled at any time. The Start Updates button is enabled
     * if the user is not requesting location updates. The Stop Updates button is enabled if the
     * user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mRequestingLocationUpdates)
            stopLocationUpdates();
        super.onDestroy();
    }

    private class LoginAsyncTask extends AsyncTask<String, Integer, Boolean> {

        private Context context;
        public LoginAsyncTask (Context context){
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length != 2) {
                Log.e(TAG, "Wrong number of params on execute method of " + getClass().getName());
                return false;
            }

            String email = params[0];
            String password = params[1];

            return RaceModel.getInstance().login(context, email, password);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success)
                Log.i(TAG, "Successful login");
            else
                Log.i(TAG, "Failed login");
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_race, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
