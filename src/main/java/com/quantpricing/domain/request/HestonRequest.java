package com.quantpricing.domain.request;

import com.quantpricing.domain.OptionType;
import jakarta.validation.constraints.DecimalMax;
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
public class HestonRequest {

    @NotNull(message = "Spot price is required")
    @DecimalMin(value = "0", inclusive = false, message = "Spot must be positive")
    private Double spot;

    @NotNull(message = "Strike price is required")
    @DecimalMin(value = "0", inclusive = false, message = "Strike must be positive")
    private Double strike;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0", inclusive = false, message = "Rate must be positive")
    private Double rate;

    @NotNull(message = "Time to expiry is required")
    @DecimalMin(value = "0", inclusive = false, message = "Time to expiry must be positive")
    private Double timeToExpiry;

    @NotNull(message = "Initial variance v0 is required")
    @DecimalMin(value = "0", inclusive = false, message = "v0 must be positive")
    private Double v0;

    @NotNull(message = "Kappa is required")
    @DecimalMin(value = "0", inclusive = false, message = "Kappa must be positive")
    private Double kappa;

    @NotNull(message = "Theta is required")
    @DecimalMin(value = "0", inclusive = false, message = "Theta must be positive")
    private Double theta;

    @NotNull(message = "Xi (vol of vol) is required")
    @DecimalMin(value = "0", inclusive = false, message = "Xi must be positive")
    private Double xi;

    @DecimalMin(value = "-1", message = "Rho must be >= -1")
    @DecimalMax(value = "1", message = "Rho must be <= 1")
    @Builder.Default
    private Double rho = -0.5;

    @NotNull(message = "Option type is required")
    private OptionType type;
}
