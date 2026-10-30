package com.dongwhan.attendance.timeclock;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongwhan.attendance.common.ConflictException;
import com.dongwhan.attendance.employee.Employee;
import com.dongwhan.attendance.employee.EmployeeRepository;
import com.dongwhan.attendance.employee.EmploymentType;
import com.dongwhan.attendance.employee.WorkCondition;
import com.dongwhan.attendance.payroll.domain.WageType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * 실제 트랜잭션 커밋끼리 경합해야 하므로 테스트 트랜잭션을 쓰지 않고 끝나면 직접 지운다.
 */
@SpringBootTest
class AttendanceConcurrencyTest {

    static final int THREADS = 10;

    @Autowired
    AttendanceService attendanceService;

    @Autowired
    AttendanceRepository attendanceRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    Long employeeId;

    @BeforeEach
    void setUp() {
        WorkCondition condition = WorkCondition.builder()
                .wageType(WageType.HOURLY)
                .hourlyWage(10_320L)
                .weeklyScheduledMinutes(40 * 60)
                .scheduledDays(Set.of(DayOfWeek.values()))
                .validationYear(2026)
                .build();
        employeeId = employeeRepository.save(
                new Employee("동시성", EmploymentType.PART_TIME, LocalDate.of(2026, 1, 2), condition)).getId();
    }

    @AfterEach
    void tearDown() {
        attendanceRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    @DisplayName("출근 요청이 동시에 여러 번 들어와도 기록은 하나만 남는다")
    void concurrentClockIn() throws Exception {
        List<Throwable> failures = runConcurrently(() -> attendanceService.clockIn(employeeId));

        assertThat(failures).hasSize(THREADS - 1).allMatch(e -> e instanceof ConflictException);
        assertThat(attendanceRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("휴게 시작 요청이 동시에 들어와도 휴게는 하나만 기록된다")
    void concurrentBreakStart() throws Exception {
        attendanceService.clockIn(employeeId);

        List<Throwable> failures = runConcurrently(() -> attendanceService.startBreak(employeeId));

        assertThat(failures).hasSize(THREADS - 1)
                .allMatch(e -> e instanceof ConflictException || e instanceof OptimisticLockingFailureException);
        assertThat(attendanceService.today(employeeId).current().breaks()).hasSize(1);
    }

    @Test
    @DisplayName("퇴근 후 재출근 요청이 동시에 여러 번 들어와도 진행 중인 근무는 하나만 생긴다")
    void concurrentClockInAfterClockOut() throws Exception {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow();
        Attendance done = new Attendance(employee, LocalDateTime.now().minusHours(3), false);
        done.clockOut(LocalDateTime.now().minusHours(1));
        attendanceRepository.save(done);

        List<Throwable> failures = runConcurrently(() -> attendanceService.clockIn(employeeId));

        assertThat(failures).hasSize(THREADS - 1).allMatch(e -> e instanceof ConflictException);
        assertThat(attendanceRepository.count()).isEqualTo(2);
    }

    /** 같은 작업을 동시에 실행하고 실패한 예외를 모은다. */
    private static List<Throwable> runConcurrently(Runnable task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    task.run();
                    return null;
                }));
            }
            start.countDown();
        }
        List<Throwable> failures = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (java.util.concurrent.ExecutionException e) {
                failures.add(e.getCause());
            }
        }
        return failures;
    }
}
