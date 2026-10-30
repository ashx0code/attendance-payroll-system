package com.dongwhan.attendance.timeclock;

import com.dongwhan.attendance.common.ConflictException;

public class AttendanceStateException extends ConflictException {

    public AttendanceStateException(String message) {
        super(message);
    }
}
