package com.quantpricing.service;

import com.quantpricing.domain.request.AsianOptionRequest;
import com.quantpricing.domain.request.BarrierOptionRequest;
import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.GreeksResult;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.engine.BinomialEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer for Binomial tree pricing operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BinomialService {

    private final BinomialEngine engine;

    /**
     * Price a standard European or American option.
     */
    public PricingResult price(OptionRequest request) {
        long startTime = System.currentTimeMillis();

        PricingResult result = engine.price(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Binomial pricing completed: price={}, style={}, elapsed={}ms",
                result.getPrice(), request.getStyle(), elapsed);

        return result;
    }

    /**
     * Price a barrier option.
     */
    public PricingResult priceBarrier(BarrierOptionRequest request) {
        long startTime = System.currentTimeMillis();

        PricingResult result = engine.priceBarrier(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Binomial barrier option pricing completed: price={}, barrierType={}, elapsed={}ms",
                result.getPrice(), request.getBarrierType(), elapsed);

        return result;
    }

    /**
     * Price an Asian option using path enumeration.
     */
    public PricingResult priceAsian(AsianOptionRequest request) {
        long startTime = System.currentTimeMillis();

        PricingResult result = engine.priceAsian(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Binomial Asian option pricing completed: price={}, steps={}, elapsed={}ms",
                result.getPrice(), request.getMonitoringSteps(), elapsed);

        return result;
    }

    /**
     * Calculate Greeks using finite differences.
     */
    public GreeksResult greeks(OptionRequest request) {
        long startTime = System.currentTimeMillis();

        GreeksResult result = engine.greeks(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Binomial Greeks calculated: delta={}, gamma={}, elapsed={}ms",
                result.getDelta(), result.getGamma(), elapsed);

        return result;
    }
}
