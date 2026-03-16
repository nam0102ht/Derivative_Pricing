package com.quantpricing.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelComparisonResult {
    private List<ConvergencePoint> convergenceTable;
    private String description;
}
