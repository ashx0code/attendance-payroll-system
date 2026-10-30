package com.dongwhan.attendance.timeclock;

import com.dongwhan.attendance.employee.Employee;
import com.dongwhan.attendance.employee.EmployeeNotFoundException;
import com.dongwhan.attendance.employee.EmployeeRepository;
import com.dongwhan.attendance.payroll.domain.WorkRecord;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;

    @Transactional
    public AttendanceResponse clockIn(Long employeeId) {
        Employee employee = findEmployee(employeeId);
        if (!employee.isActive()) {
            throw new AttendanceStateException("퇴사 처리된 직원은 출근할 수 없습니다.");
        }
        if (attendanceRepository.findOpen(employeeId).isPresent()) {
            throw notClockedOut();
        }
        LocalDateTime now = now();
        boolean holiday = isHoliday(employee, now.toLocalDate());
        try {
            Attendance attendance = attendanceRepository.saveAndFlush(new Attendance(employee, now, holiday));
            return AttendanceResponse.from(attendance, now);
        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 다른 출근 요청이 먼저 저장된 경우
            throw notClockedOut();
        }
    }

    @Transactional
    public AttendanceResponse clockOut(Long employeeId) {
        return changeOpen(employeeId, Attendance::clockOut);
    }

    @Transactional
    public AttendanceResponse startBreak(Long employeeId) {
        return changeOpen(employeeId, Attendance::startBreak);
    }

    @Transactional
    public AttendanceResponse endBreak(Long employeeId) {
        return changeOpen(employeeId, Attendance::endBreak);
    }

    public TodayResponse today(Long employeeId) {
        Employee employee = findEmployee(employeeId);
        LocalDateTime now = now();
        LocalDate today = now.toLocalDate();
        AttendanceResponse current = attendanceRepository.findOpen(employeeId)
                .map(a -> AttendanceResponse.from(a, now))
                .orElse(null);
        List<AttendanceResponse> attendances = attendanceRepository
                .findByEmployeeIdAndWorkDateOrderByClockInAtAsc(employeeId, today)
                .stream()
                .map(a -> AttendanceResponse.from(a, now))
                .toList();
        return TodayResponse.of(today, isHoliday(employee, today), current, attendances);
    }

    /**
     * 기간 안의 퇴근이 확정된 근무 기록을 급여 계산 입력으로 돌려준다.
     */
    public List<WorkRecord> workRecords(Long employeeId, LocalDate from, LocalDate to) {
        return attendanceRepository
                .findByEmployeeIdAndStatusAndWorkDateBetweenOrderByWorkDateAsc(employeeId, AttendanceStatus.DONE, from, to)
                .stream()
                .map(Attendance::toWorkRecord)
                .toList();
    }

    private AttendanceResponse changeOpen(Long employeeId, BiConsumer<Attendance, LocalDateTime> change) {
        findEmployee(employeeId);
        Attendance attendance = attendanceRepository.findOpen(employeeId)
                .orElseThrow(() -> new AttendanceStateException("출근 기록이 없습니다."));
        LocalDateTime now = now();
        change.accept(attendance, now);
        return AttendanceResponse.from(attendance, now);
    }

    /**
     * 휴일 여부. 현재는 주휴일(일요일)만 본다. 일요일이 소정근로일인 직원은 일요일 근무를 휴일근로로 보지 않는다.
     */
    private static boolean isHoliday(Employee employee, LocalDate date) {
        return employee.getWorkCondition().isWeeklyHoliday(date.getDayOfWeek());
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private static AttendanceStateException notClockedOut() {
        return new AttendanceStateException("아직 퇴근하지 않은 근무가 있습니다.");
    }
}
