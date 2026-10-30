package com.dongwhan.attendance.workplace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {

    Optional<Workplace> findFirstByOrderByIdAsc();
}
