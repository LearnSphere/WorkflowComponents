package edu.cmu.pslc.learnsphere.analysis;

import java.io.File;
import java.text.DecimalFormat;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class DetectorTesterMain extends AbstractComponent {

    public static void main(String[] args) {

        DetectorTesterMain tool = new DetectorTesterMain();
        tool.startComponent(args);
    }

    public DetectorTesterMain() {
        super();
    }

    /**
   * The test() method is used to test the known inputs prior to running.
   * @return true if passing, false otherwise
   */
      @Override
      protected Boolean test() {
        Boolean passing = true;
        return passing;
      }

      /**
       * Parse the options list.
       */
      @Override
      protected void parseOptions() {
        logger.info("Parsing options.");
      }

      @Override
      protected void processOptions() {

      }


    @Override
    protected void runComponent() {
        // Run the program and return its stdout to a file.
        File outputDirectory = this.runExternalMultipleFileOuput();

        //System.out.println(this.getOutput());
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File outputFile = new File(outputDirectory.getAbsolutePath() + "\\output.txt");

            if (outputFile != null && outputFile.exists()) {
                Integer nodeIndex = 0;
                Integer fileIndex = 0;
                String fileLabel = "text";
                logger.debug(outputDirectory.getAbsolutePath() + "\\output.txt");

                this.addOutputFile(outputFile, nodeIndex, fileIndex, fileLabel);
            } else {
                errorMessages.add("cannot add output files");
            }
        } else {
            errorMessages.add("Issue with output directory");
        }

        for (String err : errorMessages) {
            logger.error(err);
        }

        // Send the component output bakc to the workflow.
        System.out.println(this.getOutput());
    }

}
