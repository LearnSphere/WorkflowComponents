package edu.cmu.pslc.learnsphere.visualization.learningRate;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.workflows.InputHeaderOption;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class VisualizationLearningRateMain extends AbstractComponent {

    public static void main(String[] args) {

        VisualizationLearningRateMain tool = new VisualizationLearningRateMain();
        tool.startComponent(args);
    }

    public VisualizationLearningRateMain() {
        super();
    }

    @Override
    protected void parseOptions() {
	
    }

    @Override
    protected void processOptions() {
    }

    @Override
    protected void runComponent() {
    	//make sure that "First Attempt", "Anon Student Id", KCM and opportunity columns exists
    	boolean reqsMet = true;
        File inputFile = getAttachment(0, 0);
        logger.info("VisualizationLearningRatePlot inputFile: " + inputFile.getAbsolutePath());
        List<String> models = new ArrayList<String>();
        List<InputHeaderOption> modelColumnList = this.getInputHeaderOption("model", 0);
        for (InputHeaderOption iho : modelColumnList) {
        	models.add(iho.getOptionValue());
        }
        String[][] allCells = IOUtil.read2DStringArray(inputFile.getAbsolutePath(), "\t");
        String[] headers = allCells[0];
        if (headers.length < 2) {
        	allCells = IOUtil.read2DStringArray(inputFile.getAbsolutePath(), ",");
            headers = allCells[0];
        }
        String modelingType = this.getOptionAsString("modelingType");
        //check "First Attempt", "Anon Student Id", "Opportunity (...)", 'First Transaction Time'
        boolean foundFirstAttempt = false;
        boolean foundAnonStu = false;
        boolean foundFirstTxnTime = false;
        Hashtable foundOpps = new Hashtable<String, Boolean>();
        for (String model : models) {
        	foundOpps.put(model, false);
        }
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            header = header.trim();
            if (header.equals("First Attempt"))
            	foundFirstAttempt = true;
            if (header.equals("Anon Student Id"))
            	foundAnonStu = true;
            if (header.equals("First Transaction Time"))
            	foundFirstTxnTime = true;
            for (String kcModelName : models) {
            	String modelName = kcModelName.substring(kcModelName.indexOf("(") + 1, kcModelName.indexOf(")"));
            	String oppName = "Opportunity (" + modelName + ")";
            	if (header.equals(oppName)) {
            		foundOpps.put(kcModelName, true);
            	}
            }
        }
        if (!foundFirstAttempt) {
        	//send error message
            String err = "Input file is missing First Attempt column.";
            addErrorMessage(err);
            logger.info("VisualizationLearningRatePlot is aborted: " + err); 
            reqsMet = false;
        }
        if (!foundAnonStu) {
        	//send error message
            String err = "Input file is missing Anon Student Id column.";
            addErrorMessage(err);
            logger.info("VisualizationLearningRatePlot is aborted: " + err); 
            reqsMet = false;
        }
        if (modelingType.equals("tAFM") && !foundFirstTxnTime) {
        	//send error message
            String err = "Input file is missing First Transaction Time column.";
            addErrorMessage(err);
            logger.info("VisualizationLearningRatePlot is aborted: " + err); 
            reqsMet = false;
        }
        Enumeration<String> e = foundOpps.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            if (!(Boolean)foundOpps.get(key)) {
            	//send error message
            	String err = "Opportunity column is missing for " + key;
             	addErrorMessage(err);
              	logger.info("VisualizationLearningRatePlot is aborted: " + err); 
            	reqsMet = false;
            } 
        }
        
    	// Run the program...
    	File outputDirectory = this.runExternal();

        Integer fileIndex = 0;
        Integer nodeIndex = 0;
        String fileLabel = "pdf";
        File pdfFile = new File(outputDirectory.getAbsolutePath() + "/learningRatePlot.pdf");
        this.addOutputFile(pdfFile, nodeIndex, fileIndex, fileLabel);

        nodeIndex = 1;
        fileLabel = "analysis-summary";
        File analysisSumFile = new File(outputDirectory.getAbsolutePath() + "/analysis-summary.txt");
        this.addOutputFile(analysisSumFile, nodeIndex, fileIndex, fileLabel);

        nodeIndex = 2;
        fileLabel = "predictions";
        File predictionsFile = new File(outputDirectory.getAbsolutePath() + "/predictions.txt");
        this.addOutputFile(predictionsFile, nodeIndex, fileIndex, fileLabel);

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
}
