package com.quantpricing.engine;

import com.quantpricing.domain.OptionStyle;
import com.quantpricing.domain.OptionType;
import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.ConvergencePoint;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.exception.PricingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trinomial tree option pricing engine using Kamrad-Ritchken parameterization.
 * Lambda = sqrt(3/2) for optimal convergence.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TrinomialEngine implements PricingEngine {

    private static final int DEFAULT_STEPS = 200;
    private static final double LAMBDA = Math.sqrt(1.5); // sqrt(3/2) for Kamrad-Ritchken
    private static final int[] CONVERGENCE_STEPS = {10, 25, 50, 100, 200};

    private final BinomialEngine binomialEngine;

    @Override
    public PricingResult price(OptionRequest request) {
        log.debug("Trinomial pricing: S={}, K={}, sigma={}, T={}",
                request.getSpot(), request.getStrike(), request.getSigma(), request.getTimeToExpiry());

        validateInput(request);

        double price = calculateTrinomialPrice(
                request.getSpot(),
                request.getStrike(),
                request.getRate(),
                request.getSigma(),
                request.getTimeToExpiry(),
                request.getType(),
                request.getStyle(),
                DEFAULT_STEPS
        );

        return PricingResult.builder()
                .price(price)
                .model("Trinomial-KR")
                .metadata(Map.of("steps", DEFAULT_STEPS, "lambda", LAMBDA))
                .build();
    }

    /**
     * Price with custom number of steps.
     */
    public double priceWithSteps(OptionRequest request, int steps) {
        return calculateTrinomialPrice(
                request.getSpot(),
                request.getStrike(),
                request.getRate(),
                request.getSigma(),
                request.getTimeToExpiry(),
                request.getType(),
                request.getStyle(),
                steps
        );
    }

    /**
     * Generate convergence table comparing binomial and trinomial prices.
     */
    public List<ConvergencePoint> convergenceTable(OptionRequest request) {
        log.debug("Generating convergence table");

        validateInput(request);

        List<ConvergencePoint> table = new ArrayList<>();

        for (int steps : CONVERGENCE_STEPS) {
            double binomialPrice = binomialEngine.priceWithSteps(request, steps);
            double trinomialPrice = priceWithSteps(request, steps);

            table.add(ConvergencePoint.builder()
                    .steps(steps)
                    .binomialPrice(binomialPrice)
                    .trinomialPrice(trinomialPrice)
                    .build());
        }

        return table;
    }

    private double calculateTrinomialPrice(double S, double K, double r, double sigma,
                                           double T, OptionType type, OptionStyle style, int N) {
        double dt = T / N;
        double u = Math.exp(LAMBDA * sigma * Math.sqrt(dt));
        double m = 1.0;
        double d = 1.0 / u;

        // Calculate risk-neutral probabilities matching first two moments
        // Using Kamrad-Ritchken parameterization
        double nu = r - 0.5 * sigma * sigma;
        double nuDt = nu * dt;
        double sigmaSqrtDt = sigma * Math.sqrt(dt);

        // Match mean and variance
        // E[S(t+dt)/S(t)] = exp(nu*dt) = p_u*u + p_m*m + p_d*d
        // Var[ln(S(t+dt)/S(t))] = sigma^2*dt

        double erfTerm = nuDt / (LAMBDA * sigmaSqrtDt);

        double p_u = 0.5 * ((sigma * sigma * dt + nuDt * nuDt) / (LAMBDA * LAMBDA * sigma * sigma * dt) + erfTerm);
        double p_d = 0.5 * ((sigma * sigma * dt + nuDt * nuDt) / (LAMBDA * LAMBDA * sigma * sigma * dt) - erfTerm);
        double p_m = 1.0 - p_u - p_d;

        // Validate probabilities
        if (p_u < 0 || p_m < 0 || p_d < 0 || p_u > 1 || p_m > 1 || p_d > 1) {
            log.warn("Invalid probabilities: p_u={}, p_m={}, p_d={}. Adjusting timestep may help.", p_u, p_m, p_d);
            throw new PricingException(String.format(
                    "Invalid trinomial probabilities: p_u=%.4f, p_m=%.4f, p_d=%.4f. Try fewer steps or different parameters.",
                    p_u, p_m, p_d));
        }

        double discount = Math.exp(-r * dt);

        // Build trinomial tree
        // At step n, there are 2n+1 nodes: from -n to +n relative movements
        int totalNodes = 2 * N + 1;
        double[] optionValues = new double[totalNodes];
        double[] newValues = new double[totalNodes];

        // Initialize at maturity (step N)
        // Node index i corresponds to (i - N) net up moves
        for (int i = 0; i < totalNodes; i++) {
            int netUpMoves = i - N;
            double spotAtNode = S * Math.pow(u, netUpMoves);
            optionValues[i] = payoff(spotAtNode, K, type);
        }

        // Backward induction
        for (int step = N - 1; step >= 0; step--) {
            int nodesAtStep = 2 * step + 1;

            for (int i = 0; i < nodesAtStep; i++) {
                int netUpMoves = i - step;
                double spotAtNode = S * Math.pow(u, netUpMoves);

                // Map to next step indices
                // Current node at index i (relative position i - step)
                // After up: relative position (i - step + 1) -> index in next step: i + 1 (with offset)
                int indexInNext = i + (N - step - 1);

                double continuationValue = discount * (
                        p_u * optionValues[indexInNext + 2] +
                        p_m * optionValues[indexInNext + 1] +
                        p_d * optionValues[indexInNext]
                );

                if (style == OptionStyle.AMERICAN) {
                    double intrinsic = payoff(spotAtNode, K, type);
                    newValues[i + (N - step)] = Math.max(intrinsic, continuationValue);
                } else {
                    newValues[i + (N - step)] = continuationValue;
                }
            }

            // Copy new values back
            System.arraycopy(newValues, 0, optionValues, 0, totalNodes);
        }

        // Root node is at index N
        return optionValues[N];
    }

    private double payoff(double spot, double strike, OptionType type) {
        if (type == OptionType.CALL) {
            return Math.max(0, spot - strike);
        } else {
            return Math.max(0, strike - spot);
        }
    }

    private void validateInput(OptionRequest request) {
        if (request.getSpot() == null || request.getSpot() <= 0) {
            throw new PricingException("Spot price must be positive");
        }
        if (request.getStrike() == null || request.getStrike() <= 0) {
            throw new PricingException("Strike price must be positive");
        }
        if (request.getSigma() == null || request.getSigma() <= 0) {
            throw new PricingException("Volatility must be positive");
        }
        if (request.getTimeToExpiry() == null || request.getTimeToExpiry() <= 0) {
            throw new PricingException("Time to expiry must be positive");
        }
        if (request.getRate() == null || request.getRate() < 0) {
            throw new PricingException("Risk-free rate must be non-negative");
        }
        if (request.getType() == null) {
            throw new PricingException("Option type must be specified");
        }
    }
}
