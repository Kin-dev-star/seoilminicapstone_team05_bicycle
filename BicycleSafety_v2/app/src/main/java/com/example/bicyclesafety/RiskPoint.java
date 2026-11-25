package com.example.bicyclesafety;

/** 위험지점. 서버 응답과 동봉된 사고 데이터에 공통으로 쓰는 단순 모델입니다. */
public class RiskPoint {
    public String id;
    public String name;
    public String riskLevel;
    public String description;
    public double latitude;
    public double longitude;
    public int accidentCount;
    public int casualtyCount;
    public String source = "";
    public boolean demo = false;
}
