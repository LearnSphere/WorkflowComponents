package edu.cmu.learnsphere.analysis;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class D3mPipelineSearchMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        D3mPipelineSearchMain tool = new D3mPipelineSearchMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public D3mPipelineSearchMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.


    }

    @Override
    protected void parseOptions() {


    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/dataset.json");

		this.addOutputFile(outputFile0, 0, 0, "d3m-dataset");


        System.out.println(this.getOutput());

    }
}
