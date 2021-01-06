package edu.cmu.learnsphere.visualization;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class descriptionMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        descriptionMain tool = new descriptionMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public descriptionMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("csv", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("image", 0, META_DATA_LABEL, "label0", 0, null);
	this.addMetaData("image", 1, META_DATA_LABEL, "label1", 0, null);
	this.addMetaData("image", 2, META_DATA_LABEL, "label2", 0, null);
	this.addMetaData("image", 3, META_DATA_LABEL, "label3", 0, null);

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


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/output0.png");
	File outputFile1 = new File(outputDirectory.getAbsolutePath() + "/output1.png");
	File outputFile2 = new File(outputDirectory.getAbsolutePath() + "/output2.png");
	File outputFile3 = new File(outputDirectory.getAbsolutePath() + "/output3.png");

		this.addOutputFile(outputFile0, 0, 0, "image");
		this.addOutputFile(outputFile1, 1, 0, "image");
		this.addOutputFile(outputFile2, 2, 0, "image");
		this.addOutputFile(outputFile3, 3, 0, "image");


        System.out.println(this.getOutput());

    }
}
