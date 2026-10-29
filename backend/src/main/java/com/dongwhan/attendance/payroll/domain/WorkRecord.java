package com.dongwhan.attendance.payroll.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 하루치 출퇴근 기록. 퇴근이 확정된 기록만 급여 계산에 사용한다.
 *
 * @param workDate 근무일(출근일 기준). 자정을 넘겨 퇴근해도 출근일의 근무로 본다.
 * @param work     출근 ~ 퇴근
 * @param breaks   휴게 구간
 * @param holiday  휴일(주휴일·공휴일 등) 근무 여부
 */
public record WorkRecord(LocalDate workDate, TimeRange work, List<TimeRange> breaks, boolean holiday) {

    public WorkRecord {
        Objects.requireNonNull(workDate, "workDate");
        Objects.requireNonNull(work, "work");
        breaks = breaks == null ? List.of() : List.copyOf(breaks);
    }

    public long workedMinutes() {
        return work.minutes() - breaks.stream().mapToLong(work::overlapMinutes).sum();
    }

    public long nightMinutes() {
        long breakNight = breaks.stream()
                .filter(b -> work.overlapMinutes(b) > 0)
                .mapToLong(b -> clip(b).nightMinutes())
                .sum();
        return work.nightMinutes() - breakNight;
    }

    private TimeRange clip(TimeRange b) {
        return new TimeRange(
                b.start().isBefore(work.start()) ? work.start() : b.start(),
                b.end().isAfter(work.end()) ? work.end() : b.end());
    }
}
