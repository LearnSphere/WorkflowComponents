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
                    System.err.println(errMsg);
                    return;
            }
                    
            this.componentOptions.addContent(0, new Element("i").setText(independentVars));
            this.componentOptions.addContent(0, new Element("d").setText(dependentVars));
            // Run the program and return its stdout to a file.
            File output = this.runExternal();

            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileLabel = "analysis-summary";

            this.addOutputFile(output, nodeIndex, fileIndex, fileLabel);
            // Send the component output back to the workflow.
            System.out.println(this.getOutput());
    }

}
