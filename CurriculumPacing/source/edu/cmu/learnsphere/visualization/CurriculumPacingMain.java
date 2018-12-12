package edu.cmu.learnsphere.visualization;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class CurriculumPacingMain extends AbstractComponent {

	/** Component option (ProblemHierarchyOrderData). */
	String ProblemHierarchyOrderData = null;
	/** Component option (TimeScaleType). */
	String TimeScaleType = null;
	/** Component option (TimeScaleResolution). */
	String TimeScaleResolution = null;
	/** Component option (MaxTimeUnit). */
	String MaxTimeUnit = null;
	/** Component option (PlotMetric). */
	String PlotMetric = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        CurriculumPacingMain tool = new CurriculumPacingMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public CurriculumPacingMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("tab-delimited", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("image", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("ProblemHierarchyOrderData") != null) {
		ProblemHierarchyOrderData = this.getOptionAsString("ProblemHierarchyOrderData");
	}
	if(this.getOptionAsString("TimeScaleType") != null) {
		TimeScaleType = this.getOptionAsString("TimeScaleType");
	}
	if(this.getOptionAsString("TimeScaleResolution") != null) {
		TimeScaleResolution = this.getOptionAsString("TimeScaleResolution");
	}
	if(this.getOptionAsString("MaxTimeUnit") != null) {
		MaxTimeUnit = this.getOptionAsString("MaxTimeUnit");
	}
	if(this.getOptionAsString("PlotMetric") != null) {
		PlotMetric = this.getOptionAsString("PlotMetric");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/curriculumpacing.svg");

		this.addOutputFile(outputFile0, 0, 0, "image");


        System.out.println(this.getOutput());

    }
}
