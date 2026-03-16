package com.quantpricing.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConvergencePoint {
    private int steps;
    private double binomialPrice;
    private double trinomialPrice;
}
