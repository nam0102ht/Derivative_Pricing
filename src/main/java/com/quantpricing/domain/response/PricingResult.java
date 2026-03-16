package com.quantpricing.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PricingResult {
    private double price;
    private String model;
    private Map<String, Object> metadata;
}
