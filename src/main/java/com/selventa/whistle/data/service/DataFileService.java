package com.selventa.whistle.data.service;

import java.io.File;
import java.util.Collection;

import org.openbel.framework.common.model.Namespace;

import com.selventa.whistle.data.model.Comparison;
import com.selventa.whistle.data.model.Measurement;

/**
 * Service responsible for converting a file into a collection of
 * {@link Comparison}s with associated {@link Measurement}s
 *
 * @author Steve Ungerer
 */
public interface DataFileService {

    /**
     * Process a file into a collection of {@link Comparison}s.
     *
     * @param file
     * @return
     * @throws DataFileException
     */
    Collection<Comparison> process(File file, Namespace namespace) throws DataFileException;


    /**
     * Exception indicating a problem occurred while processing a data file
     *
     * @author Steve Ungerer
     */
    class DataFileException extends Exception {
        private static final long serialVersionUID = 1068532524572493196L;

        public DataFileException() {
            super();
        }

        public DataFileException(String message, Throwable cause) {
            super(message, cause);
        }

        public DataFileException(String message) {
            super(message);
        }

        public DataFileException(Throwable cause) {
            super(cause);
        }
    }

}
