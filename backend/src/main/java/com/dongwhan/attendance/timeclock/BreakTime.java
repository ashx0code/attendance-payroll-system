package com.dongwhan.attendance.timeclock;

import com.dongwhan.attendance.payroll.domain.TimeRange;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 휴게 구간. 휴게 중이면 종료 시각이 비어 있다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BreakTime {

    @Column(nullable = false)
    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private BreakTime(LocalDateTime startAt, LocalDateTime endAt) {
        this.startAt = startAt;
        this.endAt = endAt;
    }

    static BreakTime startingAt(LocalDateTime startAt) {
        return new BreakTime(startAt, null);
    }

    BreakTime endingAt(LocalDateTime endAt) {
        return new BreakTime(startAt, endAt);
    }

    boolean isOpen() {
        return endAt == null;
    }

    /**
     * 휴게 구간. 아직 끝나지 않았으면 {@code openEnd}까지로 보고, 길이가 0이면 비운다.
     */
    Optional<TimeRange> toRange(LocalDateTime openEnd) {
        LocalDateTime end = endAt != null ? endAt : openEnd;
        return end.isAfter(startAt) ? Optional.of(new TimeRange(startAt, end)) : Optional.empty();
    }
}
