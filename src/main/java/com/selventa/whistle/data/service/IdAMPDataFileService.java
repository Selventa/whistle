package com.selventa.whistle.data.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openbel.framework.common.InvalidArgument;
import org.openbel.framework.common.model.Namespace;
import org.openbel.framework.common.model.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.selventa.whistle.data.model.Comparison;
import com.selventa.whistle.data.model.Measurement;

import au.com.bytecode.opencsv.CSVReader;

/**
 * {@link DataFileService} to handle IdAMP formatted data files.<br>
 * This file must adhere to the following format:
 * <ul>
 * <li>Must be a CSV:
 * <ul>
 * <li>Columns are separated with a comma.E.g.,
 *
 * <pre>
 * col1,col2
 * </pre>
 *
 * </li>
 * <li>Column value must be enclosed in double quotes if the value contains a
 * comma. E.g.
 *
 * <pre>
 * "column,one",col2
 * </pre>
 *
 * </li>
 * <li>Column values may be enclosed in double quotes if the value does not
 * contain a comma. E.g.,
 *
 * <pre>
 * "col1","col2"
 * </pre>
 *
 * </li>
 * <li>Column values containing a double quote must escape the value with a
 * double quote. E.g.
 *
 * <pre>
 * "Quoth the raven, ""Nevermore""","col2"
 * </pre>
 *
 * </li>
 * </ul>
 * </li>
 * <li>The first row of the file must be a header row defining the columns.</li>
 * <li>The file must contain exactly one identifier column. This column must
 * have a header of [ID].
 * <ul>
 * <li>The contents of this column are subclass dependent.</li>
 * </ul>
 * </li>
 * <li>The file can define 1 or more comparisons. A comparison is a grouping of
 * columns defining the results of a statistical analysis. Valid comparison
 * columns are:
 * <ul>
 * <li>Required: [M][<i>comparison name</i>]: Fold Change</li>
 * <li>Optional: [P][<i>comparison name</i>]: P-value</li>
 * <li>Optional: [A][<i>comparison name</i>]: Abundance</li>
 * <li>Optional: [AS][<i>comparison name</i>]: Analyst Selection</li>
 * </ul>
 * </li>
 * <li>Each comparison must define each column only once.</li>
 * <li>Any column with a header not defined above will be ignored.</li>
 * </ul>
 *
 * @author Steve Ungerer
 */
public abstract class IdAMPDataFileService implements DataFileService {
    private static final Logger logger = LoggerFactory
            .getLogger(IdAMPDataFileService.class);

    private static final Pattern ID_PATTERN = Pattern.compile("\\[ID\\]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COL_PATTERN = Pattern
            .compile("\\[(.+)]\\s*\\[(.+)\\]");

    private enum Column {
        DIRECTION("M"), PVALUE("P"), ABUNDANCE("A"), ANALYST_SELECTION("AS");

        private final String label;

        private Column(String label) {
            this.label = label;
        }

        static Column forLabel(String label) {
            if (label == null) {
                throw new InvalidArgument("label must not be null");
            }
            label = label.toUpperCase();
            for (Column c : values()) {
                if (label.equals(c.label)) {
                    return c;
                }
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Comparison> process(File file, Namespace namespace)
            throws DataFileException {
        // ID col idx
        int idIdx = -1;
        // comparison name (UC) : comparison name (orig case)
        Map<String, String> compMap = new HashMap<String, String>();
        // comparison name (UC) : map<column, column index>
        Map<String, Map<Column, Integer>> colMap = new HashMap<String, Map<Column, Integer>>();
        // comparison name (UC) : collection<Measurement>
        Map<String, Collection<Measurement>> compMeasure = new HashMap<String, Collection<Measurement>>();
        try {
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] row = reader.readNext();
            for (int i = 0; i < row.length; i++) {
                if (ID_PATTERN.matcher(row[i]).matches()) {
                    if (idIdx != -1) {
                        throw new DataFileException("Duplicate id columns: "
                                + idIdx + " and " + i);
                    }
                    logger.debug("ID column at index {}", i);
                    idIdx = i;
                    continue;
                }
                Matcher m = COL_PATTERN.matcher(row[i]);
                if (!m.matches()) {
                    logger.debug("Ignoring column {} at index {}", row[i], i);
                    continue;
                }
                String colLabel = m.group(1);
                Column col = Column.forLabel(colLabel);
                if (col == null) {
                    logger.debug("Unrecognized column {} at index {}", row[i],
                            i);
                    continue;
                }
                String compName = m.group(2);
                String compNameUC = compName.toUpperCase();
                Map<Column, Integer> compCols = colMap.get(compNameUC);
                if (compCols == null) {
                    compCols = new HashMap<Column, Integer>();
                    colMap.put(compNameUC, compCols);
                    // orig name
                    compMap.put(compNameUC, compName);
                    // init measurements
                    compMeasure.put(compNameUC, new ArrayList<Measurement>());
                }
                if (compCols.containsKey(col)) {
                    throw new DataFileException("Comparison " + compName
                            + " defines duplicate " + colLabel
                            + " columns at columns " + compCols.get(col)
                            + " and " + i);
                }
                compCols.put(col, i);
            }

            // validate direction is present
            for (Map.Entry<String, Map<Column, Integer>> comp : colMap
                    .entrySet()) {
                if (!comp.getValue().containsKey(Column.DIRECTION)) {
                    throw new DataFileException("Comparison "
                            + compMap.get(comp.getKey()) + " must define a "
                            + Column.DIRECTION.label + " column");
                }
            }

            // process each row
            int lineNum = 1;
            while ((row = reader.readNext()) != null) {
                Term term;
                try {
                    term = convertIdToBel(row[idIdx], namespace);
                } catch (Exception e) {
                    throw new DataFileException("Line " + lineNum
                            + ": Cannot convert ID to BEL: " + row[idIdx]);
                }
                for (Map.Entry<String, Map<Column, Integer>> comp : colMap
                        .entrySet()) {
                    Collection<Measurement> measurements = compMeasure.get(comp
                            .getKey());
                    Double direction = null, pVal = null, abun = null;
                    boolean anlst = false;
                    for (Map.Entry<Column, Integer> col : comp.getValue()
                            .entrySet()) {
                        switch (col.getKey()) {
                        case DIRECTION:
                            try {
                                direction = Double.valueOf(row[col.getValue()]);
                            } catch (NumberFormatException e) {
                                throw new DataFileException("Line " + lineNum
                                        + ": Invalid Direction: "
                                        + row[col.getValue()]);
                            }
                            break;
                        case PVALUE:
                            try {
                                pVal = Double.valueOf(row[col.getValue()]);
                            } catch (NumberFormatException e) {
                                throw new DataFileException("Line " + lineNum
                                        + ": Invalid p-value: "
                                        + row[col.getValue()]);
                            }
                            break;
                        case ABUNDANCE:
                            try {
                                abun = Double.valueOf(row[col.getValue()]);
                            } catch (NumberFormatException e) {
                                throw new DataFileException("Line " + lineNum
                                        + ": Invalid Abundance: "
                                        + row[col.getValue()]);
                            }
                            break;
                        case ANALYST_SELECTION:
                            anlst = "1".equals(row[col.getValue()]);
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Unsupported Column enum");
                        }
                    }
                    Measurement m = createMeasurement(row[idIdx], term,
                            direction, pVal, abun, anlst);
                    measurements.add(m);
                }
                lineNum++;
            }

            Collection<Comparison> ret = new ArrayList<Comparison>();
            // create comparisons
            for (Map.Entry<String, String> entry : compMap.entrySet()) {
                ret.add(new Comparison(entry.getValue(), compMeasure.get(entry
                        .getKey())));
            }

            return ret;
        } catch (IOException e) {
            throw new DataFileException(e);
        }
    }

    /**
     * Create a measurement object
     * @param id
     * @param term
     * @param direction
     * @param pValue
     * @param abundance
     * @param analystSelection
     * @return
     */
    protected Measurement createMeasurement(String id, Term term,
            Double direction, Double pValue, Double abundance,
            boolean analystSelection) {
        return new Measurement(term, direction, pValue, abundance,
                analystSelection);
    }

    /**
     * Convert a String identifier to a valid BEL {@link Term}.
     *
     * @param id
     * @param namespace
     * @return
     * @throws Exception
     */
    protected abstract Term convertIdToBel(String id, Namespace namespace)
            throws Exception;
}
