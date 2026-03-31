package org.familydirectory.assets.lambda.function.api.graph;

import java.util.Arrays;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import static java.util.stream.Collectors.toUnmodifiableSet;

/**
 * Note when deleting/renaming enums:
 * - Need to add Relationship::name to a new SYNC table token
 */
@AllArgsConstructor
@Getter
public
enum Relationship {
    GREAT_GRAND_PIBLING("Great Grand Pibling", 4, 1, InLaw.INCLUDED),
    GRAND_PIBLING("Grand Pibling", 3, 1, InLaw.INCLUDED),
    PIBLING("Pibling", 2, 1, InLaw.INCLUDED),
    SIBLING("Sibling", 1, 1, InLaw.EXCLUDED),
    NIBLING("Nibling", 1, 2, InLaw.EXCLUDED),
    FIRST_COUSIN("Cousin", 2, 2, InLaw.EXCLUDED),
    CHILD("Child", 0, 1, InLaw.EXCLUDED),
    GRAND_CHILD("Grand Child", 0, 2, InLaw.EXCLUDED),
    GREAT_GRAND_CHILD("Great Grand Child", 0, 3, InLaw.EXCLUDED);

    private final String displayLabel;
    private final int edgesToCallerFromLCA;
    private final int edgesToTargetFromLCA;
    private final InLaw inLaws;

    public static
    Set<Relationship> fromEdges(final int edgesToCallerFromLCA, final int edgesToTargetFromLCA, final boolean isInLaw) {
        return Arrays.stream(values())
                     .filter(r -> r.shouldInclude(isInLaw))
                     .filter(r -> r.getEdgesToCallerFromLCA() == edgesToCallerFromLCA)
                     .filter(r -> r.getEdgesToTargetFromLCA() == edgesToTargetFromLCA)
                     .collect(toUnmodifiableSet());
    }

    private boolean shouldInclude(boolean isInLaw) {
        return this.inLaws == InLaw.INCLUDED
               || this.inLaws == InLaw.ONLY == isInLaw;
    }

    public enum InLaw {
        ONLY,
        INCLUDED,
        EXCLUDED
    }
}
