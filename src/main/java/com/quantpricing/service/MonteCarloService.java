package com.quantpricing.service;

import com.quantpricing.domain.McStatus;
import com.quantpricing.domain.OptionType;
import com.quantpricing.domain.request.MonteCarloRequest;
import com.quantpricing.domain.response.MonteCarloResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for Monte Carlo option pricing with async execution.
 */
@Service
@Slf4j
public class MonteCarloService {

    private final Map<UUID, MonteCarloResult> resultStore = new ConcurrentHashMap<>();

    /**
     * Submit a Monte Carlo simulation job.
     */
    public MonteCarloResult submit(MonteCarloRequest request) {
        UUID jobId = UUID.randomUUID();

        MonteCarloResult initialResult = MonteCarloResult.builder()
                .jobId(jobId)
                .status(McStatus.RUNNING)
                .build();

        resultStore.put(jobId, initialResult);

        // Start async computation
        runAsync(request, jobId);

        log.info("Monte Carlo job submitted: jobId={}, paths={}, steps={}",
                jobId, request.getNumPaths(), request.getNumSteps());

        return initialResult;
    }

    /**
     * Poll for job result.
     */
    public MonteCarloResult poll(UUID jobId) {
        MonteCarloResult result = resultStore.get(jobId);
        if (result == null) {
            log.warn("Monte Carlo job not found: jobId={}", jobId);
            return MonteCarloResult.builder()
                    .jobId(jobId)
                    .status(McStatus.FAILED)
                    .build();
        }
        return result;
    }

    /**
     * Run Monte Carlo simulation asynchronously.
     */
    @Async("mcExecutor")
    public CompletableFuture<MonteCarloResult> runAsync(MonteCarloRequest request, UUID jobId) {
        long startTime = System.currentTimeMillis();

        try {
            double S = request.getSpot();
            double K = request.getStrike();
            double r = request.getRate();
            double sigma = request.getSigma();
            double T = request.getTimeToExpiry();
            OptionType type = request.getType();
            int numPaths = request.getNumPaths();
            int numSteps = request.getNumSteps();

            double dt = T / numSteps;
            double drift = (r - 0.5 * sigma * sigma) * dt;
            double diffusion = sigma * Math.sqrt(dt);
            double discount = Math.exp(-r * T);

            // Accumulators for mean and variance
            double sumPayoffs = 0.0;
            double sumPayoffsSquared = 0.0;

            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (int path = 0; path < numPaths; path++) {
                double spot = S;

                // Simulate GBM path
                for (int step = 0; step < numSteps; step++) {
                    double z = random.nextGaussian();
                    spot = spot * Math.exp(drift + diffusion * z);
                }

                // Calculate payoff
                double payoff;
                if (type == OptionType.CALL) {
                    payoff = Math.max(0, spot - K);
                } else {
                    payoff = Math.max(0, K - spot);
                }

                sumPayoffs += payoff;
                sumPayoffsSquared += payoff * payoff;
            }

            // Calculate statistics
            double meanPayoff = sumPayoffs / numPaths;
            double variance = (sumPayoffsSquared / numPaths) - (meanPayoff * meanPayoff);
            double stdDev = Math.sqrt(variance);
            double stdError = stdDev / Math.sqrt(numPaths);

            // Discounted price
            double price = discount * meanPayoff;
            double discountedStdError = discount * stdError;

            // 95% confidence interval
            double z95 = 1.96;
            double[] confidenceInterval = new double[]{
                    price - z95 * discountedStdError,
                    price + z95 * discountedStdError
            };

            long elapsed = System.currentTimeMillis() - startTime;

            MonteCarloResult result = MonteCarloResult.builder()
                    .jobId(jobId)
                    .status(McStatus.DONE)
                    .price(price)
                    .stdError(discountedStdError)
                    .confidenceInterval(confidenceInterval)
                    .elapsedMs(elapsed)
                    .build();

            resultStore.put(jobId, result);

            log.info("Monte Carlo completed: jobId={}, price={}, stdError={}, elapsed={}ms",
                    jobId, price, discountedStdError, elapsed);

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Monte Carlo failed: jobId={}, error={}", jobId, e.getMessage(), e);

            MonteCarloResult failedResult = MonteCarloResult.builder()
                    .jobId(jobId)
                    .status(McStatus.FAILED)
                    .elapsedMs(System.currentTimeMillis() - startTime)
                    .build();

            resultStore.put(jobId, failedResult);

            return CompletableFuture.completedFuture(failedResult);
        }
    }

    /**
     * Clear completed jobs older than specified age.
     */
    public void cleanupOldJobs(long maxAgeMs) {
        // In a real system, you would track timestamps and remove old entries
        // For simplicity, this just logs the action
        log.debug("Job cleanup requested with maxAge={}ms", maxAgeMs);
    }
}
