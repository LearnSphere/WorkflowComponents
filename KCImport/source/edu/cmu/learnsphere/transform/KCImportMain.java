package edu.cmu.learnsphere.transform;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class KCImportMain extends AbstractComponent {

	/** Component option (KCModel). */
	String KCModel = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        KCImportMain tool = new KCImportMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public KCImportMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("student-step", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("tab-delimited", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("KCModel") != null) {
		KCModel = this.getOptionAsString("KCModel");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/updated_studentstep.txt");

		this.addOutputFile(outputFile0, 0, 0, "tab-delimited");


        System.out.println(this.getOutput());

    }
}
