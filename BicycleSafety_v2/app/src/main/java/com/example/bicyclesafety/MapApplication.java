package com.example.bicyclesafety;

import android.app.Application;
import android.util.Log;
import com.kakao.vectormap.KakaoMapSdk;

/** 카카오 네이티브 앱 키는 각자의 local.properties로 설정합니다. */
public class MapApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isEmpty()) {
            Log.e("BicycleSafety", "KAKAO_NATIVE_APP_KEY 누락: local.properties를 확인하세요.");
            return;
        }
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY);
    }
}
