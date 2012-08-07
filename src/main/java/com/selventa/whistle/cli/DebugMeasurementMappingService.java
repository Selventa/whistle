package com.selventa.whistle.cli;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openbel.framework.api.Kam.KamNode;

import com.selventa.whistle.data.model.Measurement;
import com.selventa.whistle.score.model.MappedMeasurement;
import com.selventa.whistle.score.service.DefaultMeasurementMappingService;

/**
 * Debug extension of {@link DefaultMeasurementMappingService} that keeps track
 * of {@link Measurement}s not mapped to any {@link Kam.KamNode} as well as
 * {@link Measurement}s that were discarded during collapsing.
 *
 * @author Steve Ungerer
 */
public class DebugMeasurementMappingService extends
        DefaultMeasurementMappingService {

    /**
     * Measurements that were not mapped to any kam node
     */
    private final Set<Measurement> unmapped = new HashSet<Measurement>();
    /**
     * Measurements mapped to the KAM but not to the population
     */
    private final Map<KamNode, Set<Measurement>> notInPopulation = new HashMap<KamNode, Set<Measurement>>();

    /**
     * Population measurements that mapped to the KAM.
     */
    private final Map<Integer, KamNode> inPopulation = new HashMap<Integer, KamNode>();

    /**
     * Measurements that were discarded in collapsing
     */
    private final Map<KamNode, Set<Measurement>> collapsed = new HashMap<KamNode, Set<Measurement>>();

    /**
     * {@inheritDoc} Keep track of nodes not mapped to Kam
     */
    @Override
    protected void unmapped(Measurement measurement) {
        super.unmapped(measurement);
        unmapped.add(measurement);
    }

    @Override
    protected void notInPopulation(KamNode node, Set<Measurement> measurements) {
        super.notInPopulation(node, measurements);
        notInPopulation.put(node, measurements);
    }

    @Override
    protected void inPopulation(KamNode node) {
        super.inPopulation(node);
        inPopulation.put(node.getId(), node);
    }

    /**
     * {@inheritDoc} Keep track of nodes discarded during collapsing
     */
    @Override
    protected MappedMeasurement collapse(KamNode kamNode,
            Set<Measurement> measurements) {
        Set<Measurement> original = new HashSet<Measurement>(measurements);
        MappedMeasurement mm = super.collapse(kamNode, measurements);
        Measurement used = mm.getMeasurement();
        original.remove(used);
        collapsed.put(kamNode, original);
        return mm;
    }

    public Set<Measurement> getUnmapped() {
        return unmapped;
    }

    public Map<KamNode, Set<Measurement>> getNotInPopulation() {
        return notInPopulation;
    }

    public Map<Integer, KamNode> getInPopulation() {
        return inPopulation;
    }

    public Map<KamNode, Set<Measurement>> getCollapsed() {
        return collapsed;
    }
}
