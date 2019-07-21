package com.example.jj.joinus.chat;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.jj.joinus.R;
import com.example.jj.joinus.model.ChatModel;
import com.example.jj.joinus.model.NotificationModel;
import com.example.jj.joinus.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MessageActivity extends AppCompatActivity {    //채팅방

    private String destinationUid;
    private Button button;
    private EditText editText;
    private String uid;
    private String chatRoomUid;
    private RecyclerView recyclerView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH.mm"); //메세지 보낸 시간 형식
    private UserModel destinationUserModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();     // 채팅을 요구 하는아이디 , 단말기에 로그인된 uid
        destinationUid = getIntent().getStringExtra("destinationUid"); //채팅을 당하는 유저 uid
        button = (Button) findViewById(R.id.messageActivity_button);
        editText = (EditText) findViewById(R.id.messageActivity_editText);
        recyclerView = (RecyclerView) findViewById(R.id.messageActivity_recyclerview); //채팅목록을 보여주는 화면

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {   //버튼을 누를시 대화방 생성
                ChatModel chatModel = new ChatModel();
                chatModel.users.put(uid, true);  //모델에 자신의 uid 입력
                chatModel.users.put(destinationUid, true);   //상대방 uid 입력

                if (chatRoomUid == null) { //새로 채팅방 생성
                    button.setEnabled(false);
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            checkChatRoom(); //데이터 입력 완료를 연락받으면 채팅방 체크, push 는 고유의 채팅방 생성
                        }
                    });
                } else {  //채팅방이 존재 할 경우
                    ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = editText.getText().toString();    //채팅방에서 쓰여진 채팅 메세지
                    comment.timesteamp = ServerValue.TIMESTAMP; //보낸시간
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            sendGcm();  //상대방과 채팅방 목록을 불러옴 , 푸시 알림
                            editText.setText(""); //기존 메세지 초기화
                        }
                    });

                }
            }
        });
        checkChatRoom();
    }

    void sendGcm() { // 푸시 알림 보내기
        Gson gson = new Gson(); //gson 객체 생성
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName(); // 사용자 이름을 담음
        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = destinationUserModel.pushToken; //상대방 토큰 달 담음
        notificationModel.notification.title = userName; // 유저이름
        notificationModel.notification.text = editText.getText().toString(); //내용을 담음 백그라운드 알림
        notificationModel.data.title = userName;
        notificationModel.data.text = editText.getText().toString(); // 포그라운드 알림의 이름,내용을 담음

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=uft8"), gson.toJson(notificationModel)); //json 형태로 title과 ,내용, to 를 보냄
        Request request = new Request.Builder()
                .header("Content-Type", "application/json") //헤더부분
                .addHeader("Authorization", "key=AIzaSyA1NTLj3m22IauF8niI7clKFRZlRIYMpqA")
                .url("https://gcm-http.googleapis.com/gcm/send") // firebase 의 gcm 을 통해 전송
                .post(requestBody) //내용
                .build(); //만듬

        OkHttpClient okHttpClient = new OkHttpClient(); // 클라이언트 생성
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {  //메세지 실패시

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException { //메세지 성공시

            }
        });

    }

    void checkChatRoom() { //채팅방 확인
        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/" + uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override           //
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    ChatModel chatModel = item.getValue(ChatModel.class); // 파이어베이스 에서 user들의 이름을 받아옴
                    if (chatModel.users.containsKey(destinationUid)) {    //상대방의 uid가 있으면
                        chatRoomUid = item.getKey(); // 채팅방 id를 가져옴
                        button.setEnabled(true);
                        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
                        recyclerView.setAdapter(new RecyclerViewAdapter());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> { //이너 클래스 글로벌변수와 공유

        List<ChatModel.Comment> comments;   //리스트
        public RecyclerViewAdapter() {      //생성자
            comments = new ArrayList<>();
            FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override   //상대방 Uid로 유저 정보를 담아옴
                public void onDataChange(DataSnapshot dataSnapshot) {
                    destinationUserModel = dataSnapshot.getValue(UserModel.class);  //유저모델로 상대방 정보를 받아옴
                    getMessageList();   //상대방과의 메세지를 불러옴
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
        });

        }

        void getMessageList() { //메세지를 받아옴
            FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").addValueEventListener(new ValueEventListener() {  //특정방의 채팅을 가져옴
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) { //읽어들어온 데이터가 이쪽으로 들어옴
                    comments.clear();       //데이터를 추가할때 마다 데이터를 다보내기 때문에 클리어없으면 쌓임

                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        comments.add(item.getValue(ChatModel.Comment.class));   //읽어들인 코맨트를 추가
                    }
                    notifyDataSetChanged();     //메세지 갱신
                    recyclerView.scrollToPosition(comments.size() - 1); // 마지막 메세지로 스크롤이 이동
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false); //message 를 띄움
            return new MessageViewHolder(view); // 뷰 재사용시 사용하는 클래스
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {  //데이터를 바인딩
            MessageViewHolder messageViewHolder = ((MessageViewHolder) viewHolder); //메세지 뷰홀더

            if (comments.get(position).uid.equals(uid)) {   //내 메세지일경우
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.blue_chat_bubble);
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.messageItem_linearlayout_main.setGravity(Gravity.RIGHT);  //내가 보낸 메세지 표시
            } else {
                Glide.with(viewHolder.itemView.getContext()) //상대방이 보낸 메세지일경우
                        .load(R.drawable.loading_icon)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);
                messageViewHolder.textView_name.setText(destinationUserModel.userName);
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.yellow_chat_bubble);
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.messageItem_linearlayout_main.setGravity(Gravity.LEFT);   //상대방 메세지 표시
            }
            long unixTime = (long) comments.get(position).timesteamp; // 타임 정보를 담아옴
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); // 서울 시간으로 설정
            String time = simpleDateFormat.format(date); //데이터 포맷으로 시간을 받아옴
            messageViewHolder.textView_timesteamp.setText(time); //출력

        }

        @Override
        public int getItemCount() {
            return comments.size();
        } // 사이즈

        private class MessageViewHolder extends RecyclerView.ViewHolder { //뷰 재사용을 위한 클래스
            public TextView textView_message;
            public TextView textView_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout messageItem_linearlayout_main;
            public TextView textView_timesteamp;

            public MessageViewHolder(View view) {
                super(view);
                textView_message = (TextView) view.findViewById(R.id.messageItem_textView_message);
                textView_name = (TextView) view.findViewById(R.id.messageItem_textView_name);   //xml 파일과 연결
                imageView_profile = (ImageView) view.findViewById(R.id.messageItem_imageview_profile);
                linearLayout_destination = (LinearLayout) view.findViewById(R.id.messageItem_linearlayout_destination);
                messageItem_linearlayout_main = (LinearLayout) view.findViewById(R.id.messageItem_linearlayout_main);
                textView_timesteamp = (TextView) view.findViewById(R.id.messageItem_textview_timesteamp);
            }
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fromleft, R.anim.toright); // 백키 눌를시 없어질때 왼쪽에서 오른쪽으로
    }
}
