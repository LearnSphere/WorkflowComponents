package edu.cmu.pslc.learnsphere.visualization.rBarChart;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class VisualizationRBarChartMain extends AbstractComponent {

    public static void main(String[] args) {

        VisualizationRBarChartMain tool = new VisualizationRBarChartMain();
        tool.startComponent(args);
    }

    public VisualizationRBarChartMain() {
        super();
    }

    @Override
    protected void runComponent() {
        //get/set -f option
    	boolean reqsMet = true;
        File inputFile = getAttachment(0, 0);
        logger.info("VisualizationRBarChartMain inputFile: " + inputFile.getAbsolutePath());
        //check yAxisColumn column, has to be numeric
        String yAxisColumn = this.getOptionAsString("yAxisColumn");
        if (!isColumnNumeric(inputFile.getAbsolutePath(), yAxisColumn)) {
			//send error message
            String err = "Y Axis has to be numeric.";
            addErrorMessage(err);
            logger.info("VisualizationRBarChartMain is aborted: " + err);
            reqsMet = false;
		}
        //check stderrColumn column, has to be numeric
        String hasStdErr = this.getOptionAsString("hasStdErr");
        String stdevColumn = this.getOptionAsString("stdevColumn");
        String lengthColumn = this.getOptionAsString("lengthColumn");
        if (hasStdErr.equals("Yes")) {
	        if (!isColumnNumeric(inputFile.getAbsolutePath(), stdevColumn)) {
				//send error message
	            String err = "Standard Deviation has to be numeric.";
	            addErrorMessage(err);
	            logger.info("VisualizationRBarChartMain is aborted: " + err);
	            reqsMet = false;
			}
	        if (!isColumnNumeric(inputFile.getAbsolutePath(), lengthColumn)) {
				//send error message
	            String err = "Sample Size has to be numeric.";
	            addErrorMessage(err);
	            logger.info("VisualizationRBarChartMain is aborted: " + err);
	            reqsMet = false;
			}
        }
        
        
        if (reqsMet) {
	        File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String label = "pdf";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/barchart.pdf");
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the VisualizationRBarChart component: barchart.pdf can't be found.");
	            }
	            
	        }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
    }
    
    private boolean isColumnNumeric(String filePath, String colName) {
    	String[][] allCells = IOUtil.read2DStringArray(filePath, "\t");
        String[] headers = allCells[0];
        if (headers.length < 2) {
        	allCells = IOUtil.read2DStringArray(filePath, ",");
            headers = allCells[0];
        }
        int cumsumColInd = -1;
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            header = header.trim();
            //also delete the quotes if there is 
            header = header.replaceAll("^\"|\"$", "");
            if (header.equals(colName)) {
            	cumsumColInd = i;
            }
        }
        
        if (cumsumColInd == -1)
        	return false;
        for (int i = 1; i < allCells.length; i++) {
        	if (!isNumeric(allCells[i][cumsumColInd]))
        		return false;
        }
        return true;
    }
    
    private boolean isNumeric(String val) {
    	boolean valIsNumber = false;
    	try {
        	Double.parseDouble(val);
        	valIsNumber = true;
        } catch (NumberFormatException e) {
        } finally {
        	return valIsNumber;
        }
    }
}
