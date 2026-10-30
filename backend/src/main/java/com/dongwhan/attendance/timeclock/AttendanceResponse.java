package com.dongwhan.attendance.timeclock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @param workedMinutes 휴게를 뺀 근로시간(분). 퇴근 전이면 조회 시각까지의 시간.
 */
public record AttendanceResponse(
        Long id,
        Long employeeId,
        LocalDate workDate,
        AttendanceStatus status,
        LocalDateTime clockInAt,
        LocalDateTime clockOutAt,
        List<BreakResponse> breaks,
        long workedMinutes,
        boolean holiday) {

    public record BreakResponse(LocalDateTime startAt, LocalDateTime endAt) {
    }

    static AttendanceResponse from(Attendance a, LocalDateTime now) {
        return new AttendanceResponse(
                a.getId(),
                a.getEmployee().getId(),
                a.getWorkDate(),
                a.getStatus(),
                a.getClockInAt(),
                a.getClockOutAt(),
                a.getBreaks().stream().map(b -> new BreakResponse(b.getStartAt(), b.getEndAt())).toList(),
                a.workedMinutes(now),
                a.isHoliday());
    }
}
