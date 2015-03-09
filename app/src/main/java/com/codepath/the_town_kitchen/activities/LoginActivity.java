package com.codepath.the_town_kitchen.activities;


//facebook

import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import com.codepath.the_town_kitchen.R;
import com.codepath.the_town_kitchen.TheTownKitchenApplication;
import com.codepath.the_town_kitchen.models.User;
import com.codepath.the_town_kitchen.net.FacebookApi;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;

import org.json.JSONObject;

import java.util.Arrays;

public class LoginActivity extends ActionBarActivity implements OnClickListener,
        ConnectionCallbacks, OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 0;
    private static final String TAG = LoginActivity.class.getSimpleName();

    private UiLifecycleHelper uiHelper;
    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    /**
     * A flag indicating that a PendingIntent is in progress and prevents us
     * from starting further intents.
     */
    private boolean mIntentInProgress;

    private boolean mSignInClicked;
    private boolean mLoggedIn;
    private ConnectionResult mConnectionResult;

    private SignInButton googleLoginBtn;

    LoginButton facebookLoginBtn;
    private boolean isResumed = false;
   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);


        setContentView(R.layout.activity_login);


        facebookLoginBtn = (LoginButton) findViewById(R.id.fb_login_button);
        facebookLoginBtn.setApplicationId(getResources().getString(R.string.FACEBOOK_APP_ID));
        facebookLoginBtn.setReadPermissions(Arrays.asList("email", "public_profile"));

        facebookLoginBtn.setUserInfoChangedCallback(TheTownKitchenApplication.getFaceBookApi()
                        .getUserInfoChangedCallback(facebookApiHandler));

        googleLoginBtn = (SignInButton) findViewById(R.id.google_login_button);
        googleLoginBtn.setOnClickListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
    }
    
    private FacebookApi.IResponseHandler facebookApiHandler = new FacebookApi.IResponseHandler() {
        @Override
        public void handle(JSONObject json) {
            setCurrentUser(json);
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();
        isResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHelper.onPause();
        isResumed = false;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    private void resolveSignInError() {
        if (mConnectionResult != null && mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
            } catch (SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this,
                    0).show();
            return;
        }

        if (!mIntentInProgress) {
            // Store the ConnectionResult for later usage
            mConnectionResult = result;

            if (mSignInClicked) {
                resolveSignInError();
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, requestCode, intent);
        uiHelper.onActivityResult(requestCode, responseCode, intent);
        if (requestCode == RC_SIGN_IN) {
            if (responseCode != RESULT_OK) {
                mSignInClicked = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }

           
        }
    }

    @Override
    public void onConnected(Bundle arg0) {
        mSignInClicked = false;
        mLoggedIn = true;
        // Get user's information
        TheTownKitchenApplication.getCurrentUser().requestCurrentUserFromGoogle(mGoogleApiClient);

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
       
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    /**
     * Button on click listener
     * */
    @Override
    public void onClick(View v) {
        signInWithGplus();

    }

    /**
     * Sign-in into google
     * */
    private void signInWithGplus() {
        if (!mGoogleApiClient.isConnecting()) {
            mSignInClicked = true;
            resolveSignInError();
        }
        
        if(mLoggedIn){
            if(TheTownKitchenApplication.getCurrentUser().getUser() != null){
                Intent intent = new Intent(this, MealListActivity.class);
                startActivity(intent);
            }
        }
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        // Only make changes if the activity is visible
        if (isResumed) {
            // If the session state is open:
            if (state.isOpened()) {
                Log.i(TAG, "state opended");
                if (TheTownKitchenApplication.getCurrentUser().getUser() == null) {
                    TheTownKitchenApplication.getFaceBookApi().getUser(session, facebookApiHandler);
                }
            }
            else if (state.isClosed()) {
                // If the session state is closed:
                Log.i(TAG, "state closed");
            }
        }
    }

    private void setCurrentUser(JSONObject response) {
        JSONObject json = response;
        User user = User.fromJson(json);
        TheTownKitchenApplication.getCurrentUser().setUser(user);
        Intent intent = new Intent(this, MealListActivity.class);
        startActivity(intent);
    }


    private Session.StatusCallback callback =  new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };


}