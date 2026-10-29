package com.dongwhan.attendance.payroll.domain;

public enum PayItemType {
    BASE_PAY("기본급"),
    WEEKLY_HOLIDAY_PAY("주휴수당"),
    OVERTIME_PAY("연장근로수당"),
    NIGHT_PAY("야간근로수당"),
    HOLIDAY_PAY("휴일근로수당"),

    NATIONAL_PENSION("국민연금"),
    HEALTH_INSURANCE("건강보험"),
    LONG_TERM_CARE("장기요양보험"),
    EMPLOYMENT_INSURANCE("고용보험"),
    INCOME_TAX("소득세"),
    LOCAL_INCOME_TAX("지방소득세");

    private final String label;

    PayItemType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
