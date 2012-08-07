package com.selventa.whistle.score.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.openbel.framework.api.Equivalencer;
import org.openbel.framework.api.EquivalencerException;
import org.openbel.framework.api.Kam;
import org.openbel.framework.api.Kam.KamNode;
import org.openbel.framework.api.KamStore;
import org.openbel.framework.api.KamStoreException;
import org.openbel.framework.common.InvalidArgument;
import org.openbel.framework.common.enums.FunctionEnum;
import org.openbel.framework.common.model.Parameter;
import org.openbel.framework.common.model.Term;
import org.openbel.framework.common.protonetwork.model.SkinnyUUID;
import org.openbel.framework.internal.KAMStoreDaoImpl.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.selventa.whistle.data.model.Measurement;
import com.selventa.whistle.data.service.CollapsingStrategy;
import com.selventa.whistle.score.model.Downstream;
import com.selventa.whistle.score.model.Hypothesis;
import com.selventa.whistle.score.model.MappedMeasurement;

/**
 * Default {@link MeasurementMappingService}. This implementation iterates over
 * all {@link Measurement}s and attempts to locate
 * {@link FunctionEnum#RNA_ABUNDANCE} nodes in the specified {@link Kam} for
 * that measurement. As it is possible for multiple measurements to map to the
 * same {@link KamNode}, the set of measurements for a single node is then
 * collapsed via the provided {@link CollapsingStrategy}.
 *
 * @author Steve Ungerer
 */
public class DefaultMeasurementMappingService implements
        MeasurementMappingService {
    private static final Logger logger = LoggerFactory
            .getLogger(DefaultMeasurementMappingService.class);

    private KamStore kamStore;
    private CollapsingStrategy collapsingStrategy;
    private Equivalencer equivalencer = new Equivalencer();

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingResult map(Kam kam, Collection<Hypothesis> hypotheses,
            Collection<Measurement> measurements) throws Exception {
        if (kamStore == null || collapsingStrategy == null) {
            throw new IllegalStateException(
                    "kamStore and collapsingStrategy must not be null");
        }
        // load kam namespaces
        logger.debug("Retrieving namespaces");
        Map<String, Namespace> jdbcNsMap = new HashMap<String, Namespace>();
        for (Namespace n : kamStore.getNamespaces(kam)) {
            jdbcNsMap.put(n.getResourceLocation(), n);
        }
        logger.debug("Retrieved {} namespaces", jdbcNsMap.size());

        // resolve nodes to the Kam
        Map<Kam.KamNode, Set<Measurement>> map = new HashMap<Kam.KamNode, Set<Measurement>>();

        int num = 0;
        for (Measurement m : measurements) {
            if (++num % 100 == 0) {
                logger.debug("Processing measurement {}/{}", num, measurements.size());
            }
            Term t = m.getTerm();
            Parameter p = t.getParameters().get(0);
            SkinnyUUID uuid = null;
            try {
                uuid = equivalencer.getUUID(p.getNamespace(), p.getValue());
            } catch (EquivalencerException e) {
                logger.warn(
                        "{} failed to equivalence; will attempt lookup by ns/val",
                        m.getTerm());
            }
            List<Kam.KamNode> nodes = null;
            try {
                if (uuid != null) {
                    nodes = kamStore.getKamNodes(kam,
                            FunctionEnum.RNA_ABUNDANCE, uuid);
                } else {
                    Namespace ns = jdbcNsMap.get(p.getNamespace()
                            .getResourceLocation());
                    if (ns != null) {
                        nodes = kamStore.getKamNodes(kam, ns, p.getValue());
                    }
                }
            } catch (KamStoreException e) {
                logger.warn("{} failed to obtain KAMNodes; ignoring",
                        m.getTerm());
                continue;
            }
            if (nodes == null || nodes.isEmpty()) {
                unmapped(m);
                continue;
            }

            // XXX Multiple returns should not be possible
            if (nodes.size() > 1) {
                logger.info("Found multiple kam nodes for {}", m.getTerm());
            }
            Kam.KamNode node = nodes.get(0);

            Set<Measurement> set = map.get(node);
            if (set == null) {
                set = new HashSet<Measurement>();
                map.put(node, set);
            }
            set.add(m);
        }

        // compute the nodes that are part of the population
        Collection<KamNode> populationNodes = getPopulationNodes(map.keySet(), hypotheses);

        // Collapse the set of Measurements for each KamNode to reduce
        // the association to 1 MappedMeasurement : 1 KamNode
        List<MappedMeasurement> mappedMeasurements = new ArrayList<MappedMeasurement>();
        for (Map.Entry<Kam.KamNode, Set<Measurement>> entry : map.entrySet()) {
            KamNode n = entry.getKey();
            Set<Measurement> m = entry.getValue();
            if (!populationNodes.contains(n)) {
                notInPopulation(n, m);
                continue;
            }

            inPopulation(n);

            MappedMeasurement mm = collapse(n, m);
            if (mm != null) {
                mappedMeasurements.add(mm);
            }
        }

        return new MappingResult(mappedMeasurements, populationNodes.size());
    }

    /**
     * Handle a {@link Measurement} not mapped to any {@link Kam.KamNode}
     * @param measurement
     */
    protected void unmapped(Measurement measurement) {
        logger.trace("{} no KAM matches found", measurement.getTerm());
    }

    /**
     * Handle {@link Measurement}s that were mapped to a {@link KamNode}, but
     * not mapped to the population.
     * @param node
     * @param measurements
     */
    protected void notInPopulation(KamNode node, Set<Measurement> measurements) {
        logger.trace("{} measurements mapping to {} are not in the population", measurements.size(), node.getLabel());
    }

    protected void inPopulation(KamNode node) {
        logger.trace("{} in the population", node.getLabel());
    }

    /**
     * Collapse a {@link Set} of {@link Measurement}s for a single {@link Kam.KamNode}
     * @param kamNode
     * @param measurements
     * @return the {@link Measurement} to use, or <code>null</code> if none is
     * selected.
     */
    protected MappedMeasurement collapse(Kam.KamNode kamNode, Set<Measurement> measurements) {
        Measurement m = collapsingStrategy.collapse(measurements);
        return m != null
                ? new MappedMeasurement(kamNode, m)
                : null;
    }

    protected Collection<KamNode> getPopulationNodes(Collection<KamNode> resolvedNodes,
            Collection<Hypothesis> hypotheses) {
        Set<KamNode> hypNodes = new HashSet<KamNode>();
        for (Hypothesis hyp : hypotheses) {
            Set<KamNode> downstreamNodes = new HashSet<KamNode>();
            for (Downstream d : hyp.getDownstreams()) {
                downstreamNodes.add(d.getKamNode());
            }

            @SuppressWarnings("unchecked")
            Collection<KamNode> possibles = CollectionUtils.intersection(
                    downstreamNodes, resolvedNodes);

            if (possibles.size() >= 4) {
                hypNodes.addAll(possibles);
            }
        }

        return hypNodes;
    }

    /**
     * Inject the {@link KamStore} to be used.
     *
     * @param kamStore, must not be null
     * @throws InvalidArgument if the given kamStore is null
     */
    public void setKamStore(KamStore kamStore) throws InvalidArgument {
        if (kamStore == null) {
            throw new InvalidArgument("kamStore must not be null");
        }
        this.kamStore = kamStore;
    }

    /**
     * Inject the {@link CollapsingStrategy} to be used.
     *
     * @param collapsingStrategy, must not be null
     * @throws InvalidArgument if the given collapsingStrategy is null
     */
    public void setCollapsingStrategy(CollapsingStrategy collapsingStrategy)
            throws InvalidArgument {
        this.collapsingStrategy = collapsingStrategy;
    }
}
