package org.familydirectory.assets.lambda.function.api.graph;

import com.fasterxml.uuid.impl.UUIDUtil;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.familydirectory.assets.ddb.models.family.FamilyRecord;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.jgrapht.Graph;
import org.jgrapht.alg.lca.NaiveLCAFinder;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultEdge;
import static java.util.Collections.singleton;

public
enum FamilyGraphUtils {
    ;
    private static FamilyRecord getPseudoVertex(UUID familyId) {
        return new FamilyRecord(familyId, UUIDUtil.maxUUID(), null, Collections.emptySet());
    }

    public static
    Set<Relationship> getRelationships(Graph<FamilyRecord, DefaultEdge> graph, MemberRecord caller, MemberRecord target) {
        if (!caller.id().equals(caller.familyId())) {
            // Caller is In-Law
            return Collections.emptySet();
        }
        final var callerPseudoVertex = getPseudoVertex(caller.familyId());
        final var targetPseudoVertex = getPseudoVertex(target.familyId());
        final var lca = new NaiveLCAFinder<>(graph).getLCA(callerPseudoVertex, targetPseudoVertex);

        final int edgesToCallerFromLCA = BFSShortestPath.findPathBetween(graph, lca, callerPseudoVertex)
                                                        .getLength();
        final int edgesToTargetFromLCA = BFSShortestPath.findPathBetween(graph, lca, targetPseudoVertex)
                                                        .getLength();

        return Relationship.fromEdges(edgesToCallerFromLCA, edgesToTargetFromLCA, target.isInLaw());
    }
}
