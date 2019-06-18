package edu.cmu.pslc.learnsphere.transform.ResourceUse;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ResourceUse extends AbstractComponent {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) {

        ResourceUse tool = new ResourceUse();
        tool.startComponent(args);
    }

    public ResourceUse() {
        super();
    }

    @Override
    protected void parseOptions() {
	logger.info("Parsing options");
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        this.addMetaDataFromInput("tab-delimited", 0, 0, ".*");
    }

    @Override
    protected void runComponent() {

        Boolean specifyRange = this.getOptionAsString("specifyRange").equals("Yes") ? true : false;

        String startDate = this.getOptionAsString("startDate");
        Boolean isValid = validateDateForR(startDate);
        if (!isValid) {
            addErrorMessage(startDate + " is an invalid date. Must use format: yyyy-MM-dd.");
            System.out.println(this.getOutput());
            return;
        }
        if (specifyRange) {
            String endDate = this.getOptionAsString("endDate");
            isValid = validateDateForR(endDate);
            // For the end date, if not specified or not valid, we default to "open" or end of file.
            if (!isValid) {
                this.setOption("endDate", "");
                logger.warn(endDate + " is an invalid date. Defaulting to last date in file.");
            }
        } else {
            this.setOption("endDate", "");
        }

        // Run the program...
        File outputDirectory = this.runExternal();

        Integer fileIndex = 0;
        Integer nodeIndex = 0;
        String fileLabel = "csv";
        File outputFile = new File(outputDirectory.getAbsolutePath() + "/stu_summary.csv");
        this.addOutputFile(outputFile, nodeIndex, fileIndex, fileLabel);

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }

    private Boolean validateDateForR(String dateStr) {
        if (dateStr == null) { return false; }
        if (dateStr.equals("")) { return false; }

        Boolean result = false;
        try {
            Date parsedDate = sdf.parse(dateStr);
            String reformatted = sdf.format(parsedDate);

            // SimpleDateFormat will force all sorts of things into the specified format.
            // Ensuring that the formatted parsed date matches the original string means
            // the original string is indeed a valid date. Thank you StackOverflow.
            if (dateStr.equals(reformatted)) {
                result = true;
            }
        } catch (Exception ex) {
            logger.debug("Failed to parse date: " + dateStr);
            return false;
        }

        return result;
    }
}
