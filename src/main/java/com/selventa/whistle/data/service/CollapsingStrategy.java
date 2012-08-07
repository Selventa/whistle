package com.selventa.whistle.data.service;

import java.util.Collection;

import com.selventa.whistle.data.model.Measurement;

/**
 * A {@link CollapsingStrategy} is responsible for determining which
 * {@link Measurement} will be used for analyses in which multiple
 * {@link Measurement}s are associated with the same object (e.g., a
 * {@link Kam.KamNode}).
 * 
 * @author Steve Ungerer
 * @see DefaultCollapsingStrategy
 */
public interface CollapsingStrategy {

    /**
     * Choose a single {@link Measurement} from a collection of related
     * {@link Measurement}s.
     * 
     * @param measurements
     * @return the selected {@link Measurement} or <code>null</code> if all
     *         measurements were discarded in the selection process.
     */
    Measurement collapse(Collection<Measurement> measurements);
}
