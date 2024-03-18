package edu.cmu.pslc.learnsphere.analysis.GamingTendencyEstimate;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class GamingTendencyEstimate extends AbstractComponent {

    /** Component option (model). */
    String modelName = null;

    public static void main(String[] args) {

        GamingTendencyEstimate tool = new GamingTendencyEstimate();
        tool.startComponent(args);
    }

    public GamingTendencyEstimate() {
        super();
    }

    @Override
    protected void runComponent() {
    	File file1 = this.getAttachment(0, 0);
		//System.out.println("---");
		logger.info("Input file: " + file1.getAbsolutePath());
		this.setOption("datasetPath", file1.getAbsolutePath());		
		
		String[] outputFileNames = {"/student_gaming_estimates.txt"};
		//String[] labels = {"gaming-tendency"};
		String[] labels = {"tab-delimited"};
		File outputDirectory = this.runExternal();

		if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
			logger.info("outputFile:" + outputDirectory.getAbsolutePath());
			Integer nodeIndex = 0;
			Integer fileIndex0 = 0;
			for (nodeIndex = 0; nodeIndex<outputFileNames.length; nodeIndex++) {
				File file0 = new File(outputDirectory.getAbsolutePath() + outputFileNames[nodeIndex]);
				if (file0 != null && file0.exists()) {
					this.addOutputFile(file0, nodeIndex, fileIndex0, labels[nodeIndex]);
				} else {
					logger.info("An unknown error has occurred with the GamingTendencyEstimate component.");
					addErrorMessage("An unknown error has occurred with the GamingTendencyEstimate component.");
				}
			}
		}

		System.out.println(this.getOutput());
		//System.out.println("---");
        
    }
    
}
