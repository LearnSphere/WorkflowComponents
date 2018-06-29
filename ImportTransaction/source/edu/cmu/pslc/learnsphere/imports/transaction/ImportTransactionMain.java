package edu.cmu.pslc.learnsphere.imports.transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.List;

import edu.cmu.pslc.datashop.extractors.ffi.HeadingReport;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

import static edu.cmu.pslc.datashop.util.FileUtils.truncateFile;

public class ImportTransactionMain extends AbstractComponent {

    private static Integer NUM_INPUT_LINES_TO_CHECK = 200;

    public static void main(String[] args) {

        ImportTransactionMain tool = new ImportTransactionMain();
        tool.startComponent(args);
    }

    public ImportTransactionMain() {
        super();
    }

    /**
     * Processes the student-step file and verify the first N lines.
     */
    @Override
    protected void runComponent() {

        File theFile = this.getAttachment(0, 0);

        try {
        	if (theFile != null && theFile.exists()) {
        		verifyInputFile(this.getAttachment(0, 0));
        	} else {
        		addErrorMessage("Could not read required input file.");
        	}

        } catch (Exception e) {
            logger.info("Verify of first " + NUM_INPUT_LINES_TO_CHECK + " failed: " + e);
            this.addErrorMessage(e.toString());
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
            // Initially, we're just verifying the headers.
            HeadingReport hr = HeadingReport.create(getHeadings(inputFile));

            List<String> warnings = hr.getWarnings();
            // At this point, only writing warnings to log, not sending to user.
            for (String w : warnings) {
                logger.warn(w);
            }

            List<String> errors = hr.getErrors();
            for (String err : errors) {
                logger.warn(err);
                this.addErrorMessage(err);
                System.err.println(err);
            }

            // truncate file...
            //            shortFile = truncateFile(inputFile, NUM_INPUT_LINES_TO_CHECK);

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

    /**
     * Helper method to get first line, the headings, from the input file.
     * @param inputFile the file
     * @return the headings, or first line of the file
     * @throws Exception any failure to read the file
     */
    private String getHeadings(File inputFile)
        throws Exception
    {
        String result = null;

        BufferedReader br = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(inputFile);
            br = new BufferedReader(new InputStreamReader(fis, "utf8"));
            result = br.readLine();
        } finally {
            if (br != null) { br.close(); }
            if (fis != null) { fis.close(); }
        }

        return result;
    }
}
