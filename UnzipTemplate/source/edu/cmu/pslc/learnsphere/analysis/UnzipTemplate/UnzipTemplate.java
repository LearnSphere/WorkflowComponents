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

protected void runComponent2() {

	Integer inNodeIndex = 0;
	Integer inFileIndex = 0;
	File zipOutputDirectory = null;
	try {
		zipOutputDirectory = this.getAttachmentAndUnzip(inNodeIndex, inFileIndex);
	} catch (Exception e) {
		System.err.println("Cannot unzip compressed input file.");
	}

	if (zipOutputDirectory != null) {
		// Get the desired file path (relative)
		String zipOutputPath = zipOutputDirectory.getAbsolutePath().replaceAll("\\\\", "/");
		// In this example, we already know the realtive path to the desired file.
		String desiredFile =  zipOutputPath + "/ds1_student_step_export.txt";
		File rCreatedFile = new File(desiredFile);
    	this.setInputFile(inNodeIndex, inFileIndex, rCreatedFile);
        // Run the program and return its stdout to a file.
        File outputDirectory = this.runExternal();

        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        String fileLabel = "text";
        // Add the new file (created by analysis.R) to the output file list.
        this.addOutputFile(new File(outputDirectory + "/my_output_file.txt"), nodeIndex, fileIndex, fileLabel);
	}
    // Send the component output back to the workflow.
    System.out.println(this.getOutput());
}

@Override
protected void runComponent() {

	Integer inNodeIndex = 0;
	Integer inFileIndex = 0;
	File zipOutputDirectory = null;
	try {
		zipOutputDirectory = this.getAttachmentAndUnzip(inNodeIndex, inFileIndex);
	} catch (Exception e) {
		System.err.println("Cannot unzip compressed input file.");
	}

	if (zipOutputDirectory != null) {
		// Get the file path in a windows-/linux-/mac- friendly format
		String zipOutputPath = zipOutputDirectory.getAbsolutePath().replaceAll("\\\\", "/");
		for (String fileName : zipOutputDirectory.list()) {

			// Use the full file path replace or add the inputs.
			// Uncompressed directories within the zipOutputPath can be used here:
            String filePath = zipOutputPath
        		+ "/"
        	/*  + "someSubDirectory/"             */
            		+ fileName;

            File rCreatedFile = new File(filePath);
            if (rCreatedFile.isFile()) {
	        	this.setInputFile(inNodeIndex, inFileIndex, rCreatedFile);
	        	inNodeIndex++;
            } else if (rCreatedFile.isDirectory()) {
            	// This File object is actually a directory.
            	// We could traverse it or simply ignore it.
            }
	    }

		// Run the program analysis.R (as specified in build.properties),
		// and return its stdout to "my_output_file.txt".
        File outputDirectory = this.runExternal();

        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        String fileLabel = "text";
        // Add the new file (created by analysis.R) to the output file list.
        this.addOutputFile(new File(outputDirectory + "/my_output_file.txt"), nodeIndex, fileIndex, fileLabel);
	}

    // Send the component output back to the workflows platform.
    System.out.println(this.getOutput());
}
}
