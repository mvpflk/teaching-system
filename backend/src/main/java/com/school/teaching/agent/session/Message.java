package com.school.teaching.agent.session;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    private String role;

    private String content;

    private String name;

    private List<ToolCallRef> toolCalls;

    private String toolCallId;

    /**
     * DeepSeek V4 思考模式下的推理内容.
     * 使用 V4-Pro + thinking mode 时 API 要求后续请求必须回传此字段,
     * 否则返回 400.
     */
    private String reasoningContent;

    public static Message system(String content) {
        return Message.builder().role("system").content(content).build();
    }

    public static Message user(String content) {
        return Message.builder().role("user").content(content).build();
    }

    public static Message assistant(String content) {
        return Message.builder().role("assistant").content(content).build();
    }

    public static Message assistantToolCall(String id, String name, String arguments) {
        ToolCallRef ref = new ToolCallRef();
        ref.setId(id);
        ref.setType("function");
        ref.setFunction(new ToolCallRef.FunctionRef(name, arguments));
        return Message.builder().role("assistant").toolCalls(List.of(ref)).build();
    }

    public static Message toolResult(String toolCallId, String content) {
        return Message.builder().role("tool").toolCallId(toolCallId).content(content).build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolCallRef {
        private String id;
        private String type;
        private FunctionRef function;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FunctionRef {
            private String name;
            private String arguments;
        }
    }
}
