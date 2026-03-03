package com.machine.ms.test.api.http.v1;

import com.machine.ms.test.core.service.functional.ForensicsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ForensicsController {

    private final ForensicsService forensicsService;

    public ForensicsController(ForensicsService forensicsService) {
        this.forensicsService = forensicsService;
    }

    @GetMapping("/tests/{testId}/regression-forensics")
    public Map<String, Object> regressionForensics(@PathVariable String testId,
                                                   @RequestParam String service,
                                                   @RequestParam(defaultValue = "main") String branch) {
        return forensicsService.regressionForensics(testId, service, branch);
    }

    @GetMapping("/commits/{commitSha}/suspects")
    public Map<String, Object> suspects(@PathVariable String commitSha,
                                        @RequestParam String service,
                                        @RequestParam String testId) {
        return forensicsService.suspects(commitSha, service, testId);
    }
}
