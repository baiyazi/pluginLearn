package com.weizu.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Trace;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // onClick
    public void jump(View view){
        try {
            Class<?> aClass = getClassLoader().loadClass("com.weizu.plugin.MainActivity");
            Log.e("TAG", "onCreate: " + aClass.getName());
            Intent intent = new Intent(MainActivity.this, aClass);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}