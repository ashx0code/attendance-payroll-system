package com.dongwhan.attendance.payroll.domain;

/**
 * 급여 명세서의 한 줄.
 *
 * @param basis 산출 근거 (예: "연장 2시간 × 10,320원 × 1.5")
 */
public record PayItem(PayItemType type, long amount, String basis) {
}
