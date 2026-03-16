package com.quantpricing.engine;

import com.quantpricing.domain.OptionType;
import com.quantpricing.domain.request.KouRequest;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.exception.PricingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kou (2002) double-exponential jump-diffusion model engine.
 *
 * The Kou model uses double-exponential distributed jumps:
 * dS/S = (r - lambda*zeta)*dt + sigma*dW + d(sum_{i=1}^{N(t)} (V_i - 1))
 *
 * where jump size Y = V - 1 has a double exponential (asymmetric Laplace) distribution:
 * f_Y(y) = p * eta1 * exp(-eta1 * y) * 1_{y >= 0} + (1-p) * eta2 * exp(eta2 * y) * 1_{y < 0}
 *
 * Parameters:
 * - lambda: jump intensity
 * - p: probability of upward jump (0 < p < 1)
 * - eta1: rate of upward jump exponential (eta1 > 1 for finite mean)
 * - eta2: rate of downward jump exponential (eta2 > 0)
 *
 * The model is priced using characteristic function and Gil-Pelaez inversion.
 */
@Component
@Slf4j
public class KouEngine {

    private static final int NUM_INTEGRATION_POINTS = 200;
    private static final double INTEGRATION_STEP = 0.01;

    /**
     * Price a European option using the Kou model.
     */
    public PricingResult price(KouRequest request) {
        log.debug("Kou pricing: S={}, K={}, sigma={}, T={}, lambda={}, p={}, eta1={}, eta2={}",
                request.getSpot(), request.getStrike(), request.getSigma(),
                request.getTimeToExpiry(), request.getLambda(),
                request.getP(), request.getEta1(), request.getEta2());

        validateInput(request);

        double price = calculatePrice(
                request.getSpot(),
                request.getStrike(),
                request.getRate(),
                request.getSigma(),
                request.getTimeToExpiry(),
                request.getType(),
                request.getLambda(),
                request.getP(),
                request.getEta1(),
                request.getEta2()
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lambda", request.getLambda());
        metadata.put("p", request.getP());
        metadata.put("eta1", request.getEta1());
        metadata.put("eta2", request.getEta2());

        return PricingResult.builder()
                .price(price)
                .model("Kou-Double-Exponential")
                .metadata(metadata)
                .build();
    }

    /**
     * Calculate option price using Gil-Pelaez Fourier inversion with Kou characteristic function.
     */
    private double calculatePrice(double S, double K, double r, double sigma, double T,
                                  OptionType type, double lambda, double p, double eta1, double eta2) {

        if (S <= 0 || K <= 0) {
            throw new PricingException("Spot and strike must be positive");
        }

        double lnK = Math.log(K);

        // Calculate call price using Gil-Pelaez
        // C = S * P1 - K * exp(-r*T) * P2
        double P1 = 0.5 + (1.0 / Math.PI) * integrateP(S, K, r, sigma, T, lambda, p, eta1, eta2, 1);
        double P2 = 0.5 + (1.0 / Math.PI) * integrateP(S, K, r, sigma, T, lambda, p, eta1, eta2, 2);

        double callPrice = S * P1 - K * Math.exp(-r * T) * P2;

        if (type == OptionType.PUT) {
            // Put-call parity
            return callPrice - S + K * Math.exp(-r * T);
        }

        return Math.max(0, callPrice);
    }

    /**
     * Numerical integration for P1 or P2.
     */
    private double integrateP(double S, double K, double r, double sigma, double T,
                              double lambda, double p, double eta1, double eta2, int j) {
        double lnK = Math.log(K);
        double sum = 0.0;

        for (int n = 1; n <= NUM_INTEGRATION_POINTS; n++) {
            double phi = n * INTEGRATION_STEP;

            Complex charFunc = characteristicFunction(phi, S, r, sigma, T, lambda, p, eta1, eta2, j);

            // exp(-i*phi*ln(K)) / (i*phi)
            Complex expTerm = new Complex(0, -phi * lnK).exp();
            Complex iPhi = new Complex(0, phi);
            Complex integrand = charFunc.multiply(expTerm).divide(iPhi);

            sum += integrand.real() * INTEGRATION_STEP;
        }

        return sum;
    }

    /**
     * Kou characteristic function.
     *
     * phi_X(u) = exp(i*u*(r - 0.5*sigma^2 - lambda*kappa_bar)*T
     *            - 0.5*sigma^2*u^2*T
     *            + lambda*T*(p*eta1/(eta1-i*u) + (1-p)*eta2/(eta2+i*u) - 1))
     *
     * where kappa_bar = p*eta1/(eta1-1) + (1-p)*eta2/(eta2+1) - 1
     *
     * For P1/P2 we use a modified characteristic function similar to Heston.
     */
    private Complex characteristicFunction(double phi, double S, double r, double sigma, double T,
                                           double lambda, double p, double eta1, double eta2, int j) {

        // kappa_bar = E[V-1] = p*eta1/(eta1-1) + (1-p)*eta2/(eta2+1) - 1
        double kappaBar = p * eta1 / (eta1 - 1) + (1 - p) * eta2 / (eta2 + 1) - 1;

        Complex iPhi = new Complex(0, phi);

        // For j=1, we compute characteristic function of X + ln(S) where X ~ P*
        // For j=2, we compute characteristic function of X + ln(S) where X ~ Q
        double u_j = (j == 1) ? 1.0 : 0.0;

        // Adjust phi for P1 calculation (change of measure)
        Complex phiAdjusted = (j == 1) ? new Complex(phi, -1) : new Complex(phi, 0);

        // Jump component: p*eta1/(eta1-i*phi) + (1-p)*eta2/(eta2+i*phi) - 1
        Complex eta1MinusIPhi = new Complex(eta1, 0).subtract(iPhi);
        Complex eta2PlusIPhi = new Complex(eta2, 0).add(iPhi);

        Complex jumpTerm1 = new Complex(p * eta1, 0).divide(eta1MinusIPhi);
        Complex jumpTerm2 = new Complex((1 - p) * eta2, 0).divide(eta2PlusIPhi);
        Complex jumpPart = jumpTerm1.add(jumpTerm2).subtract(1);

        // Full exponent for standard characteristic function
        // psi(u) = i*u*(r - 0.5*sigma^2 - lambda*kappa_bar) - 0.5*sigma^2*u^2 + lambda*jumpPart
        double drift = r - 0.5 * sigma * sigma - lambda * kappaBar;

        Complex exponent;
        if (j == 1) {
            // For P1: use change of measure
            // phi_1(phi) = phi_0(phi - i) / phi_0(-i)
            // where phi_0 is the characteristic function under Q

            Complex phi1MinusI = new Complex(phi, -1);
            Complex eta1MinusPhi1 = new Complex(eta1, 1); // eta1 - i*(phi - i) = eta1 + 1 - i*phi
            eta1MinusPhi1 = new Complex(eta1 + 1, -phi);
            Complex eta2PlusPhi1 = new Complex(eta2 - 1, phi); // eta2 + i*(phi - i) = eta2 - 1 + i*phi

            // Check for pole: eta1 - 1 > 0 required
            if (eta1 <= 1) {
                throw new PricingException("eta1 must be > 1 for finite moment");
            }

            Complex jumpTerm1Adj = new Complex(p * eta1, 0).divide(eta1MinusPhi1);
            Complex jumpTerm2Adj = new Complex((1 - p) * eta2, 0).divide(eta2PlusPhi1);
            Complex jumpPartAdj = jumpTerm1Adj.add(jumpTerm2Adj).subtract(1);

            // Drift adjustment for measure change
            double driftAdj = r + sigma * sigma - lambda * kappaBar;

            exponent = iPhi.multiply(driftAdj * T + Math.log(S))
                    .subtract(new Complex(0.5 * sigma * sigma * (phi * phi + phi), 0).multiply(T))
                    .add(jumpPartAdj.multiply(lambda * T));
        } else {
            // For P2: standard Q measure
            exponent = iPhi.multiply(drift * T + Math.log(S))
                    .subtract(new Complex(0.5 * sigma * sigma * phi * phi * T, 0))
                    .add(jumpPart.multiply(lambda * T));
        }

        return exponent.exp();
    }

    private void validateInput(KouRequest request) {
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
        if (request.getP() == null || request.getP() <= 0 || request.getP() >= 1) {
            throw new PricingException("Probability p must be between 0 and 1 (exclusive)");
        }
        if (request.getEta1() == null || request.getEta1() <= 1) {
            throw new PricingException("Upward jump rate eta1 must be > 1 for finite mean");
        }
        if (request.getEta2() == null || request.getEta2() <= 0) {
            throw new PricingException("Downward jump rate eta2 must be positive");
        }
        if (request.getType() == null) {
            throw new PricingException("Option type must be specified");
        }
    }
}
