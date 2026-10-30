package com.dongwhan.attendance.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 테스트에서 현재 시각을 옮길 수 있는 시계.
 */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private volatile Instant instant;

    public MutableClock(LocalDateTime at, ZoneId zone) {
        this.zone = zone;
        this.instant = at.atZone(zone).toInstant();
    }

    public void set(LocalDateTime at) {
        instant = at.atZone(zone).toInstant();
    }

    public void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
