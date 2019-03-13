package edu.cmu.learnsphere.d3m.data;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class MetricSelectorMain extends AbstractComponent {

	/** Component option (metric). */
	String metric = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        MetricSelectorMain tool = new MetricSelectorMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public MetricSelectorMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.


    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("metric") != null) {
		metric = this.getOptionAsString("metric");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/metric-list.tsv");

		this.addOutputFile(outputFile0, 0, 0, "metric-list");


        System.out.println(this.getOutput());

    }
}
