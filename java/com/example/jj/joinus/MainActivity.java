package com.example.jj.joinus;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.ImageView;

import com.example.jj.joinus.fragment.ChatFragment;
import com.example.jj.joinus.fragment.PeopleFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String notificationIntent = getIntent().getStringExtra("NOTIFICATION");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.mainactivity_bottomnavigationview);    //하단의 선택창
        Drawable alpha = ((ImageView) findViewById(R.id.mainactivity_imageView)).getDrawable(); //투명도
        alpha.setAlpha(65);

        if (notificationIntent != null) {
            if (notificationIntent.equals("notify")) {
                getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new ChatFragment()).commit();
            }
        }
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) { //바텀 네비게이션 선택시
                switch (menuItem.getItemId()) {
                    case R.id.action_people:
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new PeopleFragment()).commit();  //유저 목록 Fragment 화면 띄움
                        return true;
                    case R.id.action_chat:
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new ChatFragment()).commit();   //채팅방 목록 Fragment 화면 띄움
                        return true;
                    case R.id.action_account:   // account activity를 띄움
                        Intent intent = new Intent(getApplicationContext(), AccountActivity.class);
                        startActivity(intent);
                        return true;
                }
                return false;
            }
        });

        passPushTokenToServer(); //푸시 알림을 위해 토큰을 서버로 전송

    }

    void passPushTokenToServer() { //푸시 알림을 위해 토큰을 서버로 전송
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); //현재 계정의 uid
        String token = FirebaseInstanceId.getInstance().getToken(); //현재 id 의 토큰
        Map<String, Object> map = new HashMap<>(); //uid 와 토큰 정보를 hash map으로 넣음 (파이어베이스 설정 상)
        map.put("pushToken", token);

        FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(map); //토큰 업데이트

    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {  //뒤로 갈 시
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            AlertDialog.Builder d = new AlertDialog.Builder(MainActivity.this); //종료 dialog 출력
            d.setTitle("EXIT");
            d.setMessage("종료하시겠습니까?");
            d.setPositiveButton("예", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    moveTaskToBack(true); //선택시 백그라운드 진행
                    finish();   // 액티비티 종료
                }
            });
            d.setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            d.show();
            return true;    // 취소
        }
        return super.onKeyDown(keyCode, event);

    }
}