package com.quantpricing.domain.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AsianOptionRequest extends OptionRequest {

    @Min(value = 2, message = "Monitoring steps must be at least 2")
    @Max(value = 15, message = "Monitoring steps cannot exceed 15")
    @Builder.Default
    private int monitoringSteps = 12;
}
