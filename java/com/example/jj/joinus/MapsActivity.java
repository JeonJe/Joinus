package com.example.jj.joinus;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.example.jj.joinus.model.GpsData;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static String IP_ADDRESS = "ubuntu@18.223.135.213";             //URL 연결

    private GoogleApiClient mGoogleApiClient = null;
    private GoogleMap mGoogleMap = null;
    private Marker currentMarker = null;
    private Marker surroundingsMarker = null;

    private static final String TAG = "Joinus - Map";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2002;
    private static final int UPDATE_INTERVAL_MS = 5000;          // 1000 = 1sc  위치가 업데이트 되는주기
    private static final int FASTEST_UPDATE_INTERVAL_MS = 5000; // 1000 = 1sc  위치 획득후 업데이트 되는주기

    private AppCompatActivity mActivity;
    boolean askPermissionOnceAgain = false;
    boolean mRequestingLocationUpdates = false;     //지도 업데이트 필요
    Location mCurrentLocatiion;
    boolean mMoveMapByUser = true;
    boolean mMoveMapByAPI = true;                   //카메라에 필요
    LatLng currentPosition;

    public String Id;
    public String Study;
    public String Gender; //사용자 정보
    public String Level;
    public String Message;

    private ArrayList<GpsData> mArrayList;
    private String mJsonString;

    LocationRequest locationRequest = new LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL_MS)
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS); // 업데이트 주기 설정

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_maps);
        Intent data = getIntent(); //Account 액티비티 에서 가져온 사용자 정보를 가지는 인텐트
        mArrayList = new ArrayList<>();

        Log.d(TAG, "onCreate");
        mActivity = this;

        mGoogleApiClient = new GoogleApiClient.Builder(this) //구글 API 생성
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onResume() {

        super.onResume();

        if (mGoogleApiClient.isConnected()) { // 맵 api 가 연결되어있으면
            Log.d(TAG, "onResume : call startLocationUpdates");
            if (!mRequestingLocationUpdates) startLocationUpdates(); // start 위치 업데이트
        }


        if (askPermissionOnceAgain) { //연결되어 있지 않으면
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                askPermissionOnceAgain = false;
                checkPermissions(); //다시 권한 체크
            }
        }
    }


    private void startLocationUpdates() { //위치 정보 업데이트

        if (!checkLocationServicesStatus()) {   //퍼미션 가지고 있을때
            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();   // 위치 허용 알림 함수
        } else {
            //퍼미션 안가지고 있을때
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "startLocationUpdates : 퍼미션 권한 없음");
                return;
            }

            Log.d(TAG, "startLocationUpdates : call FusedLocationApi.requestLocationUpdates");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
            //일정시간과 일정거리 두개의 조건이 만족될경우 실행되는메소드

            mRequestingLocationUpdates = true;
            mGoogleMap.setMyLocationEnabled(true); //나의 위치정보 퍼미션 획득
        }
    }


    private void stopLocationUpdates() {    // 위치정보 업데이트 스탑

        Log.d(TAG, "stopLocationUpdates : LocationServices.FusedLocationApi.removeLocationUpdates");
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mRequestingLocationUpdates = false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) { //맵이 준비 되었을때
        Log.d(TAG, "onMapReady :");
        mGoogleMap = googleMap;
        setDefaultLocation(); //지도의 초기위치를 서울로

        GetData task = new GetData();
        task.execute("http://" + IP_ADDRESS + "/getjson.php", Id); // 데이터베이스에서 Id로 위치정보 가져오기

        if (mArrayList.size() != 0) {                  // 자신의 마커 그리기
            for (int i = 0; i < mArrayList.size(); i++) {

                MarkerOptions surroundingsMarkerOptions = new MarkerOptions();
                surroundingsMarkerOptions.position(new LatLng(Double.parseDouble(mArrayList.get(i).getM_lati()),
                        Double.parseDouble(mArrayList.get(i).getM_longi())))
                        .title(mArrayList.get(i).getM_id())
                        .alpha(0.7f)
                        .snippet(mArrayList.get(i).getM_study() + " ," + mArrayList.get(i).getM_gender() + " ,"
                                + mArrayList.get(i).getM_level() + " \n★" + mArrayList.get(i).getM_message() + "★");
                currentMarker = mGoogleMap.addMarker(surroundingsMarkerOptions);
            }
        }

        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true); //카메라 셋팅
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        mGoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                Log.d(TAG, "onMyLocationButtonClick : 위치에 따른 카메라 이동 활성화");
                mMoveMapByAPI = true;
                return true;
            }
        });

    }


    @Override
    public void onLocationChanged(Location location) { //장소가 바뀌었을때

        currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
        String lati = Double.toString(location.getLatitude());  //위도와 경도 받아서 위치 설정
        String longi = Double.toString(location.getLongitude());
        Id = getIntent().getStringExtra("Id");
        Study = getIntent().getStringExtra("Study");
        Gender = getIntent().getStringExtra("Gender");
        Level = getIntent().getStringExtra("Level");
        Message = getIntent().getStringExtra("Message");

        InsertData task = new InsertData();
        task.execute("http://" + IP_ADDRESS + "/query.php", lati, longi, Id, Study, Gender, Level, Message); //자신의 db에 데이터 전달

        GetData Gtask = new GetData();
        Gtask.execute("http://" + IP_ADDRESS + "/getjson.php", Id); //db에서 자신 제외한 데이터 받아오기
        // showresult함수를통해 오브젝트를담은리스트생성

        Log.d(TAG, "onLocationChanged : ");
        mGoogleMap.clear(); //맵클리어

        String markerTitle = Id;      // 마커 타이틀, snippet 설정
        String markerSnippet = Study + " ," + Gender + " ," + Level + " ,\n"
                + "★" + Message + "★";

        setCurrentLocation(location, markerTitle, markerSnippet); //현재위치 설정
        mCurrentLocatiion = location;
    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) { //현재위치로 마커그리는 함수

        mMoveMapByUser = false;
        if (currentMarker != null) currentMarker.remove(); //마커 지우기

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude()); //위치를 위도와 경도로 설정
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng).title(markerTitle).snippet(markerSnippet).draggable(true).alpha(0.7f); //현재위치 마커로 설정
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        currentMarker = mGoogleMap.addMarker(markerOptions);        //현재위치 마커 추가

        if (mArrayList.size() != 0) {                  //주변 유저 마커 그리기

            for (int i = 0; i < mArrayList.size(); i++) {
                MarkerOptions surroundingsMarkerOptions = new MarkerOptions();
                surroundingsMarkerOptions.position(new LatLng(Double.parseDouble(mArrayList.get(i).getM_lati()),
                        Double.parseDouble(mArrayList.get(i).getM_longi())))
                        .title(mArrayList.get(i).getM_id())
                        .alpha(0.7f)
                        .snippet(mArrayList.get(i).getM_study() + " ," + mArrayList.get(i).getM_gender() + " ,"
                                + mArrayList.get(i).getM_level() + " \n★" + mArrayList.get(i).getM_message() + "★");
                currentMarker = mGoogleMap.addMarker(surroundingsMarkerOptions);
            }
        }

        if (mMoveMapByAPI) { //맵움직힐때

            Log.d(TAG, "setCurrentLocation :  mGoogleMap moveCamera "
                    + location.getLatitude() + " " + location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 15);
            mGoogleMap.moveCamera(cameraUpdate);            //지도 업데이트
            mGoogleMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapsActivity.this)); //커스텀인포창
        }
    }

    @Override
    protected void onStart() {

        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            Log.d(TAG, "onStart: mGoogleApiClient connect");
            mGoogleApiClient.connect();  //사용자 api 연결

        }
        super.onStart();
    }

    @Override
    protected void onStop() {

        if (mRequestingLocationUpdates) { //지도 정보가 업데이트 되고있으면 위치정보 업데이트 중단

            Log.d(TAG, "onStop : call stopLocationUpdates");
            stopLocationUpdates();
        }

        if (mGoogleApiClient.isConnected()) { //사용자 api 연결이 연결되어있으면 연결 중단

            Log.d(TAG, "onStop : mGoogleApiClient disconnect");

        }
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) { // 백키를 누를 시

        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            AlertDialog.Builder d = new AlertDialog.Builder(MapsActivity.this);
            d.setTitle("EXIT 2013722020 이전제");
            d.setMessage("스터디 정보를 남겨두시겠습니까?");
            d.setPositiveButton("예", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MapsActivity.this.finish();
                }
            });
            d.setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DeleteData Dtask = new DeleteData();
                    Dtask.execute("http://" + IP_ADDRESS + "/delete.php", Id); //사용자 정보를 데이터베이스에서 지움
                    MapsActivity.this.finish();
                }
            });
            d.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onConnected(Bundle connectionHint) {  //연결되었을때

        if (!mRequestingLocationUpdates) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION); // Manifest 퍼미션 체크

                if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED) {

                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION); //퍼미션 없으면 요청

                } else {
                    Log.d(TAG, "onConnected : 퍼미션 가지고 있음");
                    Log.d(TAG, "onConnected : call startLocationUpdates");
                    startLocationUpdates(); //퍼미션 가지고 있으면 위치정보 업데이트 시작
                    mGoogleMap.setMyLocationEnabled(true);
                }
            } else {

                Log.d(TAG, "onConnected : call startLocationUpdates");
                startLocationUpdates(); //퍼미션 가지고 있으면 위치정보 업데이트 시작
                mGoogleMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
        setDefaultLocation();   //연결이 실패하면 기본 장소로
    }

    @Override
    public void onConnectionSuspended(int cause) { //연결 지연시
        Log.d(TAG, "onConnectionSuspended");
        if (cause == CAUSE_NETWORK_LOST)
            Log.e(TAG, "onConnectionSuspended(): Google Play services " +
                    "connection lost.  Cause: network lost.");
        else if (cause == CAUSE_SERVICE_DISCONNECTED)
            Log.e(TAG, "onConnectionSuspended():  Google Play services " +
                    "connection lost.  Cause: service disconnected");
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void setDefaultLocation() { //deafult location

        mMoveMapByUser = false;

        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97); //디폴트 위치, Seoul

        if (currentMarker != null) currentMarker.remove(); //현재위치 지우기
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15); // 카메라 업데이트
        mGoogleMap.moveCamera(cameraUpdate);

    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() { // 퍼미션 체크 함수
        boolean fineLocationRationale = ActivityCompat
                .shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager
                .PERMISSION_DENIED && fineLocationRationale) //퍼미션이 허가되지 않았을 경우
            showDialogForPermission("퍼미션을 허가 하시겠습니까?");

        else if (hasFineLocationPermission
                == PackageManager.PERMISSION_DENIED && !fineLocationRationale) {
            showDialogForPermissionSetting("퍼미션 거부 + 다시묻지않음 " +
                    "체크 박스를 설정한 경우로 설정에서 퍼미션 허가.");
        } else if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {


            Log.d(TAG, "checkPermissions : 퍼미션 가지고 있음");

            if (!mGoogleApiClient.isConnected()) {
                Log.d(TAG, "checkPermissions : 퍼미션 가지고 있음");
                mGoogleApiClient.connect();
            }
        }
    }

    @Override


    public void onRequestPermissionsResult(int permsRequestCode,        //퍼미션 요청 결과 처리
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (permsRequestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grantResults.length > 0) {

            boolean permissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (permissionAccepted) {
                if (!mGoogleApiClient.isConnected()) {
                    Log.d(TAG, "onRequestPermissionsResult : mGoogleApiClient connect");
                    mGoogleApiClient.connect();
                }
            } else {
                checkPermissions();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) { // 퍼미션 요청 함수

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this); //Dialog 생성
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},     //퍼미션 바꾸기
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForPermissionSetting(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                askPermissionOnceAgain = true;

                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + mActivity.getPackageName())); // 퍼미션 허락으로 셋팅
                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(myAppSettings);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }


    private void showDialogForLocationServiceSetting() {    // GPS 활성화를 위한 함수

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("위치 서비스 비 활성화");
        builder.setMessage("Joinus는 위치 정보가 필요합니다.\n"
                + "위치 설정을 바꾸시겠습니까?");
        builder.setCancelable(true);
        builder.setPositiveButton("위치 서비스 사용", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS); //Gps 허가 셋팅
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                if (checkLocationServicesStatus()) {    //사용자가 GPS 활성 시켰는지 검사
                    if (checkLocationServicesStatus()) {
                        Log.d(TAG, "onActivityResult : 퍼미션 가지고 있음");
                        if (!mGoogleApiClient.isConnected()) {
                            Log.d(TAG, "onActivityResult : mGoogleApiClient connect ");
                            mGoogleApiClient.connect(); //연결되지 않았으면 연결
                        }
                        return;
                    }
                }
                break;
        }
    }

    class InsertData extends AsyncTask<String, Void, String> {    // 사용자 정보를 데이터베이스에 저장
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() { //backgound 작업 시작전에 UI 작업을 진행
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MapsActivity.this,      //잠깐씩 출력됨
                    "Location Updating...", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {           //backgound 작업 시작후에 UI 작업을 진행
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d(TAG, "Insert onPostExecute");

            if (result == null) { //결과가 없으면
                progressDialog = ProgressDialog.show(MapsActivity.this,      //잠깐씩 출력됨
                        "onPostExecute error", null, true, true);
            } else {
                mJsonString = result; //결과가있으면 mJsonString에 담음
            }
        }

        @Override
        protected String doInBackground(String... params) {     //데이터 접근 할 경우 시간이 오래걸리므로 doInBackground 통해 실행

            String lati = (String) params[1];
            String longi = (String) params[2];
            String Id = (String) params[3];
            String Study = (String) params[4];
            String Gender = (String) params[5];
            String Level = (String) params[6];
            String Message = (String) params[7];

            String serverURL = (String) params[0];
            String postParameters = "lati=" + lati + "&longi=" + longi + "&Id=" + Id + "&Study=" + Study
                    + "&Gender=" + Gender + "&Level=" + Level + "&Message=" + Message;

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.setConnectTimeout(10000);         //URL 커넥션
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8")); //outputStream 셋팅
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "Insert POST response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {          //연결이 정상
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();       //연결이 비정상
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();                //데이터 받을때
                String line = null;

                while ((line = bufferedReader.readLine()) != null) {    //데이터를 읽음
                    sb.append(line);
                }

                bufferedReader.close();
                return sb.toString();

            } catch (Exception e) {         //예외처리

                Log.d(TAG, "InsertData: Error ", e);
                return new String("Error: " + e.getMessage());
            }
        }

    }

    private class GetData extends AsyncTask<String, Void, String> { //  PHP, Mysql 에서 사용자들 스터디 정보 가져옴

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(MapsActivity.this,  // 시간이 걸리면 잠깐씩 표시
                    "Getting Data...", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();

            Log.d(TAG, "Get onPostExecute");

            if (result == null) { //결과가 없으면
                progressDialog = ProgressDialog.show(MapsActivity.this,      //잠깐씩 출력됨
                        "Get onPostExecute error", null, true, true);

            } else {
                mJsonString = result; //사용자 정보를 mJsonString에 담아옴
                showResult();  // mJsonString 파싱
            }
        }

        @Override
        protected String doInBackground(String... params) { //데이터 접근 할 경우 시간이 오래걸리므로 doInBackground 통해 실행

            String serverURL = params[0];
            String postParameters = "Id=" + params[1];

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);  //URL 커넥션
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8")); //outputStream 셋팅
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "Get response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {  //연결이 정상
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();  //연결이 비정상
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {  //데이터를 읽음
                    sb.append(line);
                }

                bufferedReader.close();
                return sb.toString().trim();


            } catch (Exception e) {  //예외처리

                Log.d(TAG, "Getdata : Error ", e);
                errorString = e.toString();
                return null;
            }
        }
    }

    private class DeleteData extends AsyncTask<String, Void, String> { // IPHP, Mysql 에서 사용자 스터디 정보 삭제

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(TAG, "Delete response - " + result);

            if (result == null) {

            } else {
                mJsonString = result;

            }
        }

        @Override
        protected String doInBackground(String... params) {  //데이터 접근 할 경우 시간이 오래걸리므로 doInBackground 통해 실행

            String serverURL = params[0];
            String postParameters = "Id=" + params[1];

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");  //URL 커넥션
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));  //outputStream 셋팅
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "Delete response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {  //연결이 정상
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();    //연결이 비정상
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {  //데이터를 읽음
                    sb.append(line);
                }
                bufferedReader.close();

                return sb.toString().trim();

            } catch (Exception e) {  //예외처리
                Log.d(TAG, "Deletedata : Error ", e);
                errorString = e.toString();
                return null;
            }
        }
    }

    private void showResult() { //  데이터베이스에서 가져온 json 형식

        String TAG_JSON = "User_gps";
        String TAG_lati = "lati";
        String TAG_longi = "longi";
        String TAG_Id = "Id";
        String TAG_Study = "Study";
        String TAG_Gender = "Gender";
        String TAG_Level = "Level";
        String TAG_Message = "Message";

        try {
            JSONObject jsonObject = new JSONObject(mJsonString);  // User_gps 이름의 제이슨 오브젝트 생성
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);  //Json array 형태로 바꿈
            mArrayList.clear(); //목록 처기화
            for (int i = 0; i < jsonArray.length(); i++) {  //모든 데이터를 받음

                JSONObject item = jsonArray.getJSONObject(i);  // 아이템 오브젝트 생성

                String lati = item.getString(TAG_lati);
                String longi = item.getString(TAG_longi);           //받은데이터를 스트링형태로
                String Id = item.getString(TAG_Id);
                String Study = item.getString(TAG_Study);
                String Gender = item.getString(TAG_Gender);
                String Level = item.getString(TAG_Level);
                String Message = item.getString(TAG_Message);

                GpsData gpsData = new GpsData();                //GpsData 형태로저장

                gpsData.setM_lati(lati);
                gpsData.setM_longi(longi);
                gpsData.setM_id(Id);
                gpsData.setM_study(Study);
                gpsData.setM_gender(Gender);
                gpsData.setM_level(Level);
                gpsData.setM_message(Message);

                mArrayList.add(gpsData);                    //어레이 리스트에 넣기

            }

        } catch (JSONException e) { //예외처리
            Log.d(TAG, "showResult : ", e);
        }

    }


}