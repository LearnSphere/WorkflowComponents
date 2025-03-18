package edu.cmu.learnsphere.analysis;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ProgressScoresConsolidatedReportMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ProgressScoresConsolidatedReportMain tool = new ProgressScoresConsolidatedReportMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ProgressScoresConsolidatedReportMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("csv", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("file", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {


    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/output.csv");

		this.addOutputFile(outputFile0, 0, 0, "file");


        System.out.println(this.getOutput());

    }
}
