package com.example.bicyclesafety;

import android.app.Application;
import com.kakao.vectormap.KakaoMapSdk;

public class MapApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        KakaoMapSdk.init(
                this,
                "18be7798c2d03fdfca0069f1e368b817"
        );
    }
}