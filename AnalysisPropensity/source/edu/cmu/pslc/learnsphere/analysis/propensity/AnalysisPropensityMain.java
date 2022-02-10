package edu.cmu.pslc.learnsphere.analysis.propensity;

import java.io.File;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class AnalysisPropensityMain extends AbstractComponent {

    public static void main(String[] args) {

        AnalysisPropensityMain tool = new AnalysisPropensityMain();
        tool.startComponent(args);
    }

    public AnalysisPropensityMain() {
        super();
    }

    @Override
    protected void runComponent() {
        //get/set -f option
    	boolean reqsMet = true;
        File inputFile = getAttachment(0, 0);
        logger.info("AnalysisPropensity inputFile: " + inputFile.getAbsolutePath());
        //check treatment column, has to be 0 or 1
        String treatmentColumn = this.getOptionAsString("treatment");
        if (!isColumnZeroOrOne(inputFile.getAbsolutePath(), treatmentColumn)) {
			//send error message
            String err = "Treatment Column has to be 0 or 1.";
            addErrorMessage(err);
            logger.info("AnalysisPropensity is aborted: " + err);
            reqsMet = false;
		}
        //Covariates column is required
        List<String> covariates = this.getMultiOptionAsString("covariates");
        if (covariates == null || covariates.size() == 0) {
			//send error message
            String err = "Covariates is a required field.";
            addErrorMessage(err);
            logger.info("AnalysisPropensity is aborted: " + err);
            reqsMet = false;
		}
        //joined column is required
        String method = this.getOptionAsString("method");
        List<String> joinColumns = this.getMultiOptionAsString("joinColumns");
        if (method.equals("Full")) {
	        if (joinColumns == null || joinColumns.size() == 0) {
				//send error message
	            String err = "Columns to Join Match Data with Original is a required field.";
	            addErrorMessage(err);
	            logger.info("AnalysisPropensity is aborted: " + err);
	            reqsMet = false;
			}
        }
        List<String> exactCols = this.getMultiOptionAsString("exact");
        List<String> mahvarsCols = this.getMultiOptionAsString("mahvars");
        String includeExact = this.getOptionAsString("includeExact");
        String includeMahvars = this.getOptionAsString("includeMahvars");
        if (method.equals("Full")) {
        	boolean wrongExact = false;
        	if (includeExact.equals("Yes")) {
        		//exactCols is required
                if (exactCols == null || exactCols.size() == 0) {
        			//send error message
                    String err = "Exact column is a required field.";
                    addErrorMessage(err);
                    logger.info("AnalysisPropensity is aborted: " + err);
                    reqsMet = false;
        		} else {
		        	for (String exactCol : exactCols) {
		        		if (exactCol.equals(treatmentColumn)) {
		        			wrongExact = true;
		        			break;
		        		}
		        	}
        		}
        	}
        	boolean wrongMahvars = false;
        	if (includeMahvars.equals("Yes")) {
        		//mahvarsCols is required
                if (mahvarsCols == null || mahvarsCols.size() == 0) {
        			//send error message
                    String err = "Mahvars column is a required field.";
                    addErrorMessage(err);
                    logger.info("AnalysisPropensity is aborted: " + err);
                    reqsMet = false;
        		}  else {
		        	for (String mahvarsCol : mahvarsCols) {
		        		if (mahvarsCol.equals(treatmentColumn)) {
		        			wrongMahvars = true;
		        			break;
		        		}
		        	}
        		}
        	}
        	if (wrongExact) {
        		//send error message
                String err = "Exact columns can't include the Treatment column.";
                addErrorMessage(err);
                logger.info("AnalysisPropensity is aborted: " + err);
                reqsMet = false;
        	}
        	if (wrongMahvars) {
        		//send error message
                String err = "Mahvars columns can't include the Treatment column.";
                addErrorMessage(err);
                logger.info("AnalysisPropensity is aborted: " + err);
                reqsMet = false;
        	}
        }
        
        
        if (reqsMet) {
	        File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String label = "analysis-summary";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/analysis_propensity_result.txt");
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the AnalysisPropensity component: analysis_propensity_result.txt can't be found.");
	            }
	            //pdf
	            nodeIndex = 1;
	            label = "pdf";
	            File file1 = new File(outputDirectory.getAbsolutePath() + "/match_data_plot.pdf");
	            if (file1 != null && file1.exists()) {
	                this.addOutputFile(file1, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the AnalysisPropensity component: match_data_plot.pdf can't be found.");
	            }
	            //match data are not available for method null
	            if (!method.equals("Null")) {
		            //match data
		            nodeIndex = 2;
		            label = "match-data";
		            File file2 = new File(outputDirectory.getAbsolutePath() + "/match_data.txt");
		            if (file2 != null && file2.exists()) {
		                this.addOutputFile(file2, nodeIndex, fileIndex, label);
		            } else {
		                addErrorMessage("An error has occurred with the AnalysisPropensity component: match_data.txt can't be found.");
		            }
		            //original with match col
		            nodeIndex = 3;
		            label = "file";
		            String inputFileName = inputFile.getName();
		            String inputFileNameWithOutExt = FilenameUtils.removeExtension(inputFileName);
		            File file3 = new File(outputDirectory.getAbsolutePath() + "/" + inputFileNameWithOutExt + "_with_match_indicator.txt");
		            if (file3 != null && file3.exists()) {
		                this.addOutputFile(file3, nodeIndex, fileIndex, label);
		            } else {
		                addErrorMessage("An error has occurred with the AnalysisPropensity component: " + inputFileNameWithOutExt + "_with_match_indicator.txt can't be found.");
		            }
	            }
	            
	        }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
    }
    
    
    private boolean isColumnZeroOrOne(String filePath, String colName) {
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
        	if (!isZeroOrOne(allCells[i][cumsumColInd]))
        		return false;
        }
        return true;
    }
    
    private boolean isZeroOrOne(String val) {
    	boolean valZeroOrOne = false;
    	if (val.trim().equals("0") || val.trim().equals("1"))
    		valZeroOrOne = true;
        return valZeroOrOne;
    }
}
