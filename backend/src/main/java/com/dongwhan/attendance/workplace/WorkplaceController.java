package com.dongwhan.attendance.workplace;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workplace")
@RequiredArgsConstructor
public class WorkplaceController {

    private final WorkplaceService workplaceService;

    @GetMapping
    public WorkplaceResponse get() {
        return WorkplaceResponse.from(workplaceService.get());
    }

    @PutMapping
    public WorkplaceResponse update(@Valid @RequestBody WorkplaceRequest request) {
        return WorkplaceResponse.from(workplaceService.update(request.name(), request.smallBusiness()));
    }

    public record WorkplaceRequest(@NotBlank String name, @NotNull Boolean smallBusiness) {
    }

    public record WorkplaceResponse(Long id, String name, boolean smallBusiness) {

        static WorkplaceResponse from(Workplace w) {
            return new WorkplaceResponse(w.getId(), w.getName(), w.isSmallBusiness());
        }
    }
}
