package com.machine.ms.test.api.http.v1;

import com.machine.ms.test.api.contract.functional.QualityGateEvaluateRequest;
import com.machine.ms.test.core.service.functional.QualityGateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/quality-gates")
public class QualityGateController {

    private final QualityGateService qualityGateService;

    public QualityGateController(QualityGateService qualityGateService) {
        this.qualityGateService = qualityGateService;
    }

    @PostMapping("/evaluate")
    public Map<String, Object> evaluate(@Valid @RequestBody QualityGateEvaluateRequest request) {
        return qualityGateService.evaluate(request);
    }

    @GetMapping("/policies")
    public Map<String, Object> policies() {
        return qualityGateService.policies();
    }
}
