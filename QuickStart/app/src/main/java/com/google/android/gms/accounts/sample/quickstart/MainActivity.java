/**
 * Copyright 2013, 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.accounts.sample.quickstart;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Google Play Services Accounts sample.
 *
 * Demonstrates Google Sign-In and usage of some other Google APIs. The sample utilizes
 * GoogleApiClient to store the Google Account selected by the user and shows how to retrieve the
 * associated Google Account ID. The Account ID should be used as the key to associate any local
 * or remote data to the Google Account. This will ensure logout actions do not mix user data.
 * Note that logout actions can occur without direct user intervention on this device, such as
 * when a Google Account is assigned a new primary email address in the Google Apps for Business
 * product.
 *
 * This sample app WILL NOT WORK unless you have:
 * 1.) Created an Application on the Google Developer Console
 * 2.) Defined a Consent Screen in the Google Developer Console for your app.
 * 3.) Registered your Android app's SHA1 certificate fingerprint and package name for your app
 *     (even for testing).
 * 4.) Enabled the Google+ API in the Developer Console.
 * Detailed instructions can be found here: https://developers.google.com/+/quickstart/android
 */
public class MainActivity extends FragmentActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, View.OnClickListener {

    protected static final String TAG = "google-account-sample";

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_SIGN_IN = 1;
    protected static final int STATE_IN_PROGRESS = 2;
    protected static final int RC_SIGN_IN = 0;
    protected static final int DIALOG_PLAY_SERVICES_ERROR = 0;
    protected static final String SAVED_PROGRESS = "sign_in_progress";

    /**
     * GoogleApiClient wraps our service connection to Google Play Services and provides access
     * to the user's sign in state as well as the Google's APIs.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * We use mSignInProgress to track whether user has clicked sign in. mSignInProgress can be
     * one of three values:
     * <p/>
     * STATE_DEFAULT: The default state of the application before the user has clicked 'sign in',
     * or after they have clicked 'sign out'.  In this state we will not attempt to resolve sign
     * in errors so we will display our Activity in a signed out state.
     *
     * STATE_SIGN_IN: This state indicates that the user has clicked 'sign in', so resolve
     * successive errors preventing sign in until the user has successfully authorized an account
     * for our app.
     *
     * STATE_IN_PROGRESS: This state indicates that we have started an intent to resolve an
     * error, and so we should not start further intents until the current intent completes.
     */
    protected int mSignInProgress;

    /**
     * Used to store the PendingIntent most recently returned by Google Play Services until the
     * user clicks 'sign in'.
     */
    protected PendingIntent mSignInIntent;

    /**
     * Used to store the error code most recently returned by Google Play Services until the user
     * clicks 'sign in'.
     */
    protected int mSignInError;

    protected SignInButton mSignInButton;
    protected Button mSignOutButton;
    protected Button mRevokeButton;
    protected TextView mStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
        mSignOutButton = (Button) findViewById(R.id.sign_out_button);
        mRevokeButton = (Button) findViewById(R.id.revoke_access_button);
        mStatus = (TextView) findViewById(R.id.sign_in_status);

        mSignInButton.setOnClickListener(this);
        mSignOutButton.setOnClickListener(this);
        mRevokeButton.setOnClickListener(this);

        if (savedInstanceState != null) {
            mSignInProgress = savedInstanceState.getInt(SAVED_PROGRESS, STATE_DEFAULT);
        }

        rebuildGoogleApiClient();
    }

    /**
     * Construct a client.
     *
     * @return un-connected client.
     */
    protected synchronized void rebuildGoogleApiClient() {
        // When we build the GoogleApiClient we specify where connected and connection failed
        // callbacks should be returned, which Google APIs our app uses and which OAuth 2.0
        // scopes our app requests.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope(Scopes.PROFILE))
                // TODO(developer): Specify any additional API Scopes or APIs you need here.
                // The GoogleApiClient will ensure these APIs are available, and the Scopes
                // are approved before invoking the onConnected callbacks.
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /**
     * Delete user data to comply with using Google Account Terms and Conditions.
     */
    protected void deleteUserData() {
        // This sample caches no user data however we would normally delete user data so that we
        // comply with Google developer policies.
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_PROGRESS, mSignInProgress);
    }

    @Override
    public void onClick(View view) {
        if (!mGoogleApiClient.isConnecting()) {
            // We only process button clicks when GoogleApiClient is not transitioning between
            // connected and not connected.
            switch (view.getId()) {
                case R.id.sign_in_button:
                    mStatus.setText(R.string.status_signing_in);
                    resolveSignInError();
                    break;
                case R.id.sign_out_button:
                    // We clear the default account on sign out so that Google Play Services will
                    // not return an onConnected callback without user interaction.
                    deleteUserData();
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    rebuildGoogleApiClient();
                    mGoogleApiClient.connect();
                    break;
                case R.id.revoke_access_button:
                    // After we revoke permissions for the user with a GoogleApiClient instance,
                    // we must discard it and create a new one.
                    deleteUserData();
                    Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    rebuildGoogleApiClient();
                    mGoogleApiClient.connect();
                    break;
            }
        }
    }

    /**
     * Called when the Activity successfully connects to Google Play Services. When the function
     * is triggered, an account was selected on the device, the selected account has granted
     * requested permissions to the app, and the app has established a connection to Google Play
     * Services.
     *
     * @param connectionHint can be inspected for additional connection info
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Reaching onConnected means we consider the user signed in and all APIs previously
        // specified are available.
        Log.i(TAG, "onConnected");

        // IMPORTANT NOTE: If you are storing any user data locally or even in a remote
        // application DO NOT associate it to the accountName (which is also an email address).
        // Associate the user data to the Google Account ID. Under some circumstances it is possible
        // for a Google Account to have the primary email address change.

        Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

        // TODO(developer): Check the account ID against any previous login locally.
        // TODO(developer): Delete the local data if the account ID differs.
        // TODO(developer): Construct local storage keyed on the account ID.

        mStatus.setText(String.format(getResources().getString(R.string
                .signed_in_as), currentPerson.getDisplayName()));
        mSignInButton.setEnabled(false);
        mSignOutButton.setEnabled(true);
        mRevokeButton.setEnabled(true);

        // Indicate that the sign in process is complete.
        mSignInProgress = STATE_DEFAULT;
    }

    /**
     * Called when the Activity could not connect to Google Play services. The callback indicates
     * that the user needs to select an account, grant permissions, or resolve an error in order
     * to sign in.
     *
     * @param result can be inspected to determine the cause of the failure
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());

        if (result.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            // An API requested for GoogleApiClient is not available. The device's current
            // configuration might not be supported with the requested API or a required component
            // may not be installed, such as the Android Wear application. You may need to use a
            // second GoogleApiClient to manage the application's optional APIs.
        } else if (mSignInProgress != STATE_IN_PROGRESS) {
            // We do not have an intent in progress so we should store the latest error
            // resolution intent for use when the sign in button is clicked.
            mSignInIntent = result.getResolution();
            mSignInError = result.getErrorCode();

            if (mSignInProgress == STATE_SIGN_IN) {
                // STATE_SIGN_IN indicates the user already clicked the sign in button so we
                // should continue processing errors until the user is signed in or they click
                // cancel.
                resolveSignInError();
            }
        }

        // In this sample we consider the user signed out when they do not have a connection to
        // Google Play services.
        onSignedOut();
    }

    /**
     * Starts an appropriate intent or dialog for user interaction to resolve the current error
     * preventing the user from being signed in.  This could be a dialog allowing the user to
     * select an account, an activity allowing the user to consent to the permissions being
     * requested by your app, a setting to enable device networking, etc.
     */
    protected void resolveSignInError() {
        if (mSignInIntent != null) {
            // We have an intent which will allow our user to sign in or resolve an error. For
            // example if the user needs to select an account to sign in with,
            // or if they need to  consent to the permissions your app is requesting.

            try {
                // Send the pending intent that we stored on the most recent OnConnectionFailed
                // callback.  This will allow the user to resolve the error currently preventing
                // our connection to Google Play Services.
                mSignInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (SendIntentException e) {
                Log.i(TAG, "Sign in intent could not be sent: "
                        + e.getLocalizedMessage());
                // The intent was canceled before it was sent.  Attempt to
                // connect to get an updated ConnectionResult.
                mSignInProgress = STATE_SIGN_IN;
                mGoogleApiClient.connect();
            }
        } else {
            // Google Play Services wasn't able to provide an intent for some error types,
            // so we show the default Google Play services error dialog which may still start an
            // intent on our behalf if the user can resolve the issue.
            showDialog(DIALOG_PLAY_SERVICES_ERROR);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    // If the error resolution was successful we should continue processing errors.
                    mSignInProgress = STATE_SIGN_IN;
                } else {
                    // If the error resolution was not successful or the user canceled,
                    // we should stop processing errors.
                    mSignInProgress = STATE_DEFAULT;
                }

                if (!mGoogleApiClient.isConnecting()) {
                    // If Google Play services resolved the issue with a dialog then onStart is
                    // not called so we need to re-attempt connection here.
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    /**
     * Update the UI to reflect that the user is signed out.
     */
    protected void onSignedOut() {
        mSignInButton.setEnabled(true);
        mSignOutButton.setEnabled(false);
        mRevokeButton.setEnabled(false);

        mStatus.setText(R.string.status_signed_out);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection or get a ConnectionResult that we can attempt
        // to resolve.
        mGoogleApiClient.connect();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PLAY_SERVICES_ERROR:
                if (GooglePlayServicesUtil.isUserRecoverableError(mSignInError)) {
                    return GooglePlayServicesUtil.getErrorDialog(
                            mSignInError,
                            this,
                            RC_SIGN_IN,
                            new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Log.e(TAG, "Google Play services resolution"
                                            + " cancelled");
                                    mSignInProgress = STATE_DEFAULT;
                                    mStatus.setText(R.string.status_signed_out);
                                }
                            });
                } else {
                    return new AlertDialog.Builder(this)
                            .setMessage(R.string.play_services_error)
                            .setPositiveButton(R.string.close,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Log.e(TAG, "Google Play services error could not be "
                                                    + "resolved: " + mSignInError);
                                            mSignInProgress = STATE_DEFAULT;
                                            mStatus.setText(R.string.status_signed_out);
                                        }
                                    }).create();
                }
            default:
                return super.onCreateDialog(id);
        }
    }
}
