package com.quantpricing.controller;

import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.ModelComparisonResult;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.service.CompareService;
import com.quantpricing.service.TrinomialService;
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
 * REST controller for Trinomial tree option pricing.
 */
@RestController
@RequestMapping("/api/v1/trinomial")
@Validated
@Slf4j
@RequiredArgsConstructor
public class TrinomialController {

    private final TrinomialService trinomialService;
    private final CompareService compareService;

    /**
     * Price an option using trinomial tree.
     */
    @PostMapping("/price")
    public ResponseEntity<PricingResult> price(@Valid @RequestBody OptionRequest request) {
        log.debug("Trinomial price request: {}", request);
        return ResponseEntity.ok(trinomialService.price(request));
    }

    /**
     * Generate convergence comparison table.
     */
    @PostMapping("/convergence")
    public ResponseEntity<ModelComparisonResult> convergence(@Valid @RequestBody OptionRequest request) {
        log.debug("Convergence table request: {}", request);
        return ResponseEntity.ok(compareService.convergenceTable(request));
    }
}
