package com.example.bicyclesafety;

import android.app.Application;
import com.kakao.vectormap.KakaoMapSdk;

public class MapApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        KakaoMapSdk.init(
                this,
                BuildConfig.KAKAO_NATIVE_APP_KEY
        );
    }
}