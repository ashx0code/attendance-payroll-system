package com.dongwhan.attendance.payroll.domain;

import java.time.LocalDate;

/**
 * 같은 날짜의 근무 기록(분할 근무 포함)을 합산한 결과.
 */
record DailyWork(LocalDate date, long workedMinutes, long nightMinutes, boolean holiday) {

    static final long DAILY_LIMIT_MINUTES = 8 * 60;

    static DailyWork from(WorkRecord r) {
        return new DailyWork(r.workDate(), r.workedMinutes(), r.nightMinutes(), r.holiday());
    }

    DailyWork merge(DailyWork other) {
        return new DailyWork(date, workedMinutes + other.workedMinutes,
                nightMinutes + other.nightMinutes, holiday || other.holiday);
    }

    /** 1일 8시간 이내 평일 근로 */
    long regularMinutes() {
        return holiday ? 0 : Math.min(workedMinutes, DAILY_LIMIT_MINUTES);
    }

    /** 1일 8시간 초과 평일 근로 */
    long dailyOvertimeMinutes() {
        return holiday ? 0 : Math.max(0, workedMinutes - DAILY_LIMIT_MINUTES);
    }

    /** 휴일근로 중 8시간 이내 */
    long holidayWithinLimitMinutes() {
        return holiday ? Math.min(workedMinutes, DAILY_LIMIT_MINUTES) : 0;
    }

    /** 휴일근로 중 8시간 초과 */
    long holidayOverLimitMinutes() {
        return holiday ? Math.max(0, workedMinutes - DAILY_LIMIT_MINUTES) : 0;
    }
}
