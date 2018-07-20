package edu.cmu.learnsphere.d3m.analysis;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ModelRankMain extends AbstractComponent {

	/** Component option (ordering). */
	String ordering = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ModelRankMain tool = new ModelRankMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ModelRankMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.
		this.addMetaData("d3m-dataset", 0, META_DATA_LABEL, "label0", 0, null);


	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("dataset", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("ranked-models", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("ordering") != null) {
		ordering = this.getOptionAsString("ordering");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/ranked-models.tsv");

		this.addOutputFile(outputFile0, 0, 0, "ranked-models");


        System.out.println(this.getOutput());

    }
}
