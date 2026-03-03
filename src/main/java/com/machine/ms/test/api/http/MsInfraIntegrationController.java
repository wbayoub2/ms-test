package com.machine.ms.test.api.http;

import com.machine.ms.test.api.contract.integration.MsInfraRunRequest;
import com.machine.ms.test.core.service.integration.MsInfraBridgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/integration/ms-infra")
public class MsInfraIntegrationController {

    private final MsInfraBridgeService bridgeService;

    public MsInfraIntegrationController(MsInfraBridgeService bridgeService) {
        this.bridgeService = bridgeService;
    }

    @PostMapping("/test-run/start-and-poll")
    public Map<String, Object> startAndPoll(@Valid @RequestBody MsInfraRunRequest request,
                                            @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
        return bridgeService.startAndPoll(request, requestId);
    }
}
