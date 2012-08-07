package com.selventa.whistle.data.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A comparison is a collection of {@link Measurement}s for a statistical
 * analysis.
 * 
 * @author Steve Ungerer
 */
public class Comparison {
    private final String name;
    private final Collection<Measurement> measurements;

    public Comparison(String name, Collection<Measurement> measurements) {
        this.name = name;
        // prevent modification of Measurements
        this.measurements = Collections
                .unmodifiableList(new ArrayList<Measurement>(measurements));
    }

    /**
     * Retrieve the name of the comparison
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieve the {@link Measurement}s for the comparison
     * @return
     */
    public Collection<Measurement> getMeasurements() {
        return measurements;
    }

}
