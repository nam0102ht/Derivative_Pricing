package com.quantpricing.service;

import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.ConvergencePoint;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.engine.TrinomialEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for Trinomial tree pricing operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrinomialService {

    private final TrinomialEngine engine;

    /**
     * Price an option using trinomial tree.
     */
    public PricingResult price(OptionRequest request) {
        long startTime = System.currentTimeMillis();

        PricingResult result = engine.price(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Trinomial pricing completed: price={}, elapsed={}ms",
                result.getPrice(), elapsed);

        return result;
    }

    /**
     * Generate convergence table comparing binomial and trinomial.
     */
    public List<ConvergencePoint> convergenceTable(OptionRequest request) {
        long startTime = System.currentTimeMillis();

        List<ConvergencePoint> table = engine.convergenceTable(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Convergence table generated: {} points, elapsed={}ms",
                table.size(), elapsed);

        return table;
    }
}
