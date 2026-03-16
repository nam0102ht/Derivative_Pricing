package com.quantpricing.engine;

import com.quantpricing.domain.request.OptionRequest;
import com.quantpricing.domain.response.PricingResult;

/**
 * Common interface for all option pricing engines.
 */
public interface PricingEngine {
    PricingResult price(OptionRequest request);
}
