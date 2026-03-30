package org.familydirectory.assets.lambda.function.api.graph;

import java.util.Arrays;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import static java.util.stream.Collectors.toUnmodifiableSet;

@AllArgsConstructor
@Getter
public
enum Relationship {
    AUNT_UNCLE("Aunt/Uncle", 2, 1),
    SIBLING("Sibling", 1, 1),
    FIRST_COUSIN("First Cousin", 2, 2);

    private final String displayLabel;
    private final int edgesToCallerFromLCA;
    private final int edgesToTargetFromLCA;

    public static
    Set<Relationship> fromEdges(final int edgesToCallerFromLCA, final int edgesToTargetFromLCA, final boolean isInLaw) {
        return Arrays.stream(values())
                     .filter(r -> !isInLaw || r.getEdgesToCallerFromLCA() > r.getEdgesToTargetFromLCA())
                     .filter(r -> r.getEdgesToCallerFromLCA() == edgesToCallerFromLCA)
                     .filter(r -> r.getEdgesToTargetFromLCA() == edgesToTargetFromLCA)
                     .collect(toUnmodifiableSet());
    }
}
