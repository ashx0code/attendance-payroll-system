package com.dongwhan.attendance.workplace;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkplaceService {

    private static final String DEFAULT_NAME = "내 사업장";

    private final WorkplaceRepository workplaceRepository;

    /**
     * 사업장이 아직 없으면 기본값(5인 이상)으로 만든다.
     */
    @Transactional
    public Workplace get() {
        return workplaceRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> workplaceRepository.save(new Workplace(DEFAULT_NAME, false)));
    }

    @Transactional
    public Workplace update(String name, boolean smallBusiness) {
        Workplace workplace = get();
        workplace.update(name, smallBusiness);
        return workplace;
    }
}
