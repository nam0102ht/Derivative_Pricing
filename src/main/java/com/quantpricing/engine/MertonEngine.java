package com.quantpricing.engine;

import com.quantpricing.domain.OptionType;
import com.quantpricing.domain.request.MertonRequest;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.exception.PricingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Merton jump-diffusion model engine.
 *
 * The Merton model adds log-normal jumps to GBM:
 * dS/S = (r - lambda*k)*dt + sigma*dW + (Y-1)*dN
 *
 * where:
 * - lambda: jump intensity (average number of jumps per year)
 * - Y: jump size multiplier, ln(Y) ~ N(muJ, sigmaJ^2)
 * - k = E[Y-1] = exp(muJ + 0.5*sigmaJ^2) - 1
 *
 * The price is computed as an infinite series (truncated at 20 terms):
 * C = sum_{n=0}^{infinity} [exp(-lambda_bar*T) * (lambda_bar*T)^n / n!] * BS(S, K, r_n, sigma_n, T)
 *
 * where:
 * - lambda_bar = lambda * (1 + k) = lambda * exp(muJ + 0.5*sigmaJ^2)
 * - r_n = r - lambda*k + n*(muJ + 0.5*sigmaJ^2)/T
 * - sigma_n = sqrt(sigma^2 + n*sigmaJ^2/T)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MertonEngine {

    private static final int MAX_TERMS = 20;

    private final BlackScholesEngine blackScholesEngine;

    /**
     * Price a European option using the Merton jump-diffusion model.
     */
    public PricingResult price(MertonRequest request) {
        log.debug("Merton pricing: S={}, K={}, sigma={}, T={}, lambda={}, muJ={}, sigmaJ={}",
                request.getSpot(), request.getStrike(), request.getSigma(),
                request.getTimeToExpiry(), request.getLambda(),
                request.getMuJ(), request.getSigmaJ());

        validateInput(request);

        double price = calculatePrice(
                request.getSpot(),
                request.getStrike(),
                request.getRate(),
                request.getSigma(),
                request.getTimeToExpiry(),
                request.getType(),
                request.getLambda(),
                request.getMuJ(),
                request.getSigmaJ()
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lambda", request.getLambda());
        metadata.put("muJ", request.getMuJ());
        metadata.put("sigmaJ", request.getSigmaJ());
        metadata.put("maxTerms", MAX_TERMS);

        return PricingResult.builder()
                .price(price)
                .model("Merton-Jump-Diffusion")
                .metadata(metadata)
                .build();
    }

    /**
     * Calculate option price using the Merton series expansion.
     */
    private double calculatePrice(double S, double K, double r, double sigma, double T,
                                  OptionType type, double lambda, double muJ, double sigmaJ) {

        // k = E[Y-1] = exp(muJ + 0.5*sigmaJ^2) - 1
        double jumpExpectation = Math.exp(muJ + 0.5 * sigmaJ * sigmaJ) - 1;

        // lambda_bar = lambda * (1 + k)
        double lambdaBar = lambda * (1 + jumpExpectation);

        // Precompute exp(-lambda_bar * T)
        double expLambdaBarT = Math.exp(-lambdaBar * T);

        double price = 0.0;
        double poissonProb = expLambdaBarT; // (lambda_bar*T)^0 / 0! * exp(-lambda_bar*T)
        double lambdaBarT = lambdaBar * T;

        for (int n = 0; n <= MAX_TERMS; n++) {
            // r_n = r - lambda*k + n*(muJ + 0.5*sigmaJ^2)/T
            // But using the risk-neutral adjustment:
            // r_n = r - lambda*(exp(muJ + 0.5*sigmaJ^2) - 1) + n*ln(1+k)/T
            double r_n = r - lambda * jumpExpectation + n * (muJ + 0.5 * sigmaJ * sigmaJ) / T;

            // sigma_n = sqrt(sigma^2 + n*sigmaJ^2/T)
            double sigma_n = Math.sqrt(sigma * sigma + n * sigmaJ * sigmaJ / T);

            // BS price with adjusted parameters
            double bsPrice = blackScholesEngine.calculatePrice(S, K, r_n, sigma_n, T, type);

            price += poissonProb * bsPrice;

            // Update Poisson probability for next term
            // P(N=n+1) = P(N=n) * (lambda_bar*T) / (n+1)
            if (n < MAX_TERMS) {
                poissonProb *= lambdaBarT / (n + 1);
            }
        }

        return price;
    }

    private void validateInput(MertonRequest request) {
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
        if (request.getLambda() == null || request.getLambda() <= 0) {
            throw new PricingException("Jump intensity lambda must be positive");
        }
        if (request.getSigmaJ() == null || request.getSigmaJ() <= 0) {
            throw new PricingException("Jump volatility sigmaJ must be positive");
        }
        if (request.getType() == null) {
            throw new PricingException("Option type must be specified");
        }
    }
}
