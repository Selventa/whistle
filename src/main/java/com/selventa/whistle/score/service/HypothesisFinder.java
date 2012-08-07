package com.selventa.whistle.score.service;

import java.util.List;

import org.openbel.framework.api.Kam;
import org.openbel.framework.api.NodeFilter;

import com.selventa.whistle.score.model.Hypothesis;

/**
 * Find Hypotheses in a {@link Kam}
 *
 * @author Julian Ray
 * @author Steve Ungerer
 */
public interface HypothesisFinder {

    public List<Hypothesis> findAll(Kam kam, int maxDepth);
    public List<Hypothesis> findAll(Kam kam, int maxDepth, NodeFilter nodeFilter);

}
