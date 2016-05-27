package edu.cmu.pslc.learnsphere.analysis.afm;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class PyAfmMain extends AbstractComponent {

    public static void main(String[] args) {

        PyAfmMain tool = new PyAfmMain();
        tool.startComponent(args);
    }

    public PyAfmMain() {
        super();
    }

    @Override
    protected void runComponent() {
        // Run the program and return its stdout to a file.
        File outputDirectory = this.runExternalMultipleFileOuput();

        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        String fileLabel = "student-step";

        File file0 = new File(outputDirectory.getAbsolutePath() + "/output.txt");

        this.addOutputFile(file0, nodeIndex, fileIndex, fileLabel);
        // Send the component output bakc to the workflow.
        System.out.println(this.getOutput());
    }

}
