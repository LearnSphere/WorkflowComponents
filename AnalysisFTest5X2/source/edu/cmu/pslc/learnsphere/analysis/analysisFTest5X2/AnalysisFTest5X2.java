package edu.cmu.pslc.learnsphere.analysis.analysisFTest5X2;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class AnalysisFTest5X2 extends AbstractComponent {

    /** Component option (model). */
    String modelName = null;

    public static void main(String[] args) {

        AnalysisFTest5X2 tool = new AnalysisFTest5X2();
        tool.startComponent(args);
    }

    public AnalysisFTest5X2() {
        super();
    }

    @Override
    protected void parseOptions() {
	

     
    }

    @Override
    protected void processOptions() {
 
    }

    @Override
    protected void runComponent() {
        // Run the program...
       // File outputDirectory = this.runExternal();
        File outputDirectory = this.runExternalMultipleFileOuput();
        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        String fileLabel = "text";
        File studentStepFile = new File(outputDirectory.getAbsolutePath() + "/R_output_model_summary.txt");
        this.addOutputFile(studentStepFile, nodeIndex, fileIndex, fileLabel);
/*
	nodeIndex = 1;
        fileLabel = "model-values";
        File valuesFile = new File(outputDirectory.getAbsolutePath() + "/test.html");
        this.addOutputFile(valuesFile, nodeIndex, fileIndex, fileLabel);
*/
	
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
}
