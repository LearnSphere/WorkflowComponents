package edu.cmu.learnsphere.transform;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: generates session-level logs from educational interaction data.
 */
public class CreateSessionMain extends AbstractComponent {

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
        CreateSessionMain tool = new CreateSessionMain();
        tool.startComponent(args);
    }

    /** Constructor. */
    public CreateSessionMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // Propagate input metadata headers to output
        this.addMetaDataFromInput("file", 0, 0, ".*");

        // Add a descriptive label for the output
        this.addMetaData("file", 0, META_DATA_LABEL, "Session Output", 0, null);
    }

    @Override
    protected void parseOptions() {
        // No options to parse in this component
    }

    @Override
    protected void runComponent() {
        File outputDirectory = this.runExternal();  // executes run.py
        File outputFile0 = new File(outputDirectory, "sessions_custom.txt");

        // Register the output file with LearnSphere
        this.addOutputFile(outputFile0, 0, 0, "tab-delimited");

        // Emit output XML
        System.out.println(this.getOutput());
    }
}
