package com.machine.ms.test.api.bridge;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tools")
public class MsTestToolBridgeController {

    private final ToolRegistry toolRegistry;
    private final ToolDispatcher toolDispatcher;

    public MsTestToolBridgeController(ToolRegistry toolRegistry, ToolDispatcher toolDispatcher) {
        this.toolRegistry = toolRegistry;
        this.toolDispatcher = toolDispatcher;
    }

    @GetMapping("/catalog")
    public List<ToolDefinition> catalog() {
        return toolRegistry.catalog();
    }

    @PostMapping("/call")
    public Map<String, Object> call(@RequestBody(required = false) MsTestToolCallRequest request) {
        return toolDispatcher.dispatch(request);
    }
}
