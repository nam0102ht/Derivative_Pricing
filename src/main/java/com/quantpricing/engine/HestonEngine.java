package com.quantpricing.engine;

import com.quantpricing.domain.CalibrationPoint;
import com.quantpricing.domain.OptionType;
import com.quantpricing.domain.request.HestonRequest;
import com.quantpricing.domain.response.PricingResult;
import com.quantpricing.exception.PricingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Heston stochastic volatility model engine using Gil-Pelaez Fourier inversion.
 *
 * The Heston model:
 * dS = r*S*dt + sqrt(v)*S*dW1
 * dv = kappa*(theta - v)*dt + xi*sqrt(v)*dW2
 * dW1*dW2 = rho*dt
 *
 * Parameters:
 * - v0: initial variance
 * - kappa: mean reversion speed of variance
 * - theta: long-term variance
 * - xi: volatility of variance (vol of vol)
 * - rho: correlation between asset and variance
 */
@Component
@Slf4j
public class HestonEngine {

    private static final int NUM_INTEGRATION_POINTS = 200;
    private static final double INTEGRATION_STEP = 0.01;
    private static final double TOLERANCE = 1e-8;

    /**
     * Price a European option using the Heston model.
     */
    public PricingResult price(HestonRequest request) {
        log.debug("Heston pricing: S={}, K={}, T={}, v0={}, kappa={}, theta={}, xi={}, rho={}",
                request.getSpot(), request.getStrike(), request.getTimeToExpiry(),
                request.getV0(), request.getKappa(), request.getTheta(),
                request.getXi(), request.getRho());

        validateInput(request);
        checkFellerCondition(request.getKappa(), request.getTheta(), request.getXi());

        double price = calculatePrice(
                request.getSpot(),
                request.getStrike(),
                request.getRate(),
                request.getTimeToExpiry(),
                request.getV0(),
                request.getKappa(),
                request.getTheta(),
                request.getXi(),
                request.getRho(),
                request.getType()
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("v0", request.getV0());
        metadata.put("kappa", request.getKappa());
        metadata.put("theta", request.getTheta());
        metadata.put("xi", request.getXi());
        metadata.put("rho", request.getRho());
        metadata.put("fellerConditionMet", 2 * request.getKappa() * request.getTheta() > request.getXi() * request.getXi());

        return PricingResult.builder()
                .price(price)
                .model("Heston")
                .metadata(metadata)
                .build();
    }

    /**
     * Calibrate Heston parameters to market implied volatilities.
     */
    public Map<String, Double> calibrate(List<CalibrationPoint> marketData, double spot, double rate) {
        log.debug("Calibrating Heston model to {} market points", marketData.size());

        if (marketData == null || marketData.isEmpty()) {
            throw new PricingException("Market data required for calibration");
        }

        // Initial guess: [v0, kappa, theta, xi, rho]
        double[] initialGuess = {0.04, 2.0, 0.04, 0.3, -0.5};

        // Define bounds
        double[] lowerBounds = {0.001, 0.01, 0.001, 0.01, -0.99};
        double[] upperBounds = {1.0, 20.0, 1.0, 2.0, 0.99};

        SimplexOptimizer optimizer = new SimplexOptimizer(TOLERANCE, TOLERANCE);

        ObjectiveFunction objective = new ObjectiveFunction(params -> {
            double v0 = params[0];
            double kappa = params[1];
            double theta = params[2];
            double xi = params[3];
            double rho = params[4];

            // Enforce bounds
            if (v0 <= 0 || kappa <= 0 || theta <= 0 || xi <= 0 || rho <= -1 || rho >= 1) {
                return Double.MAX_VALUE;
            }

            double sumSquaredErrors = 0.0;

            for (CalibrationPoint point : marketData) {
                try {
                    double modelPrice = calculatePrice(spot, point.getStrike(), rate, point.getMaturity(),
                            v0, kappa, theta, xi, rho, OptionType.CALL);

                    // Convert market IV to price
                    double marketPrice = blackScholesPrice(spot, point.getStrike(), rate,
                            point.getMarketIV(), point.getMaturity(), OptionType.CALL);

                    double error = modelPrice - marketPrice;
                    sumSquaredErrors += error * error;
                } catch (Exception e) {
                    return Double.MAX_VALUE;
                }
            }

            return sumSquaredErrors;
        });

        try {
            NelderMeadSimplex simplex = new NelderMeadSimplex(5);

            PointValuePair result = optimizer.optimize(
                    MaxEval.unlimited(),
                    objective,
                    GoalType.MINIMIZE,
                    simplex,
                    new InitialGuess(initialGuess)
            );

            double[] optimizedParams = result.getPoint();

            Map<String, Double> calibratedParams = new HashMap<>();
            calibratedParams.put("v0", optimizedParams[0]);
            calibratedParams.put("kappa", optimizedParams[1]);
            calibratedParams.put("theta", optimizedParams[2]);
            calibratedParams.put("xi", optimizedParams[3]);
            calibratedParams.put("rho", optimizedParams[4]);
            calibratedParams.put("objectiveValue", result.getValue());

            log.debug("Calibration complete: {}", calibratedParams);

            return calibratedParams;

        } catch (Exception e) {
            log.error("Calibration failed: {}", e.getMessage());
            throw new PricingException("Heston calibration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate option price using Gil-Pelaez Fourier inversion.
     *
     * Call = S * P1 - K * exp(-r*T) * P2
     *
     * where P_j = 0.5 + (1/pi) * integral_0^inf Re[ exp(-i*phi*ln(K)) * f_j(phi) / (i*phi) ] dphi
     */
    private double calculatePrice(double S, double K, double r, double T,
                                  double v0, double kappa, double theta, double xi, double rho,
                                  OptionType type) {

        if (S <= 0 || K <= 0) {
            throw new PricingException("Spot and strike must be positive");
        }

        double lnK = Math.log(K);

        // Calculate P1 and P2 using numerical integration
        double P1 = 0.5 + (1.0 / Math.PI) * integrateP(S, K, r, T, v0, kappa, theta, xi, rho, 1);
        double P2 = 0.5 + (1.0 / Math.PI) * integrateP(S, K, r, T, v0, kappa, theta, xi, rho, 2);

        double callPrice = S * P1 - K * Math.exp(-r * T) * P2;

        if (type == OptionType.PUT) {
            // Put-call parity: P = C - S + K*exp(-r*T)
            return callPrice - S + K * Math.exp(-r * T);
        }

        return callPrice;
    }

    /**
     * Numerical integration for P1 or P2.
     */
    private double integrateP(double S, double K, double r, double T,
                              double v0, double kappa, double theta, double xi, double rho,
                              int j) {
        double lnK = Math.log(K);
        double sum = 0.0;

        for (int n = 1; n <= NUM_INTEGRATION_POINTS; n++) {
            double phi = n * INTEGRATION_STEP;

            Complex charFunc = characteristicFunction(phi, S, r, T, v0, kappa, theta, xi, rho, j);

            // exp(-i*phi*ln(K)) / (i*phi)
            Complex expTerm = new Complex(0, -phi * lnK).exp();
            Complex iPhi = new Complex(0, phi);
            Complex integrand = charFunc.multiply(expTerm).divide(iPhi);

            sum += integrand.real() * INTEGRATION_STEP;
        }

        return sum;
    }

    /**
     * Heston characteristic function f_j(phi).
     *
     * For j=1: u = 0.5, b = kappa - rho*xi
     * For j=2: u = -0.5, b = kappa
     */
    private Complex characteristicFunction(double phi, double S, double r, double T,
                                           double v0, double kappa, double theta, double xi, double rho,
                                           int j) {
        double u_j = (j == 1) ? 0.5 : -0.5;
        double b_j = (j == 1) ? kappa - rho * xi : kappa;

        Complex iPhi = new Complex(0, phi);
        Complex rhoXiIPhi = new Complex(0, rho * xi * phi);

        // d = sqrt((rho*xi*i*phi - b)^2 - xi^2*(2*u*i*phi - phi^2))
        Complex term1 = rhoXiIPhi.subtract(b_j);
        Complex term1Squared = term1.multiply(term1);

        Complex term2 = new Complex(0, 2 * u_j * phi).subtract(phi * phi).multiply(xi * xi);

        Complex dSquared = term1Squared.subtract(term2);
        Complex d = dSquared.sqrt();

        // g = (b - rho*xi*i*phi + d) / (b - rho*xi*i*phi - d)
        Complex numerator = new Complex(b_j, 0).subtract(rhoXiIPhi).add(d);
        Complex denominator = new Complex(b_j, 0).subtract(rhoXiIPhi).subtract(d);
        Complex g = numerator.divide(denominator);

        // C = r*i*phi*T + (kappa*theta/xi^2) * [(b - rho*xi*i*phi + d)*T - 2*ln((1 - g*exp(d*T))/(1-g))]
        Complex expDT = d.multiply(T).exp();
        Complex oneMinusGExpDT = Complex.ONE.subtract(g.multiply(expDT));
        Complex oneMinusG = Complex.ONE.subtract(g);

        Complex logTerm = oneMinusGExpDT.divide(oneMinusG).log().multiply(2);
        Complex bracketTerm = new Complex(b_j, 0).subtract(rhoXiIPhi).add(d).multiply(T).subtract(logTerm);

        Complex C = iPhi.multiply(r * T).add(bracketTerm.multiply(kappa * theta / (xi * xi)));

        // D = ((b - rho*xi*i*phi + d) / xi^2) * (1 - exp(d*T)) / (1 - g*exp(d*T))
        Complex oneMinusExpDT = Complex.ONE.subtract(expDT);
        Complex D = numerator.divide(xi * xi).multiply(oneMinusExpDT).divide(oneMinusGExpDT);

        // f(phi) = exp(C + D*v0 + i*phi*ln(S))
        Complex lnS = new Complex(Math.log(S), 0);
        Complex exponent = C.add(D.multiply(v0)).add(iPhi.multiply(lnS));

        return exponent.exp();
    }

    /**
     * Simple Black-Scholes for calibration comparison.
     */
    private double blackScholesPrice(double S, double K, double r, double sigma, double T, OptionType type) {
        double d1 = (Math.log(S / K) + (r + 0.5 * sigma * sigma) * T) / (sigma * Math.sqrt(T));
        double d2 = d1 - sigma * Math.sqrt(T);

        double Nd1 = 0.5 * (1 + erf(d1 / Math.sqrt(2)));
        double Nd2 = 0.5 * (1 + erf(d2 / Math.sqrt(2)));

        if (type == OptionType.CALL) {
            return S * Nd1 - K * Math.exp(-r * T) * Nd2;
        } else {
            return K * Math.exp(-r * T) * (1 - Nd2) - S * (1 - Nd1);
        }
    }

    /**
     * Error function approximation.
     */
    private double erf(double x) {
        // Horner form coefficients
        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;

        int sign = x < 0 ? -1 : 1;
        x = Math.abs(x);

        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);

        return sign * y;
    }

    private void checkFellerCondition(double kappa, double theta, double xi) {
        // Feller condition: 2*kappa*theta > xi^2
        boolean fellerMet = 2 * kappa * theta > xi * xi;
        if (!fellerMet) {
            log.warn("Feller condition not satisfied: 2*kappa*theta ({}) <= xi^2 ({}). " +
                            "Variance process may hit zero.",
                    2 * kappa * theta, xi * xi);
        }
    }

    private void validateInput(HestonRequest request) {
        if (request.getSpot() == null || request.getSpot() <= 0) {
            throw new PricingException("Spot price must be positive");
        }
        if (request.getStrike() == null || request.getStrike() <= 0) {
            throw new PricingException("Strike price must be positive");
        }
        if (request.getTimeToExpiry() == null || request.getTimeToExpiry() <= 0) {
            throw new PricingException("Time to expiry must be positive");
        }
        if (request.getRate() == null || request.getRate() < 0) {
            throw new PricingException("Risk-free rate must be non-negative");
        }
        if (request.getV0() == null || request.getV0() <= 0) {
            throw new PricingException("Initial variance v0 must be positive");
        }
        if (request.getKappa() == null || request.getKappa() <= 0) {
            throw new PricingException("Mean reversion kappa must be positive");
        }
        if (request.getTheta() == null || request.getTheta() <= 0) {
            throw new PricingException("Long-term variance theta must be positive");
        }
        if (request.getXi() == null || request.getXi() <= 0) {
            throw new PricingException("Vol of vol xi must be positive");
        }
        if (request.getRho() == null || request.getRho() < -1 || request.getRho() > 1) {
            throw new PricingException("Correlation rho must be between -1 and 1");
        }
        if (request.getType() == null) {
            throw new PricingException("Option type must be specified");
        }
    }
}
