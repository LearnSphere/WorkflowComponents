package edu.cmu.learnsphere.d3m.analysis;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ModelSearchMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ModelSearchMain tool = new ModelSearchMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ModelSearchMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("dataset", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("fitted-model-set", 0, META_DATA_LABEL, "label0", 0, null);
	this.addMetaData("dataset", 1, META_DATA_LABEL, "label1", 0, null);
	this.addMetaData("predictions", 2, META_DATA_LABEL, "label2", 0, null);

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


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/fit-models.tsv");
	File outputFile1 = new File(outputDirectory.getAbsolutePath() + "/datasetDoc.tsv");
	File outputFile2 = new File(outputDirectory.getAbsolutePath() + "/predictions.tsv");

		this.addOutputFile(outputFile0, 0, 0, "fitted-model-set");
		this.addOutputFile(outputFile1, 1, 0, "dataset");
		this.addOutputFile(outputFile2, 2, 0, "predictions");


        System.out.println(this.getOutput());

    }
}
