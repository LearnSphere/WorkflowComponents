package edu.cmu.learnsphere.visualization.LearningCurveGraph;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class LearningCurveGraphMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        LearningCurveGraphMain tool = new LearningCurveGraphMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public LearningCurveGraphMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
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
        
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {

	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/myplot.png");
        File outputFile1 = new File(outputDirectory.getAbsolutePath() + "/LegendPlot.png");
        
        if (outputFile0 != null && outputFile0.exists() && outputFile1 != null && outputFile1.exists()) {
            this.addOutputFile(outputFile0, 0, 0, "image");
            this.addOutputFile(outputFile1, 1, 0, "image");
        }else {
                 this.addErrorMessage("Files missing.");
        }
        
        }
        System.out.println(this.getOutput());

    }
}
