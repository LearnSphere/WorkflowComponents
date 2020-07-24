package edu.cmu.learnsphere.visualization;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class trendMAPMain extends AbstractComponent {

	/** Component option (discipline). */
	String discipline = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        trendMAPMain tool = new trendMAPMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public trendMAPMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("csv", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("image", 0, META_DATA_LABEL, "label0", 0, null);
	this.addMetaData("csv", 1, META_DATA_LABEL, "label1", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("discipline") != null) {
		discipline = this.getOptionAsString("discipline");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/output0.png");
	File outputFile1 = new File(outputDirectory.getAbsolutePath() + "/output1.csv");

		this.addOutputFile(outputFile0, 0, 0, "image");
		this.addOutputFile(outputFile1, 1, 0, "csv");


        System.out.println(this.getOutput());

    }
}
