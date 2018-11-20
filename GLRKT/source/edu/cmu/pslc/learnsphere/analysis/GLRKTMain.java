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
	this.addMetaDataFromInput("transaction", 0, 0, ".*");
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
            File file0 = new File(outputDirectory.getAbsoluteFile() + "/transaction_file_output.txt");
            File file1 = new File(outputDirectory.getAbsolutePath() + "/R_output_model_summary.txt");
            File file2 = new File(outputDirectory.getAbsolutePath() + "/model_result_values.xml");
            
            if (file0 != null && file0.exists() && file1 != null && file1.exists()) {    
            Integer nodeIndex0 = 0;
            Integer fileIndex0 = 0;
            String label0 = "transaction";
            this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

            Integer nodeIndex1 = 1;
            Integer fileIndex1 = 0;
            String label1 = "text";
            this.addOutputFile(file1, nodeIndex1, fileIndex1, label1);

            Integer nodeIndex2 = 2;
            Integer fileIndex2 = 0;
            String label2 = "text";
            this.addOutputFile(file2, nodeIndex2, fileIndex2, label2);
            
            }else{
                this.addErrorMessage("An unknown error has occurred with the TKT component.");
            }
        }

        System.out.println(this.getOutput());

    }
}
