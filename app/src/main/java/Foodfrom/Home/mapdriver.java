package Foodfrom.Home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import Foodfrom.Home.directionhelpers.FetchURL;
import Foodfrom.Home.directionhelpers.TaskLoadedCallback;
import Foodfrom.Home.helper.FirebaseHelper;
import Foodfrom.Home.helper.GoogleMapHelper;
import Foodfrom.Home.helper.MarkerAnimationHelper;
import Foodfrom.Home.helper.UiHelper;
import Foodfrom.Home.interfaces.LatLngInterpolator;
import Foodfrom.Home.Model.Driver;

public class mapdriver extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback {
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2161;
    private static final String DRIVER_ID = "0000";

    private FirebaseHelper firebaseHelper = new FirebaseHelper(DRIVER_ID);
    private GoogleMapHelper googleMapHelper = new GoogleMapHelper();
    private AtomicBoolean driverOnlineFlag = new AtomicBoolean(false);

    private GoogleMap googleMap;
    private Marker currentPositionMarker;
    private FusedLocationProviderClient locationProviderClient;
    private UiHelper uiHelper;
    private LocationRequest locationRequest;

    private TextView driverStatusTextView;
    private Button getDirection;
    private MarkerOptions place1, place2;
    private Polyline currentPolyline;
    private boolean locationFlag = true;

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();
            if (location == null) return;

            // Set place1 to Driver's current location
            place1 = new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .title("Driver Location");

            if (locationFlag) {
                locationFlag = false;
                animateCamera(location);
            }

            if (driverOnlineFlag.get())
                firebaseHelper.updateDriver(new Driver(location.getLatitude(), location.getLongitude(), DRIVER_ID));
            showOrAnimateMarker(location);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapdriver);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.supportMap);
        assert mapFragment != null;
        uiHelper = new UiHelper(this);
        mapFragment.getMapAsync(this);

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = uiHelper.getLocationRequest();

        if (!uiHelper.isPlayServicesAvailable()) {
            Toast.makeText(this, "Play Services not installed!", Toast.LENGTH_SHORT).show();
            finish();
        } else requestLocationUpdates();

        SwitchCompat driverStatusSwitch = findViewById(R.id.driverStatusSwitch);
        driverStatusTextView = findViewById(R.id.driverStatusTextView);
        driverStatusSwitch.setOnCheckedChangeListener((buttonView, b) -> {
            driverOnlineFlag.set(b);
            if (b)
                driverStatusTextView.setText(getResources().getString(R.string.online));
            else {
                driverStatusTextView.setText(getResources().getString(R.string.offline));
                firebaseHelper.deleteDriver();
            }
        });

        getDirection = findViewById(R.id.btnGetDirection);
        getDirection.setOnClickListener(view -> new FetchURL(mapdriver.this)
                .execute(getUrl(place1.getPosition(), place2.getPosition(), "driving"), "driving"));

        // Ask customer for their phone number and retrieve address
        askCustomerLocation();
    }

    private void animateCamera(Location location) {
        CameraUpdate cameraUpdate = googleMapHelper.buildCameraUpdate(new LatLng(location.getLatitude(), location.getLongitude()));
        googleMap.animateCamera(cameraUpdate);
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (!uiHelper.isHaveLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        if (uiHelper.isLocationProviderEnabled())
            uiHelper.showPositiveDialogWithListener(this, getResources().getString(R.string.need_location), getResources().getString(R.string.location_content), () -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)), "Turn On", false);
        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void showOrAnimateMarker(Location location) {
        if (currentPositionMarker == null)
            currentPositionMarker = googleMap.addMarker(googleMapHelper.getDriverMarkerOptions(location));
        else
            MarkerAnimationHelper.animateMarkerToGB(currentPositionMarker, new LatLng(location.getLatitude(), location.getLongitude()), new LatLngInterpolator.Spherical());
    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String mode = "mode=" + directionMode;
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        String output = "json";
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = googleMap.addPolyline((PolylineOptions) values[0]);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        Log.d("mylog", "Added Markers");
    }

    // Ask customer for phone number and retrieve address from Firebase
    private void askCustomerLocation() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Customer Phone Number");
        alertDialog.setMessage("Enter your phone number:");

        final EditText inputPhoneNumber = new EditText(this);
        inputPhoneNumber.setHint("Enter your phone number");
        alertDialog.setView(inputPhoneNumber);

        alertDialog.setPositiveButton("OK", (dialog, which) -> {
            String phoneNumber = inputPhoneNumber.getText().toString();
            if (!phoneNumber.isEmpty()) {
                retrieveAddressFromFirebase(phoneNumber);
            } else {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            }
        });

        alertDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void retrieveAddressFromFirebase(String phoneNumber) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference("Requests");

        reference.orderByChild("phone").equalTo(phoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Assuming there's only one request per phone number
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String address = snapshot.child("address").getValue(String.class);
                        if (address != null) {
                            convertAddressToLatLng(address);
                        } else {
                            Toast.makeText(mapdriver.this, "Address not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(mapdriver.this, "No request found for this phone number", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(mapdriver.this, "Error retrieving data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void convertAddressToLatLng(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(address, 1);
            if (!addressList.isEmpty()) {
                Address location = addressList.get(0);
                place2 = new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .title("Customer Location");

                googleMap.addMarker(place2);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place2.getPosition(), 15));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
