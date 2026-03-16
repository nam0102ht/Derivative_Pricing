package com.quantpricing.domain.request;

import com.quantpricing.domain.BarrierType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BarrierOptionRequest extends OptionRequest {

    @NotNull(message = "Barrier level is required")
    @DecimalMin(value = "0", inclusive = false, message = "Barrier level must be positive")
    private Double barrierLevel;

    @NotNull(message = "Barrier type is required")
    private BarrierType barrierType;
}
