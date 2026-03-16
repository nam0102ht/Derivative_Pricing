package com.quantpricing.domain.request;

import com.quantpricing.domain.OptionType;
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
public class ImpliedVolRequest {

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

    @NotNull(message = "Option type is required")
    private OptionType type;

    @NotNull(message = "Market price is required")
    @DecimalMin(value = "0", inclusive = false, message = "Market price must be positive")
    private Double marketPrice;
}
