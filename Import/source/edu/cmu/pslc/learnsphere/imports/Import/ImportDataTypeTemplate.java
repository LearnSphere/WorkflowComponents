/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2018
 * All Rights Reserved
 */

package edu.cmu.pslc.learnsphere.imports.Import;

import java.io.File;

public class ImportDataTypeTemplate extends AbstractImportDataType {

    /**
     * Use this function to add meta data and preprocess the input
     */
    @Override
    public void processImportFile(File importedFile, ImportMain component) {
        return;
    }

    /**
     * Determine if the imported file is in the correct format for the data type selected
     */
    @Override
    public boolean validateImportedFile(File importedFile) {
        return true;
    }
}