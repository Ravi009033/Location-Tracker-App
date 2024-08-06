package com.myapplication;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private GoogleMap mMap2;
    private DatabaseReference databaseReference;
    private DatabaseReference databaseReference2;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private EditText editTextLatitude;
    private EditText editTextLongitude;
    private EditText editTextLocality;
    FusedLocationProviderClient fusedLocationProviderClient;
    private Polyline currentPolyline;
    Button getDirection;
    private MarkerOptions place1, place2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //permission from user
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);

        editTextLatitude = findViewById(R.id.editText);
        editTextLongitude = findViewById(R.id.editText2);
        editTextLocality = findViewById(R.id.editText3);


        editTextLocality.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchLocation();
                    return true;
                }
                return false;
            }
        });

        locationEnabled();
        getLocation();

        //for database
        databaseReference = FirebaseDatabase.getInstance().getReference("Location");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                try {
                    //store local value to database
                    String databaseLatitudeString = snapshot.child("latitude").getValue().toString().substring(1, snapshot.child("latitude").getValue().toString().length() - 1);
                    String databaseLongitudeString = snapshot.child("longitude").getValue().toString().substring(1, snapshot.child("longitude").getValue().toString().length() - 1);

                    String[] stringLat = databaseLatitudeString.split(", ");
                    Arrays.sort(stringLat);
                    String[] stringLong = databaseLongitudeString.split(", ");
                    Arrays.sort(stringLong);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap2 = googleMap;

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                try {
                    upadteLocation(location);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                mMap.clear();
                // Animating to the touched position
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.addMarker(markerOptions);

                setTextvalue(latLng.latitude,latLng.longitude);
                try {
                    Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                    editTextLocality.setText(addresses.get(0).getAddressLine(0));
                } catch (Exception e) {
                    e.printStackTrace();
                }



            }
        });
    }

    public void shareButtonOnclick(View view) {

        HashMap hashMap = new HashMap();
        hashMap.put("latitude", editTextLatitude.getText().toString());
        hashMap.put("longitude", editTextLongitude.getText().toString());
        databaseReference.updateChildren(hashMap);
        Toast.makeText(this, "Share Location successful", Toast.LENGTH_SHORT).show();

    }

    public void updateLocationOnclick(View view) {
        getLocation();

    }


    private void locationEnabled() {
        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(MapsActivity.this)
                    .setMessage("GPS Enable")
                    .setPositiveButton("Settings", new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                }
                            })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

    }



    private void getLocation() {
        locationEnabled();

        //intialise fusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                       upadteLocation(location);

                        Toast.makeText(MapsActivity.this, "You are in " + addresses.get(0).getLocality(), Toast.LENGTH_SHORT).show();
                        editTextLocality.setText(addresses.get(0).getAddressLine(0));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }

        });
    }
    private void upadteLocation(Location location) {

        setTextvalue(location.getLatitude(),location.getLongitude());
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        marker_display(latLng);

    }
    private void setTextvalue(double x,double y)
    {
        String lat = Double.toString(x);
        String lon = Double.toString(y);
        editTextLatitude.setText(lat);
        editTextLongitude.setText(lon);
    }
    private void marker_display(LatLng latLng)
    {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));
        CameraPosition myPosition = new CameraPosition.Builder()
                .target(latLng).zoom(10).build();
        mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(myPosition));
    }
    public void searchLocation() {

        String location = editTextLocality.getText().toString();
        List<Address> addressList = null;

        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            Address address = addressList.get(0);

           LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            setTextvalue(address.getLatitude(),address.getLongitude());
            marker_display(latLng);
        }
    }
     public void secondLocationOnclick(View view) {
         Toast.makeText(MapsActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
         databaseReference2 = FirebaseDatabase.getInstance().getReference().child("Location2");
         //update everytime when data get updated
         databaseReference2.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot snapshot) {
                 try {
                     String databaseLatitudeString2 = snapshot.child("latitude").getValue().toString();
                     String databaseLongitudedeString2 = snapshot.child("longitude").getValue().toString();

                     LatLng latLng = new LatLng(Double.parseDouble(databaseLatitudeString2), Double.parseDouble(databaseLongitudedeString2));

                     mMap2.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                     CameraPosition myPosition = new CameraPosition.Builder()
                             .target(latLng).zoom(10).build();
                     mMap2.animateCamera(
                             CameraUpdateFactory.newCameraPosition(myPosition));

                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }
             @Override
             public void onCancelled(@NonNull DatabaseError error) {

             }
         });
     }
     public void Track0_Onclick(View view){
         mMap.clear();
         mMap2.clear();
         Trackfind(0);
         Toast.makeText(this, "Track 0 selected", Toast.LENGTH_SHORT).show();
     }
     public void Track1_Onclick(View view){
         Trackfind(1);
         Toast.makeText(this, "Track 1 selected", Toast.LENGTH_SHORT).show();
         try {
             String lat = "17.60189183896";
             String lon = "78.12661039844";
             mMap.clear();
             mMap2.clear();
             LatLng latLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));

             mMap.addMarker(new MarkerOptions().position(latLng).title("Source").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
             CameraPosition myPosition = new CameraPosition.Builder()
                     .target(latLng).zoom(20).build();
             mMap.animateCamera(
                     CameraUpdateFactory.newCameraPosition(myPosition));

             String lat2 = "17.60200779233";
             String lon2 = "78.12682733373";

             LatLng latLng2 = new LatLng(Double.parseDouble(lat2), Double.parseDouble(lon2));

             mMap2.addMarker(new MarkerOptions().position(latLng2).title("Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
             CameraPosition myPosition2 = new CameraPosition.Builder()
                     .target(latLng2).zoom(20).build();
             mMap2.animateCamera(
                     CameraUpdateFactory.newCameraPosition(myPosition2));


         } catch (Exception e) {
             e.printStackTrace();
         }

     }
     public void Track2_Onclick(View view){
         Trackfind(2);
         Toast.makeText(this, "Track 2 selected", Toast.LENGTH_SHORT).show();
         try {
             String lat = "17.60236551785";
             String lon = "78.12711645113";
             mMap.clear();
             mMap2.clear();
             LatLng latLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));

             mMap.addMarker(new MarkerOptions().position(latLng).title("Source").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
             CameraPosition myPosition = new CameraPosition.Builder()
                     .target(latLng).zoom(20).build();
             mMap.animateCamera(
                     CameraUpdateFactory.newCameraPosition(myPosition));

             String lat2 = "17.60263221625";
             String lon2 = "78.12714639040";

             LatLng latLng2 = new LatLng(Double.parseDouble(lat2), Double.parseDouble(lon2));

             mMap2.addMarker(new MarkerOptions().position(latLng2).title("Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
             CameraPosition myPosition2 = new CameraPosition.Builder()
                     .target(latLng2).zoom(20).build();
             mMap2.animateCamera(
                     CameraUpdateFactory.newCameraPosition(myPosition2));


         } catch (Exception e) {
             e.printStackTrace();
         }

     }
     private void Trackfind(int val){
         databaseReference = FirebaseDatabase.getInstance().getReference("Track");
         HashMap hashMap = new HashMap();
         hashMap.put("key", val);

         databaseReference.updateChildren(hashMap);

     }



}




