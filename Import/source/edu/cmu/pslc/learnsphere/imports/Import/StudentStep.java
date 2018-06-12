/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2018
 * All Rights Reserved
 */

package edu.cmu.pslc.learnsphere.imports.Import;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.pslc.statisticalCorrectnessModeling.utils.FileHelper;

import static edu.cmu.pslc.datashop.util.FileUtils.truncateFile;


public class StudentStep extends AbstractImportDataType {

	private static Integer NUM_INPUT_LINES_TO_CHECK = 200;

	/*
		Use this function to add meta data and preprocess the input
	*/
	@Override
	public void processImportFile(File importedFile, ImportMain component) {
		Integer outNodeIndex0 = 0;
		String type = "student-step";
		//component.addMetaDataFromInput(type, 0, outNodeIndex0, ".*");
		return;
	}

	/*
		Determine if the imported file is in the correct format for the data type selected
	*/
	@Override
	public boolean validateImportedFile(File importedFile) {
		try {
			verifyInputFile(importedFile);
		} catch (Exception e) {
			logger.error("Couldn't verify Imported file as student-step: " + e.toString());
			return false;
		}
		return true;
	}

	/**
     * Verify the input file.
     * @param inputFile the File
     * @throws failure to verify the file will throw an exception
     */
    private void verifyInputFile(File inputFile)
        throws Exception
    {
        File shortFile = null;
        try {
            // truncate file...
            shortFile = truncateFile(inputFile, NUM_INPUT_LINES_TO_CHECK);

            // Get list of model names present in file
            List<String> modelNames = getKCModelNames(shortFile);

            List<Long> invalidLines = new ArrayList<Long>();

            // Verify for each model
            for (String modelName : modelNames) {
                File sssvsFile = FileHelper.getSSSVSFromStepRollupExport(shortFile, modelName,
                                                                         false, invalidLines);
                if (sssvsFile != null && sssvsFile.length() > 0
                    && sssvsFile.exists() && sssvsFile.canRead()) {
                    logger.debug("File verified for KC: " + modelName);
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (shortFile != null) {
                try {
                    shortFile.delete();
                } catch (SecurityException e) {
                    logger.error("Failed to delete temporary file.");
                }
            }
        }
    }

    /*
     * Regex for KCM names... the name will be in group(2).
     */
    private static final Pattern KCM_PATTERN = Pattern.compile("(?i)\\s*KC\\s*\\(.*\\)\\s*");

    /**
     * Method to determine what KC models are present in the input file.
     * @param inputFile the file
     * @return list of KC model names
     */
    private List<String> getKCModelNames(File inputFile)
        throws Exception
    {
        List<String> result = new ArrayList<String>();

        BufferedReader br = null;
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(inputFile);
            br = new BufferedReader(new InputStreamReader(fis, "utf8"));

            String line = null;
            if ((line = br.readLine()) != null) {
                for (String header : line.split("\t")) {
                	/* 	DID THIS EVER WORK?
                	String modelName = this.getOptionAsString("model")
            			.replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
                	result.add(modelName);*/
                }
            }
        } catch (Exception e) {
            logger.info("Failed to determine KCModel names. " + e);
        } finally {
            if (br != null) {
                br.close();
            }
            if (fis != null) {
                fis.close();
            }
        }

        return result;
    }

}