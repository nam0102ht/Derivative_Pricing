package com.quantpricing.service;

import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.ConvergencePoint;
import com.quantpricing.domain.response.ModelComparisonResult;
import com.quantpricing.engine.TrinomialEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for model comparison operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CompareService {

    private final TrinomialEngine trinomialEngine;

    /**
     * Generate convergence table comparing binomial and trinomial trees.
     */
    public ModelComparisonResult convergenceTable(OptionRequest request) {
        long startTime = System.currentTimeMillis();

        List<ConvergencePoint> table = trinomialEngine.convergenceTable(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Model comparison completed: {} points, elapsed={}ms", table.size(), elapsed);

        return ModelComparisonResult.builder()
                .convergenceTable(table)
                .description("Convergence comparison of Binomial (CRR) and Trinomial (Kamrad-Ritchken) " +
                        "tree methods at various step counts. Both methods should converge to the " +
                        "Black-Scholes price for European options.")
                .build();
    }
}
