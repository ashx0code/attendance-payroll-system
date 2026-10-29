package com.dongwhan.attendance.employee;

import com.dongwhan.attendance.payroll.domain.TaxType;
import com.dongwhan.attendance.payroll.domain.WageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * @param weeklyScheduledMinutes 1주 소정근로시간(분). 예: 주 17시간 30분 → 1050
 * @param monthlyScheduledHours  월급제 월 소정근로시간. 생략하면 209시간
 */
public record EmployeeRequest(
        @NotBlank String name,
        @NotNull EmploymentType employmentType,
        @NotNull LocalDate hireDate,
        @NotNull WageType wageType,
        @Positive Long hourlyWage,
        @Positive Long monthlySalary,
        @Positive Integer monthlyScheduledHours,
        @NotNull @PositiveOrZero Long weeklyScheduledMinutes,
        @NotEmpty Set<DayOfWeek> scheduledDays,
        @NotNull Boolean insuranceApplied,
        TaxType taxType) {

    WorkCondition toWorkCondition(int validationYear) {
        return WorkCondition.builder()
                .wageType(wageType)
                .hourlyWage(hourlyWage)
                .monthlySalary(monthlySalary)
                .monthlyScheduledHours(monthlyScheduledHours)
                .weeklyScheduledMinutes(weeklyScheduledMinutes)
                .scheduledDays(scheduledDays)
                .insuranceApplied(insuranceApplied)
                .taxType(taxType)
                .validationYear(validationYear)
                .build();
    }
}
