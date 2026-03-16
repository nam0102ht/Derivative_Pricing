package com.quantpricing.service;

import com.quantpricing.domain.request.ImpliedVolRequest;
import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.GreeksResult;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.engine.BlackScholesEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer for Black-Scholes pricing operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BlackScholesService {

    private final BlackScholesEngine engine;

    /**
     * Price an option using Black-Scholes model.
     */
    public PricingResult price(OptionRequest request) {
        long startTime = System.currentTimeMillis();

        PricingResult result = engine.price(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Black-Scholes pricing completed: price={}, elapsed={}ms",
                result.getPrice(), elapsed);

        return result;
    }

    /**
     * Calculate analytical Greeks.
     */
    public GreeksResult greeks(OptionRequest request) {
        long startTime = System.currentTimeMillis();

        GreeksResult result = engine.greeks(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Black-Scholes Greeks calculated: delta={}, gamma={}, elapsed={}ms",
                result.getDelta(), result.getGamma(), elapsed);

        return result;
    }

    /**
     * Calculate implied volatility from market price.
     */
    public double impliedVol(ImpliedVolRequest request) {
        long startTime = System.currentTimeMillis();

        double iv = engine.impliedVol(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Implied volatility calculated: iv={}, elapsed={}ms", iv, elapsed);

        return iv;
    }
}
