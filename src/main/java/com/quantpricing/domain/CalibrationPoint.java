package com.quantpricing.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationPoint {
    private double strike;
    private double maturity;
    private double marketIV;
}
