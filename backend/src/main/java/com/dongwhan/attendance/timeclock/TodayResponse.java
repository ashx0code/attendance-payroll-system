package com.dongwhan.attendance.timeclock;

import java.time.LocalDate;
import java.util.List;

/**
 * 오늘의 근무 현황.
 *
 * @param holiday       오늘이 이 직원의 휴일인지
 * @param current       진행 중인 근무(전날 출근해 아직 퇴근하지 않은 근무 포함). 없으면 null.
 * @param attendances   오늘이 근무일인 기록. 분할 근무면 여러 건이다.
 * @param workedMinutes 오늘이 근무일인 기록의 근로시간 합계(분)
 */
public record TodayResponse(
        LocalDate date,
        boolean holiday,
        AttendanceResponse current,
        List<AttendanceResponse> attendances,
        long workedMinutes) {

    static TodayResponse of(LocalDate date, boolean holiday, AttendanceResponse current,
                            List<AttendanceResponse> attendances) {
        long worked = attendances.stream().mapToLong(AttendanceResponse::workedMinutes).sum();
        return new TodayResponse(date, holiday, current, attendances, worked);
    }
}
