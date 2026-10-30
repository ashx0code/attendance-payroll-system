package com.dongwhan.attendance.employee;

import com.dongwhan.attendance.payroll.domain.MinimumWage;
import com.dongwhan.attendance.payroll.domain.PayPolicy;
import com.dongwhan.attendance.payroll.domain.TaxType;
import com.dongwhan.attendance.payroll.domain.WageType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 근로계약상 근로 조건. 생성 시 급여 계산이 가능한 조건인지, 최저임금 이상인지 검증한다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkCondition {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WageType wageType;

    private Long hourlyWage;

    private Long monthlySalary;

    private Integer monthlyScheduledHours;

    @Column(nullable = false)
    private long weeklyScheduledMinutes;

    @Convert(converter = DayOfWeekSetConverter.class)
    @Column(nullable = false)
    private Set<DayOfWeek> scheduledDays;

    private boolean insuranceApplied;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxType taxType;

    @Builder
    private WorkCondition(WageType wageType, Long hourlyWage, Long monthlySalary, Integer monthlyScheduledHours,
                          long weeklyScheduledMinutes, Set<DayOfWeek> scheduledDays, boolean insuranceApplied,
                          TaxType taxType, int validationYear) {
        if (scheduledDays == null || scheduledDays.isEmpty()) {
            throw new IllegalArgumentException("소정근로일을 하루 이상 지정해야 합니다.");
        }
        this.wageType = wageType;
        this.hourlyWage = wageType == WageType.HOURLY ? hourlyWage : null;
        this.monthlySalary = wageType == WageType.MONTHLY ? monthlySalary : null;
        this.monthlyScheduledHours = wageType == WageType.MONTHLY
                ? (monthlyScheduledHours == null ? PayPolicy.DEFAULT_MONTHLY_SCHEDULED_HOURS : monthlyScheduledHours)
                : null;
        this.weeklyScheduledMinutes = weeklyScheduledMinutes;
        this.scheduledDays = Set.copyOf(scheduledDays);
        this.insuranceApplied = insuranceApplied;
        this.taxType = taxType == null ? TaxType.NONE : taxType;
        MinimumWage.validate(toPayPolicy(false), validationYear);
    }

    /**
     * 주휴일 여부. 주휴일은 일요일로 보며, 일요일이 소정근로일이면 주휴일이 아니다.
     */
    public boolean isWeeklyHoliday(DayOfWeek day) {
        return day == DayOfWeek.SUNDAY && !scheduledDays.contains(day);
    }

    public PayPolicy toPayPolicy(boolean smallBusiness) {
        return new PayPolicy(
                wageType,
                hourlyWage == null ? null : BigDecimal.valueOf(hourlyWage),
                monthlySalary == null ? 0 : monthlySalary,
                monthlyScheduledHours == null ? 0 : monthlyScheduledHours,
                weeklyScheduledMinutes,
                scheduledDays,
                smallBusiness,
                insuranceApplied,
                taxType,
                null);
    }
}
