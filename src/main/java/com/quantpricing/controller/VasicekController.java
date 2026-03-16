package com.quantpricing.controller;

import com.quantpricing.domain.request.VasicekRequest;
import com.quantpricing.domain.response.YieldCurveResult;
import com.quantpricing.service.VasicekService;
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
 * REST controller for Vasicek interest rate model.
 */
@RestController
@RequestMapping("/api/v1/vasicek")
@Validated
@Slf4j
@RequiredArgsConstructor
public class VasicekController {

    private final VasicekService service;

    /**
     * Generate yield curve using Vasicek model.
     */
    @PostMapping("/yield-curve")
    public ResponseEntity<YieldCurveResult> yieldCurve(@Valid @RequestBody VasicekRequest request) {
        log.debug("Vasicek yield curve request: {}", request);
        return ResponseEntity.ok(service.yieldCurve(request));
    }
}
