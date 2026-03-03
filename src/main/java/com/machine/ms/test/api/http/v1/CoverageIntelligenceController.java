package com.machine.ms.test.api.http.v1;

import com.machine.ms.test.core.service.functional.CoverageIntelligenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CoverageIntelligenceController {

    private final CoverageIntelligenceService coverageService;

    public CoverageIntelligenceController(CoverageIntelligenceService coverageService) {
        this.coverageService = coverageService;
    }

    @GetMapping("/coverage/uncovered")
    public Map<String, Object> uncovered(@RequestParam String service,
                                         @RequestParam(defaultValue = "main") String branch,
                                         @RequestParam(defaultValue = "200") int limit) {
        return coverageService.uncovered(service, branch, limit);
    }

    @GetMapping("/coverage/hotspots")
    public Map<String, Object> hotspots(@RequestParam String service,
                                        @RequestParam(defaultValue = "main") String branch,
                                        @RequestParam(defaultValue = "50") int limit) {
        return coverageService.hotspots(service, branch, limit);
    }

    @GetMapping("/qa/backlog")
    public Map<String, Object> backlog(@RequestParam String service,
                                       @RequestParam(defaultValue = "main") String branch) {
        return coverageService.backlog(service, branch);
    }

    @GetMapping("/services/overview")
    public Map<String, Object> overview(@RequestParam(defaultValue = "main") String branch) {
        return coverageService.servicesOverview(branch);
    }
}
