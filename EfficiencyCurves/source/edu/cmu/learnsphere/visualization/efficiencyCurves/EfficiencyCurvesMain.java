package edu.cmu.learnsphere.visualization.efficiencyCurves;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class EfficiencyCurvesMain extends AbstractComponent {

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        EfficiencyCurvesMain tool = new EfficiencyCurvesMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public EfficiencyCurvesMain() {
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


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/EfficiencyCurve.png");

		this.addOutputFile(outputFile0, 0, 0, "image");


        System.out.println(this.getOutput());

    }
}
