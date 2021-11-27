package com.weizu.plugin;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {
    public void showInfo(Context context) {
        Toast.makeText(context, "INFO", Toast.LENGTH_SHORT).show();
    }
}
