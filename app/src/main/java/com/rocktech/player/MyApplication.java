package com.rocktech.player;

import android.app.Application;

import com.gu.toolargetool.TooLargeTool;

/**
 * @author zhangyazhou
 * @date 2020/2/21
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
//        TooLargeTool.startLogging(this);
    }
}
