package com.dongwhan.attendance.timeclock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findFirstByEmployeeIdAndStatusNotOrderByClockInAtDesc(Long employeeId, AttendanceStatus status);

    Optional<Attendance> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    boolean existsByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    List<Attendance> findByEmployeeIdAndStatusAndWorkDateBetweenOrderByWorkDateAsc(
            Long employeeId, AttendanceStatus status, LocalDate from, LocalDate to);

    /** 아직 퇴근하지 않은 기록 */
    default Optional<Attendance> findOpen(Long employeeId) {
        return findFirstByEmployeeIdAndStatusNotOrderByClockInAtDesc(employeeId, AttendanceStatus.DONE);
    }
}
