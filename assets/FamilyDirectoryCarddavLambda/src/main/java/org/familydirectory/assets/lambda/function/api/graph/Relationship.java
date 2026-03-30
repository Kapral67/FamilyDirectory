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
    FIRST_COUSIN("First Cousin", 1, 1);

    private final String displayLabel;
    private final int edgesToCallerFromLCA;
    private final int edgesToTargetFromLCA;

    public static
    Set<Relationship> fromEdges(int edgesToCallerFromLCA, int edgesToTargetFromLCA) {
        return Arrays.stream(values())
                     .filter(r -> r.getEdgesToCallerFromLCA() == edgesToCallerFromLCA)
                     .filter(r -> r.getEdgesToTargetFromLCA() == edgesToTargetFromLCA)
                     .collect(toUnmodifiableSet());
    }
}
