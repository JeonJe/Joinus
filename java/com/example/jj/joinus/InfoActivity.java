package com.example.jj.joinus;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class InfoActivity extends AppCompatActivity {
    public static final int RESULT_CODE = 11;
    public static String Id;
    public static String Study;
    public static String Gender;
    public static String Level;
    public static String Message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);


        final Button categoryButton = (Button) findViewById(R.id.CategoryButton);
        final TextView categoryView = (TextView) findViewById(R.id.CategoryVIew);
        final Button genderButton = (Button) findViewById(R.id.GenderButton);
        final TextView genderView = (TextView) findViewById(R.id.GenderView);
        final Button levelbutton = (Button) findViewById(R.id.LevelButton);
        final TextView levelView = (TextView) findViewById(R.id.LevelView);
        final EditText messageView = (EditText) findViewById(R.id.editText);
        final Button BackButton = (Button) findViewById(R.id.BackButton);
        levelbutton.setVisibility(View.INVISIBLE); //세부상항은 스터디 종류 선택시 표시

        messageView.addTextChangedListener(new TextWatcher() {  // 하고싶은말 창에 변화가 있을때
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { // 하고싶은말을 작성하였을때
                Message = messageView.getText().toString(); //말가져오기

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        //카테고리 버튼 클릭시
        categoryButton.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View view) {  //카테고리 리스트
                                                  final List<String> listItems = new ArrayList<>();
                                                  listItems.add("토익");
                                                  listItems.add("토스,오픽");
                                                  listItems.add("면접");
                                                  listItems.add("대외활동");
                                                  listItems.add("전공");
                                                  listItems.add("취업 스터디");
                                                  listItems.add("직접입력");
                                                  final CharSequence[] items = listItems.toArray(new String[listItems.size()]); //배열 복사
                                                  final List SelectedItems = new ArrayList();
                                                  int defaultItem = 0;
                                                  SelectedItems.add(defaultItem);

                                                  AlertDialog.Builder builder = new AlertDialog.Builder(InfoActivity.this); // dialog 생성
                                                  builder.setTitle("스터디 선택");
                                                  builder.setSingleChoiceItems(items, defaultItem,
                                                          new DialogInterface.OnClickListener() {
                                                              @Override
                                                              public void onClick(DialogInterface dialog, int which) { //스터디 선택 버튼 클릭시
                                                                  SelectedItems.clear();
                                                                  SelectedItems.add(which);
                                                              }
                                                          });
                                                  builder.setPositiveButton("선택", //선택버튼 클릭시
                                                          new DialogInterface.OnClickListener() {
                                                              public void onClick(DialogInterface dialog, int which) {
                                                                  String msg = "";
                                                                  if (!SelectedItems.isEmpty()) {
                                                                      int index = (int) SelectedItems.get(0);
                                                                      msg = listItems.get(index);         //msg 에 선택한 카테고리 가져옴
                                                                  }
                                                                  Toast.makeText(getApplicationContext(),
                                                                          msg, Toast.LENGTH_LONG).show();
                                                                  categoryView.setText(msg);
                                                                  categoryView.setPaintFlags(categoryView.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
                                                                  categoryView.setVisibility(View.VISIBLE); //카테고리 뷰에 나타냄
                                                                  levelbutton.setVisibility(View.VISIBLE);
                                                                  Study = categoryView.getText().toString();
                                                              }
                                                          });
                                                  builder.setNegativeButton("취소",     //취소 버튼클릭시
                                                          new DialogInterface.OnClickListener() {
                                                              public void onClick(DialogInterface dialog, int which) {
                                                              }
                                                          });
                                                  builder.show();
                                              }
                                          }
        );

        levelbutton.setOnClickListener(new View.OnClickListener() { //세부사항 버튼 표시
                                           @Override
                                           public void onClick(View view) { //스터디 종류에 따른 세부사항 리스트 표시
                                               final List<String> listItems = new ArrayList<>();
                                               if (Study.equals("토익")) {
                                                   listItems.add("900점 이상 ");
                                                   listItems.add("800~900점");
                                                   listItems.add("700~800점");
                                                   listItems.add("600~700점");
                                                   listItems.add("600점 이하");
                                               } else if (Study.equals("토스,오픽")) {
                                                   listItems.add("Level 8, AL ");
                                                   listItems.add("Level 7, IH");
                                                   listItems.add("Level 6, IM");
                                                   listItems.add("Level 5, IL");
                                                   listItems.add("Level 4이하, Novice 이하");
                                               } else if (Study.equals("면접")) {
                                                   listItems.add("개별 면접");
                                                   listItems.add("토론 면접");
                                                   listItems.add("PT 면접");
                                                   listItems.add("영어 면접");
                                                   listItems.add("그 외 ");
                                               } else if (Study.equals("대외활동")) {
                                                   listItems.add("공모전 ");
                                                   listItems.add("서포터즈");
                                                   listItems.add("봉사활동");
                                                   listItems.add("인턴");
                                                   listItems.add("그 외");
                                               } else if (Study.equals("전공")) {
                                                   listItems.add("학점 4.0점 이상");
                                                   listItems.add("학점 3.7 ~ 4.0점");
                                                   listItems.add("학점 3.4 ~ 3.7점");
                                                   listItems.add("학점 3.0 ~ 3.4점");
                                                   listItems.add("학점 3.0점 미만");
                                               } else if (Study.equals("취업 스터디")) {
                                                   listItems.add("인적성");
                                                   listItems.add("NCS");
                                                   listItems.add("자기 소개서");
                                                   listItems.add("코딩 테스트");
                                                   listItems.add("그 외");
                                               } else if (Study.equals("직접입력")) {
                                                   listItems.add("하단에 입력");
                                               }

                                               final CharSequence[] items = listItems.toArray(new String[listItems.size()]); //배열 복사
                                               final List SelectedItems = new ArrayList();
                                               int defaultItem = 0;
                                               SelectedItems.add(defaultItem);

                                               AlertDialog.Builder builder = new AlertDialog.Builder(InfoActivity.this); //dialog 생성
                                               builder.setTitle("세부 정보 선택");
                                               builder.setSingleChoiceItems(items, defaultItem,
                                                       new DialogInterface.OnClickListener() {
                                                           @Override
                                                           public void onClick(DialogInterface dialog, int which) {
                                                               SelectedItems.clear();
                                                               SelectedItems.add(which);
                                                           }
                                                       });
                                               builder.setPositiveButton("선택", //선택버튼 클릭시
                                                       new DialogInterface.OnClickListener() {
                                                           public void onClick(DialogInterface dialog, int which) {
                                                               String msg = "";
                                                               if (!SelectedItems.isEmpty()) {
                                                                   int index = (int) SelectedItems.get(0);
                                                                   msg = listItems.get(index);         //msg 에 카테고리 가져옴
                                                               }
                                                               Toast.makeText(getApplicationContext(),
                                                                       msg, Toast.LENGTH_LONG).show();
                                                               levelView.setText(msg);
                                                               levelView.setPaintFlags(levelView.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
                                                               levelView.setVisibility(View.VISIBLE); //선택한 view 를 표시
                                                               Level = levelView.getText().toString();  //레벨에 선택한것을 담음
                                                           }
                                                       });
                                               builder.setNegativeButton("취소",     //취소 버튼클릭시
                                                       new DialogInterface.OnClickListener() {
                                                           public void onClick(DialogInterface dialog, int which) {
                                                           }
                                                       });
                                               builder.show();
                                           }
                                       }
        );

        //성별 버튼 클릭시
        genderButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                final List<String> listItems = new ArrayList<>(); //성별
                                                listItems.add("남자");
                                                listItems.add("여자");
                                                final CharSequence[] items = listItems.toArray(new String[listItems.size()]); //배열 복사
                                                final List SelectedItems = new ArrayList();
                                                int defaultItem = 0;
                                                SelectedItems.add(defaultItem);

                                                AlertDialog.Builder builder = new AlertDialog.Builder(InfoActivity.this); //dialog 생성
                                                builder.setTitle("성별을 선택해주세요");
                                                builder.setSingleChoiceItems(items, defaultItem,
                                                        new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                SelectedItems.clear();
                                                                SelectedItems.add(which);
                                                            }
                                                        });
                                                builder.setPositiveButton("선택", //선택버튼 클릭시
                                                        new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                String msg = "";
                                                                if (!SelectedItems.isEmpty()) {
                                                                    int index = (int) SelectedItems.get(0);
                                                                    msg = listItems.get(index);         //msg 에 카테고리 가져옴
                                                                }
                                                                Toast.makeText(getApplicationContext(),
                                                                        msg, Toast.LENGTH_LONG).show();
                                                                genderView.setText(msg);
                                                                genderView.setPaintFlags(genderView.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
                                                                genderView.setVisibility(View.VISIBLE); //젠더 view 표시
                                                                messageView.setVisibility(View.VISIBLE); //메세지창 표시
                                                                Gender = genderView.getText().toString(); // 젠더에 성별을 담음

                                                            }
                                                        });
                                                builder.setNegativeButton("취소",     //취소 버튼클
                                                        // 릭시
                                                        new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int which) {
                                                            }
                                                        });
                                                builder.show();
                                            }
                                        }
        );


        BackButton.setOnClickListener(new View.OnClickListener() { // 입력완료 버튼클릭시
            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                data.putExtra("Id", Id); //사용자의 스터디 정보를 Intent에 담음
                data.putExtra("Study", Study);
                data.putExtra("Gender", Gender);
                data.putExtra("Level", Level);
                data.putExtra("Message", Message);
                setResult(InfoActivity.RESULT_CODE, data); //InfoActivity result_code 로 결과를 보냄
                finish();
            }
        });


    }

}
