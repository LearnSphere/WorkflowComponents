/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2018
 * All Rights Reserved
 */

package edu.cmu.pslc.learnsphere.imports.Import;

import java.io.File;

public class TabDelimited extends AbstractImportDataType {


    /*
    	Use this function to add meta data and preprocess the input
    */
    @Override
    public void processImportFile(File importedFile, ImportMain component) {
        Integer outNodeIndex0 = 0;
        String type = "tab-delimited";
        //this.addMetaDataFromInput("tab-delimited", 0, outNodeIndex0, ".*");
        return;
    }

    /*
    	Determine if the imported file is in the correct format for the data type selected
    */
    @Override
    public boolean validateImportedFile(File importedFile) {
        return true;
    }
}