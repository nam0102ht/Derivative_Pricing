package com.quantpricing.engine;

import com.quantpricing.domain.OptionType;
import com.quantpricing.domain.request.ImpliedVolRequest;
import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.GreeksResult;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.exception.PricingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Black-Scholes option pricing engine with analytical Greeks and implied volatility solver.
 */
@Component
@Slf4j
public class BlackScholesEngine implements PricingEngine {

    private static final NormalDistribution NORMAL = new NormalDistribution();
    private static final double EPSILON = 1e-10;
    private static final int MAX_ITERATIONS = 100;
    private static final double VOL_LOWER_BOUND = 1e-6;
    private static final double VOL_UPPER_BOUND = 5.0;

    @Override
    @Cacheable(value = "bs-pricing", key = "#request.hashCode()")
    public PricingResult price(OptionRequest request) {
        log.debug("Black-Scholes pricing: S={}, K={}, r={}, sigma={}, T={}, type={}",
                request.getSpot(), request.getStrike(), request.getRate(),
                request.getSigma(), request.getTimeToExpiry(), request.getType());

        validateInput(request);

        double price = calculatePrice(
                request.getSpot(),
                request.getStrike(),
                request.getRate(),
                request.getSigma(),
                request.getTimeToExpiry(),
                request.getType()
        );

        return PricingResult.builder()
                .price(price)
                .model("Black-Scholes")
                .metadata(Map.of(
                        "d1", calculateD1(request.getSpot(), request.getStrike(), request.getRate(),
                                request.getSigma(), request.getTimeToExpiry()),
                        "d2", calculateD2(request.getSpot(), request.getStrike(), request.getRate(),
                                request.getSigma(), request.getTimeToExpiry())
                ))
                .build();
    }

    /**
     * Calculate price with explicit parameters (for reuse by other engines).
     */
    public double calculatePrice(double S, double K, double r, double sigma, double T, OptionType type) {
        if (S <= 0 || K <= 0 || sigma <= 0 || T <= 0) {
            throw new PricingException("Invalid parameters: S, K, sigma, T must be positive");
        }

        double d1 = calculateD1(S, K, r, sigma, T);
        double d2 = d1 - sigma * Math.sqrt(T);

        if (type == OptionType.CALL) {
            return S * N(d1) - K * Math.exp(-r * T) * N(d2);
        } else {
            return K * Math.exp(-r * T) * N(-d2) - S * N(-d1);
        }
    }

    @Cacheable(value = "bs-pricing", key = "'greeks-' + #request.hashCode()")
    public GreeksResult greeks(OptionRequest request) {
        log.debug("Calculating Greeks for: S={}, K={}, sigma={}, T={}",
                request.getSpot(), request.getStrike(), request.getSigma(), request.getTimeToExpiry());

        validateInput(request);

        double S = request.getSpot();
        double K = request.getStrike();
        double r = request.getRate();
        double sigma = request.getSigma();
        double T = request.getTimeToExpiry();
        OptionType type = request.getType();

        double d1 = calculateD1(S, K, r, sigma, T);
        double d2 = d1 - sigma * Math.sqrt(T);
        double sqrtT = Math.sqrt(T);
        double phiD1 = phi(d1);
        double discountFactor = Math.exp(-r * T);

        double price = calculatePrice(S, K, r, sigma, T, type);
        double delta;
        double gamma = phiD1 / (S * sigma * sqrtT);
        double vega = S * phiD1 * sqrtT / 100.0;
        double theta;
        double rho;

        if (type == OptionType.CALL) {
            delta = N(d1);
            theta = (-S * phiD1 * sigma / (2 * sqrtT) - r * K * discountFactor * N(d2)) / 365.0;
            rho = K * T * discountFactor * N(d2) / 100.0;
        } else {
            delta = N(d1) - 1;
            theta = (-S * phiD1 * sigma / (2 * sqrtT) + r * K * discountFactor * N(-d2)) / 365.0;
            rho = -K * T * discountFactor * N(-d2) / 100.0;
        }

        return GreeksResult.builder()
                .price(price)
                .delta(delta)
                .gamma(gamma)
                .theta(theta)
                .vega(vega)
                .rho(rho)
                .build();
    }

    /**
     * Calculate implied volatility using Brent's method.
     */
    public double impliedVol(ImpliedVolRequest request) {
        log.debug("Calculating implied vol for market price: {}", request.getMarketPrice());

        double S = request.getSpot();
        double K = request.getStrike();
        double r = request.getRate();
        double T = request.getTimeToExpiry();
        OptionType type = request.getType();
        double marketPrice = request.getMarketPrice();

        // Validate market price bounds
        double intrinsicValue;
        if (type == OptionType.CALL) {
            intrinsicValue = Math.max(0, S - K * Math.exp(-r * T));
        } else {
            intrinsicValue = Math.max(0, K * Math.exp(-r * T) - S);
        }

        if (marketPrice <= intrinsicValue) {
            throw new PricingException("Market price is below intrinsic value");
        }

        UnivariateFunction objectiveFunction = sigma -> {
            double modelPrice = calculatePrice(S, K, r, sigma, T, type);
            return modelPrice - marketPrice;
        };

        try {
            BrentSolver solver = new BrentSolver(EPSILON, EPSILON);

            // Check if solution exists in bracket
            double lowValue = objectiveFunction.value(VOL_LOWER_BOUND);
            double highValue = objectiveFunction.value(VOL_UPPER_BOUND);

            if (lowValue * highValue > 0) {
                // Try to find valid bracket
                double adjustedLow = VOL_LOWER_BOUND;
                double adjustedHigh = VOL_UPPER_BOUND;

                for (double testVol = 0.01; testVol <= 3.0; testVol += 0.01) {
                    double testValue = objectiveFunction.value(testVol);
                    if (testValue * lowValue < 0) {
                        adjustedHigh = testVol;
                        break;
                    }
                }

                if (objectiveFunction.value(adjustedLow) * objectiveFunction.value(adjustedHigh) > 0) {
                    throw new PricingException("No valid implied volatility found for given market price");
                }

                return solver.solve(MAX_ITERATIONS, objectiveFunction, adjustedLow, adjustedHigh);
            }

            return solver.solve(MAX_ITERATIONS, objectiveFunction, VOL_LOWER_BOUND, VOL_UPPER_BOUND);

        } catch (Exception e) {
            log.warn("Implied vol solver failed: {}", e.getMessage());
            throw new PricingException("Failed to converge on implied volatility: " + e.getMessage(), e);
        }
    }

    private double calculateD1(double S, double K, double r, double sigma, double T) {
        if (S <= 0 || K <= 0) {
            throw new PricingException("Spot and Strike must be positive for log calculation");
        }
        return (Math.log(S / K) + (r + 0.5 * sigma * sigma) * T) / (sigma * Math.sqrt(T));
    }

    private double calculateD2(double S, double K, double r, double sigma, double T) {
        return calculateD1(S, K, r, sigma, T) - sigma * Math.sqrt(T);
    }

    /**
     * Standard normal CDF
     */
    private double N(double x) {
        return NORMAL.cumulativeProbability(x);
    }

    /**
     * Standard normal PDF
     */
    private double phi(double x) {
        return NORMAL.density(x);
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
