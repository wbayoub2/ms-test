package com.machine.ms.test.api.http.v1;

import com.machine.ms.test.core.service.functional.TraceabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    public TraceabilityController(TraceabilityService traceabilityService) {
        this.traceabilityService = traceabilityService;
    }

    @GetMapping("/tests/{testId}/history")
    public Map<String, Object> history(@PathVariable String testId,
                                       @RequestParam String service,
                                       @RequestParam(defaultValue = "main") String branch,
                                       @RequestParam(defaultValue = "100") int limit) {
        return traceabilityService.history(testId, service, branch, limit);
    }

    @GetMapping("/tests/{testId}/status-at/{commitSha}")
    public Map<String, Object> statusAt(@PathVariable String testId,
                                        @PathVariable String commitSha,
                                        @RequestParam String service) {
        return traceabilityService.statusAt(testId, commitSha, service);
    }

    @GetMapping("/tests/{testId}/last-green-first-red")
    public Map<String, Object> lastGreenFirstRed(@PathVariable String testId,
                                                 @RequestParam String service,
                                                 @RequestParam(defaultValue = "main") String branch) {
        return traceabilityService.lastGreenFirstRed(testId, service, branch);
    }

    @GetMapping("/tests/{testId}/compare")
    public Map<String, Object> compare(@PathVariable String testId,
                                       @RequestParam String service,
                                       @RequestParam("from") String fromCommit,
                                       @RequestParam("to") String toCommit) {
        return traceabilityService.compare(testId, service, fromCommit, toCommit);
    }

    @GetMapping("/tests/{testId}/coverage-at/{commitSha}")
    public Map<String, Object> coverageAt(@PathVariable String testId,
                                          @PathVariable String commitSha,
                                          @RequestParam String service) {
        return traceabilityService.coverageAt(testId, commitSha, service);
    }

    @GetMapping("/tests/{testId}/coverage-diff")
    public Map<String, Object> coverageDiff(@PathVariable String testId,
                                            @RequestParam String service,
                                            @RequestParam("from") String fromCommit,
                                            @RequestParam("to") String toCommit) {
        return traceabilityService.coverageDiff(testId, service, fromCommit, toCommit);
    }
}
