package com.machine.ms.test.api.http.v1;

import com.machine.ms.test.api.contract.functional.ImpactComputeFromCommitsRequest;
import com.machine.ms.test.api.contract.functional.ImpactComputeRequest;
import com.machine.ms.test.core.service.functional.ImpactComputationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/impact")
public class ImpactController {

    private final ImpactComputationService impactService;

    public ImpactController(ImpactComputationService impactService) {
        this.impactService = impactService;
    }

    @PostMapping("/compute")
    public Map<String, Object> compute(@Valid @RequestBody ImpactComputeRequest request) {
        return impactService.compute(request);
    }

    @PostMapping("/compute-from-commits")
    public Map<String, Object> computeFromCommits(@Valid @RequestBody ImpactComputeFromCommitsRequest request) {
        return impactService.computeFromCommits(request);
    }
}
