package com.vhallyun.rtc;

import android.app.Application;

import com.vhallyun.rtc.util.SpUtils;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SpUtils.init(this);
    }
}
