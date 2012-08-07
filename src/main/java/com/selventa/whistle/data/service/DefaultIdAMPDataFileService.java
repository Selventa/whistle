package com.selventa.whistle.data.service;

import org.openbel.framework.common.enums.FunctionEnum;
import org.openbel.framework.common.model.Namespace;
import org.openbel.framework.common.model.Parameter;
import org.openbel.framework.common.model.Term;

/**
 * Handle IdAMP files that are either in BEL format, or raw identifiers.<br>
 * If the identifier is in BEL format, simply parse it to a {@link Term}. If the
 * identifier is not BEL, create a {@link FunctionEnum#RNA_ABUNDANCE}
 * {@link Term} for the value.
 *
 * @author Steve Ungerer
 */
public class DefaultIdAMPDataFileService extends IdAMPDataFileService {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Term convertIdToBel(String id, Namespace ns) {
        //FIXME handle BEL
        Term t = new Term(FunctionEnum.RNA_ABUNDANCE);
        t.addFunctionArgument(new Parameter(ns, id));
        return t;
    }

}
