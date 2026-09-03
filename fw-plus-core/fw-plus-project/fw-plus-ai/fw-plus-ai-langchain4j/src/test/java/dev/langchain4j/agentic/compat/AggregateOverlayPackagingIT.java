package dev.langchain4j.agentic.compat;

import org.junit.jupiter.api.Test;

/** Runs packaged aggregate owner checks after the package phase. */
class AggregateOverlayPackagingIT {

    @Test
    void verifiesPackagedAggregateOwnerContract() throws Exception {
        AggregateOverlayAllowlistContractTest.packagedAggregateHasExactOwnerBoundaries();
    }
}
