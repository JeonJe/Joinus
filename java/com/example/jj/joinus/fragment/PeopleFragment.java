package com.example.jj.joinus.fragment;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.jj.joinus.R;
import com.example.jj.joinus.chat.MessageActivity;
import com.example.jj.joinus.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PeopleFragment extends Fragment { // 바텀네비게이터가 친구를 클릭하였을때 보여지는 화면
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people, container, false);        // fragment_people 화면을 띄워줌
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.peoplefragment_recyclerview); // 사용자 목록을 보여주기위한 recyclerView

        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));  //레이아웃 매니저 설정
        recyclerView.setAdapter(new PeopleFragmentRecyclerViewAdapter());   //어뎁터 설정
        FloatingActionButton floatingActionButton = (FloatingActionButton)view.findViewById(R.id.peoplefragment_floatingButton);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(),SelectFriendActivity.class));
            }
        });

        return view;
    }

    class PeopleFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> { //recyclerView 의 adapter
        List<UserModel> userModels;

        public PeopleFragmentRecyclerViewAdapter() { //생성자
            userModels = new ArrayList<>(); //유저목록 생성
            final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();  //나의 uid

            FirebaseDatabase.getInstance().getReference().child("users").addValueEventListener(new ValueEventListener() {   //users의 이벤트 리스너 등록
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) { //데이터가 변했을때
                    userModels.clear(); //목록 초기화
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) { //데이터를 가져와서

                        UserModel userModel = snapshot.getValue(UserModel.class);
                        if (userModel.uid.equals(myUid)) { ////내 아이디는 유저목록에서 제외
                            continue;
                        }
                        userModels.add(userModel);  // 유저모델에 추가
                    }
                    notifyDataSetChanged(); //유저 목록 새로고침
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);  //아이템_frend xml 을 띄움
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {     //frienditem 에 이미지와 텍스트 넣어주는 부분
            ((CustomViewHolder) holder).imageView.setImageResource(R.drawable.loading_icon);    //캐스팅 이미지뷰에 이미지를 넣어줌
            ((CustomViewHolder) holder).textView.setText(userModels.get(position).userName);    //유저의 이미지를 넣어줌

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {   // 유저목록창에서 아이디 클릭 시

                    Intent intent = new Intent(getView().getContext(), MessageActivity.class); // 메세지 엑티비티 호출
                    intent.putExtra("destinationUid", userModels.get(position).uid); // 클릭 당한 유저의 uid
                    ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(getView().getContext(), R.anim.fromright, R.anim.toleft); //오른쪽에서 왼쪽으로 날라오는 효과
                    startActivity(intent, activityOptions.toBundle());  //인텐트 시작
                }
            });
        }

        @Override
        public int getItemCount() {
            return userModels.size();
        } // 유저 수
    }

    private class CustomViewHolder extends RecyclerView.ViewHolder {    //Inner class , 친구 목록 1개
        public ImageView imageView;
        public TextView textView;

        public CustomViewHolder(View view) {
            super(view);
            imageView = (ImageView) view.findViewById(R.id.frienditem_imageview);   //사용자의 이미지
            textView = (TextView) view.findViewById(R.id.frienditem_textview);      //사용자의 아이디
        }
    }
}