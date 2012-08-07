package com.selventa.whistle.data.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.selventa.whistle.data.model.Measurement;


/**
 * Default implementation of {@link CollapsingStrategy}. This implementation
 * selects measurements based on the following criteria:
 * <ol>
 * <li>If there is only a single measurement, return it.</li>
 * <li>If respecting analyst selection, check for analyst selection.
 * <ol>
 * <li>If there is a single measurement with an analyst selection return it.</li>
 * <li>If there are multiple measurements with analyst selection, select these
 * measurements.</li>
 * <li>If no measurements have an analyst selection, select all measurements.</li>
 * </ol>
 * </li>
 * <li>If there is a unique measurement with the lowest (most-significant)
 * p-value, return it. Otherwise select the measurements with the lowest
 * p-values.</li>
 * <li>Group measurements with identical lowest p-values according to direction
 * of fold change.
 * <ol>
 * <li>If the number of positive and negative fold changes are equal, return a
 * generated measurement with a fold change of 0, p-value of 1 and abundance of
 * 0. This ensures the measurement is included in population size calculations
 * but excluded when any (rational) cutoffs are applied.</li>
 * <li>Select the group of measurements with the highest cardinality.</li>
 * </ol>
 * </li>
 * <li>From this group of measurements, if there is a unique measurement with
 * the highest absolute fold change, return it. Otherwise select the
 * measurements with the highest absolute fold changes.</li>
 * <li>If there is a unique measurement with the highest abundance, return it.
 * Otherwise select the measurements with the highest abundance.</li>
 * <li>Sort the measurements alphabetically and return the first unique
 * measurement. If there are multiple measurements with the same ID, log as a
 * warning and return <code>null</code>.</li>
 * </ol>
 *
 * @author Steve Ungerer
 */
public class DefaultCollapsingStrategy implements CollapsingStrategy {
    private static final Logger logger = LoggerFactory
            .getLogger(DefaultCollapsingStrategy.class);

    private final boolean respectAnalystSelection;

    public DefaultCollapsingStrategy(boolean respectAnalystSelection) {
        this.respectAnalystSelection = respectAnalystSelection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Measurement collapse(Collection<Measurement> measurements) {
        if (CollectionUtils.isEmpty(measurements)) {
            return null;
        }
        if (measurements.size() == 1) {
            return measurements.iterator().next();
        }

        if (respectAnalystSelection) {
            Collection<Measurement> analystSelections = getAnalystSelections(measurements);
            if (analystSelections.size() == 1) {
                return analystSelections.iterator().next();
            } else if (analystSelections.size() > 1) {
                measurements = analystSelections;
            }
            // if analyst selections is empty, continue with original measurements
        }

        List<Measurement> lowestPValue = getLowestPValue(measurements);
        if (lowestPValue.size() == 1) {
            return lowestPValue.get(0);
        }
        measurements = lowestPValue;

        Collection<Measurement> positiveFoldChanges = getPositiveFoldChange(measurements);
        Collection<Measurement> negativeFoldChanges = getNegativeFoldChange(measurements);
        if (CollectionUtils.isNotEmpty(positiveFoldChanges)
                && CollectionUtils.isNotEmpty(negativeFoldChanges)) {
            if (positiveFoldChanges.size() == negativeFoldChanges.size()) {
                /*
                 * Equal number of positive and negative. Return a generated
                 * measurement with a fold change of 0, pVal of 1 and abundance
                 * of 0. This ensures the measurement is included in population
                 * size calculation but excluded when any (rational) cutoffs are
                 * used.
                 */
                Measurement sample = positiveFoldChanges.iterator().next();
                return new Measurement(sample.getTerm(), 0D, 1D, 0D);
            }

            if (positiveFoldChanges.size() > negativeFoldChanges.size()) {
                measurements = positiveFoldChanges;
            } else {
                measurements = negativeFoldChanges;
            }
        }

        Collection<Measurement> highestDirection = getHighestFoldChange(measurements);
        if (highestDirection.size() == 1) {
            return highestDirection.iterator().next();
        }
        measurements = highestDirection;

        Collection<Measurement> highestAbundance = getHighestAbundance(measurements);
        if (highestAbundance.size() == 1) {
            return highestAbundance.iterator().next();
        }
        measurements = highestAbundance;

        return getLowestReferenceId(measurements);
    }

    protected Collection<Measurement> getAnalystSelections(Collection<Measurement> measurements) {
        Set<Measurement> ret = new HashSet<Measurement>();
        for (Measurement m : measurements) {
            if (m.isAnalystSelection()) {
                ret.add(m);
            }
        }
        return ret;
    }

    /**
     * Get {@link Measurement} with the lowest alphabetical ID
     *
     * @param measurements
     * @return measurement with the lowest id, null if value is not unique
     */
    protected Measurement getLowestReferenceId(
            Collection<Measurement> measurements) {
        List<Measurement> lowestRows = new ArrayList<Measurement>();
        String lowestId = null;
        for (Measurement m : measurements) {
            String cur = m.getTerm().toBELShortForm();
            if (lowestId == null) {
                lowestId = cur;
                lowestRows.add(m);
                continue;
            }

            if (cur.compareTo(lowestId) < 0) {
                lowestRows.clear();
            }

            if (cur.compareTo(lowestId) <= 0) {
                lowestId = cur;
                lowestRows.add(m);
            }
        }

        if (lowestRows.size() != 1) {
            logger.warn("Non-unique measurement id found: {}.", lowestId);
            return null;
        }
        return lowestRows.get(0);
    }

    /**
     * @param measurements
     * @return rows with the highest abundance
     */
    protected Collection<Measurement> getHighestAbundance(
            Collection<Measurement> measurements) {
        return getHighest(measurements, new DoubleAccessor() {
            @Override
            public Double getDouble(Measurement m) {
                return m.getAbundance();
            }
        });
    }

    /**
     * Get measurements with highest absolute fold change value
     *
     * @param measurements
     * @return measurements with the highest fold change
     */
    protected Collection<Measurement> getHighestFoldChange(
            Collection<Measurement> measurements) {
        return getHighest(measurements, new DoubleAccessor() {
            @Override
            public Double getDouble(Measurement m) {
                return Math.abs(m.getFoldChange());
            }
        });
    }

    /**
     * Get measurements with a negative fold change
     *
     * @param measurements
     * @return all negative fold changes
     */
    protected Collection<Measurement> getNegativeFoldChange(
            Collection<Measurement> measurements) {
        Collection<Measurement> ret = new ArrayList<Measurement>();
        for (Measurement m : measurements) {
            Double dir = m.getFoldChange();
            if (dir != null && dir < 0) {
                ret.add(m);
            }
        }
        return ret;
    }

    /**
     * @param measurements
     * @return all positive fold changes
     */
    protected Collection<Measurement> getPositiveFoldChange(
            Collection<Measurement> measurements) {
        Collection<Measurement> ret = new ArrayList<Measurement>();
        for (Measurement m : measurements) {
            Double dir = m.getFoldChange();
            if (dir != null && dir > 0) {
                ret.add(m);
            }
        }
        return ret;
    }

    /**
     * Get the Measurement with the lowest p-value
     *
     * @param measurements
     * @return Measurements with lowest p-value
     */
    protected List<Measurement> getLowestPValue(
            Collection<Measurement> measurements) {
        List<Measurement> lowestRows = new ArrayList<Measurement>();
        Double lowestPValue = null;
        for (Measurement m : measurements) {
            Double cur = m.getFoldChange();
            if (lowestPValue == null) {
                lowestPValue = cur;
                lowestRows.add(m);
                continue;
            }

            if (cur < lowestPValue) {
                lowestRows.clear();
            }

            if (cur <= lowestPValue) {
                lowestPValue = cur;
                lowestRows.add(m);
            }
        }
        return lowestRows;
    }

    /**
     * Interface to be used in
     * {@link DefaultCollapsingStrategy#getHighest(Collection, DoubleAccessor)},
     * likely as an anonymous inner class.
     */
    protected static interface DoubleAccessor {
        /**
         * Retrieve the double from a {@link Measurement}
         *
         * @param m
         * @return
         */
        Double getDouble(Measurement m);
    }

    /**
     * Obtain the measurements with the highest double.
     *
     * @param measurements
     * @param accessor
     * @return
     */
    protected static Collection<Measurement> getHighest(
            Collection<Measurement> measurements, DoubleAccessor accessor) {
        List<Measurement> highest = new ArrayList<Measurement>();
        Double max = null;
        for (Measurement m : measurements) {
            Double cur = accessor.getDouble(m);
            if (max == null) {
                max = cur;
                highest.add(m);
                continue;
            }

            if (cur > max) {
                highest.clear();
            }

            if (cur >= max) {
                max = cur;
                highest.add(m);
            }
        }
        return highest;
    }
}
