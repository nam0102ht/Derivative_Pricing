package com.quantpricing.domain.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VasicekRequest {

    @NotNull(message = "Initial rate r0 is required")
    @DecimalMin(value = "0", inclusive = false, message = "r0 must be positive")
    private Double r0;

    @NotNull(message = "Kappa is required")
    @DecimalMin(value = "0", inclusive = false, message = "Kappa must be positive")
    private Double kappa;

    @NotNull(message = "Theta is required")
    @DecimalMin(value = "0", inclusive = false, message = "Theta must be positive")
    private Double theta;

    @NotNull(message = "Sigma is required")
    @DecimalMin(value = "0", inclusive = false, message = "Sigma must be positive")
    private Double sigma;
}
