package com.quantpricing.controller;

import com.quantpricing.domain.request.KouRequest;
import com.quantpricing.domain.request.MertonRequest;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.service.JumpDiffusionService;
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
 * REST controller for jump-diffusion option pricing models.
 */
@RestController
@RequestMapping("/api/v1/jump-diffusion")
@Validated
@Slf4j
@RequiredArgsConstructor
public class JumpDiffusionController {

    private final JumpDiffusionService service;

    /**
     * Price an option using Merton jump-diffusion model.
     */
    @PostMapping("/merton")
    public ResponseEntity<PricingResult> merton(@Valid @RequestBody MertonRequest request) {
        log.debug("Merton price request: {}", request);
        return ResponseEntity.ok(service.priceMerton(request));
    }

    /**
     * Price an option using Kou double-exponential jump-diffusion model.
     */
    @PostMapping("/kou")
    public ResponseEntity<PricingResult> kou(@Valid @RequestBody KouRequest request) {
        log.debug("Kou price request: {}", request);
        return ResponseEntity.ok(service.priceKou(request));
    }
}
