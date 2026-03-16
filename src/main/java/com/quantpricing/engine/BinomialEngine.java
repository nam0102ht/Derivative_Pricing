package com.quantpricing.engine;

import com.quantpricing.domain.BarrierType;
import com.quantpricing.domain.OptionStyle;
import com.quantpricing.domain.OptionType;
import com.quantpricing.domain.request.AsianOptionRequest;
import com.quantpricing.domain.request.BarrierOptionRequest;
import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.GreeksResult;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.exception.PricingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Binomial tree option pricing engine using Cox-Ross-Rubinstein (CRR) parameterization.
 * Supports European, American, Barrier, and Asian options.
 */
@Component
@Slf4j
public class BinomialEngine implements PricingEngine {

    private static final int DEFAULT_STEPS = 200;
    private static final int MAX_ASIAN_STEPS = 15;
    private static final double SPOT_BUMP = 0.01;
    private static final double VOL_BUMP = 0.001;
    private static final double RATE_BUMP = 0.0001;
    private static final double TIME_BUMP = 1.0 / 365.0;

    @Override
    public PricingResult price(OptionRequest request) {
        log.debug("Binomial pricing: S={}, K={}, sigma={}, T={}, style={}",
                request.getSpot(), request.getStrike(), request.getSigma(),
                request.getTimeToExpiry(), request.getStyle());

        validateInput(request);

        double price = calculateBinomialPrice(
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
                .model("Binomial-CRR")
                .metadata(Map.of("steps", DEFAULT_STEPS))
                .build();
    }

    /**
     * Price with custom number of steps.
     */
    public double priceWithSteps(OptionRequest request, int steps) {
        return calculateBinomialPrice(
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
     * Price barrier options.
     */
    public PricingResult priceBarrier(BarrierOptionRequest request) {
        log.debug("Barrier option pricing: barrier={}, type={}",
                request.getBarrierLevel(), request.getBarrierType());

        validateInput(request);

        if (request.getBarrierLevel() == null || request.getBarrierLevel() <= 0) {
            throw new PricingException("Barrier level must be positive");
        }
        if (request.getBarrierType() == null) {
            throw new PricingException("Barrier type must be specified");
        }

        double price = calculateBarrierPrice(
                request.getSpot(),
                request.getStrike(),
                request.getRate(),
                request.getSigma(),
                request.getTimeToExpiry(),
                request.getType(),
                request.getBarrierLevel(),
                request.getBarrierType(),
                DEFAULT_STEPS
        );

        return PricingResult.builder()
                .price(price)
                .model("Binomial-Barrier")
                .metadata(Map.of(
                        "steps", DEFAULT_STEPS,
                        "barrierLevel", request.getBarrierLevel(),
                        "barrierType", request.getBarrierType().toString()
                ))
                .build();
    }

    /**
     * Price Asian options using path enumeration (non-recombining tree).
     */
    public PricingResult priceAsian(AsianOptionRequest request) {
        int steps = request.getMonitoringSteps();
        if (steps > MAX_ASIAN_STEPS) {
            throw new PricingException("Asian option monitoring steps cannot exceed " + MAX_ASIAN_STEPS);
        }

        log.debug("Asian option pricing: monitoringSteps={}", steps);

        validateInput(request);

        double price = calculateAsianPrice(
                request.getSpot(),
                request.getStrike(),
                request.getRate(),
                request.getSigma(),
                request.getTimeToExpiry(),
                request.getType(),
                steps
        );

        return PricingResult.builder()
                .price(price)
                .model("Binomial-Asian")
                .metadata(Map.of("monitoringSteps", steps))
                .build();
    }

    /**
     * Calculate Greeks using finite differences.
     */
    public GreeksResult greeks(OptionRequest request) {
        log.debug("Calculating Greeks via finite differences");

        validateInput(request);

        double S = request.getSpot();
        double K = request.getStrike();
        double r = request.getRate();
        double sigma = request.getSigma();
        double T = request.getTimeToExpiry();
        OptionType type = request.getType();
        OptionStyle style = request.getStyle();

        // Base price
        double price = calculateBinomialPrice(S, K, r, sigma, T, type, style, DEFAULT_STEPS);

        // Delta: dV/dS
        double priceUp = calculateBinomialPrice(S + SPOT_BUMP, K, r, sigma, T, type, style, DEFAULT_STEPS);
        double priceDown = calculateBinomialPrice(S - SPOT_BUMP, K, r, sigma, T, type, style, DEFAULT_STEPS);
        double delta = (priceUp - priceDown) / (2 * SPOT_BUMP);

        // Gamma: d2V/dS2
        double gamma = (priceUp - 2 * price + priceDown) / (SPOT_BUMP * SPOT_BUMP);

        // Vega: dV/dsigma (per 1% move)
        double priceVolUp = calculateBinomialPrice(S, K, r, sigma + VOL_BUMP, T, type, style, DEFAULT_STEPS);
        double priceVolDown = calculateBinomialPrice(S, K, r, sigma - VOL_BUMP, T, type, style, DEFAULT_STEPS);
        double vega = (priceVolUp - priceVolDown) / (2 * VOL_BUMP) / 100.0;

        // Rho: dV/dr (per 1% move)
        double priceRateUp = calculateBinomialPrice(S, K, r + RATE_BUMP, sigma, T, type, style, DEFAULT_STEPS);
        double priceRateDown = calculateBinomialPrice(S, K, r - RATE_BUMP, sigma, T, type, style, DEFAULT_STEPS);
        double rho = (priceRateUp - priceRateDown) / (2 * RATE_BUMP) / 100.0;

        // Theta: dV/dT (per day)
        double priceTimeUp = calculateBinomialPrice(S, K, r, sigma, T + TIME_BUMP, type, style, DEFAULT_STEPS);
        double priceTimeDown = T > TIME_BUMP ?
                calculateBinomialPrice(S, K, r, sigma, T - TIME_BUMP, type, style, DEFAULT_STEPS) : price;
        double theta = T > TIME_BUMP ?
                (priceTimeDown - priceTimeUp) / (2 * TIME_BUMP) / 365.0 :
                (price - priceTimeUp) / TIME_BUMP / 365.0;

        return GreeksResult.builder()
                .price(price)
                .delta(delta)
                .gamma(gamma)
                .theta(theta)
                .vega(vega)
                .rho(rho)
                .build();
    }

    private double calculateBinomialPrice(double S, double K, double r, double sigma,
                                          double T, OptionType type, OptionStyle style, int N) {
        double dt = T / N;
        double u = Math.exp(sigma * Math.sqrt(dt));
        double d = 1.0 / u;
        double discount = Math.exp(-r * dt);
        double p = (Math.exp(r * dt) - d) / (u - d);

        if (p < 0 || p > 1) {
            throw new PricingException("Invalid risk-neutral probability: " + p);
        }

        // Build asset price tree at maturity
        double[] prices = new double[N + 1];
        for (int i = 0; i <= N; i++) {
            prices[i] = S * Math.pow(u, N - i) * Math.pow(d, i);
        }

        // Calculate option values at maturity
        double[] optionValues = new double[N + 1];
        for (int i = 0; i <= N; i++) {
            optionValues[i] = payoff(prices[i], K, type);
        }

        // Backward induction
        for (int step = N - 1; step >= 0; step--) {
            for (int i = 0; i <= step; i++) {
                double spotAtNode = S * Math.pow(u, step - i) * Math.pow(d, i);
                double continuationValue = discount * (p * optionValues[i] + (1 - p) * optionValues[i + 1]);

                if (style == OptionStyle.AMERICAN) {
                    double intrinsic = payoff(spotAtNode, K, type);
                    optionValues[i] = Math.max(intrinsic, continuationValue);
                } else {
                    optionValues[i] = continuationValue;
                }
            }
        }

        return optionValues[0];
    }

    private double calculateBarrierPrice(double S, double K, double r, double sigma,
                                         double T, OptionType type, double barrier,
                                         BarrierType barrierType, int N) {
        double dt = T / N;
        double u = Math.exp(sigma * Math.sqrt(dt));
        double d = 1.0 / u;
        double discount = Math.exp(-r * dt);
        double p = (Math.exp(r * dt) - d) / (u - d);

        if (p < 0 || p > 1) {
            throw new PricingException("Invalid risk-neutral probability");
        }

        // For knock-in: use in-out parity (knock_in = vanilla - knock_out).
        // We price the corresponding knock-OUT option, then subtract from vanilla at the end.
        double[][] optionValues = new double[N + 1][N + 1];

        // Initialize at maturity — treat knock-in as its knock-out counterpart
        BarrierType effectiveType = barrierType;
        if (barrierType == BarrierType.UP_IN)   effectiveType = BarrierType.UP_OUT;
        if (barrierType == BarrierType.DOWN_IN) effectiveType = BarrierType.DOWN_OUT;

        for (int i = 0; i <= N; i++) {
            double spotAtNode = S * Math.pow(u, N - i) * Math.pow(d, i);
            boolean breached = isBarrierBreached(spotAtNode, barrier, effectiveType);
            optionValues[N][i] = breached ? 0 : payoff(spotAtNode, K, type);
        }

        // Backward induction — always pricing the knock-OUT (then apply parity for knock-in)
        for (int step = N - 1; step >= 0; step--) {
            for (int i = 0; i <= step; i++) {
                double spotAtNode = S * Math.pow(u, step - i) * Math.pow(d, i);
                boolean breached = isBarrierBreached(spotAtNode, barrier, effectiveType);

                if (breached) {
                    optionValues[step][i] = 0;
                } else {
                    optionValues[step][i] = discount *
                            (p * optionValues[step + 1][i] + (1 - p) * optionValues[step + 1][i + 1]);
                }
            }
        }

        // For knock-in: price = vanilla - knock_out (in-out parity)
        if (barrierType == BarrierType.UP_IN || barrierType == BarrierType.DOWN_IN) {
            double vanillaPrice = calculateBinomialPrice(S, K, r, sigma, T, type, OptionStyle.EUROPEAN, N);
            return vanillaPrice - optionValues[0][0];
        }

        return optionValues[0][0];
    }

    private boolean isBarrierBreached(double spot, double barrier, BarrierType type) {
        switch (type) {
            case UP_IN:
            case UP_OUT:
                return spot >= barrier;
            case DOWN_IN:
            case DOWN_OUT:
                return spot <= barrier;
            default:
                return false;
        }
    }

    /**
     * Asian option pricing using full path enumeration.
     * This is exponential in steps, so we cap at MAX_ASIAN_STEPS.
     */
    private double calculateAsianPrice(double S, double K, double r, double sigma,
                                       double T, OptionType type, int N) {
        double dt = T / N;
        double u = Math.exp(sigma * Math.sqrt(dt));
        double d = 1.0 / u;
        double discount = Math.exp(-r * T);
        double p = (Math.exp(r * dt) - d) / (u - d);

        if (p < 0 || p > 1) {
            throw new PricingException("Invalid risk-neutral probability");
        }

        // Enumerate all 2^N paths
        long numPaths = 1L << N;
        double sumPayoffs = 0.0;
        double sumProbs = 0.0;

        for (long path = 0; path < numPaths; path++) {
            double spot = S;
            double sumSpot = S; // Include initial spot in average
            double pathProb = 1.0;
            int upMoves = 0;

            for (int step = 0; step < N; step++) {
                boolean isUp = ((path >> (N - 1 - step)) & 1) == 1;
                if (isUp) {
                    spot *= u;
                    pathProb *= p;
                    upMoves++;
                } else {
                    spot *= d;
                    pathProb *= (1 - p);
                }
                sumSpot += spot;
            }

            double avgSpot = sumSpot / (N + 1);
            double payoffValue = payoff(avgSpot, K, type);
            sumPayoffs += pathProb * payoffValue;
            sumProbs += pathProb;
        }

        return discount * sumPayoffs;
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
