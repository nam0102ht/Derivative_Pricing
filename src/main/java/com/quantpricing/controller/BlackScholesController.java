package com.quantpricing.controller;

import com.quantpricing.domain.request.ImpliedVolRequest;
import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.GreeksResult;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.service.BlackScholesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for Black-Scholes option pricing.
 */
@RestController
@RequestMapping("/api/v1/black-scholes")
@Validated
@Slf4j
@RequiredArgsConstructor
public class BlackScholesController {

    private final BlackScholesService service;

    /**
     * Price an option using Black-Scholes model.
     */
    @PostMapping("/price")
    public ResponseEntity<PricingResult> price(@Valid @RequestBody OptionRequest request) {
        log.debug("Black-Scholes price request: {}", request);
        return ResponseEntity.ok(service.price(request));
    }

    /**
     * Calculate analytical Greeks.
     */
    @PostMapping("/greeks")
    public ResponseEntity<GreeksResult> greeks(@Valid @RequestBody OptionRequest request) {
        log.debug("Black-Scholes Greeks request: {}", request);
        return ResponseEntity.ok(service.greeks(request));
    }

    /**
     * Calculate implied volatility from market price.
     */
    @PostMapping("/implied-vol")
    public ResponseEntity<Map<String, Double>> impliedVol(@Valid @RequestBody ImpliedVolRequest request) {
        log.debug("Implied volatility request: {}", request);
        double iv = service.impliedVol(request);
        return ResponseEntity.ok(Map.of("impliedVolatility", iv));
    }
}
