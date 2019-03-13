package edu.cmu.learnsphere.d3m.data;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ModelSelectorMain extends AbstractComponent {

	/** Component option (model_id). */
	String model_id = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ModelSelectorMain tool = new ModelSelectorMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ModelSelectorMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("model-set", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("model", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("model_id") != null) {
		model_id = this.getOptionAsString("model_id");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/model.json");

		this.addOutputFile(outputFile0, 0, 0, "model");


        System.out.println(this.getOutput());

    }
}
