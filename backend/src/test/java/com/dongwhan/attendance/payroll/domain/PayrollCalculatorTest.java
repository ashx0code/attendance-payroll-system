package com.dongwhan.attendance.payroll.domain;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 2026년 11월 기준. 11/2(월) ~ 11/8(일)이 한 주.
 */
class PayrollCalculatorTest {

    static final long WAGE = 10_320;
    static final YearMonth NOV = YearMonth.of(2026, 11);
    static final LocalDate MON = LocalDate.of(2026, 11, 2);
    static final Set<DayOfWeek> WEEKDAYS = Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);

    final PayrollCalculator calculator = new PayrollCalculator();

    @Nested
    @DisplayName("주휴수당")
    class WeeklyHolidayPay {

        @Test
        @DisplayName("주 40시간 개근 시 8시간분 지급")
        void fullTime() {
            PayStatement s = calculator.calculate(PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS), nineToSix(MON, 5), NOV);

            assertThat(s.amountOf(PayItemType.BASE_PAY)).isEqualTo(40 * WAGE);
            assertThat(s.amountOf(PayItemType.WEEKLY_HOLIDAY_PAY)).isEqualTo(8 * WAGE);
            assertThat(s.amountOf(PayItemType.OVERTIME_PAY)).isZero();
        }

        @Test
        @DisplayName("주 20시간이면 소정근로시간 비례로 4시간분 지급")
        void partTime() {
            List<WorkRecord> records = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                records.add(work(MON.plusDays(i), "10:00", "14:00"));
            }

            PayStatement s = calculator.calculate(PayPolicy.hourly(WAGE, 20 * 60, WEEKDAYS), records, NOV);

            assertThat(s.amountOf(PayItemType.WEEKLY_HOLIDAY_PAY)).isEqualTo(4 * WAGE);
        }

        @Test
        @DisplayName("주 15시간 미만이면 미지급")
        void underFifteenHours() {
            List<WorkRecord> records = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                records.add(work(MON.plusDays(i), "10:00", "13:30"));
            }
            Set<DayOfWeek> days = Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY);

            PayStatement s = calculator.calculate(PayPolicy.hourly(WAGE, 14 * 60, days), records, NOV);

            assertThat(s.find(PayItemType.WEEKLY_HOLIDAY_PAY)).isEmpty();
        }

        @Test
        @DisplayName("소정근로일에 결근하면 미지급")
        void absent() {
            PayStatement s = calculator.calculate(PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS), nineToSix(MON, 4), NOV);

            assertThat(s.find(PayItemType.WEEKLY_HOLIDAY_PAY)).isEmpty();
        }

        @Test
        @DisplayName("월을 걸친 주는 일요일이 속한 달에 정산")
        void weekSpanningMonths() {
            List<WorkRecord> octoberWeek = nineToSix(LocalDate.of(2026, 10, 26), 5);
            PayPolicy policy = PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS);

            PayStatement october = calculator.calculate(policy, octoberWeek, YearMonth.of(2026, 10));
            PayStatement november = calculator.calculate(policy, octoberWeek, NOV);

            assertThat(october.amountOf(PayItemType.BASE_PAY)).isEqualTo(40 * WAGE);
            assertThat(october.find(PayItemType.WEEKLY_HOLIDAY_PAY)).isEmpty();
            assertThat(november.amountOf(PayItemType.BASE_PAY)).isZero();
            assertThat(november.amountOf(PayItemType.WEEKLY_HOLIDAY_PAY)).isEqualTo(8 * WAGE);
        }
    }

    @Nested
    @DisplayName("연장근로수당")
    class OvertimePay {

        @Test
        @DisplayName("1일 8시간 초과분은 1.5배")
        void daily() {
            List<WorkRecord> records = List.of(work(MON, "09:00", "20:00", "12:00", "13:00"));

            PayStatement s = calculator.calculate(PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS), records, NOV);

            assertThat(s.amountOf(PayItemType.BASE_PAY)).isEqualTo(8 * WAGE);
            assertThat(s.amountOf(PayItemType.OVERTIME_PAY)).isEqualTo(2 * WAGE * 3 / 2);
            assertThat(s.workSummary().overtimeMinutes()).isEqualTo(120);
        }

        @Test
        @DisplayName("하루 8시간 이내라도 주 40시간 초과분은 1.5배")
        void weekly() {
            PayStatement s = calculator.calculate(PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS), nineToSix(MON, 6), NOV);

            assertThat(s.amountOf(PayItemType.BASE_PAY)).isEqualTo(40 * WAGE);
            assertThat(s.amountOf(PayItemType.OVERTIME_PAY)).isEqualTo(8 * WAGE * 3 / 2);
        }

        @Test
        @DisplayName("5인 미만 사업장은 가산 없이 1배")
        void smallBusiness() {
            List<WorkRecord> records = List.of(work(MON, "09:00", "20:00", "12:00", "13:00"));
            PayPolicy policy = PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS).withSmallBusiness(true);

            PayStatement s = calculator.calculate(policy, records, NOV);

            assertThat(s.amountOf(PayItemType.OVERTIME_PAY)).isEqualTo(2 * WAGE);
        }
    }

    @Nested
    @DisplayName("야간근로수당")
    class NightPay {

        @Test
        @DisplayName("22시~06시 근무분에 0.5배 가산, 자정을 넘긴 퇴근도 출근일 근무로 계산")
        void crossMidnight() {
            List<WorkRecord> records = List.of(new WorkRecord(MON,
                    range(MON, "20:00", MON.plusDays(1), "02:00"), List.of(), false));

            PayStatement s = calculator.calculate(PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS), records, NOV);

            assertThat(s.amountOf(PayItemType.BASE_PAY)).isEqualTo(6 * WAGE);
            assertThat(s.amountOf(PayItemType.NIGHT_PAY)).isEqualTo(4 * WAGE / 2);
        }

        @Test
        @DisplayName("야간 시간대의 휴게시간은 야간근로에서 제외")
        void breakDuringNight() {
            List<WorkRecord> records = List.of(new WorkRecord(MON,
                    range(MON, "22:00", MON.plusDays(1), "07:00"),
                    List.of(range(MON.plusDays(1), "02:00", MON.plusDays(1), "03:00")), false));

            PayStatement s = calculator.calculate(PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS), records, NOV);

            assertThat(s.workSummary().nightMinutes()).isEqualTo(7 * 60);
            assertThat(s.amountOf(PayItemType.NIGHT_PAY)).isEqualTo(7 * WAGE / 2);
        }

        @Test
        @DisplayName("5인 미만 사업장은 야간 가산 없음")
        void smallBusiness() {
            List<WorkRecord> records = List.of(new WorkRecord(MON,
                    range(MON, "20:00", MON.plusDays(1), "02:00"), List.of(), false));
            PayPolicy policy = PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS).withSmallBusiness(true);

            PayStatement s = calculator.calculate(policy, records, NOV);

            assertThat(s.find(PayItemType.NIGHT_PAY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("휴일근로수당")
    class HolidayPay {

        @Test
        @DisplayName("8시간 이내 1.5배, 8시간 초과 2배")
        void overEightHours() {
            LocalDate sunday = MON.plusDays(6);
            List<WorkRecord> records = List.of(new WorkRecord(sunday,
                    range(sunday, "09:00", sunday, "20:00"),
                    List.of(range(sunday, "12:00", sunday, "13:00")), true));

            PayStatement s = calculator.calculate(PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS), records, NOV);

            assertThat(s.amountOf(PayItemType.HOLIDAY_PAY)).isEqualTo(8 * WAGE * 3 / 2 + 2 * WAGE * 2);
            assertThat(s.amountOf(PayItemType.BASE_PAY)).isZero();
        }
    }

    @Nested
    @DisplayName("월급제")
    class Monthly {

        @Test
        @DisplayName("통상시급(월급 ÷ 209시간) 기준으로 연장수당을 계산하고 4대 보험을 공제")
        void overtimeAndInsurance() {
            List<WorkRecord> records = List.of(work(MON, "09:00", "20:00", "12:00", "13:00"));

            PayStatement s = calculator.calculate(PayPolicy.monthly(2_500_000, WEEKDAYS), records, NOV);

            assertThat(s.amountOf(PayItemType.BASE_PAY)).isEqualTo(2_500_000);
            assertThat(s.amountOf(PayItemType.OVERTIME_PAY)).isEqualTo(35_885);
            assertThat(s.grossPay()).isEqualTo(2_535_885);
            assertThat(s.amountOf(PayItemType.NATIONAL_PENSION)).isEqualTo(120_450);
            assertThat(s.amountOf(PayItemType.HEALTH_INSURANCE)).isEqualTo(91_160);
            assertThat(s.amountOf(PayItemType.LONG_TERM_CARE)).isEqualTo(11_970);
            assertThat(s.amountOf(PayItemType.EMPLOYMENT_INSURANCE)).isEqualTo(22_820);
            assertThat(s.netPay()).isEqualTo(2_535_885 - 246_400);
        }
    }

    @Nested
    @DisplayName("원천징수 3.3%")
    class BusinessIncomeTax {

        @Test
        @DisplayName("소득세 3%, 지방소득세는 소득세의 10%, 각각 10원 미만 절사")
        void withholding() {
            PayPolicy policy = PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS).withTaxType(TaxType.BUSINESS_INCOME_3_3);

            PayStatement s = calculator.calculate(policy, nineToSix(MON, 5), NOV);

            assertThat(s.grossPay()).isEqualTo(495_360);
            assertThat(s.amountOf(PayItemType.INCOME_TAX)).isEqualTo(14_860);
            assertThat(s.amountOf(PayItemType.LOCAL_INCOME_TAX)).isEqualTo(1_480);
            assertThat(s.netPay()).isEqualTo(479_020);
        }

        @Test
        @DisplayName("소득세 1,000원 미만은 소액부징수")
        void smallAmount() {
            PayPolicy policy = PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS).withTaxType(TaxType.BUSINESS_INCOME_3_3);

            PayStatement s = calculator.calculate(policy, List.of(work(MON, "09:00", "12:00")), NOV);

            assertThat(s.deductions()).isEmpty();
        }
    }

    @Test
    @DisplayName("명세서 항목마다 산출 근거를 남긴다")
    void basis() {
        List<WorkRecord> records = List.of(work(MON, "09:00", "20:30", "12:00", "13:00"));

        PayStatement s = calculator.calculate(PayPolicy.hourly(WAGE, 40 * 60, WEEKDAYS), records, NOV);

        assertThat(s.find(PayItemType.OVERTIME_PAY)).get()
                .extracting(PayItem::basis)
                .isEqualTo("연장 2시간 30분 × 10,320원 × 1.5");
    }

    private static List<WorkRecord> nineToSix(LocalDate from, int days) {
        List<WorkRecord> records = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            records.add(work(from.plusDays(i), "09:00", "18:00", "12:00", "13:00"));
        }
        return records;
    }

    private static WorkRecord work(LocalDate date, String in, String out, String... breakRange) {
        List<TimeRange> breaks = breakRange.length == 2
                ? List.of(range(date, breakRange[0], date, breakRange[1]))
                : List.of();
        return new WorkRecord(date, range(date, in, date, out), breaks, false);
    }

    private static TimeRange range(LocalDate startDate, String start, LocalDate endDate, String end) {
        return new TimeRange(LocalDateTime.of(startDate, LocalTime.parse(start)), LocalDateTime.of(endDate, LocalTime.parse(end)));
    }
}
