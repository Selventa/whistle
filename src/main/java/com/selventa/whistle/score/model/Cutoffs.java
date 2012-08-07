package com.selventa.whistle.score.model;

import org.openbel.framework.common.InvalidArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.selventa.whistle.data.model.Measurement;


/**
 * Represents the cutoffs to be used in selecting significant
 * {@link MappedMeasurement}s as state changes.
 * 
 * @author Steve Ungerer
 */
public class Cutoffs {
    private static final Logger logger = LoggerFactory.getLogger(Cutoffs.class);
    
    /**
     * If <code>true</code>, any {@link Measurement} with a <code>false</code> analyst selection
     * will be discarded.
     */
    private final boolean useAnalystSelection;
    
    /**
     * fold changes with an absolute value <em>less than</em> this value will be
     * discarded
     */
    private final Double foldChangeCutoff;
    /**
     * p values with a value <em>greater than</em> this value will be discarded
     */
    private final Double pValueCutoff;

    /**
     * abundances with a value <em>less than</em> this value will be discarded
     */
    private final Double abundanceCutoff;
    
    public Cutoffs(boolean useAnalystSelection) {
        if (!useAnalystSelection) {
            throw new InvalidArgument("Use Cutoffs(Double, Double, Double) if not using analyst selection");
        }
        this.useAnalystSelection = true;
        this.foldChangeCutoff = null;
        this.pValueCutoff = null;
        this.abundanceCutoff = null;
    }
    
    public Cutoffs(Double foldChangeCutoff, Double pValueCutoff, Double abundanceCutoff) {
        this.useAnalystSelection = false;
        this.foldChangeCutoff = foldChangeCutoff;
        this.pValueCutoff = pValueCutoff;
        this.abundanceCutoff = abundanceCutoff;
    }

    /**
     * Evaluate a {@link Measurement} against the cutoffs.
     * @param m
     * @return
     */
    public boolean evaluate(Measurement m) {
        if (useAnalystSelection) {
            return m.isAnalystSelection();
        }
        if (foldChangeCutoff != null && Math.abs(m.getFoldChange()) < foldChangeCutoff.doubleValue()) {
            logger.trace("{} Fold change rejection", m.getTerm());
            return false;
        } else if (pValueCutoff != null && m.getpValue().compareTo(pValueCutoff) > 0) {
            logger.trace("{} P-value rejection", m.getTerm());
            return false;
        } else if (abundanceCutoff != null && m.getAbundance().compareTo(abundanceCutoff) < 0) {
            logger.trace("{} Abundance rejection", m.getTerm());
            return false;
        }
        return true;
    }
    
    
}
