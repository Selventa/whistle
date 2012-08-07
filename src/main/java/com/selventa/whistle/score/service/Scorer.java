package com.selventa.whistle.score.service;

import static org.openbel.framework.common.BELUtilities.nulls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.openbel.framework.api.Kam.KamNode;
import org.openbel.framework.common.InvalidArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.selventa.whistle.data.enums.DirectionType;
import com.selventa.whistle.score.model.Cutoffs;
import com.selventa.whistle.score.model.Downstream;
import com.selventa.whistle.score.model.Hypothesis;
import com.selventa.whistle.score.model.MappedMeasurement;
import com.selventa.whistle.score.model.ScoredHypothesis;
import com.selventa.whistle.score.util.MathException;
import com.selventa.whistle.score.util.MathUtil;

/**
 *
 * @author julianray
 * @author Steve Ungerer
 */
public class Scorer {
    private static final Logger logger = LoggerFactory.getLogger(Scorer.class);

    /**
     *
     * @param hypothesis
     * @param mappedMeasurements
     * @param populationSize
     * @return
     * @throws ScoringException
     */
    public ScoredHypothesis score(Hypothesis hypothesis,
            Collection<MappedMeasurement> mappedMeasurements, Cutoffs cutoffs,
            Integer populationSize) throws ScoringException {
        Set<KamNode> measuredNodes = createMeasuredNodes(mappedMeasurements);
        Map<KamNode, MappedMeasurement> scMap = createStateChangeMap(
                mappedMeasurements, cutoffs);
        logger.info("{} mapped measurements are state changes", scMap.size());
        return getScore(hypothesis, measuredNodes, scMap, populationSize);
    }

    /**
     *
     * @param hypotheses
     * @param mappedMeasurements
     * @param populationSize
     * @return
     * @throws ScoringException
     */
    public Collection<ScoredHypothesis> score(
            Collection<Hypothesis> hypotheses,
            Collection<MappedMeasurement> mappedMeasurements, Cutoffs cutoffs,
            Integer populationSize) throws ScoringException {
        Set<KamNode> measuredNodes = createMeasuredNodes(mappedMeasurements);
        Map<KamNode, MappedMeasurement> scMap = createStateChangeMap(
                mappedMeasurements, cutoffs);
        logger.info("{} mapped measurements are state changes", scMap.size());
        List<ScoredHypothesis> results = new ArrayList<ScoredHypothesis>(
                hypotheses.size());
        for (Hypothesis hypothesis : hypotheses) {
            results.add(getScore(hypothesis, measuredNodes, scMap,
                    populationSize));
        }
        return results;
    }

    /**
     * Constructs a {@link Map} of {@link Kam.KamNode} to the
     * {@link MappedMeasurement} recorded for the node. Only
     * {@link MappedMeasurement}s passing the given {@link Cutoffs} will be
     * included in the returned map.
     *
     * @param mappedMeasurements
     * @param cutoffs
     * @return
     */
    protected Map<KamNode, MappedMeasurement> createStateChangeMap(
            Collection<MappedMeasurement> mappedMeasurements, Cutoffs cutoffs) {
        Map<KamNode, MappedMeasurement> ret = new HashMap<KamNode, MappedMeasurement>(
                mappedMeasurements.size());
        for (MappedMeasurement mm : mappedMeasurements) {
            if (cutoffs.evaluate(mm.getMeasurement())) {
                ret.put(mm.getKamNode(), mm);
            }
        }
        return ret;
    }

    /**
     * Construct a {@link Set} of all measured {@link Kam.KamNode}s based on a
     * collection of {@link MappedMeasurement}s.
     *
     * @param mappedMeasurements
     * @return
     */
    protected Set<KamNode> createMeasuredNodes(
            Collection<MappedMeasurement> mappedMeasurements) {
        Set<KamNode> measuredNodes = new HashSet<KamNode>(
                mappedMeasurements.size());
        for (MappedMeasurement mm : mappedMeasurements) {
            measuredNodes.add(mm.getKamNode());
        }
        return measuredNodes;
    }

    /**
     * @param hypothesis
     * @param measuredNodes
     * @param stateChangeMap
     * @param populationSize
     * @return
     *      * The returned score will be {@code null} if the number of downstream nodes
     * for the starting node is less than four.
     * @throws ScoringException
     */
    protected ScoredHypothesis getScore(Hypothesis hypothesis,
            Set<KamNode> measuredNodes,
            Map<KamNode, MappedMeasurement> stateChangeMap,
            Integer populationSize) throws ScoringException {

        if (hypothesis == null || hypothesis.getDownstreams() == null
                || measuredNodes == null || stateChangeMap == null) {
            throw new ScoringException(
                    "Hypothesis, Downstream Nodes, and State Changes cannot be null.");
        }

        if (populationSize == null || populationSize < 0) {
            throw new ScoringException(
                    "populationSize must be a positive number.");
        }

        ScoredHypothesis scoredHypothesis = new ScoredHypothesis(hypothesis);

        /*
         * Compute the number of possibles. Possibles are the # of downstreams
         * that have been measured (aka are MappedMeasurements)
         */
        Set<KamNode> downstreamNodes = new HashSet<KamNode>(hypothesis
                .getDownstreams().size());
        for (Downstream d : hypothesis.getDownstreams()) {
            downstreamNodes.add(d.getKamNode());

            if (stateChangeMap.containsKey(d.getKamNode())) {
            	scoredHypothesis.getDownstreams().add(d);
            }
        }
        @SuppressWarnings("unchecked")
        Collection<KamNode> possibles = CollectionUtils.intersection(
                downstreamNodes, measuredNodes);
        scoredHypothesis.setPossible(possibles.size());

        // Filter out any starting nodes which have < 4 downstreams as they
        // won't make a richness cutoff
        if (possibles.size() >= 4) {
            // Get the prediction for this startingNode
            Prediction prediction = getPrediction(hypothesis, stateChangeMap);

            int correct = prediction.getNumberCorrect();
            int contra = prediction.getNumberContra();
            int ambigs = prediction.getNumberAmbiguous();

            // get the direction
            DirectionType direction = DirectionType.UP;
            if (contra > correct) {
                // swap correct with contra
                direction = DirectionType.DOWN;
                contra = prediction.getNumberCorrect();
                correct = prediction.getNumberContra();

                // and flip prediction
                prediction = new Prediction(prediction.contra,
                        prediction.correct, prediction.ambiguous);
            }

            int numPossibleChanged = contra + correct + ambigs;
            // Calculate richness
            double richness = 1.0;

            try {
                richness = MathUtil
                        .richness(numPossibleChanged, possibles.size(),
                                stateChangeMap.size(), populationSize);
            } catch (MathException e) {
                richness = 1.0;
            }

            double concordance = 1.0d;
            if (correct > 0 && contra >= 0) {
                concordance = MathUtil.concordance(correct, contra);
            }

            // create scored object for this hypothesis
            scoredHypothesis.setPrediction(prediction);
            scoredHypothesis.setRichness(richness);
            scoredHypothesis.setConcordance(concordance);
            scoredHypothesis
                    .setDirectionType(correct == 0 ? DirectionType.UNMEASURED
                            : direction);
            scoredHypothesis.setObserved(numPossibleChanged);
        } else {
            logger.trace("Hyp {} discarded with {} possibles", hypothesis
                    .getKamNode().getLabel(), possibles.size());
            scoredHypothesis.setDirectionType(DirectionType.UNMEASURED);
        }

        return scoredHypothesis;
    }

    protected Prediction getPrediction(Hypothesis hypothesis,
            Map<KamNode, MappedMeasurement> stateChangeMap) {

        final Set<Downstream> correct = new HashSet<Downstream>();
        final Set<Downstream> contra = new HashSet<Downstream>();
        final Set<Downstream> ambiguous = new HashSet<Downstream>();

        // Check each downstream node
        for (Downstream observation : hypothesis.getDownstreams()) {

            // See if it is in the stateChange list
            MappedMeasurement stateChange = stateChangeMap.get(observation
                    .getKamNode());

            if (null != stateChange) {

                DirectionType observedDirection = observation
                        .getDirectionType();
                // Check to see if this direction is ambiguous. Skip if it is
                if (DirectionType.AMBIG.equals(observedDirection)) {
                    ambiguous.add(observation);
                } else if (DirectionType.UP.equals(observedDirection)) {
                    // Prediction is up
                    if (stateChange.getMeasurement().getFoldChange() < 0.0) {
                        // observed is down so its a contra
                        contra.add(observation);
                    } else {
                        // both predict the same so its a for
                        correct.add(observation);
                    }
                } else {
                    // Prediction is down
                    if (stateChange.getMeasurement().getFoldChange() < 0.0) {
                        // observed is down so its a for
                        correct.add(observation);
                    } else {
                        // stateChange predicts and up so its a contra
                        contra.add(observation);
                    }
                }
            }
        }
        return new Prediction(correct, contra, ambiguous);
    }

    /**
     * Prediction for a hypothesis.
     *
     * @author julianray
     */
    public class Prediction {
        private final Set<Downstream> correct;
        private final Set<Downstream> contra;
        private final Set<Downstream> ambiguous;

        public Prediction(final Set<Downstream> correct, final Set<Downstream> contra,
                final Set<Downstream> ambiguous) {
            if (nulls(correct, contra, ambiguous)) {
                throw new InvalidArgument("downstreams are null");
            }

            this.correct = correct;
            this.contra = contra;
            this.ambiguous = ambiguous;
        }

        public final Set<Downstream> getCorrect() {
            return correct;
        }

        public final int getNumberCorrect() {
            return correct.size();
        }

        public final Set<Downstream> getContra() {
            return contra;
        }

        public final int getNumberContra() {
            return contra.size();
        }

        public final Set<Downstream> getAmbiguous() {
            return ambiguous;
        }

        public final int getNumberAmbiguous() {
            return ambiguous.size();
        }
    }
}
