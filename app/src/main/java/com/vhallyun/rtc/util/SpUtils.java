package com.vhallyun.rtc.util;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class SpUtils {
    public final static String KEY_APP_ID = "appid";
    public final static String KEY_USER_ID = "userid";
    public final static String KEY_PIX_TYPE = "pix_type";
    public final static String KEY_LSS_ID = "lss_id";//直播id
    public static final String KEY_INAV_ID = "inavId";
    public static final String KEY_TOKEN = "token";
    static SpUtils instance;
    public static SpUtils share(){
        if(instance == null){
            instance = new SpUtils();
        }
        return instance;
    }

    private static Application app;
    public static void init(Application app){
        SpUtils.app = app;
    }
    private SharedPreferences sp;
    private SpUtils(){
        sp = app.getSharedPreferences("config", Context.MODE_PRIVATE);
    }

    public String getAppId(){
        return sp.getString(KEY_APP_ID,"90b5668b");
    }


    public int getDefinition(){
        return sp.getInt(KEY_PIX_TYPE,0);
    }

    public String getBroadcastId(){
        return sp.getString(KEY_LSS_ID,"");
    }

    public String getUserId(){
        return sp.getString(KEY_USER_ID, Build.MODEL);
    }

    public void commitStr(String key,String txt){
        sp.edit().putString(key,txt).apply();
    }

    public String getStr(String key,String def){
        return sp.getString(key,def);
    }
}
