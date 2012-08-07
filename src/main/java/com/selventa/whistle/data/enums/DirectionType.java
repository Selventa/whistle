package com.selventa.whistle.data.enums;

import static org.openbel.framework.common.BELUtilities.constrainedHashMap;

import java.util.Map;

public enum DirectionType {

    UP(+1, "UP"),
    DOWN(-1, "DOWN"),
    AMBIG(3, "AMBIG"),
    UNMEASURED(0, "UNMEASURED");

    private final Integer value;
    private final String displayValue;

    private static final Map<String, DirectionType> STRINGTOENUM;
    private static final Map<Integer, DirectionType> VALUETOENUM;

    static {
        // These maps will never resize
        STRINGTOENUM = constrainedHashMap(values().length);
        VALUETOENUM = constrainedHashMap(values().length);

        for (final DirectionType d : values()) {
            STRINGTOENUM.put(d.toString(), d);
            VALUETOENUM.put(d.value, d);
        }
    }

    private DirectionType(Integer value, String displayValue) {
        this.value = value;
        this.displayValue = displayValue;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return displayValue;
    }

    /**
     * Returns the direction type's value.
     *
     * @return value
     * @see java.lang.Enum#ordinal() Contrast with {@code ordinal}
     */
    public Integer getValue() {
        return value;
    }

    /**
     * Returns the direction type's display value.
     *
     * @return display value
     */
    public String getDisplayValue() {
        return displayValue;
    }

    public static DirectionType fromValue(Integer value) {
        if (value == null) {
            return null;
        }

        return VALUETOENUM.get(value);
    }

    public static DirectionType fromString(final String s) {
        return getDirectionType(s);
    }
    public static DirectionType getDirectionType(final String s) {
    	DirectionType r = STRINGTOENUM.get(s);
        if (r != null) return r;

        for (final String dispval : STRINGTOENUM.keySet()) {
            if (dispval.equalsIgnoreCase(s)) return STRINGTOENUM.get(dispval);
        }

        return null;
    }

    public static DirectionType evaluate(DirectionType d1, DirectionType d2) {
    	return d1.evaluate(d2);
    }
    /**
     * Evaluates two direction types and determines the resulting type.
     * [1] If one or the other is ambig, the result is also ambig
     * [2] if one or the other is unmeasured, the result is the value of the other
     * [3] if they are the same the result is also the same
     * [4] if they are different, the result is ambig
     * @param that
     * @return
     */
    public DirectionType evaluate(DirectionType that) {
    	if (this.equals(AMBIG) || that.equals(AMBIG)) {
    		return AMBIG;
    	} else if (this.equals(UNMEASURED)) {
    		return that;
    	} else if (that.equals(UNMEASURED)) {
    		return this;
    	} else if (this.equals(that)) {
    		return this;
    	} else {
    		return AMBIG;
    	}
    }
    /**
     * Evaluates the compounding of another direction type onto the exisiting direction. This
     * basically evaluates the relationship of this on the direction of that
     * [1] If one or the other is ambig, the result is also ambig
     * [2] if one or the other is unmeasured, the result is the value of the other
     * [3] if they are the same the result is also the same
     * [4] if they are different, the result is the value of the second
     * @param that
     * @return
     */
    public DirectionType compound(DirectionType that) {
    	if (this.equals(AMBIG) || that.equals(AMBIG)) {
    		return AMBIG;
    	} else if (this.equals(UNMEASURED)) {
    		return that;
    	} else if (that.equals(UNMEASURED)) {
    		return that;
    	} else if (this.equals(that)) {
    		return this;
    	} else {
    		return that;
    	}
    }
}
