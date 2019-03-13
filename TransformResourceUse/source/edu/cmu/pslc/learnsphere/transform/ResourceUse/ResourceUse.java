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
    }

    @Override
    protected void runComponent() {

        String lastRunDate = this.getOptionAsString("lastRunDate");
        Boolean isValid = validateLastRunDate(lastRunDate);
        if (!isValid) {
            addErrorMessage(lastRunDate + " is an invalid date. Must use format: yyyy-MM-dd.");
            System.out.println(this.getOutput());
            return;
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

    private Boolean validateLastRunDate(String lastRunDate) {
        if (lastRunDate == null) { return false; }
        if (lastRunDate.equals("")) { return false; }

        Boolean result = false;
        try {
            Date parsedDate = sdf.parse(lastRunDate);
            String reformatted = sdf.format(parsedDate);

            // SimpleDateFormat will force all sorts of things into the specified format.
            // Ensuring that the formatted parsed date matches the original string means
            // the original string is indeed a valid date. Thank you StackOverflow.
            if (lastRunDate.equals(reformatted)) {
                result = true;
            }
        } catch (Exception ex) {
            logger.debug("Failed to parse date: " + lastRunDate);
            return false;
        }

        return result;
    }
}
