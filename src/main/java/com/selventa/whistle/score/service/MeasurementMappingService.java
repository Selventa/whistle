package com.selventa.whistle.score.service;

import java.util.Collection;
import java.util.Collections;

import org.openbel.framework.api.Kam;
import org.openbel.framework.common.InvalidArgument;

import com.selventa.whistle.data.model.Measurement;
import com.selventa.whistle.score.model.Hypothesis;
import com.selventa.whistle.score.model.MappedMeasurement;

/**
 * Service for creation of {@link MappedMeasurement}s.
 * 
 * @author Steve Ungerer
 */
public interface MeasurementMappingService {

    /**
     * Create {@link MappedMeasurement}s by mapping the collection of
     * {@link Measurement}s to the given {@link Hypothesis hypotheses}.<br>
     * This method must ensure a 1:1 {@link Kam.KamNode}:
     * {@link MappedMeasurement} relationship. i.e., the returned collection of
     * MappedMeasurements must not contain multiple MappedMeasurements that
     * return the same {@link Kam.KamNode} via
     * {@link MappedMeasurement#getKamNode()}.
     * 
     * @param kam the {@link Kam} from which all hypotheses have been derived
     * @param hypotheses the {@link Hypothesis hypotheses} to which the
     *            {@link Measurement}s will be mapped
     * @param measurements the collection of {@link Measurement}s to map
     * @return
     * @throws Exception
     */
    MappingResult map(Kam kam, Collection<Hypothesis> hypotheses,
            Collection<Measurement> measurements) throws Exception;

    /**
     * Encapsulation of mapping results containing the {@link MappedMeasurement}
     * and the computed population size.
     * 
     * @author Steve Ungerer
     */
    class MappingResult {
        private final Collection<MappedMeasurement> mappedMeasurements;
        private final Integer populationSize;

        /**
         * @param mappedMeasurements Must not be null.
         * @param populationSize Must not be null
         */
        public MappingResult(Collection<MappedMeasurement> mappedMeasurements,
                Integer populationSize) {
            if (mappedMeasurements == null || populationSize == null) {
                throw new InvalidArgument(
                        "MappedMeasurements and populationSize must be non-null");
            }
            this.mappedMeasurements = Collections
                    .unmodifiableCollection(mappedMeasurements);
            this.populationSize = populationSize;
        }

        /**
         * Retrieve the {@link MappedMeasurement}s: Measurements mapped to the
         * population.
         * 
         * @return
         */
        public Collection<MappedMeasurement> getMappedMeasurements() {
            return mappedMeasurements;
        }

        /**
         * Retrieve the size of the population
         * 
         * @return
         */
        public Integer getPopulationSize() {
            return populationSize;
        }

    }

}