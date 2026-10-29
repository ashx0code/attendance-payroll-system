package com.dongwhan.attendance.payroll.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 시작~종료 시각 구간. 종료 시각은 포함하지 않는다(반개구간).
 */
public record TimeRange(LocalDateTime start, LocalDateTime end) {

    private static final LocalTime NIGHT_START = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(6, 0);

    public TimeRange {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("종료 시각은 시작 시각보다 늦어야 합니다: " + start + " ~ " + end);
        }
    }

    public long minutes() {
        return Duration.between(start, end).toMinutes();
    }

    public long overlapMinutes(TimeRange other) {
        LocalDateTime s = start.isAfter(other.start) ? start : other.start;
        LocalDateTime e = end.isBefore(other.end) ? end : other.end;
        return e.isAfter(s) ? Duration.between(s, e).toMinutes() : 0;
    }

    /**
     * 야간근로 시간대(22:00 ~ 익일 06:00)와 겹치는 분.
     */
    public long nightMinutes() {
        long total = 0;
        for (LocalDate d = start.toLocalDate().minusDays(1); !d.isAfter(end.toLocalDate()); d = d.plusDays(1)) {
            total += overlapMinutes(new TimeRange(d.atTime(NIGHT_START), d.plusDays(1).atTime(NIGHT_END)));
        }
        return total;
    }
}
