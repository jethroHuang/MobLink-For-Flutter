package com.example.moblinkexample;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.mob.moblink.MobLink;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugins.GeneratedPluginRegistrant;


public class MainActivity extends FlutterActivity {

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);
    }

    // 必须重写该方法，防止MobLink在某些情景下无法还原
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        MobLink.updateNewIntent(getIntent(), this);
    }
}
