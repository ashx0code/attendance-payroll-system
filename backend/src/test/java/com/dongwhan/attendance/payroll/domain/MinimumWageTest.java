package com.dongwhan.attendance.payroll.domain;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MinimumWageTest {

    @Test
    @DisplayName("고시되지 않은 연도는 직전 연도 최저임금을 적용한다")
    void fallbackToLatestYear() {
        assertThat(MinimumWage.hourly(2026)).isEqualTo(10_320);
        assertThat(MinimumWage.hourly(2030)).isEqualTo(10_320);
    }

    @Test
    @DisplayName("최저임금과 같은 시급은 허용한다")
    void equalToMinimum() {
        assertThatCode(() -> MinimumWage.validate(PayPolicy.hourly(10_320, 2400, Set.of(MONDAY)), 2026))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("월급제는 월급 ÷ 209시간으로 비교한다")
    void monthly() {
        // 2,156,880 ÷ 209 = 10,320
        assertThatCode(() -> MinimumWage.validate(PayPolicy.monthly(2_156_880, Set.of(MONDAY)), 2026))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> MinimumWage.validate(PayPolicy.monthly(2_156_879, Set.of(MONDAY)), 2026))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
