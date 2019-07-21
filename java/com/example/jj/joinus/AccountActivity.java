package com.example.jj.joinus;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jj.joinus.model.GpsData;
import com.example.jj.joinus.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

public class AccountActivity extends FragmentActivity {

    private static String TAG = "Joinus - Account "; // 디버그를 위한 Log
    public static final int mapREQUEST_CODE = 9;
    public static final int REQUEST_CODE = 10;                 // 인텐트 요청코드
    public static final int RESULT_CODE = 11;

    private TextView textView;
    public String Id;
    public String Study = "입력해주세요";
    public String Gender = "입력해주세요";
    public String Level = "입력해주세요";
    public String Message = "입력해주세요"; //사용자 입력 메세지
    private ArrayList<GpsData> mArrayList;
    private static String IP_ADDRESS = "ubuntu@18.223.135.213";             //URL 연결
    private String mJsonString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        final Button infoButton = (Button) findViewById(R.id.accountFragment_button_comment);
        final Button mapButton = (Button) findViewById(R.id.accountFragment_mapbutton);
        textView = (TextView) findViewById(R.id.accountFragment_Textview);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); //파이어베이스 uid 획득
        FirebaseDatabase.getInstance().getReference().child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) { //파이어베이스에서 유저 정보를 가져옴
                UserModel userModel = dataSnapshot.getValue(UserModel.class);
                Id = userModel.userName; //파이어베이스에서 Id를 받아서 입력
                textView.setText(Id); // 채팅방이미지, 이름

                mArrayList = new ArrayList<>();
                GetUserData task = new GetUserData(); // 사용자 정보를 PHP, Mysql 에서 가져옴
                task.execute("http://" + IP_ADDRESS + "/getdata.php", Id); //Id 를 PHP와 Mysql 로 보내 mJsonString에 결과를 담아옴

                if (mJsonString == null) {  //결과가 null 이면 입력해주세요 표시
                    textView.setText("아이디 : " + Id + "\n"
                            + "스터디 : " + Study + "\n"
                            + "세부 정보 : " + Level + "\n"
                            + "성별 : " + Gender + "\n"
                            + "하고 싶은 말 : " + Message + "\n");
                    textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) { //데이터를 읽지 못했을 시
                Toast.makeText(AccountActivity.this, "FireBase에서 데이터를 읽지못했습니다.", Toast.LENGTH_SHORT).show();
            }
        })
        ;


        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //정보추가 버튼 클릭시
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                        startActivityForResult(intent, REQUEST_CODE); //인턴테 생성해서 액태비티 시작
                    }
                }, 500); // 파이어베이스에서 정보를 읽어 오는중 일 수 때문에 딜레이 0.5초
            }
        });
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //맵버튼 클릭시
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent map_intent = new Intent(getApplicationContext(), MapsActivity.class);
                        map_intent.putExtra("Id", Id);
                        map_intent.putExtra("Study", Study);       //사용자 정보를 맵 액티비티로 전달
                        map_intent.putExtra("Gender", Gender);
                        map_intent.putExtra("Level", Level);
                        map_intent.putExtra("Message", Message);
                        startActivity(map_intent);
                    }
                }, 500);  // 파이어베이스에서 정보를 읽어 오는중 일 수 때문에 딜레이 0.5초
            }
        });
    } //on create

    private class GetUserData extends AsyncTask<String, Void, String> { // Id 로 PHP, Mysql 에서 사용자 스터디 정보 가져옴

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(AccountActivity.this, // 시간이 걸리면 잠깐씩 표시
                    "Getting Data...", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(TAG, "Get onPostExecute");

            if (result == null) { //결과가 없으면
                progressDialog = ProgressDialog.show(AccountActivity.this,      //잠깐씩 출력됨
                        "Get onPostExecute error", null, true, true);

            } else {
                mJsonString = result; //사용자 정보를 mJsonString에 담아옴
                showResult(); // mJsonString 파싱
            }
        }

        @Override
        protected String doInBackground(String... params) { //데이터 접근 할 경우 시간이 오래걸리므로 doInBackground 통해 실행

            String serverURL = params[0];
            String postParameters = "Id=" + params[1];

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000); //URL 커넥션
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush(); //outputStream 셋팅
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "Get response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) { //연결이 정상
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream(); //연결이 비정상
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) { //데이터를 읽음
                    sb.append(line);
                }
                bufferedReader.close();
                return sb.toString().trim();


            } catch (Exception e) { //예외처리

                Log.d(TAG, "Getdata : Error ", e);
                errorString = e.toString();
                return null;
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) //사용자 info 정보 가져온것
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CODE) {
            if (requestCode == REQUEST_CODE) { //Info Activtiy 에서 사용자 정보를 입력하였으면
                Study = data.getStringExtra("Study");
                Gender = data.getStringExtra("Gender"); // 변수 셋팅
                Level = data.getStringExtra("Level");
                Message = data.getStringExtra("Message");
                textView.setText("아이디 : " + Id + "\n"
                        + "스터디 : " + Study + "\n" // 정보 화면에 출력
                        + "세부 정보 : " + Level + "\n"
                        + "성별 : " + Gender + "\n"
                        + "하고 싶은 말 : " + Message + "\n");
                textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

            } else if (requestCode == mapREQUEST_CODE) //지도랑 주고받은 것
            {

            }
        }
    }

    private void showResult() { // Id를 통해 데이터베이스에서 가져온 json 형식

        String TAG_JSON = "User_gps";
        String TAG_lati = "lati";
        String TAG_longi = "longi";
        String TAG_Id = "Id";
        String TAG_Study = "Study";
        String TAG_Gender = "Gender";
        String TAG_Level = "Level";
        String TAG_Message = "Message";

        try {
            JSONObject jsonObject = new JSONObject(mJsonString); // User_gps 제이슨 오브젝트 생성
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON); //Json array 형태로 바꿈
            mArrayList.clear();
            for (int i = 0; i < jsonArray.length(); i++) { //모든 데이터를 받음

                JSONObject item = jsonArray.getJSONObject(i); // 아이템 오브젝트 생성

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
            textView.setText("아이디 : " + Id + "\n"
                    + "스터디 : " + mArrayList.get(0).getM_study() + "\n"
                    + "세부 정보 : " + mArrayList.get(0).getM_level() + "\n"
                    + "성별 : " + mArrayList.get(0).getM_gender() + "\n"
                    + "하고 싶은 말 : " + mArrayList.get(0).getM_message() + "\n"); // 받은 데이터를 textView 에 표시
            if (!mArrayList.get(0).getM_study().equals(""))
                Study = mArrayList.get(0).getM_study().toString();
            if (!mArrayList.get(0).getM_level().equals(""))
                Level = mArrayList.get(0).getM_level().toString();   // 변수에 설정
            if (!mArrayList.get(0).getM_gender().equals(""))
                Gender = mArrayList.get(0).getM_gender().toString();
            if (!mArrayList.get(0).getM_message().equals(""))
                Message = mArrayList.get(0).getM_message().toString();
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

        } catch (JSONException e) { // 예외처리
            Log.d(TAG, "showResult : ", e);
        }

    }

}

