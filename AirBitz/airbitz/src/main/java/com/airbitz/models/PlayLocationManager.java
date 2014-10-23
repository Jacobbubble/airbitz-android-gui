package com.airbitz.models;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.airbitz.models.CurrentLocationManager.OnLocationChange;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by matt on 6/30/14.
 */
public class PlayLocationManager implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    static String TAG = PlayLocationManager.class.getSimpleName();

    private static PlayLocationManager mInstance = null;
    private LocationRequest mLocationRequest;
    private LocationClient locationClient;
    private Location mCurrentLocation;
    private Context mContext;
    // Callback interface for adding and removing location change listeners
    private List<OnLocationChange> mObservers = new CopyOnWriteArrayList<OnLocationChange>();

    public PlayLocationManager(Context context) {
        mContext = context;
    }

    public static PlayLocationManager getLocationManager(Context context) {
        if (null == mInstance) {
            mInstance = new PlayLocationManager(context);
        }
        return mInstance;
    }

    public void addLocationChangeListener(OnLocationChange listener) {
        if (mObservers.isEmpty()) {
            attemptConnection();
        }
        if (!mObservers.contains(listener)) {
            mObservers.add(listener);
            Log.d(TAG, "Listener added: " + listener);
        }
        if (null != listener && null != mCurrentLocation) {
            listener.OnCurrentLocationChange(mCurrentLocation);
        }
    }

    public void removeLocationChangeListener(OnLocationChange listener) {
        mObservers.remove(listener);
        Log.d(TAG, "Listener removed: " + listener);
        if (mObservers.size() <= 0) {
            locationClient.disconnect();
        }
    }

    public Location getLocation() {
        if (null == mCurrentLocation && locationClient != null && locationClient.isConnected()) {
            mCurrentLocation = locationClient.getLastLocation();
        }
        return mCurrentLocation;
    }

    public void attemptConnection() {
        if (locationClient == null || !locationClient.isConnected()) {
            Log.d(TAG, "Attempting connection");
            locationClient = new LocationClient(mContext, this, this);
            locationClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected.");
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(1000 * 60 * 60);
        mLocationRequest.setFastestInterval(30000);
        locationClient.requestLocationUpdates(mLocationRequest, this);
        mCurrentLocation = locationClient.getLastLocation();
        if (mCurrentLocation != null) {
            onLocationChanged(mCurrentLocation);
        }
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "Disconnected. Please re-connect.");
        if (mObservers.size() != 0) {
            attemptConnection();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged");
        if (location.hasAccuracy() && !mObservers.isEmpty()) {
            mCurrentLocation = location;
            Log.d(TAG, "CUR LOC: " + mCurrentLocation.getLatitude() + "; "
                                   + mCurrentLocation.getLongitude());

            Iterator<OnLocationChange> i = mObservers.iterator();
            while (i.hasNext()) {
                i.next().OnCurrentLocationChange(mCurrentLocation);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection to LocationClient failed");
    }
}
