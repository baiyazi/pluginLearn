package com.weizu.myapplication;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class LoadUtils {

    public static void loadClass(Context context, String pluginPath) {
        if(context == null) return;
        try {
            // 获取应用程序App的dexElements
            PathClassLoader classLoader = (PathClassLoader) context.getClassLoader();
            Class<?> baseDexClassLoaderClazz = Class.forName("dalvik.system.BaseDexClassLoader");
            Field dexPathListField = baseDexClassLoaderClazz.getDeclaredField("pathList");
            dexPathListField.setAccessible(true);
            Object dexPathListValue = dexPathListField.get(classLoader);
            Field dexElementsField = dexPathListValue.getClass().getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            Object dexElementsValue = dexElementsField.get(dexPathListValue);

            // 获取外部插件的dexElements
            DexClassLoader dexClassLoader = new DexClassLoader(pluginPath,
                    context.getDir("plugin", Context.MODE_PRIVATE).getAbsolutePath(),
                    null, context.getClassLoader());

            Object pluginDexPathListValue = dexPathListField.get(dexClassLoader);
            Object pluginDexElementsValue = dexElementsField.get(pluginDexPathListValue);

            // 合并两个dexElements
            int appDexElementsLength = Array.getLength(dexElementsValue);
            int pluginDexElementsLength = Array.getLength(pluginDexElementsValue);
            int newLength = appDexElementsLength + pluginDexElementsLength;

            Class<?> componentType = dexElementsValue.getClass().getComponentType();
            Object newArray = Array.newInstance(componentType, newLength);
            System.arraycopy(dexElementsValue, 0, newArray, 0, appDexElementsLength);
            System.arraycopy(pluginDexElementsValue, 0, newArray, appDexElementsLength, pluginDexElementsLength);

            // 设置新的内容到app的PathList中的Elements[]
            dexElementsField.set(dexPathListValue, newArray);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static Resources loadPluginResource(Context context, String pluginPath){
        Resources resources = null;
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(assetManager, pluginPath);

            resources = new Resources(assetManager, context.getResources().getDisplayMetrics(),
                    context.getResources().getConfiguration());
        }catch (Exception e){
            e.printStackTrace();
        }

        return resources;
    }

}
