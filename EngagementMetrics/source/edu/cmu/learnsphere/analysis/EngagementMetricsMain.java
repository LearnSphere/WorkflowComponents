package edu.cmu.learnsphere.analysis;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class EngagementMetricsMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        EngagementMetricsMain tool = new EngagementMetricsMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public EngagementMetricsMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("file", 0, 0, ".*");

	// Add additional meta-data for each output file.
this.addMetaData("file", 0, META_DATA_LABEL, "Engagement Metrics Output", 0, null);


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


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/engagement_metrics.txt");

		this.addOutputFile(outputFile0, 0, 0, "tab-delimited");


        System.out.println(this.getOutput());

    }
}
