package com.dongwhan.attendance.payroll.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 4대 보험료와 원천징수 세액을 계산한다. 모든 금액은 10원 미만 절사.
 */
class DeductionCalculator {

    /** 소액부징수: 원천징수 소득세가 1,000원 미만이면 징수하지 않는다. */
    static final long MIN_WITHHOLDING_TAX = 1_000;

    private static final BigDecimal BUSINESS_INCOME_TAX_RATE = new BigDecimal("0.03");
    private static final BigDecimal LOCAL_INCOME_TAX_RATE = new BigDecimal("0.1");

    List<PayItem> calculate(PayPolicy policy, long grossPay) {
        List<PayItem> items = new ArrayList<>();
        if (policy.insuranceApplied()) {
            InsuranceRates rates = policy.insuranceRates();
            long health = floor10(grossPay, rates.healthInsurance());
            items.add(item(PayItemType.NATIONAL_PENSION, floor10(grossPay, rates.nationalPension()), grossPay, rates.nationalPension()));
            items.add(item(PayItemType.HEALTH_INSURANCE, health, grossPay, rates.healthInsurance()));
            items.add(new PayItem(PayItemType.LONG_TERM_CARE, floor10(health, rates.longTermCare()),
                    "건강보험료 " + PayrollCalculator.won(health) + " × " + percent(rates.longTermCare())));
            items.add(item(PayItemType.EMPLOYMENT_INSURANCE, floor10(grossPay, rates.employmentInsurance()), grossPay, rates.employmentInsurance()));
        }
        if (policy.taxType() == TaxType.BUSINESS_INCOME_3_3) {
            long incomeTax = floor10(grossPay, BUSINESS_INCOME_TAX_RATE);
            if (incomeTax >= MIN_WITHHOLDING_TAX) {
                items.add(item(PayItemType.INCOME_TAX, incomeTax, grossPay, BUSINESS_INCOME_TAX_RATE));
                items.add(new PayItem(PayItemType.LOCAL_INCOME_TAX, floor10(incomeTax, LOCAL_INCOME_TAX_RATE),
                        "소득세 " + PayrollCalculator.won(incomeTax) + " × 10%"));
            }
        }
        return items;
    }

    private static PayItem item(PayItemType type, long amount, long base, BigDecimal rate) {
        return new PayItem(type, amount, "총지급액 " + PayrollCalculator.won(base) + " × " + percent(rate));
    }

    private static long floor10(long base, BigDecimal rate) {
        return BigDecimal.valueOf(base).multiply(rate)
                .divide(BigDecimal.TEN, 0, RoundingMode.DOWN)
                .multiply(BigDecimal.TEN)
                .longValueExact();
    }

    private static String percent(BigDecimal rate) {
        return rate.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
    }
}
