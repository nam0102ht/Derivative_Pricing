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
public class KouRequest {

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

    @NotNull(message = "Lambda (jump intensity) is required")
    @DecimalMin(value = "0", inclusive = false, message = "Lambda must be positive")
    private Double lambda;

    @DecimalMin(value = "0", inclusive = false, message = "p must be > 0")
    @DecimalMax(value = "1", inclusive = false, message = "p must be < 1")
    @Builder.Default
    private Double p = 0.5;

    @NotNull(message = "Eta1 (upward jump rate) is required")
    @DecimalMin(value = "1", inclusive = false, message = "Eta1 must be > 1 for finite mean")
    private Double eta1;

    @NotNull(message = "Eta2 (downward jump rate) is required")
    @DecimalMin(value = "0", inclusive = false, message = "Eta2 must be positive")
    private Double eta2;
}
