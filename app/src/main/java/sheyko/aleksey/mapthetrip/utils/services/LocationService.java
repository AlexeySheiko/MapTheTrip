package sheyko.aleksey.mapthetrip.utils.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.parse.ParseObject;

import sheyko.aleksey.mapthetrip.utils.recievers.AlarmReceiver;


public class LocationService extends Service
        implements ConnectionCallbacks, LocationListener {

    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;

    private String mTripId;
    private String mLatitude;
    private String mLongitude;
    private String mAltitude;
    private String mAccuracy;

    private boolean isTripJustStarted = true;

    public LocationService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getStringExtra("Trip ID") != null ||
                intent.getStringExtra("Action") == null) {
            // Request to start sending location updates catched,
            // so we need to connect Location Client
            createLocationClient().connect();
            // Set trip ID from onTripRegistered() callback
            mTripId = intent.getStringExtra("Trip ID");

        } else if (intent.getStringExtra("Action") != null) {
            // Location client connected, and we get a callback here.
            // In case there's no network available, we will store data in
            // local database (and then send it «SummaryActivity»)
            pinCurrentCoordinates();
        }
        return START_STICKY;
    }

    private void pinCurrentCoordinates() {
        ParseObject coordinates = new ParseObject("Coordinates");
        coordinates.put("trip_id", mTripId);
        coordinates.put("latitude", mLatitude);
        coordinates.put("longitude", mLongitude);
        coordinates.put("altitude", mAltitude);
        coordinates.put("accuracy", mAccuracy);
        coordinates.pinInBackground();
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates(mLocationClient);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pinCurrentCoordinates();
        stopLocationUpdates(mLocationClient);
        cancelAlarm(this);
    }

    private static void startAlarm(Context context) {
        Intent receiverIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 123456789, receiverIntent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 10, sender); // Millisec * Second * Minute
    }

    private static void cancelAlarm(Context context) {
        Intent receiverIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 123456789, receiverIntent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    private LocationClient createLocationClient() {
        // Create location client
        mLocationClient = new LocationClient(this, this, null);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        int UPDATE_INTERVAL = 5 * 1000; // 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(UPDATE_INTERVAL);
        return mLocationClient;
    }

    private void startLocationUpdates(LocationClient client) {
        client.requestLocationUpdates(mLocationRequest, this);
    }

    private void stopLocationUpdates(LocationClient client) {
        if (mLocationClient.isConnected())
            client.removeLocationUpdates(this);
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isTripJustStarted) {
            startAlarm(this);
            isTripJustStarted = false;
        }
        updateMapFragment(location);

        mLatitude = location.getLatitude() + "";
        mLongitude = location.getLongitude() + "";
        mAltitude = location.getAltitude() + "";
        mAccuracy = location.getAccuracy() + "";
    }

    private void updateMapFragment(Location location) {
        Intent intent = new Intent("LocationUpdates");
        // You can also include some extra data.
        Bundle b = new Bundle();
        b.putParcelable("Location", location);
        intent.putExtra("Location", b);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
