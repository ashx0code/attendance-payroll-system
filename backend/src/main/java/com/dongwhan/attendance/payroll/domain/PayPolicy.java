package com.dongwhan.attendance.payroll.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.Objects;
import java.util.Set;

/**
 * 근로자 한 명에게 적용되는 근로 조건.
 *
 * @param wageType               시급제 / 월급제
 * @param hourlyWage             시급 (시급제)
 * @param monthlySalary          월 기본급 (월급제, 주휴수당 포함)
 * @param monthlyScheduledHours  월 소정근로시간 (월급제 통상시급 산정용, 주 40시간 기준 209시간)
 * @param weeklyScheduledMinutes 1주 소정근로시간(분)
 * @param scheduledDays          소정근로일 (주휴수당 개근 판단용)
 * @param smallBusiness          상시 근로자 5인 미만 사업장 여부 (연장·야간·휴일 가산수당 미적용)
 * @param insuranceApplied       4대 보험 적용 여부
 * @param taxType                원천징수 방식
 * @param insuranceRates         4대 보험 요율
 */
public record PayPolicy(
        WageType wageType,
        BigDecimal hourlyWage,
        long monthlySalary,
        int monthlyScheduledHours,
        long weeklyScheduledMinutes,
        Set<DayOfWeek> scheduledDays,
        boolean smallBusiness,
        boolean insuranceApplied,
        TaxType taxType,
        InsuranceRates insuranceRates) {

    public static final int DEFAULT_MONTHLY_SCHEDULED_HOURS = 209;

    public PayPolicy {
        Objects.requireNonNull(wageType, "wageType");
        scheduledDays = Set.copyOf(scheduledDays);
        taxType = taxType == null ? TaxType.NONE : taxType;
        insuranceRates = insuranceRates == null ? InsuranceRates.Y2026 : insuranceRates;
        if (wageType == WageType.HOURLY && (hourlyWage == null || hourlyWage.signum() <= 0)) {
            throw new IllegalArgumentException("시급제는 시급이 필요합니다.");
        }
        if (wageType == WageType.MONTHLY && (monthlySalary <= 0 || monthlyScheduledHours <= 0)) {
            throw new IllegalArgumentException("월급제는 월 기본급과 월 소정근로시간이 필요합니다.");
        }
    }

    public static PayPolicy hourly(long hourlyWage, long weeklyScheduledMinutes, Set<DayOfWeek> scheduledDays) {
        return new PayPolicy(WageType.HOURLY, BigDecimal.valueOf(hourlyWage), 0, 0,
                weeklyScheduledMinutes, scheduledDays, false, false, TaxType.NONE, null);
    }

    public static PayPolicy monthly(long monthlySalary, Set<DayOfWeek> scheduledDays) {
        return new PayPolicy(WageType.MONTHLY, null, monthlySalary, DEFAULT_MONTHLY_SCHEDULED_HOURS,
                40 * 60, scheduledDays, false, true, TaxType.NONE, null);
    }

    public PayPolicy withSmallBusiness(boolean smallBusiness) {
        return new PayPolicy(wageType, hourlyWage, monthlySalary, monthlyScheduledHours, weeklyScheduledMinutes,
                scheduledDays, smallBusiness, insuranceApplied, taxType, insuranceRates);
    }

    public PayPolicy withInsurance(boolean insuranceApplied) {
        return new PayPolicy(wageType, hourlyWage, monthlySalary, monthlyScheduledHours, weeklyScheduledMinutes,
                scheduledDays, smallBusiness, insuranceApplied, taxType, insuranceRates);
    }

    public PayPolicy withTaxType(TaxType taxType) {
        return new PayPolicy(wageType, hourlyWage, monthlySalary, monthlyScheduledHours, weeklyScheduledMinutes,
                scheduledDays, smallBusiness, insuranceApplied, taxType, insuranceRates);
    }

    /**
     * 가산수당 계산의 기준이 되는 통상시급. 월급제는 월 기본급 / 월 소정근로시간.
     */
    public BigDecimal ordinaryHourlyWage() {
        return wageType == WageType.HOURLY
                ? hourlyWage
                : BigDecimal.valueOf(monthlySalary).divide(BigDecimal.valueOf(monthlyScheduledHours), 4, RoundingMode.HALF_UP);
    }
}
