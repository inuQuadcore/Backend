package com.everybuddy.domain.translate.client;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TritonInferRequest {

    private List<Input> inputs;
    private List<Output> outputs;

    @Getter
    @Builder
    public static class Input {
        private String name;
        private List<Integer> shape;
        private String datatype;
        private List<Object> data;
    }

    @Getter
    @Builder
    public static class Output {
        private String name;
        private Map<String, Object> parameters;
    }
}
