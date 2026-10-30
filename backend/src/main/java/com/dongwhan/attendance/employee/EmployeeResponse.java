package com.dongwhan.attendance.employee;

import com.dongwhan.attendance.payroll.domain.TaxType;
import com.dongwhan.attendance.payroll.domain.WageType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public record EmployeeResponse(
        Long id,
        String name,
        EmploymentType employmentType,
        LocalDate hireDate,
        boolean active,
        WageType wageType,
        Long hourlyWage,
        Long monthlySalary,
        Integer monthlyScheduledHours,
        long weeklyScheduledMinutes,
        Set<DayOfWeek> scheduledDays,
        boolean insuranceApplied,
        TaxType taxType) {

    static EmployeeResponse from(Employee e) {
        WorkCondition c = e.getWorkCondition();
        return new EmployeeResponse(
                e.getId(),
                e.getName(),
                e.getEmploymentType(),
                e.getHireDate(),
                e.isActive(),
                c.getWageType(),
                c.getHourlyWage(),
                c.getMonthlySalary(),
                c.getMonthlyScheduledHours(),
                c.getWeeklyScheduledMinutes(),
                EnumSet.copyOf(c.getScheduledDays()),
                c.isInsuranceApplied(),
                c.getTaxType());
    }
}
