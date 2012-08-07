package com.selventa.whistle.score.model;

import org.openbel.framework.api.Kam.KamNode;

import com.selventa.whistle.data.model.Measurement;

/**
 * A {@link MappedMeasurement} is a {@link Measurement} from a data set mapped
 * to a {@link Downstream}
 * 
 * @author Steve Ungerer
 */
public class MappedMeasurement extends Downstream {

    private Measurement measurement;

    public MappedMeasurement(final KamNode kamNode,
            final Measurement measurement) {
        super(kamNode, measurement.getDirection());
        this.measurement = measurement;
    }

    public Measurement getMeasurement() {
        return measurement;
    }
}
