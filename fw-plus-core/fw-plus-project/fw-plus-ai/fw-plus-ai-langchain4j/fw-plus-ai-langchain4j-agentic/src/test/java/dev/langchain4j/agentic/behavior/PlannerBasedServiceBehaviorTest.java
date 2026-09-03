package dev.langchain4j.agentic.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.planner.PlanningContext;
import org.junit.jupiter.api.Test;

public class PlannerBasedServiceBehaviorTest {

    @Test
    void annotatedPlannerSupplierBuildsAndRunsThroughPublicRootProxy() {
        AnnotatedPlannerRoot root = AgenticServices.plannerBuilder(AnnotatedPlannerRoot.class).build();

        assertEquals("annotated-result", root.run());
    }

    @Test
    void programmaticPlannerSupplierOverridesMissingAnnotation() {
        ProgrammaticPlannerRoot root = AgenticServices.plannerBuilder(ProgrammaticPlannerRoot.class)
                .planner(() -> plannerReturning("programmatic-result"))
                .build();

        assertEquals("programmatic-result", root.run());
    }

    @Test
    void missingPlannerSupplierFailsAtBuildWithConfigurationMessage() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AgenticServices.plannerBuilder(ProgrammaticPlannerRoot.class).build());

        assertTrue(error.getMessage().contains("@PlannerSupplier"));
    }

    private static Planner plannerReturning(final String result) {
        return new Planner() {
            @Override
            public Action nextAction(PlanningContext planningContext) {
                return done(result);
            }
        };
    }

    public interface AnnotatedPlannerRoot {

        @PlannerAgent(outputKey = "result", subAgents = {})
        String run();

        @PlannerSupplier
        static Planner planner() {
            return plannerReturning("annotated-result");
        }
    }

    public interface ProgrammaticPlannerRoot {

        @PlannerAgent(outputKey = "result", subAgents = {})
        String run();
    }
}
