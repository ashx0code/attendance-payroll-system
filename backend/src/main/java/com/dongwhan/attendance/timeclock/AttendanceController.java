package com.dongwhan.attendance.timeclock;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 출퇴근·휴게 기록. 기록 시각은 요청 본문이 아니라 서버 시각을 쓴다.
 */
@RestController
@RequestMapping("/api/employees/{employeeId}/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public AttendanceResponse clockIn(@PathVariable Long employeeId) {
        return attendanceService.clockIn(employeeId);
    }

    @PostMapping("/clock-out")
    public AttendanceResponse clockOut(@PathVariable Long employeeId) {
        return attendanceService.clockOut(employeeId);
    }

    @PostMapping("/break-start")
    public AttendanceResponse startBreak(@PathVariable Long employeeId) {
        return attendanceService.startBreak(employeeId);
    }

    @PostMapping("/break-end")
    public AttendanceResponse endBreak(@PathVariable Long employeeId) {
        return attendanceService.endBreak(employeeId);
    }

    @GetMapping("/today")
    public TodayResponse today(@PathVariable Long employeeId) {
        return attendanceService.today(employeeId);
    }
}
