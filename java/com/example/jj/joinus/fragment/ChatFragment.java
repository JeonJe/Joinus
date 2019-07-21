package com.example.jj.joinus.fragment;


import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.jj.joinus.R;
import com.example.jj.joinus.chat.MessageActivity;
import com.example.jj.joinus.model.ChatModel;
import com.example.jj.joinus.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class ChatFragment extends Fragment {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false); //fragment_chat 화면을 불러옴

        RecyclerView recyclerView = view.findViewById(R.id.chatfragment_recyclerview);
        recyclerView.setAdapter(new ChatRecyclerViewAdapter()); //어뎁터 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));  //레이아웃 매니저 설정
        return view;
    }

    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<ChatModel> chatModels = new ArrayList<>(); //채팅목록
        private String uid;
        private ArrayList<String> destinationUsers = new ArrayList<>();

        public ChatRecyclerViewAdapter() { //생성자
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); //채팅목록 가져옴
            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/" + uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    chatModels.clear(); //목록 초기화
                    for (DataSnapshot item : dataSnapshot.getChildren()) { //파이어베이스에서 채팅목록을 가져와서 chatModels 에 추가
                        chatModels.add(item.getValue(ChatModel.class));
                    }
                    notifyDataSetChanged(); //새로고침
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) { //받아온 데이터를 보여줌
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_chat, viewGroup, false);
            return new CustomViewHolder(view); // 재사용할수있도록 커스텀 뷰홀더
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int i) {
            final CustomViewHolder customViewHolder = (CustomViewHolder) viewHolder; //뷰홀더 선언
            String destinationUid = null;   //대화 상대

            for (String user : chatModels.get(i).users.keySet()) { //채팅방에 있는 유저를 체크
                if (!user.equals(uid)) {    //내가 아닌 유저이면
                    destinationUid = user;      //그 유저 값 가져옴
                    destinationUsers.add(destinationUid);
                }
            }
            FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    UserModel userModel = dataSnapshot.getValue(UserModel.class); //가져온 값을 모델에 담음.
                    Glide.with(customViewHolder.imageView.getContext())
                            .load(R.drawable.loading_icon)
                            .apply(new RequestOptions().circleCrop())
                            .into(customViewHolder.imageView);
                    customViewHolder.textView_title.setText(userModel.userName); //채팅방이미지, 이름
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            //메세지를 내림 차순으로 정렬 후 마지막 메세지의 키값을 가져옴
            Map<String, ChatModel.Comment> commentMap = new TreeMap<>(Collections.reverseOrder());
            commentMap.putAll(chatModels.get(i).comments);  //채팅 내용을 넣어줌
            String lastMessageKey = (String) commentMap.keySet().toArray()[0]; //마지막 채팅 메세지를 채팅목록 화면뿌리기 위해 저장
            customViewHolder.textView_last_message.setText(chatModels.get(i).comments.get(lastMessageKey).message); //메세지를 커스텀뷰와 바인딩

            customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { //채팅 목록 클릭시
                    Intent intent = new Intent(v.getContext(), MessageActivity.class); //채팅방 액티비티 를
                    intent.putExtra("destinationUid", destinationUsers.get(i)); // 상대방 uid 를 담아서 호출
                    ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(v.getContext(), R.anim.fromright, R.anim.toleft); // 오른쪽에서 왼쪽으로 화면
                    startActivity(intent, activityOptions.toBundle()); //시작
                }
            });

            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));  //보낸 시간을 출력하기 위해 설정
            long unixTime = (long) chatModels.get(i).comments.get(lastMessageKey).timesteamp; //마지막 메세지 보낸시간 가져옴
            Date date = new Date(unixTime);
            customViewHolder.textView_timestamp.setText(simpleDateFormat.format(date)); // 보낸시간 화면에 뿌려줌
        }

        @Override
        public int getItemCount() {
            return chatModels.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder { //xml 형식을 담는 holder

            public ImageView imageView;
            public TextView textView_title;
            public TextView textView_last_message;
            public TextView textView_timestamp;

            public CustomViewHolder(View view) {
                super(view);
                imageView = (ImageView) view.findViewById(R.id.chatitem_imageview);
                textView_title = (TextView) view.findViewById(R.id.chatitem_textview_title);
                textView_last_message = (TextView) view.findViewById(R.id.chatitem_textview_lastMessage);
                textView_timestamp = (TextView) view.findViewById(R.id.chatitem_textview_timestamp);
            }
        }
    }
}
