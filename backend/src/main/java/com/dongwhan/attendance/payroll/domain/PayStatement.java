package com.dongwhan.attendance.payroll.domain;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public record PayStatement(
        YearMonth month,
        WorkSummary workSummary,
        List<PayItem> earnings,
        List<PayItem> deductions) {

    public PayStatement {
        earnings = List.copyOf(earnings);
        deductions = List.copyOf(deductions);
    }

    public long grossPay() {
        return earnings.stream().mapToLong(PayItem::amount).sum();
    }

    public long totalDeduction() {
        return deductions.stream().mapToLong(PayItem::amount).sum();
    }

    public long netPay() {
        return grossPay() - totalDeduction();
    }

    public long amountOf(PayItemType type) {
        return find(type).map(PayItem::amount).orElse(0L);
    }

    public Optional<PayItem> find(PayItemType type) {
        return earnings.stream().filter(i -> i.type() == type).findFirst()
                .or(() -> deductions.stream().filter(i -> i.type() == type).findFirst());
    }
}
