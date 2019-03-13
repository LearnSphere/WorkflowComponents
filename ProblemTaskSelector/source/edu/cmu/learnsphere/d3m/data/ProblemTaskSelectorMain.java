package edu.cmu.learnsphere.d3m.data;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ProblemTaskSelectorMain extends AbstractComponent {

	/** Component option (task_name). */
	String task_name = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ProblemTaskSelectorMain tool = new ProblemTaskSelectorMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ProblemTaskSelectorMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("dataset", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("problem-task", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("task_name") != null) {
		task_name = this.getOptionAsString("task_name");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/problemTask.tsv");

		this.addOutputFile(outputFile0, 0, 0, "problem-task");


        System.out.println(this.getOutput());

    }
}
