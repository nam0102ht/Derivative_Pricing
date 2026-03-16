package com.quantpricing.service;

import com.quantpricing.domain.request.VasicekRequest;
import com.quantpricing.domain.response.YieldCurveResult;
import com.quantpricing.engine.VasicekEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer for Vasicek interest rate model operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VasicekService {

    private final VasicekEngine engine;

    /**
     * Generate yield curve using Vasicek model.
     */
    public YieldCurveResult yieldCurve(VasicekRequest request) {
        long startTime = System.currentTimeMillis();

        YieldCurveResult result = engine.yieldCurve(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Vasicek yield curve generated: {} maturities, elapsed={}ms",
                result.getMaturitiesToYields().size(), elapsed);

        return result;
    }
}
