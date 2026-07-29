package com.school.teaching.agent.tool;

import com.school.teaching.agent.tool.annotation.ToolCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    private String name;
    private String description;
    private ToolCategory category;
    private List<String> allowedRoles;
    private Map<String, Object> parameters;

    public Map<String, Object> toDeepSeekFormat() {
        return Map.of(
            "type", "function",
            "function", Map.of(
                "name", name,
                "description", description,
                "parameters", parameters
            )
        );
    }
}
