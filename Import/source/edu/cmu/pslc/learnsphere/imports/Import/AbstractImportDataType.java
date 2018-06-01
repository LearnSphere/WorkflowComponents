/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2018
 * All Rights Reserved
 */

package edu.cmu.pslc.learnsphere.imports.Import;

import java.io.File;

import org.apache.log4j.Logger;

public abstract class AbstractImportDataType {

	public Logger logger = null;

	public void addLogger(Logger l) {
		this.logger = l;
	}

	/*
		Use this function to add meta data and preprocess the input
	*/
	public void processImportFile(File importedFile) {
		return;
	}

	/*
		Determine if the imported file is in the correct format for the data type selected
	*/
	public boolean validateImportedFile(File importedFile) {
		return true;
	}
}