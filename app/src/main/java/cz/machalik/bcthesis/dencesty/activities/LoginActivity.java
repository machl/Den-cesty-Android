package cz.machalik.bcthesis.dencesty.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import cz.machalik.bcthesis.dencesty.R;
import cz.machalik.bcthesis.dencesty.model.User;
import cz.machalik.bcthesis.dencesty.model.User.LoginResult;

/**
 * A login screen that offers login via email/password.
 *
 * @author Lukáš Machalík
 */
public class LoginActivity extends Activity {

    protected static final String TAG = "LoginActivity";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    /**
     * Called when the activity is starting.  This is where most initialization
     * should go.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email_textfield);

        mPasswordView = (EditText) findViewById(R.id.password_textfield);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptNewLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptNewLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);


        attemptAutomaticLogin();
    }

    /**
     * Attempt automatic login with already saved credentials.
     */
    private void attemptAutomaticLogin() {
        if (User.get().hasSavedCredentials(this)) {
            String email = User.get().getSavedCredentialsEmail(this);
            String password = User.get().getSavedCredentialsPassword(this);

            mEmailView.setText(email);

            Log.i(TAG, "Attempting automatic login");
            performLoginTask(email, password);
        }
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptNewLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            performLoginTask(email, password);
        }
    }

    /**
     * Simple email validity check.
     */
    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Show a progress spinner, and kick off a background task to
     * perform the user login attempt.
     *
     * @param email entered email
     * @param password entered password
     */
    private void performLoginTask(String email, String password) {
        showProgress(true);
        mAuthTask = new UserLoginTask(this, email, password);
        mAuthTask.execute((Void) null);
    }

    /**
     * Called on successful login attempt.
     */
    private void onSuccessfulLogin() {
        Intent intent = new Intent(this, RacesListActivity.class);
        startActivity(intent);
    }

    /**
     * Called on failed login attempt due to incorrect credentials.
     */
    private void onFailedLogin() {
        mPasswordView.setError(getString(R.string.error_incorrect_password));
        mPasswordView.requestFocus();
    }

    /**
     * Called on failed login attempt due to connection problems.
     */
    private void onConnectionError() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.connection_error_title))
                .setMessage(getString(R.string.connection_error_message))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Represents an asynchronous login task used to authenticate the user.
     */
    private class UserLoginTask extends AsyncTask<Void, Void, LoginResult> {

        private final Context mContext;
        private final String mEmail;
        private final String mPassword;

        public UserLoginTask(Context context, String email, String password) {
            mContext = context;
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected LoginResult doInBackground(Void... params) {
            return User.get().attemptLogin(mContext, mEmail, mPassword);
        }

        @Override
        protected void onPostExecute(final LoginResult result) {
            mAuthTask = null;
            showProgress(false);

            switch (result) {
                case SUCCESS:
                    onSuccessfulLogin();
                    break;
                case FAILED:
                    onFailedLogin();
                    break;
                case CONNECTION_ERROR:
                    onConnectionError();
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}



