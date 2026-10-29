package com.dongwhan.attendance.payroll.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

/**
 * 근무 기록과 근로 조건으로 월 급여를 산출한다.
 *
 * <p>주 단위 항목(주휴수당, 주 40시간 초과 연장근로)은 그 주의 마지막 날(일요일)이 속한 달에 정산한다.
 * 따라서 월초 주차를 계산하려면 전월 말일의 근무 기록도 함께 넘겨야 한다.
 */
public class PayrollCalculator {

    static final long WEEKLY_LIMIT_MINUTES = 40 * 60;
    static final long WEEKLY_HOLIDAY_MIN_SCHEDULED_MINUTES = 15 * 60;

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ONE_HALF = new BigDecimal("1.5");
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private final DeductionCalculator deductionCalculator = new DeductionCalculator();

    public PayStatement calculate(PayPolicy policy, List<WorkRecord> records, YearMonth month) {
        Map<LocalDate, DailyWork> allDays = records.stream()
                .collect(Collectors.toMap(WorkRecord::workDate, DailyWork::from, DailyWork::merge, TreeMap::new));
        List<DailyWork> monthDays = allDays.values().stream()
                .filter(d -> YearMonth.from(d.date()).equals(month))
                .toList();
        Map<LocalDate, List<DailyWork>> weeksEndingInMonth = allDays.values().stream()
                .collect(Collectors.groupingBy(d -> weekStart(d.date()), TreeMap::new, Collectors.toList()));
        weeksEndingInMonth.keySet().removeIf(start -> !YearMonth.from(start.plusDays(6)).equals(month));

        long regular = sum(monthDays, DailyWork::regularMinutes);
        long dailyOvertime = sum(monthDays, DailyWork::dailyOvertimeMinutes);
        long night = sum(monthDays, DailyWork::nightMinutes);
        long holidayWithin = sum(monthDays, DailyWork::holidayWithinLimitMinutes);
        long holidayOver = sum(monthDays, DailyWork::holidayOverLimitMinutes);
        long weeklyOvertime = weeksEndingInMonth.values().stream()
                .mapToLong(week -> Math.max(0, sum(week, DailyWork::regularMinutes) - WEEKLY_LIMIT_MINUTES))
                .sum();
        long overtime = dailyOvertime + weeklyOvertime;

        BigDecimal wage = policy.ordinaryHourlyWage();
        boolean premium = !policy.smallBusiness();
        List<PayItem> earnings = new ArrayList<>();

        if (policy.wageType() == WageType.MONTHLY) {
            earnings.add(new PayItem(PayItemType.BASE_PAY, policy.monthlySalary(),
                    "월 기본급 (통상시급 " + won(wage) + " = " + won(policy.monthlySalary()) + " ÷ "
                            + policy.monthlyScheduledHours() + "시간)"));
        } else {
            long baseMinutes = regular - weeklyOvertime;
            earnings.add(timePay(PayItemType.BASE_PAY, "근로", baseMinutes, wage, ONE));
            long weeklyHolidayMinutes = weeksEndingInMonth.values().stream()
                    .mapToLong(week -> weeklyHolidayMinutes(policy, week))
                    .sum();
            addIfPositive(earnings, timePay(PayItemType.WEEKLY_HOLIDAY_PAY, "주휴", weeklyHolidayMinutes, wage, ONE));
        }

        addIfPositive(earnings, timePay(PayItemType.OVERTIME_PAY, "연장", overtime, wage, premium ? ONE_HALF : ONE));
        if (premium) {
            addIfPositive(earnings, timePay(PayItemType.NIGHT_PAY, "야간 가산", night, wage, HALF));
        }
        PayItem holidayWithinPay = timePay(PayItemType.HOLIDAY_PAY, "휴일 8시간 이내", holidayWithin, wage, premium ? ONE_HALF : ONE);
        PayItem holidayOverPay = timePay(PayItemType.HOLIDAY_PAY, "휴일 8시간 초과", holidayOver, wage, premium ? TWO : ONE);
        if (holidayWithinPay.amount() + holidayOverPay.amount() > 0) {
            String basis = holidayOver > 0 ? holidayWithinPay.basis() + " + " + holidayOverPay.basis() : holidayWithinPay.basis();
            earnings.add(new PayItem(PayItemType.HOLIDAY_PAY, holidayWithinPay.amount() + holidayOverPay.amount(), basis));
        }

        long gross = earnings.stream().mapToLong(PayItem::amount).sum();
        WorkSummary summary = new WorkSummary(
                (int) monthDays.stream().filter(d -> d.workedMinutes() > 0).count(),
                sum(monthDays, DailyWork::workedMinutes),
                overtime,
                night,
                holidayWithin + holidayOver);
        return new PayStatement(month, summary, earnings, deductionCalculator.calculate(policy, gross));
    }

    /**
     * 주휴수당 시간: 1주 소정근로시간 15시간 이상이고 소정근로일을 개근한 경우
     * (1주 소정근로시간 / 40시간) × 8시간, 최대 8시간.
     */
    private long weeklyHolidayMinutes(PayPolicy policy, List<DailyWork> week) {
        if (policy.weeklyScheduledMinutes() < WEEKLY_HOLIDAY_MIN_SCHEDULED_MINUTES) {
            return 0;
        }
        Set<DayOfWeek> attended = week.stream()
                .filter(d -> !d.holiday() && d.workedMinutes() > 0)
                .map(d -> d.date().getDayOfWeek())
                .collect(Collectors.toSet());
        if (!attended.containsAll(policy.scheduledDays())) {
            return 0;
        }
        long scheduled = Math.min(policy.weeklyScheduledMinutes(), WEEKLY_LIMIT_MINUTES);
        return scheduled * DailyWork.DAILY_LIMIT_MINUTES / WEEKLY_LIMIT_MINUTES;
    }

    private static PayItem timePay(PayItemType type, String label, long minutes, BigDecimal wage, BigDecimal rate) {
        long amount = BigDecimal.valueOf(minutes).multiply(wage).multiply(rate)
                .divide(MINUTES_PER_HOUR, 0, RoundingMode.DOWN)
                .longValueExact();
        String basis = label + " " + hours(minutes) + " × " + won(wage)
                + (rate.compareTo(ONE) == 0 ? "" : " × " + rate.stripTrailingZeros().toPlainString());
        return new PayItem(type, amount, basis);
    }

    private static void addIfPositive(List<PayItem> items, PayItem item) {
        if (item.amount() > 0) {
            items.add(item);
        }
    }

    private static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static long sum(List<DailyWork> days, ToLongFunction<DailyWork> f) {
        return days.stream().mapToLong(f).sum();
    }

    static String hours(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        return m == 0 ? h + "시간" : h + "시간 " + m + "분";
    }

    static String won(BigDecimal amount) {
        return new DecimalFormat("#,##0.##").format(amount) + "원";
    }

    static String won(long amount) {
        return won(BigDecimal.valueOf(amount));
    }
}
