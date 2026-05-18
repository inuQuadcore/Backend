package com.everybuddy.domain.translate.client;

import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TritonInferResponse {

    private List<OutputData> outputs;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutputData {
        private String name;
        private String datatype;
        private List<Object> data;
    }

    public String getOutputValue(String outputName) {
        if (outputs == null) {
            throw new CustomException(ErrorCode.MODEL_ERROR);
        }

        OutputData output = outputs.stream()
                .filter(o -> outputName.equals(o.getName()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.MODEL_ERROR));

        if (output.getData() == null || output.getData().isEmpty()) {
            throw new CustomException(ErrorCode.MODEL_ERROR);
        }
        return String.valueOf(output.getData().getFirst());
    }
}
