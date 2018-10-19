package com.idiots.bottomup;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int DISPLAY_MARK_NUM = 3;

    private static final String TAG_RESULT = "result";
    private static final String TAG_CURRENT_LOCATION = "currentLocation";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_NAME = "label";

    private String uploadName;
    private File upleadFile;

    private Button uploadBtn, navigateBtn;
    //private ConnectFTP connectFTP;
    private SFTP sftp;

    private GoogleMap googleMap = null;

    private LatLng selectedLocation = null;

    private Set<String> history = null;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    Location lastKnownLocation;

    private long lastTimeMarkTouched;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getFragmentManager();
        MapFragment mapFragment = (MapFragment) fragmentManager
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        verifyStoragePermissions(this);

        init();

    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;



    public void onClick1(View v) {
        dispatchTakePictureIntent();
    }

    public void onClick2(View v) {

        upleadFile = dispatchTakePictureIntent();
        uploadName = upleadFile.getName();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK){
                    getCurrentLocationAndUpload();
                }

                break;
        }

    }

    @Override
    public void onMapReady(final GoogleMap map) {

        googleMap = map;

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            return;
        }

        googleMap.setMyLocationEnabled(true);

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //selectedLocation = marker.getPosition();

                if(marker.getPosition().equals(selectedLocation)){
                    if(System.currentTimeMillis() - lastTimeMarkTouched < 1500){
                        openKakaoMap();
                    }
                }

                selectedLocation = marker.getPosition();
                lastTimeMarkTouched = System.currentTimeMillis();
                return false;
            }
        });

        LatLng SEOUL = new LatLng(36.365008, 127.347598);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(SEOUL);
        markerOptions.title("공대 4호관");
        markerOptions.snippet("극혐");
        map.addMarker(markerOptions);

        map.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
        map.animateCamera(CameraUpdateFactory.zoomTo(10));

    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSSSZ").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        imageFileName = imageFileName.replace("-", "Minus");
        imageFileName = imageFileName.replace("+", "Plus");   //서버에 올릴 파일 이름
        File sdCardPath = Environment.getExternalStorageDirectory();
        File dcim = new File(sdCardPath, "DCIM");
        File path = new File(dcim, "Camera");
        File file = new File(path, imageFileName);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                path      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    static final int REQUEST_TAKE_PHOTO = 1;

    private File dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent

        File photoFile = null;

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go

            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }

        return photoFile;
    }

    //    private void dispatchTakePictureIntent() {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//        }
//    }

    private void init() {    //초기화
        /*
        connectFTP = new ConnectFTP() {
            @Override
            protected void afterMyFTPUpload(Boolean tf) {
                if (tf) {
                    parseJSON(uploadName);  //이미지 upload가 끝난 후 parseJSON을 호출한다.
                }
            }
        };
        */
        /*
        uploadBtn = findViewById(R.id.main_upload_btn);
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //hideKeyboard();
                DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSSSZ");
                uploadName = df.format(Calendar.getInstance().getTime()) + ".jpg";
                uploadName = uploadName.replace("-", "Minus");
                uploadName = uploadName.replace("+", "Plus");   //서버에 올릴 파일 이름

                getCurrentLocationAndUpload();
            }
        });*/
        /*
        navigateBtn = findViewById(R.id.main_navigate_btn);
        navigateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedLocation != null){
                    openKakaoMap();
                }else{
                    msg("Select Destination");
                }
            }
        });
        */

        sftp = new SFTP("hj", "12341234", "168.188.115.191", 22) {
            @Override
            protected void onUploadEnded(Boolean tf) {
                if(tf) parseJSON(uploadName);
            }
        };

        FragmentManager fragmentManager = getFragmentManager();
        MapFragment mapFragment = (MapFragment)fragmentManager
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        history = new Set();
    }

    private void openKakaoMap() {
        String auth = "daummaps://route?ep=" + selectedLocation.latitude + "," + selectedLocation.longitude + "&by=FOOT";
        Uri uri = Uri.parse(auth);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(mapIntent);
    }

    private void msg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void parseJSON(String uploadName) {  //php 파일 호출하여 추천 위치를 JSON 형식으로 받아온다.
        GetDataFromHTTP getDataFromHTTP = new GetDataFromHTTP() {
            @Override
            protected void onReceive(String result) {
                try {
                    JSONObject jsonObj = new JSONObject(result);

                    JSONArray JSONLocations = jsonObj.getJSONArray(TAG_RESULT);
                    JSONObject JSONCurrentLocation = jsonObj.getJSONObject(TAG_CURRENT_LOCATION);

                    LatLngBounds.Builder bounds = new LatLngBounds.Builder();

                    googleMap.clear();

                    if (JSONCurrentLocation.length() != 0) {
                        String name = JSONCurrentLocation.getString(TAG_NAME);
                        history.add(name);
                        msg("You are at " + name + " now");
                    }

                    for (int i = 0, count = 0; count < DISPLAY_MARK_NUM && i < JSONLocations.length(); i++) {
                        JSONObject c = JSONLocations.getJSONObject(i);
                        double lat = c.getDouble(TAG_LATITUDE);
                        double lng = c.getDouble(TAG_LONGITUDE);
                        String name = c.getString(TAG_NAME);

                        if (!history.has(name)) {
                            LatLng position = new LatLng(lat, lng);

                            bounds.include(position);

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(position);
                            markerOptions.title(name);
                            googleMap.addMarker(markerOptions);

                            count++;
                        }
                    }

                    //googleMap.setLatLngBoundsForCameraTarget(bounds.build());

                    //map.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 120));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        getDataFromHTTP.execute("http://168.188.115.191/exe.php?filename=" + uploadName + "&lat=" + lastKnownLocation.getLatitude() + "&lng=" + lastKnownLocation.getLongitude());
    }

    private void getCurrentLocationAndUpload(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 네트워크 프로바이더 사용가능여부
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            return;
        }

        if (isGPSEnabled) {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }else if(isNetworkEnabled){
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }else{
            lastKnownLocation = null;
        }
        if(lastKnownLocation != null){
            //connectFTP.myFTPUpload(uploadName, uploadName);
            sftp.upload(upleadFile);
        }else{
            msg("Can't get current location");
        }
    }

}