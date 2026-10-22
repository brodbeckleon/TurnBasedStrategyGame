package game.game;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The research tree is a tree: one root per branch, one parent per project, no cycles, and it forks. */
class ResearchTreeTest {

    @Test
    void everyBranchGrowsFromExactlyOneRoot() {
        for (Research.Branch branch : Research.Branch.values()) {
            long roots = java.util.Arrays.stream(Research.values())
                    .filter(research -> research.branch() == branch && research.prerequisite() == null).count();
            assertEquals(1, roots, branch + " has one root");
        }
    }

    @Test
    void everyProjectNeedsAProjectOfItsOwnBranchAndLeadsBackToTheRoot() {
        for (Research research : Research.values()) {
            Research parent = research.prerequisite();
            if (parent != null) {
                assertEquals(research.branch(), parent.branch(), research + " stays in its branch");
                assertTrue(parent.followUps().contains(research));
            }
            // walking up never loops and ends at the branch's root
            Set<Research> seen = new HashSet<>();
            Research step = research;
            while (step.prerequisite() != null) {
                assertTrue(seen.add(step), "no cycle through " + step);
                step = step.prerequisite();
            }
            assertEquals(research.branch().root(), step);
        }
    }

    @Test
    void everyBranchForks() {
        for (Research.Branch branch : Research.Branch.values()) {
            assertTrue(java.util.Arrays.stream(Research.values())
                    .anyMatch(research -> research.branch() == branch && research.followUps().size() >= 2),
                    branch + " splits somewhere");
        }
    }

    @Test
    void everyAbilityAndLockedUnitHasItsProjectInTheTree() {
        for (Ability ability : Ability.values()) {
            assertNotNull(ability.research());
        }
        for (game.enums.UnitEnum type : game.enums.UnitEnum.values()) {
            Research.requiredFor(type).ifPresent(research -> assertNotNull(research.branch()));
        }
    }
}
