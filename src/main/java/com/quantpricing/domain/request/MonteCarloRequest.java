package com.quantpricing.domain.request;

import com.quantpricing.domain.OptionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonteCarloRequest {

    @NotNull(message = "Spot price is required")
    @DecimalMin(value = "0", inclusive = false, message = "Spot must be positive")
    private Double spot;

    @NotNull(message = "Strike price is required")
    @DecimalMin(value = "0", inclusive = false, message = "Strike must be positive")
    private Double strike;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0", inclusive = false, message = "Rate must be positive")
    private Double rate;

    @NotNull(message = "Sigma is required")
    @DecimalMin(value = "0", inclusive = false, message = "Sigma must be positive")
    private Double sigma;

    @NotNull(message = "Time to expiry is required")
    @DecimalMin(value = "0", inclusive = false, message = "Time to expiry must be positive")
    private Double timeToExpiry;

    @NotNull(message = "Option type is required")
    private OptionType type;

    @Min(value = 1000, message = "Number of paths must be at least 1000")
    @Max(value = 1000000, message = "Number of paths cannot exceed 1000000")
    @Builder.Default
    private int numPaths = 100000;

    @Min(value = 1, message = "Number of steps must be at least 1")
    @Max(value = 1000, message = "Number of steps cannot exceed 1000")
    @Builder.Default
    private int numSteps = 252;
}
