package com.example.jj.joinus;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class LoginActivity extends AppCompatActivity {

    private EditText id;
    private EditText password;
    private Button login;
    private Button signup;
    private FirebaseRemoteConfig FirebaseRemoteConfig;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener; // 로그인 됬는지 확인해주는 리스너

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(); //원격으로 설정하기 위해 인스턴스 get
        firebaseAuth = FirebaseAuth.getInstance();  //Authentication 인스턴스 get
        firebaseAuth.signOut(); //로그인된 리소스를 비워줌

        String splash_background = FirebaseRemoteConfig.getString(getString(R.string.rc_color)); // 원격으로 테마설정
        getWindow().setStatusBarColor(Color.parseColor(splash_background));  //상태색 지정

        id = (EditText) findViewById(R.id.loginActivity_edittext_id);
        password = (EditText) findViewById(R.id.loginActivity_edittext_password);

        login = (Button) findViewById(R.id.loginActivity_button_login);
        signup = (Button) findViewById(R.id.loginActivity_button_signup);
        login.setBackgroundColor(Color.parseColor(splash_background));  //백그라운드 색 지정
        signup.setBackgroundColor(Color.parseColor(splash_background));

        login.setOnClickListener(new View.OnClickListener() {   //로그인 버튼 클릭시
            @Override
            public void onClick(View v) {           //로그인 버튼 클릭시
                if (id.getText().length() == 0 || password.getText().length() == 0) {   //아이디와 비밀번호가 입력이 안되어있을 시
                    Toast.makeText(LoginActivity.this, "정보를 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                } else {    //입력이 되었다면
                    loginEvent();   //로그인 이벤트 호출
                    login.setEnabled(false);
                    signup.setEnabled(false);
                }
            }
        });
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {   //회원가입 클릭시
                login.setEnabled(true);
                signup.setEnabled(true);
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });


        authStateListener = new FirebaseAuth.AuthStateListener() {    //로그인 인터페이스 리스너
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) { //( 로그인, 로그아웃시 상태 변화시 호출)
                FirebaseUser user = firebaseAuth.getCurrentUser();  //유저를 얻어와서
                if (user != null) { //로그인
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class); //인텐트를 통해 Main 액티비티 호출
                    intent.putExtra("Id", id.getText().toString()); //Id를 담아서 액티비티 호출
                    login.setEnabled(true);
                    signup.setEnabled(true);
                    startActivity(intent);
                    finish();       //로그인 화면 종료
                } else {    //로그아웃
                }

            }
        };

    }

    void loginEvent() {     // 로그인 버튼을 눌렀을 시
        Toast.makeText(LoginActivity.this, "로그인 중 입니다.", Toast.LENGTH_SHORT).show();
        firebaseAuth.signInWithEmailAndPassword(id.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override                           //입력한 정보를 파이어베이스로 보냄
            public void onComplete(@NonNull Task<AuthResult> task) {    //로그인 실패,성공 여부만 알려줌
                if (!task.isSuccessful()) {     //로그인 실패시
                    Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "로그인 성공.", Toast.LENGTH_SHORT).show();   //로그인 성공시
                }
                login.setEnabled(true);
                signup.setEnabled(true);
            }
        });

    }

    @Override
    protected void onStart() {   //활동을 초기화할때 현재 로그인되어있는지 확인
        super.onStart();
        if (authStateListener != null)
            firebaseAuth.addAuthStateListener(authStateListener);   //auth state 리스너를 붙여줌
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(authStateListener);    //auth state 리스너를 떼어줌
    }
}
