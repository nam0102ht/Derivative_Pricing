package com.quantpricing.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GreeksResult {
    private double price;
    private double delta;
    private double gamma;
    private double theta;
    private double vega;
    private double rho;
}
