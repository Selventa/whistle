package com.selventa.whistle.cli;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openbel.framework.api.Kam.KamNode;

import com.selventa.whistle.data.model.Measurement;
import com.selventa.whistle.score.model.Cutoffs;
import com.selventa.whistle.score.model.MappedMeasurement;
import com.selventa.whistle.score.service.Scorer;

/**
 * Extension of {@link Scorer} to record debug information.
 *
 * @author Steve Ungerer
 */
public class DebugScorer extends Scorer {

    private static final String FAILED_CUTOFFS = "Failed cutoffs";
    private static final String STATE_CHANGE = "State Change";

    /**
     * String debug information
     */
    private final Map<Measurement, String> debugInfo;

    /**
     * Save the computed state change map for reporting
     */
    private Map<KamNode, MappedMeasurement> stateChangeMap =
            new HashMap<KamNode, MappedMeasurement>();

    /**
     * Construct
     * @param debugInfo
     */
    public DebugScorer(Map<Measurement, String> debugInfo) {
        this.debugInfo = debugInfo;
    }

    /**
     * Return an unmodifiable {@link Map} of state changes.
     *
     * @return {@link Map} of {@link KamNode} to {@link MappedMeasurement}
     */
    public Map<KamNode, MappedMeasurement> getStateChangeMap() {
        return Collections.unmodifiableMap(stateChangeMap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<KamNode, MappedMeasurement> createStateChangeMap(
            Collection<MappedMeasurement> mappedMeasurements, Cutoffs cutoffs) {
        stateChangeMap.clear();
        stateChangeMap = new HashMap<KamNode, MappedMeasurement>(
                mappedMeasurements.size());

        for (MappedMeasurement mm : mappedMeasurements) {
            if (cutoffs.evaluate(mm.getMeasurement())) {
                stateChangeMap.put(mm.getKamNode(), mm);
                debugInfo.put(mm.getMeasurement(), STATE_CHANGE);
            } else {
                debugInfo.put(mm.getMeasurement(), FAILED_CUTOFFS);
            }
        }
        return stateChangeMap;
    }
}
