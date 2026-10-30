package com.dongwhan.attendance.timeclock;

import com.dongwhan.attendance.employee.Employee;
import com.dongwhan.attendance.payroll.domain.TimeRange;
import com.dongwhan.attendance.payroll.domain.WorkRecord;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하루치 출퇴근 기록.
 *
 * <p>직원·근무일마다 하나만 저장되도록 유니크 제약을 두어, 출근 버튼을 여러 번 눌러도 기록이 하나만 남게 한다.
 * 휴게·퇴근처럼 같은 기록을 고치는 요청이 동시에 들어오면 {@link Version}으로 나중 요청을 거부한다.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_attendance_employee_work_date", columnNames = {"employee_id", "work_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** 근무일(출근일 기준). 자정을 넘겨 퇴근해도 출근일의 근무로 본다. */
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Column(nullable = false)
    private LocalDateTime clockInAt;

    private LocalDateTime clockOutAt;

    @ElementCollection
    @CollectionTable(name = "attendance_break", joinColumns = @JoinColumn(name = "attendance_id"))
    @OrderColumn(name = "seq")
    private List<BreakTime> breaks = new ArrayList<>();

    /** 휴일(주휴일 등) 근무 여부 */
    private boolean holiday;

    @Version
    private Long version;

    public Attendance(Employee employee, LocalDateTime clockInAt, boolean holiday) {
        this.employee = employee;
        this.workDate = clockInAt.toLocalDate();
        this.status = AttendanceStatus.WORKING;
        this.clockInAt = clockInAt;
        this.holiday = holiday;
    }

    public void startBreak(LocalDateTime at) {
        if (status != AttendanceStatus.WORKING) {
            throw new AttendanceStateException(status == AttendanceStatus.ON_BREAK
                    ? "이미 휴게 중입니다." : "이미 퇴근했습니다.");
        }
        breaks.add(BreakTime.startingAt(at));
        status = AttendanceStatus.ON_BREAK;
    }

    public void endBreak(LocalDateTime at) {
        if (status != AttendanceStatus.ON_BREAK) {
            throw new AttendanceStateException("휴게 중이 아닙니다.");
        }
        closeOpenBreak(at);
        status = AttendanceStatus.WORKING;
    }

    /**
     * 퇴근. 휴게 중이었다면 퇴근 시각에 휴게도 끝난 것으로 본다.
     */
    public void clockOut(LocalDateTime at) {
        if (status == AttendanceStatus.DONE) {
            throw new AttendanceStateException("이미 퇴근했습니다.");
        }
        if (!at.isAfter(clockInAt)) {
            throw new AttendanceStateException("출근 시각 이후에만 퇴근할 수 있습니다.");
        }
        if (status == AttendanceStatus.ON_BREAK) {
            closeOpenBreak(at);
        }
        clockOutAt = at;
        status = AttendanceStatus.DONE;
    }

    /**
     * 급여 계산 입력으로 변환한다. 퇴근이 확정된 기록만 변환할 수 있다.
     */
    public WorkRecord toWorkRecord() {
        if (status != AttendanceStatus.DONE) {
            throw new IllegalStateException("퇴근하지 않은 근무는 급여 계산에 사용할 수 없습니다: " + id);
        }
        return new WorkRecord(workDate, new TimeRange(clockInAt, clockOutAt), breakRanges(clockOutAt), holiday);
    }

    /**
     * 휴게를 뺀 근로시간(분). 아직 퇴근하지 않았으면 {@code now}까지 일한 것으로 본다.
     */
    public long workedMinutes(LocalDateTime now) {
        LocalDateTime end = clockOutAt != null ? clockOutAt : now;
        if (!end.isAfter(clockInAt)) {
            return 0;
        }
        return new WorkRecord(workDate, new TimeRange(clockInAt, end), breakRanges(end), holiday).workedMinutes();
    }

    private List<TimeRange> breakRanges(LocalDateTime openBreakEnd) {
        return breaks.stream().map(b -> b.toRange(openBreakEnd)).flatMap(Optional::stream).toList();
    }

    private void closeOpenBreak(LocalDateTime at) {
        int last = breaks.size() - 1;
        if (last < 0 || !breaks.get(last).isOpen()) {
            throw new IllegalStateException("진행 중인 휴게가 없습니다: " + id);
        }
        breaks.set(last, breaks.get(last).endingAt(at));
    }
}
