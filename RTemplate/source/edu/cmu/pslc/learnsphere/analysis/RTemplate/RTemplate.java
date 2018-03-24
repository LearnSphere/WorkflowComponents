package edu.cmu.pslc.learnsphere.analysis.RTemplate;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class RTemplate extends AbstractComponent {

    public static void main(String[] args) {

        RTemplate tool = new RTemplate();
        tool.startComponent(args);
    }

    public RTemplate() {
        super();
    }

    @Override
    protected void runComponent() {
        // Run the program and return its stdout to a file.
        File outputDirectory = this.runExternal();

        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        String fileLabel = "text";

        this.addOutputFile(new File(outputDirectory + "/my_output_file.txt"), nodeIndex, fileIndex, fileLabel);
        // Send the component output bakc to the workflow.
        System.out.println(this.getOutput());
    }

}
