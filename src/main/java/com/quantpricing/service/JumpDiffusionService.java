package com.quantpricing.service;

import com.quantpricing.domain.request.KouRequest;
import com.quantpricing.domain.request.MertonRequest;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.engine.KouEngine;
import com.quantpricing.engine.MertonEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer for jump-diffusion model pricing operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JumpDiffusionService {

    private final MertonEngine mertonEngine;
    private final KouEngine kouEngine;

    /**
     * Price an option using Merton jump-diffusion model.
     */
    public PricingResult priceMerton(MertonRequest request) {
        long startTime = System.currentTimeMillis();

        PricingResult result = mertonEngine.price(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Merton pricing completed: price={}, lambda={}, elapsed={}ms",
                result.getPrice(), request.getLambda(), elapsed);

        return result;
    }

    /**
     * Price an option using Kou double-exponential jump-diffusion model.
     */
    public PricingResult priceKou(KouRequest request) {
        long startTime = System.currentTimeMillis();

        PricingResult result = kouEngine.price(request);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Kou pricing completed: price={}, lambda={}, elapsed={}ms",
                result.getPrice(), request.getLambda(), elapsed);

        return result;
    }
}
