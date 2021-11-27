package com.weizu.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Observable;

public class HookAMSUtils {
    private static final int LAUNCH_ACTIVITY = 100;
    private static final int EXECUTE_TRANSACTION = 159;

    public static final String ORIGIN_INTENT = "ORIGIN_INTENT";

    public static void getActivityManagerService(Context context, Class<? extends Activity> proxyActivityClazz) {
        try {
            Object iActivityManagerSingletonValue = null;
            //
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                Field iActivityManagerSingletonField = ActivityManager.class.getDeclaredField("IActivityManagerSingleton");
                iActivityManagerSingletonField.setAccessible(true);
                iActivityManagerSingletonValue = iActivityManagerSingletonField.get(null);
            }else{
                Class<?> aClass = Class.forName("android.app.ActivityManagerNative");
                Field getDefault = aClass.getDeclaredField("gDefault");
                getDefault.setAccessible(true);
                // 获取静态的gDefault对象
                iActivityManagerSingletonValue = getDefault.get(null);
            }

            // 而实际上AMS在单例Singleton中
            Class<?> singletonClazz = Class.forName("android.util.Singleton");
            Field mInstance = singletonClazz.getDeclaredField("mInstance");
            mInstance.setAccessible(true);
            Object amsObj = mInstance.get(iActivityManagerSingletonValue); // AMS
            Log.e("TAG", "getActivityManagerService: " + amsObj.toString());


            // 创建AMS的代理对象
            Class<?> aClass1 = Class.forName("android.app.IActivityManager");
            // 得到AMS的代理对象
            Object amsProxy = Proxy.newProxyInstance(context.getClassLoader(), new Class[]{aClass1}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    // 代理方法处理
                    if (method.getName().equals("startActivity")) {
                        // 查找参数，找到Intent对象
                        int index = 0;
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] instanceof Intent) {
                                index = i;
                                break;
                            }
                        }
                        // 拿到意图
                        Intent oldIntent = (Intent) args[index];
                        // 创建一个新的意图，将这个旧的意图添加到新的意图中
                        Intent newIntent = new Intent(context, proxyActivityClazz);
                        // 将旧的意图放入到新的意图中
                        newIntent.putExtra(ORIGIN_INTENT, oldIntent);
                        // 设置startActivity的意图对象为新的意图
                        args[index] = newIntent;
                    }
                    return method.invoke(amsObj, args);
                }
            });
            // 将AMS代理对象设置为原本的AMS对象，
            // 也就是设置ActivityManagerNative.java中属性字段gDefault的值为代理对象
            mInstance.set(iActivityManagerSingletonValue, amsProxy);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hookActivityThreadToLaunchActivity(){
        try {
            // 得到ActivityThread的对象
            Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = activityThreadClazz.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object activityThreadValue = sCurrentActivityThreadField.get(null);

            // 找到Handler，即mH
            Field mHField = activityThreadClazz.getDeclaredField("mH");
            mHField.setAccessible(true);
            Object mHValue = mHField.get(activityThreadValue);

            // 重新赋值
            Class<?> handlerClazz = Class.forName("android.os.Handler");
            Field mCallBackField = handlerClazz.getDeclaredField("mCallback");
            mCallBackField.setAccessible(true);
            mCallBackField.set(mHValue, new HandlerCallBack());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 因为在ActivityThread中通过Handler来接受消息，
    // 所以这里为了替换，就实现其回调接口

    private static class HandlerCallBack implements Handler.Callback{

        @Override
        public boolean handleMessage(Message message) {
            // 处理消息
            switch (message.what) {
                case LAUNCH_ACTIVITY:  // H.LAUNCH_ACTIVITY  （SDK版本小于28）
                    handleLaunchActivity(message);
                    break;
                case EXECUTE_TRANSACTION: // SDK版本大于等于28
                    handleLaunchActivity28(message);
                    break;
            }

            return false;
        }

        private void handleLaunchActivity28(Message message) {
            try {
                Object clientTransactionObj = message.obj; // ClientTransaction
                Field mActivityCallbacksField = clientTransactionObj.getClass().getDeclaredField("mActivityCallbacks");
                mActivityCallbacksField.setAccessible(true);
                List mActivityCallbacksValue = (List) mActivityCallbacksField.get(clientTransactionObj);

                // 遍历mActivityCallbacks
                for (Object o : mActivityCallbacksValue) {
                    if(o.getClass().getName().equals("android.app.servertransaction.LaunchActivityItem")){
                        Field mIntentField = o.getClass().getDeclaredField("mIntent");
                        mIntentField.setAccessible(true);
                        Intent newIntent = (Intent) mIntentField.get(o);
                        // 从这个newIntent得到真正的意图
                        Intent oldIntent = newIntent.getParcelableExtra(ORIGIN_INTENT);
                        if(oldIntent != null){
                            // 设置r中的intent为当前的这个oldIntent
                            mIntentField.set(o, oldIntent);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleLaunchActivity(Message message) {
            try {
                // 得到ActivityClientRecord r对象
                Object r = message.obj;
                // 而在得到ActivityClientRecord中就存储着传进来的Intent意图对象
                // 所以可以先获取到意图，然后修改意图对象
                Field intentField = r.getClass().getDeclaredField("intent");
                // 取出intent的值
                intentField.setAccessible(true);
                Intent newIntent = (Intent) intentField.get(r);
                // 从这个newIntent得到真正的意图
                Intent oldIntent = newIntent.getParcelableExtra(ORIGIN_INTENT);
                Log.e("TAG", "handleLaunchActivity: " + oldIntent.toString());
                if(oldIntent != null){
                    // 设置r中的intent为当前的这个oldIntent
                    intentField.set(r, oldIntent);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
