package com.dongwhan.attendance.workplace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사업장. 현재는 단일 사업장만 운영한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workplace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** 상시 근로자 5인 미만 사업장 여부 */
    private boolean smallBusiness;

    public Workplace(String name, boolean smallBusiness) {
        this.name = name;
        this.smallBusiness = smallBusiness;
    }

    public void update(String name, boolean smallBusiness) {
        this.name = name;
        this.smallBusiness = smallBusiness;
    }
}
