/* Design and Developed by :Mr. Nishant
   Designation:Founder at Melonskart.
   School:School of computer and system science,JNU,New delhi
   Copyright of melonskart

 */


package com.melonskart.smartdnd.smartdnd;

import android.Manifest;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity
        implements LocService.OnLocationChangerListener {
    public static final String SELECTED_LATTIDUDE = "selected_lattitude";
    public static final String SELECTED_LONGITUDE = "selected_longitude";
    private static final int REQUEST_LOCATION_PERMISSION = 5656;
    private static final String SHARED_PREF = "com.melonskart.smartdnd.smartdnd.";
    private static final String SERVICE_STARTED = "service_started";
    private static final int PLACE_PICKER_REQUEST = 2369;
    private static final String SELECTED_ADDRESS = "selected_address";
    private static final String SELECTED_TIME = "selected_time";
    private static double startLatitude = 28.544400;
    private static double startLongitude = 77.161544;

    private TextView selectedTextView, currentTextView, distanceTextView;
    private Place mPlace;
    private boolean serviceStarted = false;
    private SwitchCompat mSwitch;
    private SharedPreferences mSharedPreference;
    private Location mLocation;
    private float mResult;
    private String mAddress = " Wait...";
    private LocService mService;
    private LocService.LocalBinder mBinder;
    private boolean mBound = false;
    private ServiceConnection mServiceConnection;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mSharedPreference = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);


        //Initialization of Variables
        mSwitch = findViewById(R.id.tracking_switch);
        selectedTextView = findViewById(R.id.selected_location_textView);
        currentTextView = findViewById(R.id.current_location_textView);
        distanceTextView = findViewById(R.id.distance_textView);
        if (mSharedPreference != null) {
            serviceStarted = mSharedPreference.getBoolean(SERVICE_STARTED, false);
            String st = mSharedPreference.getString(SELECTED_LATTIDUDE, Double.toString(startLatitude));
            startLatitude = Double.parseDouble(st);

            startLongitude = Double.parseDouble(mSharedPreference.getString(SELECTED_LONGITUDE,
                    Double.toString(startLongitude)));

            selectedTextView.setText(getString(R.string.selected_location_address_lanLong,
                    mSharedPreference.getString(SELECTED_ADDRESS, " "),
                    mSharedPreference.getLong(SELECTED_TIME, System.currentTimeMillis()),
                    startLatitude,
                    startLongitude
            ));

        }


        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSwitch.setChecked(serviceStarted);


        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mBinder = (LocService.LocalBinder) service;
                mService = ((LocService.LocalBinder) service).getService();
                mBound = true;
                mService.registerListener(MainActivity.this);


//                mLocation = mService.getLocation();
//                float[] data=mService.getResult();
//                mResult = data[0];
//                mAddress = mService.getAddress();
//                TextView textView = findViewById(R.id.distance_textView);
//                textView.setText(getString(R.string.distance,mResult));
//                currentLocationUpdate();

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mBound = false;
                Log.d("MainActivity", "Bounding Failed");

            }
        };

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    serviceStarted = true;
                    mSwitch.setText(R.string.stop_service);
                    startLocationTracking();


                } else if (!isChecked) {
                    serviceStarted = false;
                    mSwitch.setText(R.string.start_service);

                    Log.d(MainActivity.class.getSimpleName(), "Stop Service ");

                    Intent intent = new Intent(MainActivity.this, LocService.class);
                    if (mBound) {
                        mBound = false;
                        unbindService(mServiceConnection);
                    }


                    if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    }
                    stopService(intent);

                }


            }
        });

        requestAllPermission();

    }


    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, LocService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void saveOnSharedPreference() {

        SharedPreferences.Editor editor = mSharedPreference.edit();
        editor.putBoolean(SERVICE_STARTED, serviceStarted);
        if (mPlace != null) {
            editor.putString(SELECTED_ADDRESS, mPlace.getAddress().toString());
        }
        editor.putLong(SELECTED_TIME, System.currentTimeMillis());

        editor.putString(SELECTED_LATTIDUDE, Double.toString(startLatitude));
        editor.putString(SELECTED_LONGITUDE, Double.toString(startLongitude));
        editor.apply();
    }


    private void startLocationTracking() {
        Bundle bundle = new Bundle();
        bundle.putDouble(SELECTED_LATTIDUDE, startLatitude);
        bundle.putDouble(SELECTED_LONGITUDE, startLongitude);
        Log.d("Mainactivity", startLatitude + " Start LocTrac" + startLongitude);

        LocService.startActionFoo(MainActivity.this, bundle);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PLACE_PICKER_REQUEST) {
                mPlace = PlacePicker.getPlace(data, this);

                LatLng latLng = mPlace.getLatLng();
                startLatitude = latLng.latitude;
                startLongitude = latLng.longitude;

                selectedTextView.setText(getString(R.string.selected_location_address_lanLong,
                        mPlace.getAddress(), System.currentTimeMillis(), startLatitude, startLongitude));
                saveOnSharedPreference();


            }
        } else if (resultCode == RESULT_CANCELED) {

            if (requestCode == REQUEST_LOCATION_PERMISSION) {
                Toast.makeText(getApplicationContext(),
                        "Turn On SMART DND ",
                        Toast.LENGTH_SHORT)
                        .show();
                requestAllPermission();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        ) {
                    if (serviceStarted) {
                        startLocationTracking();
                    }


                } else {

                    Toast.makeText(this, "Permission Denied App Closing ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

        }

    }

    public void locationPicker(View view) {

        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

    }


    private void requestAllPermission() {


        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION,}, REQUEST_LOCATION_PERMISSION);

        }


        NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!n.isNotificationPolicyAccessGranted()) {
                // Ask the user to grant access
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivityForResult(intent, REQUEST_LOCATION_PERMISSION);
            }
        }


    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mSwitch.isChecked() && !serviceStarted) {
            startLocationTracking();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (serviceStarted && !mSwitch.isChecked()) {

        }
        saveOnSharedPreference();


    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            mService.unRegisterListener();
            unbindService(mServiceConnection);
        }
    }


    private void currentLocationUpdate() {

        if (mLocation != null) {
            currentTextView.setText(getString(R.string.current_location_address_lanLong,
                    mAddress, mLocation.getTime(), mLocation.getLatitude(), mLocation.getLongitude()));
        }

    }

    @Override
    public void onLocationChanged(float[] result, Location location) {
        Log.d("MainActivity", result + "" + location.getLongitude() + "" + location.getLatitude() + "");
        mResult = result[0];
        mLocation = location;

        distanceTextView.setText(getString(R.string.distance, mResult));

        currentLocationUpdate();

    }

    @Override
    public void onAddressChanged(String address) {
        Log.d("MainActivity", address);
        mAddress = address;
        currentLocationUpdate();
    }
}






