package com.everybuddy.domain.translate.client;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class TritonInferResponse {

    private List<OutputData> outputs;

    @Getter
    @NoArgsConstructor
    public static class OutputData {
        private String name;
        private String datatype;
        private List<Object> data;
    }

    public String extractString(String outputName) {
        if (outputs == null) return null;
        return outputs.stream()
                .filter(o -> outputName.equals(o.getName()))
                .findFirst()
                .map(o -> o.getData() != null && !o.getData().isEmpty()
                        ? String.valueOf(o.getData().get(0))
                        : null)
                .orElse(null);
    }
}
