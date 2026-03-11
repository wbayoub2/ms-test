package com.machine.ms.test.api.http.v1;

import com.machine.ms.test.api.contract.integration.MsInfraE2eConsultationResponse;
import com.machine.ms.test.core.service.integration.MsInfraE2eConsultationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/e2e")
public class E2eConsultationController {

    private final MsInfraE2eConsultationService consultationService;

    public E2eConsultationController(MsInfraE2eConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping("/runs/{runId}")
    public MsInfraE2eConsultationResponse consult(@PathVariable String runId,
                                                  @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
        return consultationService.consult(runId, requestId);
    }
}
