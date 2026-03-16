package com.quantpricing.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.quantpricing.domain.McStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonteCarloResult {
    private UUID jobId;
    private McStatus status;
    private Double price;
    private Double stdError;
    private double[] confidenceInterval;
    private Long elapsedMs;
}
