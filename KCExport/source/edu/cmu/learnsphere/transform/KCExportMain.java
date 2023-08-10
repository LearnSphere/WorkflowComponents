package edu.cmu.learnsphere.transform;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class KCExportMain extends AbstractComponent {

	/** Component option (KCModel). */
	String KCModel = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        KCExportMain tool = new KCExportMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public KCExportMain() {
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


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/kcm_export.txt");

		this.addOutputFile(outputFile0, 0, 0, "tab-delimited");


        System.out.println(this.getOutput());

    }
}
