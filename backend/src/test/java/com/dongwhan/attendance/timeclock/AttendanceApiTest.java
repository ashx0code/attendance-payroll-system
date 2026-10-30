package com.dongwhan.attendance.timeclock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongwhan.attendance.config.AppConfig;
import com.dongwhan.attendance.employee.EmployeeRepository;
import com.dongwhan.attendance.payroll.domain.PayItemType;
import com.dongwhan.attendance.payroll.domain.PayStatement;
import com.dongwhan.attendance.payroll.domain.PayrollCalculator;
import com.dongwhan.attendance.payroll.domain.WorkRecord;
import com.dongwhan.attendance.support.MutableClock;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AttendanceApiTest {

    /** 월·수·금 근무하는 아르바이트. 2026-11-02는 월요일, 2026-11-08은 일요일이다. */
    static final String PART_TIMER = """
            {
              "name": "김알바",
              "employmentType": "PART_TIME",
              "hireDate": "2026-03-02",
              "wageType": "HOURLY",
              "hourlyWage": 10320,
              "weeklyScheduledMinutes": 1200,
              "scheduledDays": ["MONDAY", "WEDNESDAY", "FRIDAY"],
              "insuranceApplied": false,
              "taxType": "NONE"
            }
            """;

    static final MutableClock CLOCK = new MutableClock(LocalDateTime.of(2026, 11, 2, 9, 0), AppConfig.ZONE);

    @TestBean
    Clock clock;

    static Clock clock() {
        return CLOCK;
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AttendanceService attendanceService;

    @Autowired
    EmployeeRepository employeeRepository;

    String employeeUrl;
    Long employeeId;

    @BeforeEach
    void setUp() throws Exception {
        CLOCK.set(LocalDateTime.of(2026, 11, 2, 9, 0));
        employeeUrl = mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(PART_TIMER))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        employeeId = Long.valueOf(employeeUrl.substring(employeeUrl.lastIndexOf('/') + 1));
    }

    @Test
    @DisplayName("출근, 휴게, 퇴근을 기록하면 휴게를 뺀 근로시간이 계산된다")
    void fullDay() throws Exception {
        action("clock-in")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WORKING"))
                .andExpect(jsonPath("$.workDate").value("2026-11-02"))
                .andExpect(jsonPath("$.holiday").value(false));

        at(12, 0);
        action("break-start").andExpect(jsonPath("$.status").value("ON_BREAK"));
        at(13, 0);
        action("break-end").andExpect(jsonPath("$.status").value("WORKING"));
        at(18, 0);
        action("clock-out")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.clockOutAt").value("2026-11-02T18:00:00"))
                .andExpect(jsonPath("$.breaks", hasSize(1)))
                .andExpect(jsonPath("$.workedMinutes").value(8 * 60));
    }

    @Test
    @DisplayName("출근하지 않았으면 퇴근·휴게를 기록할 수 없다")
    void notClockedIn() throws Exception {
        action("clock-out")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("출근 기록이 없습니다")));
        action("break-start").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("퇴근하기 전에는 다시 출근할 수 없다")
    void clockInWhileWorking() throws Exception {
        action("clock-in").andExpect(status().isOk());
        at(9, 1);
        action("clock-in")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("퇴근하지 않은 근무")));
    }

    @Test
    @DisplayName("퇴근한 뒤 같은 날 다시 출근할 수 있다 (분할 근무)")
    void splitShift() throws Exception {
        action("clock-in");
        at(12, 0);
        action("clock-out");
        at(17, 0);
        action("clock-in")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workDate").value("2026-11-02"));

        at(19, 30);
        mockMvc.perform(get(employeeUrl + "/attendance/today"))
                .andExpect(jsonPath("$.current.status").value("WORKING"))
                .andExpect(jsonPath("$.attendances", hasSize(2)))
                .andExpect(jsonPath("$.attendances[0].status").value("DONE"))
                .andExpect(jsonPath("$.workedMinutes").value(3 * 60 + 2 * 60 + 30));
    }

    @Test
    @DisplayName("휴게 중에 퇴근하면 퇴근 시각에 휴게가 끝난 것으로 본다")
    void clockOutDuringBreak() throws Exception {
        action("clock-in");
        at(17, 0);
        action("break-start");
        at(18, 0);
        action("clock-out")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breaks[0].endAt").value("2026-11-02T18:00:00"))
                .andExpect(jsonPath("$.workedMinutes").value(8 * 60));
    }

    @Test
    @DisplayName("휴게 상태가 맞지 않으면 거부한다")
    void invalidBreakTransition() throws Exception {
        action("clock-in");
        action("break-end")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("휴게 중이 아닙니다")));
        at(12, 0);
        action("break-start");
        action("break-start")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("이미 휴게 중")));
    }

    @Test
    @DisplayName("자정을 넘겨 퇴근하면 출근일의 근무로 기록한다")
    void overnight() throws Exception {
        CLOCK.set(LocalDateTime.of(2026, 11, 2, 22, 0));
        action("clock-in");

        CLOCK.set(LocalDateTime.of(2026, 11, 3, 2, 0));
        mockMvc.perform(get(employeeUrl + "/attendance/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-03"))
                .andExpect(jsonPath("$.current.workDate").value("2026-11-02"))
                .andExpect(jsonPath("$.current.workedMinutes").value(4 * 60))
                .andExpect(jsonPath("$.attendances", hasSize(0)));

        CLOCK.set(LocalDateTime.of(2026, 11, 3, 6, 0));
        action("clock-out")
                .andExpect(jsonPath("$.workDate").value("2026-11-02"))
                .andExpect(jsonPath("$.workedMinutes").value(8 * 60));
    }

    @Test
    @DisplayName("출근 전에는 오늘 근무가 비어 있다")
    void todayBeforeClockIn() throws Exception {
        mockMvc.perform(get(employeeUrl + "/attendance/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-02"))
                .andExpect(jsonPath("$.holiday").value(false))
                .andExpect(jsonPath("$.current").isEmpty())
                .andExpect(jsonPath("$.attendances", hasSize(0)))
                .andExpect(jsonPath("$.workedMinutes").value(0));
    }

    @Test
    @DisplayName("소정근로일이 아닌 일요일 근무는 휴일 근무로 표시한다")
    void sundayIsHoliday() throws Exception {
        CLOCK.set(LocalDateTime.of(2026, 11, 8, 9, 0));
        mockMvc.perform(get(employeeUrl + "/attendance/today")).andExpect(jsonPath("$.holiday").value(true));
        action("clock-in").andExpect(jsonPath("$.holiday").value(true));
    }

    @Test
    @DisplayName("퇴사 처리한 직원은 출근할 수 없다")
    void inactiveEmployee() throws Exception {
        mockMvc.perform(delete(employeeUrl));
        action("clock-in")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("퇴사")));
    }

    @Test
    @DisplayName("없는 직원은 404")
    void employeeNotFound() throws Exception {
        mockMvc.perform(post("/api/employees/999999/attendance/clock-in"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("저장된 기록을 급여 계산에 넘길 수 있다")
    void toPayroll() throws Exception {
        // 월요일 22:00 ~ 화요일 06:00 (야간 8시간), 수요일 09:00 ~ 18:00 (휴게 1시간)
        CLOCK.set(LocalDateTime.of(2026, 11, 2, 22, 0));
        action("clock-in");
        CLOCK.set(LocalDateTime.of(2026, 11, 3, 6, 0));
        action("clock-out");
        CLOCK.set(LocalDateTime.of(2026, 11, 4, 9, 0));
        action("clock-in");
        at(12, 0);
        action("break-start");
        at(13, 0);
        action("break-end");
        at(18, 0);
        action("clock-out");
        // 퇴근하지 않은 기록은 제외된다
        CLOCK.set(LocalDateTime.of(2026, 11, 6, 9, 0));
        action("clock-in");

        List<WorkRecord> records = attendanceService.workRecords(
                employeeId, LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 30));
        assertThat(records).extracting(WorkRecord::workDate)
                .containsExactly(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4));

        var policy = employeeRepository.findById(employeeId).orElseThrow().getWorkCondition().toPayPolicy(false);
        PayStatement statement = new PayrollCalculator().calculate(policy, records, YearMonth.of(2026, 11));

        assertThat(statement.workSummary().workedMinutes()).isEqualTo(16 * 60);
        assertThat(statement.workSummary().nightMinutes()).isEqualTo(8 * 60);
        assertThat(statement.find(PayItemType.NIGHT_PAY)).isPresent();
    }

    @Test
    @DisplayName("분할 근무는 하루 근로시간으로 합산해 8시간 초과분을 연장근로로 계산한다")
    void splitShiftPayroll() throws Exception {
        // 월요일 09:00 ~ 13:00, 18:00 ~ 23:00 → 하루 9시간 (연장 1시간, 야간 1시간)
        action("clock-in");
        at(13, 0);
        action("clock-out");
        at(18, 0);
        action("clock-in");
        at(23, 0);
        action("clock-out");

        List<WorkRecord> records = attendanceService.workRecords(
                employeeId, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2));
        assertThat(records).hasSize(2);

        var policy = employeeRepository.findById(employeeId).orElseThrow().getWorkCondition().toPayPolicy(false);
        PayStatement statement = new PayrollCalculator().calculate(policy, records, YearMonth.of(2026, 11));

        assertThat(statement.workSummary().workDays()).isEqualTo(1);
        assertThat(statement.workSummary().workedMinutes()).isEqualTo(9 * 60);
        assertThat(statement.workSummary().overtimeMinutes()).isEqualTo(60);
        assertThat(statement.workSummary().nightMinutes()).isEqualTo(60);
    }

    private void at(int hour, int minute) {
        CLOCK.set(LocalDateTime.ofInstant(CLOCK.instant(), AppConfig.ZONE).toLocalDate().atTime(hour, minute));
    }

    private ResultActions action(String name) throws Exception {
        return mockMvc.perform(post(employeeUrl + "/attendance/" + name));
    }
}
