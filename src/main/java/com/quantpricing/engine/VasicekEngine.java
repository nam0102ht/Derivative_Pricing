package com.quantpricing.engine;

import com.quantpricing.domain.request.VasicekRequest;
import com.quantpricing.domain.response.YieldCurveResult;
import com.quantpricing.exception.PricingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vasicek interest rate model engine for zero-coupon bond pricing and yield curves.
 *
 * The Vasicek model: dr = kappa*(theta - r)*dt + sigma*dW
 * - kappa: mean reversion speed
 * - theta: long-term mean rate
 * - sigma: volatility of rate
 * - r0: initial short rate
 */
@Component
@Slf4j
public class VasicekEngine {

    private static final double[] STANDARD_MATURITIES = {0.25, 0.5, 1, 2, 3, 5, 7, 10, 15, 20, 30};

    /**
     * Generate yield curve for standard maturities.
     */
    @Cacheable(value = "vasicek-bonds", key = "#request.hashCode()")
    public YieldCurveResult yieldCurve(VasicekRequest request) {
        log.debug("Vasicek yield curve: r0={}, kappa={}, theta={}, sigma={}",
                request.getR0(), request.getKappa(), request.getTheta(), request.getSigma());

        validateInput(request);

        Map<Double, Double> yields = new LinkedHashMap<>();

        for (double T : STANDARD_MATURITIES) {
            double bondPrice = zeroCouponBondPrice(
                    request.getR0(),
                    request.getKappa(),
                    request.getTheta(),
                    request.getSigma(),
                    T
            );

            double yield = -Math.log(bondPrice) / T;
            yields.put(T, yield);

            log.debug("Maturity={}, P(0,T)={}, yield={}", T, bondPrice, yield);
        }

        return YieldCurveResult.builder()
                .maturitiesToYields(yields)
                .model("Vasicek")
                .build();
    }

    /**
     * Calculate zero-coupon bond price P(0,T) under the Vasicek model.
     *
     * P(0,T) = exp(A(T) - B(T)*r0)
     *
     * where:
     * B(T) = (1 - exp(-kappa*T)) / kappa
     * A(T) = (theta - sigma^2/(2*kappa^2)) * (B(T) - T) - sigma^2 * B(T)^2 / (4*kappa)
     */
    public double zeroCouponBondPrice(double r0, double kappa, double theta, double sigma, double T) {
        if (T <= 0) {
            return 1.0; // Bond maturing now has price 1
        }

        double B = calculateB(kappa, T);
        double A = calculateA(kappa, theta, sigma, T, B);

        return Math.exp(A - B * r0);
    }

    /**
     * Calculate B(tau) = (1 - exp(-kappa*tau)) / kappa
     */
    private double calculateB(double kappa, double tau) {
        if (Math.abs(kappa) < 1e-10) {
            // Limit as kappa -> 0: B(tau) -> tau
            return tau;
        }
        return (1 - Math.exp(-kappa * tau)) / kappa;
    }

    /**
     * Calculate A(tau) in the affine term structure.
     * A(tau) = (theta - sigma^2/(2*kappa^2)) * (B(tau) - tau) - sigma^2 * B(tau)^2 / (4*kappa)
     */
    private double calculateA(double kappa, double theta, double sigma, double tau, double B) {
        if (Math.abs(kappa) < 1e-10) {
            // Limit case for very small kappa
            return -sigma * sigma * tau * tau * tau / 6.0;
        }

        double kappaSquared = kappa * kappa;
        double sigmaSquared = sigma * sigma;

        double term1 = (theta - sigmaSquared / (2 * kappaSquared)) * (B - tau);
        double term2 = sigmaSquared * B * B / (4 * kappa);

        return term1 - term2;
    }

    /**
     * Calculate forward rate f(0,T) = -d/dT ln(P(0,T))
     */
    public double forwardRate(double r0, double kappa, double theta, double sigma, double T) {
        if (T <= 0) {
            return r0;
        }

        // f(0,T) = r0 * exp(-kappa*T) + theta*(1 - exp(-kappa*T))
        //          - sigma^2/(2*kappa^2) * (1 - exp(-kappa*T))^2
        double expKappaT = Math.exp(-kappa * T);
        double oneMinusExp = 1 - expKappaT;

        double term1 = r0 * expKappaT;
        double term2 = theta * oneMinusExp;
        double term3 = (sigma * sigma / (2 * kappa * kappa)) * oneMinusExp * oneMinusExp;

        return term1 + term2 - term3;
    }

    private void validateInput(VasicekRequest request) {
        if (request.getR0() == null) {
            throw new PricingException("Initial rate r0 is required");
        }
        if (request.getKappa() == null || request.getKappa() <= 0) {
            throw new PricingException("Mean reversion speed kappa must be positive");
        }
        if (request.getTheta() == null || request.getTheta() <= 0) {
            throw new PricingException("Long-term mean theta must be positive");
        }
        if (request.getSigma() == null || request.getSigma() <= 0) {
            throw new PricingException("Volatility sigma must be positive");
        }
    }
}
