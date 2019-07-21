package com.example.jj.joinus;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class SplashActivity extends AppCompatActivity {

    private LinearLayout linearLayout;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 상태바를 없앰
        linearLayout = (LinearLayout) findViewById(R.id.splashactivity_linearlayout);

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();             //파이어베이스 remote config 사용을 위한 개발자 모드로 사용 설정 (샘플코드)
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        mFirebaseRemoteConfig.setDefaults(R.xml.default_config); // default 값
        mFirebaseRemoteConfig.fetch(0)   //서버의 값이 있을경우 사용 , 디버기용으로 0시간 마다 요청
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mFirebaseRemoteConfig.activateFetched();
                        } else {
                        }
                        displayMessage();
                    }
                });

    }

    void displayMessage() {
        String splash_background = mFirebaseRemoteConfig.getString("splash_background");    //파이어베이스에서 원격설정한 값을 읽어옴
        boolean caps = mFirebaseRemoteConfig.getBoolean("splash_message_caps");
        String splash_message = mFirebaseRemoteConfig.getString("splash_message");
        linearLayout.setBackgroundColor(Color.parseColor(splash_background)); // 백그라운드 색정

        if (caps) {                                              //firebase 에서 경고창을 띄우고 싶을때 사용
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(splash_message).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.create().show();
        } else {
            startActivity(new Intent(this, LoginActivity.class)); //로그인 activity 시작
            finish();   //스플래쉬 종료
        }
    }


}