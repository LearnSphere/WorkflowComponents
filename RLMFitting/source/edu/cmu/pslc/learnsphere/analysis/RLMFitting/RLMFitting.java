package edu.cmu.pslc.learnsphere.analysis.RLMFitting;

import java.io.File;
import java.util.List;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class RLMFitting extends AbstractComponent {

    public static void main(String[] args) {

        RLMFitting tool = new RLMFitting();
        tool.startComponent(args);
    }

    public RLMFitting() {
        super();
    }
    
    @Override
    protected Boolean test() {
        Boolean passing = true;

        return passing;
    }
    
    @Override
    protected void processOptions() {
        logger.info("Processing Options");
    }

    @Override
    protected void runComponent() {
            List<String> independentVarList = this.getMultiOptionAsString("i_wf");
            List<String> dependentVarList = this.getMultiOptionAsString("d_wf");
            String independentVars = "";
            String dependentVars = "";
            if (independentVarList != null && independentVarList.size() != 0){
                    for (String key : independentVarList)
                            independentVars += key + ",";
                    //delete the last comma
                    if (independentVars.lastIndexOf(",") == independentVars.length()-1) {
                            independentVars = independentVars.substring(0, independentVars.length()-1);
                    }
            }
            if (dependentVarList != null && dependentVarList.size() != 0){
                    for (String key : dependentVarList)
                            dependentVars += key + ",";
                    //delete the last comma
                    if (dependentVars.lastIndexOf(",") == dependentVars.length()-1) {
                            dependentVars = dependentVars.substring(0, dependentVars.length()-1);
                    }
            }
            if (dependentVars.trim().length() == 1 || independentVars.trim().length() == 1) {
                    //send out error message
                    String errMsg = "Analysis variables are not defined properly.";
                    addErrorMessage(errMsg);
                    logger.info("RLMFitting aborted: " + errMsg + ". ");
            } else {
                    /*
	            this.componentOptions.addContent(0, new Element("i").setText(independentVars));
	            this.componentOptions.addContent(0, new Element("d").setText(dependentVars));
	            this.componentOptions.addContent(0, new Element("outputFile").setText("R-summary.txt"));
	            */
                    this.setOption("i", independentVars);
                    this.setOption("d", dependentVars);
	            // Run the program and return its stdout to a file.
	            File outputDirectory = this.runExternal();
	            if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                            File file0 = new File(outputDirectory.getAbsolutePath() + "/R-summary.txt");
                            File file1 = new File(outputDirectory.getAbsolutePath() + "/model-values.xml");
                            File file2 = new File(outputDirectory.getAbsolutePath() + "/Parameter-estimate-values.xml");
                            if (file0 != null && file0.exists() && file1 != null && file1.exists() && file2 != null && file2.exists()) {
                                    Integer nodeIndex = 0;
                                    Integer fileIndex = 0;
                                    String label = "analysis-summary";
                                    this.addOutputFile(file0, nodeIndex, fileIndex, label);
                                    nodeIndex = 1;
                                    label = "model-values";
                                    this.addOutputFile(file1, nodeIndex, fileIndex, label);
                                    nodeIndex = 2;
                                    label = "parameters";
                                    this.addOutputFile(file2, nodeIndex, fileIndex, label);
                            } else {
                                    this.addErrorMessage("Can't find expected output file.");
                            }
	            }
            }
            // Send the component output back to the workflow.
            System.out.println(this.getOutput());
            
            for (String err : this.errorMessages) {
                    // These will also be picked up by the workflows platform and relayed to the user.
                    System.err.println(err);
            }

    }

}
