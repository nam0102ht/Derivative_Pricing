package com.quantpricing.controller;

import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.ModelComparisonResult;
import com.quantpricing.service.CompareService;
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
 * REST controller for model comparison operations.
 */
@RestController
@RequestMapping("/api/v1/compare")
@Validated
@Slf4j
@RequiredArgsConstructor
public class CompareController {

    private final CompareService service;

    /**
     * Generate convergence comparison table between binomial and trinomial trees.
     */
    @PostMapping("/convergence")
    public ResponseEntity<ModelComparisonResult> convergence(@Valid @RequestBody OptionRequest request) {
        log.debug("Model comparison request: {}", request);
        return ResponseEntity.ok(service.convergenceTable(request));
    }
}
