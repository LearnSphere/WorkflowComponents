package edu.cmu.pslc.learnsphere.analysis.UnzipTemplate;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class UnzipTemplate extends AbstractComponent {

    public static void main(String[] args) {

        UnzipTemplate tool = new UnzipTemplate();
        tool.startComponent(args);
    }

    public UnzipTemplate() {
        super();
    }

    @Override
    protected void runComponent() {

    	Integer inNodeIndex = 0;
    	Integer inFileIndex = 0;
    	File inputFile = null;
    	try {
    		inputFile = this.getAttachmentAndUnzip(inNodeIndex, inFileIndex);
    	} catch (Exception e) {
    		System.err.println("Cannot unzip compressed input file.");
    	}

    	if (inputFile != null) {
    		File rCreatedFile = new File(inputFile.getAbsolutePath().replaceAll("\\\\", "/") + "/ds1_student_step_export.txt");
	    	this.setInputFile(inNodeIndex, inFileIndex, rCreatedFile);
	        // Run the program and return its stdout to a file.
	        File outputDirectory = this.runExternal();

	        Integer nodeIndex = 0;
	        Integer fileIndex = 0;
	        String fileLabel = "text";

	        this.addOutputFile(new File(outputDirectory + "/my_output_file.txt"), nodeIndex, fileIndex, fileLabel);
    	}
        // Send the component output bakc to the workflow.
        System.out.println(this.getOutput());
    }

}
