package edu.cmu.pslc.learnsphere.analysis.GLRKT;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class GLRKTMain extends AbstractComponent {

	/** Component option (model). */

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        GLRKTMain tool = new GLRKTMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public GLRKTMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("tab-delimited", 0, 0, ".*");
	// Add additional meta-data for each output file.
	this.addMetaData("file", 0, META_DATA_LABEL, "label0", 0, null);

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
        
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File outputFile = new File(outputDirectory.getAbsoluteFile() + "/output.txt");
            Integer nodeIndex0 = 0;
            Integer fileIndex0 = 0;
            String label0 = "tab-delimited";
            logger.info("Added file: " + outputFile.getAbsolutePath());
            this.addOutputFile(outputFile, nodeIndex0, fileIndex0, label0);
        }

        System.out.println(this.getOutput());

    }
}
