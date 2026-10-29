package com.dongwhan.attendance.payroll.domain;

import java.math.BigDecimal;

/**
 * 4대 보험 근로자 부담 요율.
 *
 * @param nationalPension     국민연금 (보수 대비)
 * @param healthInsurance     건강보험 (보수 대비)
 * @param longTermCare        장기요양보험 (건강보험료 대비)
 * @param employmentInsurance 고용보험 실업급여 (보수 대비)
 */
public record InsuranceRates(
        BigDecimal nationalPension,
        BigDecimal healthInsurance,
        BigDecimal longTermCare,
        BigDecimal employmentInsurance) {

    /** 2026년 기준 요율. 매년 고시되는 요율로 갱신해야 한다. */
    public static final InsuranceRates Y2026 = new InsuranceRates(
            new BigDecimal("0.0475"),
            new BigDecimal("0.03595"),
            new BigDecimal("0.1314"),
            new BigDecimal("0.009"));
}
