package com.quantpricing.controller;

import com.quantpricing.domain.request.MonteCarloRequest;
import com.quantpricing.domain.response.MonteCarloResult;
import com.quantpricing.service.MonteCarloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for Monte Carlo option pricing.
 */
@RestController
@RequestMapping("/api/v1/monte-carlo")
@Validated
@Slf4j
@RequiredArgsConstructor
public class MonteCarloController {

    private final MonteCarloService service;

    /**
     * Submit a Monte Carlo pricing job.
     * Returns immediately with job ID and RUNNING status.
     */
    @PostMapping("/submit")
    public ResponseEntity<MonteCarloResult> submit(@Valid @RequestBody MonteCarloRequest request) {
        log.debug("Monte Carlo submit request: paths={}, steps={}",
                request.getNumPaths(), request.getNumSteps());
        return ResponseEntity.accepted().body(service.submit(request));
    }

    /**
     * Poll for Monte Carlo job result.
     */
    @GetMapping("/poll/{jobId}")
    public ResponseEntity<MonteCarloResult> poll(@PathVariable UUID jobId) {
        log.debug("Monte Carlo poll request: jobId={}", jobId);
        return ResponseEntity.ok(service.poll(jobId));
    }
}
