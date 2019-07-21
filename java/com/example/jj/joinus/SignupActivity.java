package com.example.jj.joinus;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.jj.joinus.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class SignupActivity extends AppCompatActivity {

    private EditText email;
    private EditText name;
    private EditText password;
    private Button signup;
    private String splash_background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(); //원격 설정을 위한 선언

        splash_background = mFirebaseRemoteConfig.getString(getString(R.string.rc_color));
        getWindow().setStatusBarColor(Color.parseColor(splash_background));

        email = (EditText) findViewById(R.id.signupActivity_edittext_email);
        name = (EditText) findViewById(R.id.signupActivity_edittext_name);
        password = (EditText) findViewById(R.id.signupActivity_edittext_password);
        signup = (Button) findViewById(R.id.signupActivity_button_signup);

        signup.setBackgroundColor(Color.parseColor(splash_background)); //색 지정
        signup.setEnabled(true);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {   //회원가입 버튼 클릭시

                if (email.getText().toString().equals("") ||        //입력을 하지 않았으면
                        name.getText().toString().equals("") ||
                        password.getText().toString().equals("")) {
                    Toast.makeText(SignupActivity.this, "정보를 입력하세요.", Toast.LENGTH_SHORT).show();   //메세지 출력
                    return;
                } else if (password.getText().length() < 6) {
                    Toast.makeText(SignupActivity.this, "비밀번호를 6자리\n 이상 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Toast.makeText(SignupActivity.this, "회원가입 중입니다.\n 잠시만 기다려주세요", Toast.LENGTH_SHORT).show();
                    signup.setEnabled(false);
                    FirebaseAuth.getInstance()
                            .createUserWithEmailAndPassword(email.getText().toString(), //파이어베이스 라이브러리 사용 사용자가 입력한 email과 password를 파이어베이스로 전송
                                    password.getText().toString())
                            .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {    // 회원가입이 진행되었으면

                                    if (task.isSuccessful()) {  //회원가입이 정상적으로 완료되었을 시
                                        UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(name.getText().toString()).build();
                                        task.getResult().getUser().updateProfile(userProfileChangeRequest); //채팅을 위해 파이어베이스 유저 프로파일 바꿈
                                        UserModel userModel = new UserModel();  //유저 모델 클래스 객체 생성
                                        userModel.userName = name.getText().toString(); //유저가 입력한 아이디를 담음
                                        userModel.uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); //회원 가입할때 생기는 uid 를 담음

                                        String uid = task.getResult().getUser().getUid();
                                        FirebaseDatabase.getInstance().getReference().child("users").child(uid).setValue(userModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            //파이어베이스의 인스턴스를 받아와서 users 목록에 유저 uid에 유저모델을 넣음
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(SignupActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                                                SignupActivity.this.finish();
                                            }
                                        });
                                    } else {   // 회원가입 실패
                                        Toast.makeText(SignupActivity.this, "이메일이 존재하거나\n 이메일형식이 아닙니다.", Toast.LENGTH_SHORT).show();
                                        signup.setEnabled(true);
                                    }
                                }
                            });
                } catch (Exception e) {
                    Toast.makeText(SignupActivity.this, "다시 시도하세요.", Toast.LENGTH_SHORT).show();
                    signup.setEnabled(true);
                }
            }
        });
    }
}