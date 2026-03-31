package org.familydirectory.assets.lambda.function.api.graph;

import com.fasterxml.uuid.impl.UUIDUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.familydirectory.assets.ddb.models.family.FamilyRecord;
import org.familydirectory.assets.ddb.models.member.IMemberRecord;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.alg.lca.NaiveLCAFinder;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.AsUnmodifiableGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableSet;

public final
class FamilyTree {
    private final Graph<FamilyRecord, DefaultEdge> graph;
    private final NaiveLCAFinder<FamilyRecord, DefaultEdge> naiveLCAFinder;
    private final BFSShortestPath<FamilyRecord, DefaultEdge> bfsShortestPath;
    private final MemberRecord caller;
    private final FamilyRecord callerPseudoVertex;
    private final Map<IMemberRecord, Set<Relationship>> relationshipCache = new HashMap<>();

    public FamilyTree(final @NotNull DirectedAcyclicGraph<FamilyRecord, DefaultEdge> graph, final @NotNull MemberRecord caller) {
        super();
        this.graph = new AsUnmodifiableGraph<>(graph);
        this.naiveLCAFinder = new NaiveLCAFinder<>(this.graph);
        this.bfsShortestPath = new BFSShortestPath<>(this.graph);
        this.caller = requireNonNull(caller);
        this.callerPseudoVertex = getPseudoVertex(this.caller.familyId());
    }

    private static FamilyRecord getPseudoVertex(UUID familyId) {
        return new FamilyRecord(familyId, UUIDUtil.maxUUID(), null, Collections.emptySet());
    }
    private static IMemberRecord getPseudoMember(UUID id, UUID familyId) {
        return new IMemberRecord() {
            @Override
            public
            UUID id () {
                return id;
            }
            @Override
            public
            UUID familyId () {
                return familyId;
            }
            @Override
            public
            boolean equals (final Object o) {
                return IMemberRecord.equals(this, o);
            }
            @Override
            public
            int hashCode () {
                return IMemberRecord.hashCode(this);
            }
        };
    }

    public
    Set<Relationship> getRelationships(final IMemberRecord relative) {
        return relationshipCache.computeIfAbsent(relative, target -> {
            final var targetPseudoVertex = getPseudoVertex(target.familyId());
            final var lca = naiveLCAFinder.getLCA(callerPseudoVertex, targetPseudoVertex);

            final int edgesToCallerFromLCA = bfsShortestPath.getPath(lca, callerPseudoVertex)
                                                            .getLength();
            final int edgesToTargetFromLCA = bfsShortestPath.getPath(lca, targetPseudoVertex)
                                                            .getLength();

            final boolean isInLawByCaller = caller.isInLaw() && edgesToTargetFromLCA <= edgesToCallerFromLCA;
            final boolean isInLaw = isInLawByCaller || target.isInLaw();
            var relationships = Relationship.fromEdges(edgesToCallerFromLCA, edgesToTargetFromLCA, isInLaw);
            if (isInLawByCaller) {
                relationships = relationships.filter(r -> r.getInLaws() == Relationship.InLaw.ONLY);
            }
            return relationships.collect(toUnmodifiableSet());
        });
    }

    public Set<IMemberRecord> getRelatives(final Relationship relationship) {
        return this.graph.vertexSet()
                         .stream()
                         .flatMap(familyRecord -> {
                             final var builder = Stream.<IMemberRecord>builder();
                             builder.add(getPseudoMember(familyRecord.id(), familyRecord.id()));
                             if (familyRecord.spouse() != null) {
                                 builder.add(getPseudoMember(familyRecord.spouse(), familyRecord.id()));
                             }
                             return builder.build();
                         })
                         .filter(pseudoMember -> this.getRelationships(pseudoMember).contains(relationship))
                         .collect(toUnmodifiableSet());
    }
}
