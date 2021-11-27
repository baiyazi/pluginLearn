package com.weizu.myapplication;

import android.app.Application;
import android.content.res.Resources;

public class BaseApplication extends Application {

    private static final String pluginPath = "/sdcard/plugin-debug.apk";
    private Resources pluginResources;

    @Override
    public void onCreate() {
        super.onCreate();

        LoadUtils.loadClass(this, pluginPath); // 原init方法，修改了名字
        HookAMSUtils.getActivityManagerService(this, ProxyActivity.class);
        HookAMSUtils.hookActivityThreadToLaunchActivity();
        pluginResources = LoadUtils.loadPluginResource(this, pluginPath);
    }

    @Override
    public Resources getResources() {
        if (pluginResources != null) return pluginResources;
        return super.getResources();
    }
}
