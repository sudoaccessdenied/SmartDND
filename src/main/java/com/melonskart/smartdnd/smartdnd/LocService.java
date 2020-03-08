/* Developer :Mr. Nishant
   Designation:Founder at Melonskart.
   Copyright @melonskart
   School:School of computer and system science.

 */

package com.melonskart.smartdnd.smartdnd;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocService extends Service {
    private static final String ACTION_FOO = "com.melonskart.smartdnd.smartdnd.action.FOO";
    private static final String NOTIFICATION_CHANNEL_ID = "com.melonskart.smartdnd.smartdnd";
    private static final String channelName = "Location Updates";
    // TODO: Rename parameters
    private static final String LOCATION_PARAM = "com.melonskart.smartdnd.smartdnd.extra.PARAM1";
    private static final int NOTIFICATION_ID = 456;
    static Context mContext;
    static Intent mIntent;
    private final IBinder mBinder = new LocalBinder();
    NotificationManager mNotificationManager;
    float[] results = new float[50];
    FusedLocationProviderClient mFusedLocationClient;
    LocationCallback mLocationCallBack;
    Location mLocation;
    AudioManager audioManager;
    OnLocationChangerListener mOnLocationChange;
    private String mUpdatedAddress = "";

    public LocService() {

    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, Bundle location) {
        Intent intent = new Intent(context, LocService.class);
        mContext = context;
        intent.setAction(ACTION_FOO);
        intent.putExtra(LOCATION_PARAM, location);
        mIntent = intent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ) {
            context.startForegroundService(intent);
        } else {

            context.startService(intent);
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        onHandleIntent(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallBack);
        }
        Log.d(LocService.class.getName(), "Service Destroyed");
        super.onDestroy();

    }

    public String getAddress() {
        return mUpdatedAddress;
    }

    public void registerListener(OnLocationChangerListener onLocationChangerListener) {
        mOnLocationChange = onLocationChangerListener;
    }

    public void unRegisterListener() {
        mOnLocationChange = null;
    }

    public float[] getResult() {
        return results;
    }

    public Location getLocation() {
        return mLocation;
    }

    private void startLocationTracking() {

        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {


            if (mOnLocationChange != null) {
                mOnLocationChange.onAddressChanged(getString(R.string.loading));
            }
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.requestLocationUpdates(getLocationRequest(),
                    mLocationCallBack, null);


            Log.d(LocService.class.getName(), "IN");
        }
        Log.d(LocService.class.getName(), "OUT");

    }

    private void silentMode() {


        Log.d(LocService.class.getName(), "Service RUNNING");


        if (results[0] < 100 && audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else if (results[0] > 100 && audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }

    }

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void updateMyNotification(String data) {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        Notification notification = notificationBuilder.setOngoing(false)
                .setSmallIcon(R.drawable.ic_do_not_disturb_on_white_24dp)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentTitle("Smart DND is On")
                .setContentText(data)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, notification);


    }

    private void runAsForegroundWithNotification() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel chan = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH);

            chan.setLightColor(Color.RED);
            chan.enableLights(true);
            chan.enableVibration(true);
            chan.setDescription(mContext.getString(R.string.notif_by_smartdnd));
            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);


            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(chan);
            }
            startForeground(NOTIFICATION_ID, notificationBuilder());
        }


    }

    private Notification notificationBuilder() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_do_not_disturb_on_white_24dp)
                .setContentTitle("Smart DND is On")
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                //.setCategory(Notification.CATEGORY_SERVICE)
                .build();
        return notification;
    }

    private void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    runAsForegroundWithNotification();
                else
                    startForeground(NOTIFICATION_ID, notificationBuilder());

                Bundle bundle = intent.getBundleExtra(LOCATION_PARAM);

                Log.d(LocService.class.getSimpleName(),
                        "Service Started ");

                handleActionFoo(bundle);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(final Bundle myLocation) {

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


//        Do Background work Here


        mLocationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mLocation = locationResult.getLastLocation();

                Location.distanceBetween(myLocation.getDouble(MainActivity.SELECTED_LATTIDUDE),
                        myLocation.getDouble(MainActivity.SELECTED_LONGITUDE),
                        mLocation.getLatitude(),
                        mLocation.getLongitude(), results);


//                updateMyNotification(Float.toString(results[0]));

                if (mOnLocationChange != null) {
                    mOnLocationChange.onLocationChanged(results, mLocation);
                }

                silentMode();

//                mCurLocTextView.setText(getString(R.string.current_location_address_lanLong,
//                        mLocation.getLatitude(),
//                        mLocation.getLongitude(),
//                        mLocation.getTime()));


                new LocService.FetchAsynkTask(LocService.this)
                        .execute(locationResult.getLastLocation());
            }
        };


        startLocationTracking();
        Log.d(LocService.class.getSimpleName(), "Action Handled Service Stopped ");


    }


    interface OnLocationChangerListener {
        void onLocationChanged(float[] result, Location location);

        void onAddressChanged(String address);
    }

    public class LocalBinder extends Binder {
        public LocService getService() {
            return LocService.this;
        }

    }

    public class FetchAsynkTask extends AsyncTask<Location, Void, String> {
        private Context mServiceContext;

        FetchAsynkTask(Context context) {
            mServiceContext = context;
        }

        @Override
        protected String doInBackground(Location... locations) {
            Geocoder geocoder = new Geocoder(mServiceContext, Locale.getDefault());
            Location location = locations[0];
            List<Address> addresses = null;
            String resultString = "";
            try {
                addresses = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);

            } catch (IOException e) {
                resultString = mServiceContext.getString(R.string.service_not_available);
            } catch (IllegalArgumentException e) {
                resultString = mServiceContext.getString(R.string.invalid_lat_long_used);
            }

            if (addresses != null && addresses.size() > 0) {
                if (resultString.isEmpty()) {
                    resultString = mServiceContext.getString(R.string.no_address_found);

                } else {
                    Address address = addresses.get(0);

                    ArrayList<String> addressParts = new ArrayList<>();

                    // Fetch the address lines using getAddressLine,
                    // join them, and send them to the thread
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        addressParts.add(address.getAddressLine(i));
                    }

                    resultString = TextUtils.join("\n", addressParts);
                }
            }
            return resultString;
        }


        @Override
        protected void onPostExecute(String s) {
            if (mOnLocationChange != null) {
                mOnLocationChange.onAddressChanged(s);
            }

            super.onPostExecute(s);
        }


    }


}



