package com.example.jj.joinus.model;

public class NotificationModel {
    public String to;
    public Notification notification = new Notification(); //초기화
    public Data data =new Data();

    public static class Notification { //백그라운드
        public String title;
        public String text;
    }

    public static class Data {  //포그라운드
        public String title;
        public String text;

    }
}
