package channelier.locationtrackingapp;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AppLocationService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "App Location Service";

    private String customer_id;
    private String lastsyncdate;
    private String email, password;
    protected LocationManager locationManager;
    Location tempLocation;
    static Context context;
    static double latitude; // latitude
    static double longitude; // longitude
    String location_timestamp = "";
    String macAddress = "";
    static String date;
    private Looper mServiceLooper;
    LocationManager lm;
    Handler handler;

    Notification notification;
    private static final long MIN_DISTANCE_FOR_UPDATE = 1; // in Meters
    private static final long MIN_TIME_FOR_UPDATE = 300 * 1 * 1000; // in milliseconds

    private GoogleApiClient mGoogleApiClient;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    public Location mLastLocation;
    private LocationRequest mLocationRequest;
    private final int LOCATION_PERMISSION = 1;
    Runnable runnable;
    private Intent intent;

    public AppLocationService() {
        super("AppLocationService");
    }

    public AppLocationService(String name) {
        super(name);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        this.intent = intent;
        context = getApplicationContext();
        //startForeground(123, createForegroundNotification());
        insertLog("service started onhandle" + getTimeStamp());
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        buildGoogleApiClient();
        //AlarmReceiver.completeWakefulIntent(intent);
    }

    //fetching locations
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    public String getTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "google api client connected and in OnConnected" + getTimeStamp());
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(createLocationRequest());
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        getLatLong();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.d(TAG, "location settings problem");
                        insertLog("location settings problem");
                        createNotification();
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.d(TAG, "location settings problem unavailable");
                        insertLog("location settings problem unavailable");
                        break;
                }
            }
        });


    }

    private void getLatLong() {
        if (mGoogleApiClient.isConnected()) {
            int permissionCheck = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            insertLog("getlatlong permission check" + permissionCheck + " , " + getTimeStamp());
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                insertLog("getlatlong permission available" + getTimeStamp());
                mLastLocation = LocationServices.FusedLocationApi
                        .getLastLocation(mGoogleApiClient);
                insertLog("getlatlong location is " + mLastLocation + " , " + getTimeStamp());
                date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                //if (time.compareToIgnoreCase("08:00:00") > 0 && time.compareToIgnoreCase("20:00:00") < 0) {
                    /*if (mLastLocation != null) {
                        LocationAsyncTask locationAsyncTask = new LocationAsyncTask(mLastLocation.getLatitude(), mLastLocation.getLongitude(), date);
                        locationAsyncTask.execute();
                    } else {*/
                LocationServices.FusedLocationApi
                        .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                insertLog("getlatlong location is afterloctionupdate began " + getTimeStamp());
                //LocationAsyncTask locationAsyncTask = new LocationAsyncTask(0, 0, date);
                //locationAsyncTask.execute();
                //}
                //}
            }
            //mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = getApplicationContext();
        //startForeground(123, createForegroundNotification());
        insertLog("service started " + getTimeStamp());
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        buildGoogleApiClient();
        /*handler = new Handler();
        runnable = new Runnable() {


            @Override
            public void run() {
                try {
                    //if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
                    insertLog("google api client not connected " + getTimeStamp());
                    Log.d(TAG, "google api client not connected " + getTimeStamp());
                    if (mGoogleApiClient != null) {
                        mGoogleApiClient.disconnect();
                    }
                    buildGoogleApiClient();
                    *//*} else {
                        Log.d(TAG, "google api client connected " + getTimeStamp());
                        insertLog("google api client connected " + getTimeStamp());
                        mGoogleApiClient.connect();
                    }*//*

                    // handler.postDelayed(this, 30000);
                } catch (Exception e) {
                    e.printStackTrace();
                    //addToast(context, "Please check internet connection or Turn on Location Services");
                } finally {
                    //also call the same runnable
                    handler.postDelayed(this, 30000);
                }

            }
        };

        handler.postDelayed(runnable, 30000);*/
        return START_REDELIVER_INTENT;
    }


    @Override
    public void onLocationChanged(Location mLastLocation) {
        this.mLastLocation = mLastLocation;
        insertLog("onLoctionchanged location is " + mLastLocation + " , " + getTimeStamp());
        if (mLastLocation != null) {
            insertLog("onLoctionchanged location is not null" + getTimeStamp());
            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi
                        .removeLocationUpdates(mGoogleApiClient, this);

                mGoogleApiClient.disconnect();
            }
        }


    }

    private void insertLog(String s) {
        Log.d(TAG, s);
    }

    public void addToast(final Context context, final String message) {
        Toast.makeText(
                context,
                message,
                Toast.LENGTH_LONG).show();
    }

    private void createNotification() {
        // i = 1 for settings not met ; i = 2 for permission not given
        String title, body;
        Intent notificationIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = null;
        PendingIntent nextPendingIntent = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(notificationIntent);
            nextPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        body = "Location Tracking";
        title = "Fetching";
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title);
        if (nextPendingIntent != null) {
            mBuilder.setContentIntent(nextPendingIntent);
        }
        mBuilder.setContentText(body);
        mBuilder.setAutoCancel(true);
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(125, mBuilder.build());
    }

    protected LocationRequest createLocationRequest() {
        mLocationRequest = new LocationRequest();
        //mLocationRequest.setInterval(miscUtil.getIntervalValue());
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        insertLog("location request created ");
        return mLocationRequest;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        Log.d(TAG, "destroy called" + handler + " , " + runnable);
        insertLog("destroy called" + handler + " , " + runnable);
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "lowmemory called");
        insertLog("lowmemory called");

    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.d(TAG, "ontrimmemory called");
        insertLog("ontrimmemory called");

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "ontaskremoved called");
        insertLog("ontaskremoved called");
    }

}
