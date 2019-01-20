package edu.cmu.learnsphere.d3m.visualization;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class CompareModelPredictionsMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        CompareModelPredictionsMain tool = new CompareModelPredictionsMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public CompareModelPredictionsMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("dataset", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("inline-html", 0, META_DATA_LABEL, "label0", 0, null);

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


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/viz.html");

		this.addOutputFile(outputFile0, 0, 0, "inline-html");


        System.out.println(this.getOutput());

    }
}
