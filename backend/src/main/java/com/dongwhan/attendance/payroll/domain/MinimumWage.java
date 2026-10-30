package com.dongwhan.attendance.payroll.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 연도별 최저임금(시급). 매년 고시되는 금액을 추가해야 하며,
 * 등록되지 않은 연도는 그 이전 가장 최근 연도의 금액을 적용한다.
 */
public final class MinimumWage {

    private static final NavigableMap<Integer, Long> HOURLY = new TreeMap<>(Map.of(
            2025, 10_030L,
            2026, 10_320L));

    private MinimumWage() {
    }

    public static long hourly(int year) {
        Map.Entry<Integer, Long> entry = HOURLY.floorEntry(year);
        if (entry == null) {
            throw new IllegalArgumentException(year + "년 최저임금이 등록되어 있지 않습니다.");
        }
        return entry.getValue();
    }

    /**
     * 통상시급이 해당 연도 최저임금 이상인지 검사한다.
     */
    public static void validate(PayPolicy policy, int year) {
        long minimum = hourly(year);
        if (policy.ordinaryHourlyWage().compareTo(BigDecimal.valueOf(minimum)) < 0) {
            throw new IllegalArgumentException(
                    "통상시급 " + PayrollCalculator.won(policy.ordinaryHourlyWage()) + "이 "
                            + year + "년 최저임금 " + PayrollCalculator.won(minimum) + "보다 낮습니다.");
        }
    }
}
