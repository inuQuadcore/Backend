package com.everybuddy.domain.translate.client;

import com.everybuddy.global.exception.CustomException;
import com.everybuddy.global.exception.ErrorCode;
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
