package com.dongwhan.attendance.employee;

import com.dongwhan.attendance.common.NotFoundException;

public class EmployeeNotFoundException extends NotFoundException {

    public EmployeeNotFoundException(Long id) {
        super("직원을 찾을 수 없습니다: " + id);
    }
}
