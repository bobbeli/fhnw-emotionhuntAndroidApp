package ch.fhnw.ip5.emotionhunt.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import ch.fhnw.ip5.emotionhunt.R;
import ch.fhnw.ip5.emotionhunt.helper.DbHelper;
import ch.fhnw.ip5.emotionhunt.helper.PermissionHelper;
import ch.fhnw.ip5.emotionhunt.models.ReceivedExperience;
import ch.fhnw.ip5.emotionhunt.services.ApiService;
import ch.fhnw.ip5.emotionhunt.services.LocationService;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleMap mMap;
    public static final int ONBAORDING_CODE = 1;

    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

    LatLng latLng;
    boolean isCameraMoved = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SQLiteDatabase db = new DbHelper(getApplicationContext()).getWritableDatabase();
        DbHelper.getStatus(db);

        //check permission for location listener and require its permissions if necessary
        if (!PermissionHelper.checkLocationPermission(this)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PermissionHelper.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.btn_main_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ExperienceCreateActivity.class);
                startActivity(intent);
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG,"onMapReady");
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);

        startExperienceListener();

        if (PermissionHelper.checkLocationPermission(this)) {
            mMap.setMyLocationEnabled(true);
            buildGoogleApiClient();
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(TAG,"onRequestPermissionsResult");
        switch (requestCode) {
            case PermissionHelper.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //start api service
                    if (PermissionHelper.checkLocationPermission(this)) {
                        Intent intent= new Intent(this, LocationService.class);
                        startService(intent);
                        mMap.setMyLocationEnabled(true);
                        buildGoogleApiClient();
                        mGoogleApiClient.connect();
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG,"buildGoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_action, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try{
            switch (item.getItemId()) {
                case R.id.btn_main_profile:
                    Intent intentDetail = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(intentDetail);
                    return true;
                case R.id.btn_main_experience_list:
                    Intent intentList = new Intent(getApplicationContext(), ExperienceListActivity.class);
                    startActivity(intentList);
                    return true;
                default:
                    throw new IllegalArgumentException("Invalid Action Menu Item");
            }
        } catch (Exception e) {
            Log.d(TAG, "Could not found appropriate Menu Action. Error Message: "+e.getMessage());
        }
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG,"onLocationChanged");
        //place marker at current position
        latLng = new LatLng(location.getLatitude(), location.getLongitude());

        //zoom to current position:
        if (!isCameraMoved) {
            CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(16).build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            isCameraMoved = true;
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG,"onConnected");
        if (PermissionHelper.checkLocationPermission(this)) {
            Log.d(TAG,"onConnected permissions granted");
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(5000); //5 seconds
            mLocationRequest.setFastestInterval(3000); //3 seconds
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            //mLocationRequest.setSmallestDisplacement(0.1F); //1/10 meter

            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG,"onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG,"onConnectionFailed");
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();

        //obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //start api service
        Intent intent= new Intent(this, ApiService.class);
        startService(intent);

        //start location service - check permissions first
        if (PermissionHelper.checkLocationPermission(this)) {
            intent = new Intent(this, LocationService.class);
            startService(intent);
        }
    }

    /**
     * Starts the experience listener and update or initializes the marker.
     */
    public void startExperienceListener() {
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    if (mMap == null) {
                        Log.d(TAG, Thread.currentThread().getId() + ": Map is not ready for listener...");
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) { }
                        continue;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMap.clear();
                        }
                    });

                    ArrayList<ReceivedExperience> receivedExperiences = ReceivedExperience.getAll(getApplicationContext());
                    for (final ReceivedExperience receivedExperience : receivedExperiences) {
                        //add a marker in Sydney and move the camera
                        final LatLng marker = new LatLng(receivedExperience.lat, receivedExperience.lon);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mMap.addMarker(new MarkerOptions().position(marker).title(receivedExperience.text));
                            }
                        });
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }
}
