package com.jobbs.parkingapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements
        PlaceSelectionListener,
        OnMapReadyCallback,
        GoogleMap.OnMarkerDragListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;

    //search view
    private TextView mPlaceDetailsText;
    private TextView mPlaceAttribution;


    //map
    private GoogleApiClient mGoogleApiClient;
    private Marker m1;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;
    private Location mCurrentLocation;
    private com.google.android.gms.location.LocationListener locationListener;
    private LocationRequest mLocationRequest;
    private ArrayList<Marker> mAllMarkers;
    private Marker selectedParking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        buildGoogleApiClient();
        mGoogleApiClient.connect();


        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_fragment);


        autocompleteFragment.setOnPlaceSelectedListener(this);

        mPlaceDetailsText = (TextView) findViewById(R.id.place_details);
        mPlaceAttribution = (TextView) findViewById(R.id.place_attribution);


    }





    //search view
    @Override
    public void onPlaceSelected(Place place) {
        Log.i("Hey", "Place Selected: " + place.getName());

        // Format the returned place's details and display them in the TextView.
        mPlaceDetailsText.setText(formatPlaceDetails(getResources(), place.getName(), place.getId(),
                place.getAddress(), place.getPhoneNumber(), place.getWebsiteUri()));

        CharSequence attributions = place.getAttributions();
        if (!TextUtils.isEmpty(attributions)) {
            mPlaceAttribution.setText(Html.fromHtml(attributions.toString()));
        } else {
            mPlaceAttribution.setText("");
        }


        m1.setPosition(place.getLatLng());
        CameraUpdate center =
                CameraUpdateFactory.newLatLng(place.getLatLng());
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(12);
        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

        setMarkerVisibility(m1);
    }



    @Override
    public void onError(Status status) {
        Log.e("Hey", "onError: Status = " + status.toString());

        Toast.makeText(this, "Place selection failed: " + status.getStatusMessage(),
                Toast.LENGTH_SHORT).show();
    }
    private static Spanned formatPlaceDetails(Resources res, CharSequence name, String id,
                                              CharSequence address, CharSequence phoneNumber, Uri websiteUri) {
        Log.e("hey", res.getString(R.string.place_details, name, id, address, phoneNumber,
                websiteUri));
        return Html.fromHtml(res.getString(R.string.place_details, name, id, address, phoneNumber,
                websiteUri));

    }


    //google map
    @Override
    public void onConnected(Bundle connectionHint) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }


    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this ,
                        this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();

        locationListener = new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (m1==null) {
                    Log.e("locationChane","t");
                    mCurrentLocation = location;
                    LatLng current = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                    m1 = mMap.addMarker(new MarkerOptions().position(current).title("My location").draggable(true));
                    m1.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.my_location_icon));

                    setMarkerVisibility(m1);
//                mMap.moveCamera(CameraUpdateFactory.newLatLng(current));


                    CameraUpdate center =
                            CameraUpdateFactory.newLatLng(current);
                    CameraUpdate zoom = CameraUpdateFactory.zoomTo(12);
                    mMap.moveCamera(center);
                    mMap.animateCamera(zoom);
                }
            }
        };


    }
    private void getDeviceLocation() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, locationListener);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }

        if (mLocationPermissionGranted) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mCurrentLocation = null;
        }
    }
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(2*60*60*1000);

        mLocationRequest.setFastestInterval(2*60*60*1000);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateLocationUI();
        mMap.setOnMarkerDragListener(this);


        LatLng park = new LatLng(6.940969, 79.857329);
        Marker m2 = mMap.addMarker(new MarkerOptions().position(park).title("Parking Lot 1").draggable(false));
        m2.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_lot_img));

        park = new LatLng(6.937088, 79.920629);
        Marker m3 = mMap.addMarker(new MarkerOptions().position(park).title("Parking Lot 2").draggable(false));
        m3.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_lot_img));

        park = new LatLng(6.920907, 79.879794);
        Marker m4 = mMap.addMarker(new MarkerOptions().position(park).title("Parking Lot 3").draggable(false));
        m4.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_lot_img));

        park = new LatLng(6.919626, 79.855551);
        Marker m5 = mMap.addMarker(new MarkerOptions().position(park).title("Parking Lot 4").draggable(false));
        m5.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_lot_img));

        park = new LatLng(6.917453, 79.864255);
        Marker m6 = mMap.addMarker(new MarkerOptions().position(park).title("Parking Lot 5").draggable(false));
        m6.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_lot_img));

        park = new LatLng(6.920920, 79.876140);
        Marker m7 = mMap.addMarker(new MarkerOptions().position(park).title("Parking Lot 6").draggable(false));
        m7.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_lot_img));

        park = new LatLng(6.842807, 79.872435);
        Marker m8 = mMap.addMarker(new MarkerOptions().position(park).title("Parking Lot 7").draggable(false));
        m8.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_lot_img));

        park = new LatLng(6.891050, 79.876412);
        Marker m9 = mMap.addMarker(new MarkerOptions().position(park).title("Parking Lot 8").draggable(false));
        m9.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_lot_img));

        park = new LatLng(6.916782, 79.864195);
        Marker m10 = mMap.addMarker(new MarkerOptions().position(park).title("Parking Lot 9").draggable(false));
        m10.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_lot_img));


        mAllMarkers=new ArrayList<>();
        mAllMarkers.add(m2);
        mAllMarkers.add(m3);
        mAllMarkers.add(m4);
        mAllMarkers.add(m5);
        mAllMarkers.add(m6);
        mAllMarkers.add(m7);
        mAllMarkers.add(m8);
        mAllMarkers.add(m9);
        mAllMarkers.add(m10);

        Log.e("seteed markers","t");

        for (int i=0;i<mAllMarkers.size();i++){
            mAllMarkers.get(i).setVisible(false);
        }


        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                if(marker.equals(m1)) {return false;}
                selectedParking = marker;
                new AlertDialog.Builder(MapsActivity.this)
                        .setTitle(marker.getTitle())
                        .setMessage("Do yo want to book a parking space here?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                showCounter();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return false;
            }
        });

        getDeviceLocation();

    }

    private void showCounter() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.popup_countdown, null);

        TextView textView=(TextView)dialogView.findViewById(R.id.countdownText);
        startTimer(30*60*1000,textView);
        dialogBuilder.setView(dialogView)
                .setCancelable(false)
                .setTitle("Slot Number 7 is reserved for you")
                .setPositiveButton("Navigate", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        String temp = "daddr="+selectedParking.getPosition().latitude+","
                                +selectedParking.getPosition().longitude+ "(My parking Lot)&mode=driving";

                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                                Uri.parse("http://maps.google.com/maps?"+temp));

                        String mapsPackageName = "com.google.android.apps.maps";
                        if (isPackageInstalled(getApplicationContext(), mapsPackageName)) {
                            intent.setClassName(mapsPackageName, "com.google.android.maps.MapsActivity");
                            intent.setPackage(mapsPackageName);
                        }

                        startActivity(intent);

                    }
                })
                .setNegativeButton("Cancel Booking", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }
    @Override
    public void onMarkerDrag(Marker marker) {

    }
    @Override
    public void onMarkerDragEnd(Marker marker) {
        setMarkerVisibility(marker);
    }
    @Override
    public void onConnectionSuspended(int i) {

    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void startTimer(int noOfMinutes, final TextView countdownTimerText) {
        CountDownTimer countDownTimer = new CountDownTimer(noOfMinutes, 1000) {
            public void onTick(long millisUntilFinished) {
                long millis = millisUntilFinished;
                //Convert milliseconds into hour,minute and seconds
                String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
                countdownTimerText.setText(hms);//set text
            }
            public void onFinish() {
                countdownTimerText.setText("TIME'S UP!!"); //On finish change timer text
            }
        }.start();

    }

    private void setMarkerVisibility(Marker marker){
        Location currentLoc = new Location("");
        currentLoc.setLatitude(marker.getPosition().latitude);
        currentLoc.setLongitude(marker.getPosition().longitude);

        Location parkingLoc = new Location("");

        for (int i=0;i<mAllMarkers.size();i++){
            parkingLoc.setLongitude(mAllMarkers.get(i).getPosition().longitude);
            parkingLoc.setLatitude(mAllMarkers.get(i).getPosition().latitude);
            Log.e("distance",currentLoc.distanceTo(parkingLoc)+"");
            if (currentLoc.distanceTo(parkingLoc)<3000){
                mAllMarkers.get(i).setVisible(true);
            }else{
                mAllMarkers.get(i).setVisible(false);
            }

        }
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        if (intent == null) {
            return false;
        }
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
}
