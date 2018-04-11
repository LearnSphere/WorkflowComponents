package edu.cmu.pslc.learnsphere.imports.discoursedb;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ImportDiscourseDB extends AbstractComponent {

    public static void main(String[] args) {

        ImportDiscourseDB tool = new ImportDiscourseDB();
        tool.startComponent(args);
    }

    public ImportDiscourseDB() {
        super();
    }

    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternal();
        // Attach the output files to the component output.
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            Integer nodeIndex = 0;
            this.addOutputFiles(outputDirectory.getAbsolutePath(), nodeIndex);

        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }

}
