package com.machine.ms.test.api.bridge;

import com.machine.ms.test.core.port.in.StepValidationUseCase;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ToolDispatcher {

    private final StepValidationUseCase stepValidationUseCase;
    private final ToolRegistry toolRegistry;
    private final ToolParamsMapper paramsMapper;
    private final ToolResponseMapper responseMapper;

    public ToolDispatcher(StepValidationUseCase stepValidationUseCase,
                          ToolRegistry toolRegistry,
                          ToolParamsMapper paramsMapper,
                          ToolResponseMapper responseMapper) {
        this.stepValidationUseCase = stepValidationUseCase;
        this.toolRegistry = toolRegistry;
        this.paramsMapper = paramsMapper;
        this.responseMapper = responseMapper;
    }

    public Map<String, Object> dispatch(MsTestToolCallRequest request) {
        if (request == null || request.toolId() == null || request.toolId().isBlank()) {
            throw new BridgeInvalidParametersException("Missing toolId", Map.of("param", "toolId"));
        }
        if (!toolRegistry.supports(request.toolId())) {
            throw new BridgeInvalidParametersException(
                    "Unknown toolId: " + request.toolId(),
                    Map.of("param", "toolId", "value", request.toolId()));
        }
        return switch (request.toolId()) {
            case ToolRegistry.STEP_EXECUTION_DIFF_GET, ToolRegistry.STEP_REVIEW_DIFF_GET ->
                    responseMapper.diffResponse(stepValidationUseCase.getDiff(
                            paramsMapper.toStepContext(request.toolId(), request.requestId(), request.params())));
            case ToolRegistry.STEP_EXECUTION_IMPACTED_TESTS_GET, ToolRegistry.STEP_REVIEW_IMPACTED_TESTS_GET -> {
                long start = System.nanoTime();
                var impacted = stepValidationUseCase.getImpactedTests(
                        paramsMapper.toStepContext(request.toolId(), request.requestId(), request.params()));
                long latencyMs = (System.nanoTime() - start) / 1_000_000;
                yield responseMapper.impactedResponse(impacted, latencyMs);
            }
            case ToolRegistry.STEP_GATE_EVALUATE,
                 ToolRegistry.STEP_REVIEW_GATE_EVALUATE,
                 ToolRegistry.STEP_EXECUTION_GATE_EVALUATE ->
                    responseMapper.gateResponse(stepValidationUseCase.evaluateGate(
                            paramsMapper.toGateRequest(request.toolId(), request.requestId(), request.params())));
            default -> throw new BridgeInvalidParametersException(
                    "Unknown toolId: " + request.toolId(),
                    Map.of("param", "toolId", "value", request.toolId()));
        };
    }
}
