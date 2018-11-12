package edu.cmu.learnsphere.analysis.rise;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class RISEMain extends AbstractComponent {

    /** Component option (generatePlot). */
    String generatePlot = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        RISEMain tool = new RISEMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public RISEMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.
	// Add additional meta-data for each output file.
	this.addMetaData("pdf", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("generatePlot") != null) {
		generatePlot = this.getOptionAsString("generatePlot");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();

	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/rise.txt");
	File outputFile1 = new File(outputDirectory.getAbsolutePath() + "/rise.pdf");

        this.addOutputFile(outputFile0, 0, 0, "tab-delimited");
        this.addOutputFile(outputFile1, 1, 0, "pdf");

        System.out.println(this.getOutput());

    }
}
