package edu.cmu.pslc.learnsphere.transform.MOOCdb;

import java.io.File;

import org.jdom.Element;

import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class GenerateMOOCdbFeatures extends AbstractComponent {
	/** Dummy option (string). */
    String dummyStr = null;
    /** Dummy option (boolean). */
    Boolean dummyBool = null;

	public static void main(String[] args) {
		GenerateMOOCdbFeatures tool = new GenerateMOOCdbFeatures();
		tool.startComponent(args);
	}

	public GenerateMOOCdbFeatures() {
		super();
	}

	@Override
	protected void parseOptions() {

		if (this.getOptionAsString("dummyStr") != null) {
			dummyStr = this.getOptionAsString("dummyStr");
		}
		if (this.getOptionAsString("dummyBool") != null) {
			dummyBool = this.getOptionAsBoolean("dummyBool");
		}

	}


    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // Add column headers we might expect to generate in the output nodes
        this.addMetaData("tab-delimited", 0, META_DATA_LABEL, "label0", 0, "Dummy Header (" + dummyStr + ")");
    }

	@Override
	protected void runComponent() {
		// Run the program and add the files it generates to the component
		// output.
		File outputDirectory = this.runExternal();

		// Attach the output files to the component output with
		// addOutputFile(..>)
		if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
			File file0 = new File(outputDirectory.getAbsolutePath() + "/ft_output_pickle");
			if (file0 != null && file0.exists()) {
				Integer nodeIndex0 = 0;
				Integer fileIndex0 = 0;
				String label0 = "tab-deliminated";
				this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

			} else {
				this.addErrorMessage("An unknown error has occurred.");
			}

		}

		// Send the component output back to the workflow.
		System.out.println(this.getOutput());
	}

}
