package edu.cmu.learnsphere.d3m.data;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ProblemCreatorMain extends AbstractComponent {

	/** Component option (probname). */
	String probname = null;
	/** Component option (probdesc). */
	String probdesc = null;
	/** Component option (targetname). */
	String targetname = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ProblemCreatorMain tool = new ProblemCreatorMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ProblemCreatorMain() {
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

	if(this.getOptionAsString("probname") != null) {
		probname = this.getOptionAsString("probname");
	}
	if(this.getOptionAsString("probdesc") != null) {
		probdesc = this.getOptionAsString("probdesc");
	}
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
