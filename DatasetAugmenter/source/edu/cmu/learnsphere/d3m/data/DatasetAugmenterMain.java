package edu.cmu.learnsphere.d3m.data;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class DatasetAugmenterMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        DatasetAugmenterMain tool = new DatasetAugmenterMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public DatasetAugmenterMain() {
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


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/datasetQuery.json");
	File outputFile1 = new File(outputDirectory.getAbsolutePath() + "/app.html");

		this.addOutputFile(outputFile0, 0, 0, "dataset-query");
		this.addOutputFile(outputFile1, 1, 0, "inline-html");


        System.out.println(this.getOutput());

    }
}
