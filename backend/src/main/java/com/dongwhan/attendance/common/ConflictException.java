package com.dongwhan.attendance.common;

/**
 * 현재 상태와 맞지 않아 처리할 수 없는 요청 (409).
 */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}
