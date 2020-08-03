package edu.cmu.learnsphere.visualization;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class iAFMgraphMain extends AbstractComponent {

	/** Component option (discipline). */
	String discipline = null;
	/** Component option (choice). */
	String choice = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        iAFMgraphMain tool = new iAFMgraphMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public iAFMgraphMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("csv", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("png", 0, META_DATA_LABEL, "label0", 0, null);
	this.addMetaData("png", 1, META_DATA_LABEL, "label1", 0, null);
	this.addMetaData("csv", 2, META_DATA_LABEL, "label2", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("discipline") != null) {
		discipline = this.getOptionAsString("discipline");
	}
	if(this.getOptionAsString("choice") != null) {
		choice = this.getOptionAsString("choice");
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
	File outputFile1 = new File(outputDirectory.getAbsolutePath() + "/output1.png");
	File outputFile2 = new File(outputDirectory.getAbsolutePath() + "/output2.csv");

		this.addOutputFile(outputFile0, 0, 0, "png");
		this.addOutputFile(outputFile1, 1, 0, "png");
		this.addOutputFile(outputFile2, 2, 0, "csv");


        System.out.println(this.getOutput());

    }
}
