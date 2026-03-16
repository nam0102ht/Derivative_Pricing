package com.quantpricing.controller;

import com.quantpricing.domain.CalibrationPoint;
import com.quantpricing.domain.request.HestonRequest;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.service.HestonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Heston stochastic volatility model.
 */
@RestController
@RequestMapping("/api/v1/heston")
@Validated
@Slf4j
@RequiredArgsConstructor
public class HestonController {

    private final HestonService service;

    /**
     * Price an option using Heston model.
     */
    @PostMapping("/price")
    public ResponseEntity<PricingResult> price(@Valid @RequestBody HestonRequest request) {
        log.debug("Heston price request: {}", request);
        return ResponseEntity.ok(service.price(request));
    }

    /**
     * Calibrate Heston parameters to market implied volatilities.
     */
    @PostMapping("/calibrate")
    public ResponseEntity<PricingResult> calibrate(
            @RequestBody List<CalibrationPoint> marketData,
            @RequestParam double spot,
            @RequestParam double rate) {

        log.debug("Heston calibration request: {} points, spot={}, rate={}",
                marketData.size(), spot, rate);

        Map<String, Double> calibratedParams = service.calibrate(marketData, spot, rate);

        return ResponseEntity.ok(PricingResult.builder()
                .price(0.0)
                .model("Heston-Calibrated")
                .metadata(Map.of(
                        "v0", calibratedParams.get("v0"),
                        "kappa", calibratedParams.get("kappa"),
                        "theta", calibratedParams.get("theta"),
                        "xi", calibratedParams.get("xi"),
                        "rho", calibratedParams.get("rho"),
                        "objectiveValue", calibratedParams.get("objectiveValue")
                ))
                .build());
    }
}
