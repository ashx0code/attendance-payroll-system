package com.dongwhan.attendance.payroll.domain;

/**
 * 해당 월 근무 집계(분 단위).
 */
public record WorkSummary(
        int workDays,
        long workedMinutes,
        long overtimeMinutes,
        long nightMinutes,
        long holidayMinutes) {
}
