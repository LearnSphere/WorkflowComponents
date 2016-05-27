package edu.cmu.pslc.learnsphere.analysis.RLMFitting;

import java.io.File;

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
