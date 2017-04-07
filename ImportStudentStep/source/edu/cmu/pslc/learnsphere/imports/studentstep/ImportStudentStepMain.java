package edu.cmu.pslc.learnsphere.imports.studentstep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.pslc.statisticalCorrectnessModeling.utils.FileHelper;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

import static edu.cmu.pslc.datashop.util.FileUtils.truncateFile;

public class ImportStudentStepMain extends AbstractComponent {

    private static Integer NUM_INPUT_LINES_TO_CHECK = 200;

    public static void main(String[] args) {

        ImportStudentStepMain tool = new ImportStudentStepMain();
        tool.startComponent(args);
    }

    public ImportStudentStepMain() {
        super();
    }

    /**
     * Processes the student-step file and verify the first N lines.
     */
    @Override
    protected void runComponent() {

        File theFile = this.getAttachment(0, 0);

        try {

            verifyInputFile(this.getAttachment(0, 0));

        } catch (Exception e) {
            logger.info("Verify of first " + NUM_INPUT_LINES_TO_CHECK + " failed: " + e);
            this.addErrorMessage(e.toString());
            System.err.println(e);
            return;
        }

        System.out.println(this.getOutput());
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
    private static final Pattern KCM_PATTERN = Pattern.compile("(KC \\()(.*)\\)");

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
                    Matcher m = KCM_PATTERN.matcher(header);
                    while (m.find()) {
                        result.add(m.group(2));
                    }
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
