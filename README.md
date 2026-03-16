# Quantitative Option Pricing API

A production-quality Spring Boot REST API implementing option pricing models from quantitative finance (MScFE 620 — Derivative Pricing).

## Tech Stack

- Java 21 + Spring Boot 3.3.5
- Apache Commons Math 3.6.1 (NormalDistribution, BrentSolver, SimplexOptimizer)
- Caffeine 3.x (in-memory caching)
- Lombok

## Models Implemented

| Module | Model | Method |
|--------|-------|--------|
| M1/M2 | Binomial tree (European, American, Barrier, Asian) | CRR backward induction |
| M3 | Trinomial tree | Kamrad-Ritchken, convergence table |
| M4 | Black-Scholes closed form + Greeks + implied vol | Analytical / Brent solver |
| M4 | Vasicek interest rate model | Affine term structure |
| M6 | Heston stochastic volatility | Gil-Pelaez Fourier inversion |
| M7 | Merton jump-diffusion | Weighted BS series (20 terms) |
| M7 | Kou double-exponential | Characteristic function + Fourier |
| — | Monte Carlo GBM | Async, job-based polling |

## Getting Started

```bash
./gradlew bootRun
```

The API starts on `http://localhost:8080`.

## API Reference

### Black-Scholes

#### Price
```
POST /api/v1/black-scholes/price
```
```json
{
  "spot": 100,
  "strike": 100,
  "rate": 0.05,
  "sigma": 0.2,
  "timeToExpiry": 1.0,
  "type": "CALL",
  "style": "EUROPEAN"
}
```
Response: `{ "price": 10.4506, "model": "Black-Scholes", "metadata": { "d1": 0.35, "d2": 0.15 } }`

#### Greeks
```
POST /api/v1/black-scholes/greeks
```
Same body as above. Response: `{ "price", "delta", "gamma", "theta", "vega", "rho" }`

#### Implied Volatility
```
POST /api/v1/black-scholes/implied-vol
```
```json
{
  "spot": 100,
  "strike": 100,
  "rate": 0.05,
  "timeToExpiry": 1.0,
  "type": "CALL",
  "marketPrice": 10.45
}
```

---

### Binomial Tree

#### Vanilla (European / American)
```
POST /api/v1/binomial/price
```
Same body as Black-Scholes. Set `"style": "AMERICAN"` for American options.

#### Barrier Option
```
POST /api/v1/binomial/price/barrier
```
```json
{
  "spot": 100, "strike": 100, "rate": 0.05, "sigma": 0.2,
  "timeToExpiry": 1.0, "type": "CALL",
  "barrierLevel": 120,
  "barrierType": "UP_OUT"
}
```
`barrierType`: `UP_IN` | `UP_OUT` | `DOWN_IN` | `DOWN_OUT`

> Knock-in pricing uses in-out parity: `knock_in = vanilla − knock_out`

#### Asian Option
```
POST /api/v1/binomial/price/asian
```
```json
{
  "spot": 100, "strike": 100, "rate": 0.05, "sigma": 0.2,
  "timeToExpiry": 1.0, "type": "CALL",
  "monitoringSteps": 12
}
```
> Max `monitoringSteps` = 15 (path enumeration is O(2^N))

#### Greeks (finite differences)
```
POST /api/v1/binomial/greeks
```

---

### Trinomial Tree

```
POST /api/v1/trinomial/price
POST /api/v1/trinomial/convergence
```
`/convergence` returns a table comparing binomial vs trinomial prices for steps = [10, 25, 50, 100, 200].

---

### Vasicek Bond Pricing

```
POST /api/v1/vasicek/yield-curve
```
```json
{
  "r0": 0.03,
  "kappa": 0.5,
  "theta": 0.05,
  "sigma": 0.01
}
```
Returns yields for maturities: 0.25, 0.5, 1, 2, 3, 5, 7, 10, 15, 20, 30 years.

Formula: `P(0,T) = exp(A(τ) − B(τ)·r₀)`

---

### Heston Stochastic Volatility

#### Price
```
POST /api/v1/heston/price
```
```json
{
  "spot": 80, "strike": 80, "rate": 0.055, "timeToExpiry": 0.25,
  "v0": 0.032, "kappa": 1.85, "theta": 0.045, "xi": 0.30, "rho": -0.30,
  "type": "CALL"
}
```

#### Calibrate
```
POST /api/v1/heston/calibrate
```
```json
{
  "spot": 80, "rate": 0.055,
  "calibrationPoints": [
    { "strike": 75, "maturity": 0.25, "marketIV": 0.38 },
    { "strike": 80, "maturity": 0.25, "marketIV": 0.35 }
  ]
}
```
Returns calibrated `{v0, kappa, theta, xi, rho}` + RMSE.

---

### Jump-Diffusion

#### Merton
```
POST /api/v1/jump-diffusion/merton
```
```json
{
  "spot": 80, "strike": 80, "rate": 0.055, "sigma": 0.35,
  "timeToExpiry": 0.25, "type": "CALL",
  "lambda": 0.75, "muJ": -0.5, "sigmaJ": 0.228
}
```

#### Kou Double-Exponential
```
POST /api/v1/jump-diffusion/kou
```
```json
{
  "spot": 80, "strike": 80, "rate": 0.055, "sigma": 0.35,
  "timeToExpiry": 0.25, "type": "CALL",
  "lambda": 0.75, "p": 0.4, "eta1": 10.0, "eta2": 5.0
}
```
Constraints: `eta1 > 1`, `0 < p < 1`.

---

### Monte Carlo (Async GBM)

#### Submit job
```
POST /api/v1/monte-carlo/submit
```
```json
{
  "spot": 80, "strike": 80, "rate": 0.055, "sigma": 0.35,
  "timeToExpiry": 0.25, "type": "CALL",
  "numPaths": 100000, "numSteps": 252
}
```
Returns immediately with `{ "jobId": "...", "status": "RUNNING" }`.

#### Poll result
```
GET /api/v1/monte-carlo/poll/{jobId}
```
Returns `{ "jobId", "status": "DONE", "price", "stdError", "confidenceInterval": [lower, upper], "elapsedMs" }`.

---

### Model Comparison

```
POST /api/v1/compare/convergence
```
Returns binomial vs trinomial convergence table for a given option.

---

## Reference Parameters (notebook)

| Parameter | Value |
|-----------|-------|
| S₀ | 80 |
| r | 5.5% |
| σ | 35% |
| T | 0.25 yr |
| K (ATM) | 80 |
| ν₀ (Heston) | 0.032 |
| κ | 1.85 |
| θ | 0.045 |
| σᵥ | 0.30 |
| μⱼ (Merton) | −0.5 |
| δⱼ | 0.228 |

## Error Responses

| HTTP | Cause |
|------|-------|
| 400 | Validation failure (missing/invalid fields) |
| 422 | Pricing error (no implied vol, invalid barrier, Feller violation) |
| 500 | Unexpected server error |

```json
{
  "title": "Pricing error",
  "detail": "No valid implied volatility found for given market price",
  "status": 422
}
```

## Caching

Deterministic closed-form results are cached with Caffeine (10 min TTL, max 1000 entries):
- `bs-pricing` — Black-Scholes price and Greeks
- `vasicek-bonds` — Vasicek yield curves

Tree-based and Monte Carlo results are never cached.
