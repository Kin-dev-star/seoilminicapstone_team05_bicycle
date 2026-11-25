package com.example.bicyclesafety.navigation;

/**
 * 안내 방향. 화면에서는 이 값으로 화살표를 고르면 됩니다.
 * (디자인 팀 Turn 으로 바꾸기: LEFT·SLIGHT_LEFT → Left, RIGHT·SLIGHT_RIGHT → Right, UTURN → UTurn, 나머지 → Straight)
 */
public enum Maneuver {
    START("출발"),
    STRAIGHT("직진"),
    LEFT("좌회전"),
    RIGHT("우회전"),
    SLIGHT_LEFT("왼쪽 방향"),
    SLIGHT_RIGHT("오른쪽 방향"),
    UTURN("유턴"),
    ARRIVE("목적지");

    /** 화면에 쓰는 짧은 말 */
    public final String label;

    Maneuver(String label) {
        this.label = label;
    }

    /**
     * 카카오 자전거 길찾기 guidance 문구로 방향 고르기
     * (예: "좌회전", "오른쪽 방향", "2시 방향", "유턴", "목적지").
     */
    public static Maneuver fromGuidance(String guidance) {
        String g = guidance == null ? "" : guidance.replace(" ", "");
        if (g.contains("목적지") || g.contains("도착")) return ARRIVE;
        if (g.contains("출발")) return START;
        if (g.contains("유턴") || g.contains("회전교차로6시")) return UTURN;
        if (g.contains("12시")) return STRAIGHT;
        if (g.contains("10시") || g.contains("11시")) return SLIGHT_LEFT;
        if (g.contains("1시") || g.contains("2시")) return SLIGHT_RIGHT;
        if (g.contains("좌회전") || g.contains("왼쪽") || g.contains("좌측") || g.contains("9시")) return LEFT;
        if (g.contains("우회전") || g.contains("오른쪽") || g.contains("우측") || g.contains("3시")) return RIGHT;
        return STRAIGHT;
    }
}
