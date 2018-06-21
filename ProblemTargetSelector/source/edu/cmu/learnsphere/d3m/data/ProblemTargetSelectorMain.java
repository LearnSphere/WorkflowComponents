package edu.cmu.learnsphere.d3m.data;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ProblemTargetSelectorMain extends AbstractComponent {

	/** Component option (targetname). */
	String targetname = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ProblemTargetSelectorMain tool = new ProblemTargetSelectorMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ProblemTargetSelectorMain() {
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
	this.addMetaData("problem-target", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("targetname") != null) {
		targetname = this.getOptionAsString("targetname");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/problemTarget.tsv");

		this.addOutputFile(outputFile0, 0, 0, "problem-target");


        System.out.println(this.getOutput());

    }
}
