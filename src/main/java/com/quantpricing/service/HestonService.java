package com.quantpricing.service;

import com.quantpricing.domain.CalibrationPoint;
import com.quantpricing.domain.request.HestonRequest;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.engine.HestonEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service layer for Heston stochastic volatility model operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HestonService {

    private final HestonEngine engine;

    /**
     * Price an option using Heston model.
     */
    public PricingResult price(HestonRequest request) {
        long startTime = System.currentTimeMillis();

        PricingResult result = engine.price(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Heston pricing completed: price={}, elapsed={}ms",
                result.getPrice(), elapsed);

        return result;
    }

    /**
     * Calibrate Heston parameters to market data.
     */
    public Map<String, Double> calibrate(List<CalibrationPoint> marketData, double spot, double rate) {
        long startTime = System.currentTimeMillis();

        Map<String, Double> params = engine.calibrate(marketData, spot, rate);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Heston calibration completed: v0={}, kappa={}, theta={}, xi={}, rho={}, elapsed={}ms",
                params.get("v0"), params.get("kappa"), params.get("theta"),
                params.get("xi"), params.get("rho"), elapsed);

        return params;
    }
}
