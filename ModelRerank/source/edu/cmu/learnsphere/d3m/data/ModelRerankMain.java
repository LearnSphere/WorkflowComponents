package edu.cmu.learnsphere.d3m.data;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ModelRerankMain extends AbstractComponent {

	/** Component option (model_id). */
	String model_id = null;
	/** Component option (new_rank). */
	String new_rank = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ModelRerankMain tool = new ModelRerankMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ModelRerankMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("ranked-models", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("ranked-models", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("model_id") != null) {
		model_id = this.getOptionAsString("model_id");
	}
	if(this.getOptionAsString("new_rank") != null) {
		new_rank = this.getOptionAsString("new_rank");
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
