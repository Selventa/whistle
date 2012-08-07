package com.selventa.whistle.cli;

import java.util.HashMap;
import java.util.Map;

import org.openbel.framework.common.model.Term;

import com.selventa.whistle.data.model.Measurement;
import com.selventa.whistle.data.service.DefaultIdAMPDataFileService;

public class DebugIdAmpDataFileService extends DefaultIdAMPDataFileService {

    private final Map<Measurement, String> idMap = new HashMap<Measurement, String>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected Measurement createMeasurement(String id, Term term,
            Double direction, Double pValue, Double abundance,
            boolean analystSelection) {
        Measurement m = super.createMeasurement(id, term, direction, pValue, abundance,
                analystSelection);
        idMap.put(m, id);
        return m;
    }

    public Map<Measurement, String> getIdMap() {
        return idMap;
    }

}
