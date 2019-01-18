package edu.cmu.learnsphere.d3m.data;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ModelExportMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ModelExportMain tool = new ModelExportMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ModelExportMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.
		this.addMetaData("d3m-dataset", 0, META_DATA_LABEL, "label0", 0, null);


	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("ranked-models", 0, 0, ".*");

	// Add additional meta-data for each output file.

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





        System.out.println(this.getOutput());

    }
}
