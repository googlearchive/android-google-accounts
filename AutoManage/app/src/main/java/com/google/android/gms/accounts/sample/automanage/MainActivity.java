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

package com.google.android.gms.accounts.sample.automanage;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

/**
 * Google Play Services Accounts sample.
 *
 * Demonstrates Google Sign-In and the usage of some of the Google APIs available in Google Play
 * services. The application saves whether the user was last signed into the app to determine
 * whether to try to sign them in again. The sample utilizes GoogleApiClient to store the
 * Google Account selected by the user during sign-in. It also shows how to retrieve the
 * associated Google Account ID. The Account ID should be used as the key to associate any local or
 * remote data to the Google Account. This will ensure logout actions do not mix user data. Note
 * that logout actions can occur without direct user intervention on this device, such as when a
 * Google Account is assigned a new primary email address in Google for Work.
 *
 * This sample app WILL NOT WORK unless you have:
 * 1.) Created an Application on the Google Developer Console
 * 2.) Defined a Consent Screen in the Google Developer Console for your app.
 * 3.) Registered your Android app's SHA1 certificate fingerprint and package name for your app
 * (even for testing).
 * 4.) Enabled the Google+ API in the Developer Console.
 * Detailed instructions can be found here: https://developers.google.com/+/quickstart/android
 */
public class MainActivity extends FragmentActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, View.OnClickListener {

    protected static final String TAG = "google-account-sample";

    private static final String SHARED_PREFS = "GoogleAccountSamplePrefs";

    /**
     * Preference that tracks whether the user is currently signed into the app.
     * Specifically, if a user signs into the app via a Google Account and then comes back to it
     * later this indicates they were last signed in. This preference is used to detetermine
     * whether to initiate the GoogleApiClient connection immediately upon opening the activity.
     * This logic prevents the user's first experience with your app from being an OAuth2 consent
     * dialog.
     */
    private static final String PREFS_IS_SIGNED_IN = "IS_SIGNED_IN";

    /**
     * GoogleApiClient is a service connection to Google Play services and provides access
     * to the user's OAuth2 and API availability state for the APIs and scopes requested.
     */
    protected GoogleApiClient mGoogleApiClient;

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

        if (isSignedIn()) {
            rebuildGoogleApiClient();
        }
    }

    /**
     * Construct a client using AutoManage functionality.
     */
    protected synchronized void rebuildGoogleApiClient() {
        // When we build the GoogleApiClient we specify where connected and connection failed
        // callbacks should be returned, which Google APIs our app uses and which OAuth 2.0
        // scopes our app requests. Since we are using enableAutoManage to register the failed
        // connection listener we only get failed connection when auto-resolution attempts were not
        // successful or possible.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        0 /* googleApiClientId used when auto-managing multiple googleApiClients */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this /* ConnectionCallbacks */)
                .addApi(Plus.API)
                .addScope(new Scope(Scopes.PLUS_ME))
                        // TODO(developer): Specify any additional API Scopes or APIs you need here.
                        // The GoogleApiClient will ensure these APIs are available, and the Scopes
                        // are approved before invoking the onConnected callbacks.
                .build();
    }

    /**
     * Delete user data to comply with using Google Account Terms and Conditions.
     */
    protected void deleteUserData() {
        // TODO(developer): This sample caches no user data however we would normally delete user
        // data so that we comply with Google developer policies.
    }

    @Override
    public void onClick(View view) {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnecting()) {
            // We only process button clicks when GoogleApiClient is not transitioning between
            // connected and not connected.
            switch (view.getId()) {
                case R.id.sign_in_button:
                    rebuildGoogleApiClient();
                    mStatus.setText(R.string.status_signing_in);
                    break;
                case R.id.sign_out_button:
                    // We clear the default account on sign out so that Google Play Services will
                    // not return an onConnected callback without user interaction.
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);

                    // We must stop auto managing before disconnecting a the client.  Disconnecting
                    // an auto managed client causes a fatal exception.
                    mGoogleApiClient.stopAutoManage(this);

                    mGoogleApiClient.disconnect();
                    mGoogleApiClient = null;

                    onSignedOut();
                    break;
                case R.id.revoke_access_button:
                    deleteUserData();

                    // After we revoke permissions for the user with a GoogleApiClient instance,
                    // we must discard it and create a new one.
                    Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);

                    // We must stop auto managing before disconnecting a the client.  Disconnecting
                    // an auto managed client causes a fatal exception.
                    mGoogleApiClient.stopAutoManage(this);

                    mGoogleApiClient.disconnect();
                    mGoogleApiClient = null;

                    onSignedOut();
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

        // IMPORTANT NOTE: If you are storing any user data locally or even in a remote
        // application DO NOT associate it to the accountName (which is also an email address).
        // Associate the user data to the Google Account ID. Under some circumstances it is possible
        // for a Google Account to have the primary email address change.

        Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

        // The logic below ensures you won't mix accounts if a user switches between Google Accounts.
        // TODO(developer): Check the account ID against any previous login locally.
        // TODO(developer): Delete the local data if the account ID differs.
        // TODO(developer): Construct local storage keyed on the account ID.

        onSignedIn(currentPerson);
    }

    /**
     * Called when the Activity could not connect to Google Play services AND the auto manager
     * could not resolve the error automatically.
     *
     * @param result can be inspected to determine the cause of the failure
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed. Since this sample is the AutoManage sample only unresolvable errors
        // will be returned.

        Log.i(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());

        if (result.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            // An API requested for GoogleApiClient is not available. The device's current
            // configuration might not be supported with the requested API or a required component
            // may not be installed, such as the Android Wear application. You may need to use a
            // second GoogleApiClient to manage the application's optional APIs.
            Log.i(TAG, "onConnectionFailed because an API was unavailable");
        }

        // In this sample we consider the user signed out when they do not have a connection to
        // Google Play Services.
        onSignedOut();
    }

    /**
     * Update the UI to reflect that the user is signed out of the app.
     */
    protected void onSignedOut() {
        storeSignInState(false);
        mSignInButton.setEnabled(true);
        mSignOutButton.setEnabled(false);
        mRevokeButton.setEnabled(false);

        mStatus.setText(R.string.status_signed_out);
    }

    /**
     * Update the UI to reflect that the user is signed into the app.
     */
    protected void onSignedIn(Person currentPerson) {
        storeSignInState(true);
        mSignInButton.setEnabled(false);
        mSignOutButton.setEnabled(true);
        mRevokeButton.setEnabled(true);

        mStatus.setText(String.format(getResources().getString(R.string
                .signed_in_as), currentPerson.getDisplayName()));
    }

    /**
     * The connection to Google Play services was lost for some reason, the auto management
     * facility will try to reconnect automatically since we registered with autoManage. This
     * callback should be used to suspend use of the {@link GoogleApiClient} until it becomes
     * connected again.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        // TODO(developer): Implement logic to halt API calls to Google Play services,
        // including cancelling AsyncTasks or Intents that might be working with the
        // GoogleApiClient.
    }

    /**
     * Returns whether the user is signed into the app.
     */
    private boolean isSignedIn() {
        Context context = getApplicationContext();
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREFS,
                MODE_PRIVATE);
        return sharedPrefs.getBoolean(PREFS_IS_SIGNED_IN, false);
    }

    /**
     * Changes the user's app sign in state.
     *
     * @param signedIn Whether the user is signed in.
     */
    private void storeSignInState(boolean signedIn) {
        Context context = getApplicationContext();
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(PREFS_IS_SIGNED_IN, signedIn);
        editor.apply();
    }
}