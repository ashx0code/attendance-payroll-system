package com.dongwhan.attendance.timeclock;

import java.time.LocalDate;

/**
 * 오늘의 근무 현황.
 *
 * @param holiday    오늘이 이 직원의 휴일인지
 * @param attendance 진행 중인 근무(전날 출근해 아직 퇴근하지 않은 근무 포함) 또는 오늘 근무. 출근 전이면 null.
 */
public record TodayResponse(LocalDate date, boolean holiday, AttendanceResponse attendance) {
}
