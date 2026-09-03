package dev.langchain4j.agentic.compat;

import org.junit.jupiter.api.Test;

/** Runs packaged Core overlay checks after the package phase. */
class CoreOverlayPackagingIT {

    @Test
    void verifiesPackagedCoreOverlayContract() throws Exception {
        CoreOverlayAllowlistContractTest.packagedCoreHasUniqueEntriesAndExactOverlayBytes();
    }
}
