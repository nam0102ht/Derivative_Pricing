package com.quantpricing.controller;

import com.quantpricing.domain.request.AsianOptionRequest;
import com.quantpricing.domain.request.BarrierOptionRequest;
import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.GreeksResult;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.service.BinomialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Binomial tree option pricing.
 */
@RestController
@RequestMapping("/api/v1/binomial")
@Validated
@Slf4j
@RequiredArgsConstructor
public class BinomialController {

    private final BinomialService service;

    /**
     * Price a standard European or American option.
     */
    @PostMapping("/price")
    public ResponseEntity<PricingResult> price(@Valid @RequestBody OptionRequest request) {
        log.debug("Binomial price request: {}", request);
        return ResponseEntity.ok(service.price(request));
    }

    /**
     * Price a barrier option.
     */
    @PostMapping("/price/barrier")
    public ResponseEntity<PricingResult> priceBarrier(@Valid @RequestBody BarrierOptionRequest request) {
        log.debug("Binomial barrier price request: {}", request);
        return ResponseEntity.ok(service.priceBarrier(request));
    }

    /**
     * Price an Asian option.
     */
    @PostMapping("/price/asian")
    public ResponseEntity<PricingResult> priceAsian(@Valid @RequestBody AsianOptionRequest request) {
        log.debug("Binomial Asian price request: {}", request);
        return ResponseEntity.ok(service.priceAsian(request));
    }

    /**
     * Calculate Greeks using finite differences.
     */
    @PostMapping("/greeks")
    public ResponseEntity<GreeksResult> greeks(@Valid @RequestBody OptionRequest request) {
        log.debug("Binomial Greeks request: {}", request);
        return ResponseEntity.ok(service.greeks(request));
    }
}
