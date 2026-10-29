package com.dongwhan.attendance.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType;

    @Column(nullable = false)
    private LocalDate hireDate;

    @Embedded
    private WorkCondition workCondition;

    /** 퇴사 처리된 직원도 과거 급여 내역 보존을 위해 삭제하지 않는다. */
    private boolean active = true;

    public Employee(String name, EmploymentType employmentType, LocalDate hireDate, WorkCondition workCondition) {
        this.name = name;
        this.employmentType = employmentType;
        this.hireDate = hireDate;
        this.workCondition = workCondition;
    }

    public void update(String name, EmploymentType employmentType, LocalDate hireDate, WorkCondition workCondition) {
        this.name = name;
        this.employmentType = employmentType;
        this.hireDate = hireDate;
        this.workCondition = workCondition;
    }

    public void deactivate() {
        this.active = false;
    }
}
