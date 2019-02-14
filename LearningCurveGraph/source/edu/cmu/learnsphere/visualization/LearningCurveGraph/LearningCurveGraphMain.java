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

        if (outputFile0 != null && outputFile0.exists() ) {
            this.addOutputFile(outputFile0, 0, 0, "image");
        }else {
                 this.addErrorMessage("Files missing.");
        }
        
        }
        System.out.println(this.getOutput());

    }
}
