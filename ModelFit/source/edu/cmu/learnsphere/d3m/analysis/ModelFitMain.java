package edu.cmu.learnsphere.d3m.analysis;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ModelFitMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ModelFitMain tool = new ModelFitMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ModelFitMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.


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

		this.addOutputFile(outputFile0, 0, 0, "fitted-model-set");
		this.addOutputFile(outputFile1, 1, 0, "predictions");


        System.out.println(this.getOutput());

    }
}
