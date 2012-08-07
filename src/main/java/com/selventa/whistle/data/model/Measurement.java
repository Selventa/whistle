package com.selventa.whistle.data.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openbel.framework.api.Kam;
import org.openbel.framework.common.InvalidArgument;
import org.openbel.framework.common.enums.FunctionEnum;
import org.openbel.framework.common.model.Term;

import com.selventa.whistle.data.enums.DirectionType;

/**
 * A Measurement is a biological observation that has been made but is not
 * necessarily present in any {@link Kam}.<br>
 * Currently a Measurement supports only {@link FunctionEnum#RNA_ABUNDANCE} of a
 * single parameter.
 *
 * @author Steve Ungerer
 */
public class Measurement {
    private final DirectionType direction;
    private final Term term;
    private final Double directionVal;
    private final Double pValue;
    private final Double abundance;
    private final Boolean analystSelection;

    public Measurement(Term term, Double foldChange, Double pValue, Double abundance) {
        this(term, foldChange, pValue, abundance, null);
    }

    public Measurement(Term term, Double directionVal, Double pValue, Double abundance, Boolean analystSelection)
            throws InvalidArgument {
        if (term == null) {
            throw new InvalidArgument("term must not be blank");
        } else if (!(FunctionEnum.RNA_ABUNDANCE.equals(term.getFunctionEnum()))
                || term.getNumberOfParameters() != 1) {
            throw new InvalidArgument(
                    "term must be an RNA_ABUNDANCE of a single parameter");
        }
        this.term = term;
        if (directionVal == null) {
            throw new InvalidArgument("directionVal must not be null");
        }
        this.directionVal = directionVal;
        this.direction = eval(directionVal);
        this.pValue = pValue;
        this.abundance = abundance;
        this.analystSelection = analystSelection;
    }

    public Term getTerm() {
        return term;
    }

    public Double getFoldChange() {
        return directionVal;
    }

    public Double getpValue() {
        return pValue;
    }

    public Double getAbundance() {
        return abundance;
    }

    public Boolean isAnalystSelection() {
        return analystSelection == null
            ? false
            : analystSelection;
    }

    public DirectionType getDirection() {
        return direction;
    }

    private static DirectionType eval(Double dir) {
        if (dir == null) {
            throw new InvalidArgument("dir must not be null");
        }
        // use primitive equality so that -0.0d and 0.0d are equal
        double d = dir.doubleValue();
        if (d == 0d) {
            return DirectionType.UNMEASURED;
        } else if (d > 0d) {
            return DirectionType.UP;
        } else {
            return DirectionType.DOWN;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(getTerm())
            .append(getFoldChange())
            .append(getpValue())
            .append(getAbundance())
            .append(isAnalystSelection())
            .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Measurement)) {
            return false;
        }
        Measurement rhs = (Measurement)obj;
        return new EqualsBuilder()
            .append(getTerm(), rhs.getTerm())
            .append(getFoldChange(), rhs.getFoldChange())
            .append(getpValue(), rhs.getpValue())
            .append(getAbundance(), rhs.getAbundance())
            .append(isAnalystSelection(), rhs.isAnalystSelection())
            .isEquals();
    }


}
