package edu.cmu.pslc.learnsphere.transform.columnMunging;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class TransformColumnMungingMain extends AbstractComponent {

    public static void main(String[] args) {

        TransformColumnMungingMain tool = new TransformColumnMungingMain();
        tool.startComponent(args);
    }

    public TransformColumnMungingMain() {
        super();
    }

    @Override
    protected void runComponent() {
        //get/set -f option
    	boolean reqsMet = true;
        File inputFile = getAttachment(0, 0);
        logger.info("TransformColumnMunging inputFile: " + inputFile.getAbsolutePath());
        String columnOperation = this.getOptionAsString("columnOperation");
        //Cumulative sum: cumsumName is required; cumsumCol should be numeric column
        if (columnOperation.equals("Cumulative sum")) {
        	String cumsumName = this.getOptionAsString("cumsumName");
        	if (cumsumName == null || cumsumName.trim().equals("") ) {
        		//send error message
                    String err = "Column Name for Cumulative Sum is a required field.";
                    addErrorMessage(err);
                    logger.info("TransformColumnMunging is aborted: " + err); 
                    reqsMet = false;
        	}
        	//when cumsumVal = "Column cumulative sum within group", cumsumCol must be numeric
        	String cumsumVal = this.getOptionAsString("cumsumVal");
        	String cumsumCol = this.getOptionAsString("cumsumCol");
        	if (cumsumVal != null && cumsumVal.equals("Column cumulative sum within group")) {
        		if (!isColumnNumeric(inputFile.getAbsolutePath(), cumsumCol)) {
        			//send error message
                    String err = "Cumulative Sum Column has non-numeric value.";
                    addErrorMessage(err);
                    logger.info("TransformColumnMunging is aborted: " + err);
                    reqsMet = false;
        		}
        	}
        } else if (columnOperation.equals("Change column names")) {
        	//Change column names: columnXNewName is required; nOfColNamesToChange should be integer
            Integer nOfColNamesToChange = getOptionAsInteger("nOfColNamesToChange");
            for (int i = 1; i <= nOfColNamesToChange; i++) {
            	String columnXNewNameVar = "column" + i + "NewName";
            	String columnXNewName = this.getOptionAsString(columnXNewNameVar);
            	if (columnXNewName == null || columnXNewName.trim().equals("")) {
            		//send error message
                    String err = "Column " + i + " New Name is a required field.";
                    addErrorMessage(err);
                    logger.info("TransformColumnMunging is aborted: " + err);
                    reqsMet = false;
            	}
            }
        } else if (columnOperation.equals("Add a column")) {
        	//new column name is required
        	String newColumnName = this.getOptionAsString("newColumnName");
        	if (newColumnName == null || newColumnName.trim().equals("") ) {
        		//send error message
                    String err = "New Column Name is a required field.";
                    addErrorMessage(err);
                    logger.info("TransformColumnMunging is aborted: " + err); 
                    reqsMet = false;
        	}
        	//if colValType1 = "A constant", factorConst1 is required
        	String colValType1 = this.getOptionAsString("colValType1");
        	String factorConst1 = this.getOptionAsString("factorConst1");
        	/*if (colValType1.equals("A constant") && 
        			(factorConst1 == null || factorConst1.trim().equals(""))) {
        		String err = "Factor 1 Value is a required field.";
                addErrorMessage(err);
                logger.info("TransformColumnMunging is aborted: " + err);
                reqsMet = false;
        	}*/
        	//if colValType2 = "A constant", factorConst2 is required
        	//if addFactor2 = "Yes" and operation1 != "Concatenate", factorConst1, factorConst2, factorCol1 and factorCol2 have to be numeric
        	String addFactor2 = this.getOptionAsString("addFactor2");
        	if (addFactor2.equals("Yes")) {
        		String colValType2 = this.getOptionAsString("colValType2");
            	String factorConst2 = this.getOptionAsString("factorConst2");
            	/*if (colValType2.equals("A constant") && 
            			(factorConst2 == null || factorConst2.trim().equals(""))) {
            		String err = "Factor 2 Value is a required field.";
                    addErrorMessage(err);
                    logger.info("TransformColumnMunging is aborted: " + err);
                    reqsMet = false;
            	}*/
            	String operation1 = this.getOptionAsString("operation1");
            	if (!operation1.equals("Concatenate")) {
            		//check factorConst1
            		if (colValType1.equals("A constant")) {
            			if (!isNumeric(factorConst1)) {
            				String err = "Factor 1 Value isn't a number.";
                            addErrorMessage(err);
                            logger.info("TransformColumnMunging is aborted: " + err);
                            reqsMet = false;
            			}
            		}
            		//check factorConst2
            		if (colValType2.equals("A constant")) {
            			if (!isNumeric(factorConst2)) {
            				String err = "Factor 2 Value isn't a number.";
                            addErrorMessage(err);
                            logger.info("TransformColumnMunging is aborted: " + err);
                            reqsMet = false;
            			}
            		}
            		//check factorCol1
            		if (colValType1.equals("Value from column")) {
            			String factorCol1 = this.getOptionAsString("factorCol1");
            			if (!isColumnNumeric(inputFile.getAbsolutePath(), factorCol1)) {
            				String err = "Factor 1 Column has non-numeric value.";
                            addErrorMessage(err);
                            logger.info("TransformColumnMunging is aborted: " + err);
                            reqsMet = false;
            			}
            		}
            		//check factorCol2
            		if (colValType2.equals("Value from column")) {
            			String factorCol2 = this.getOptionAsString("factorCol2");
            			if (!isColumnNumeric(inputFile.getAbsolutePath(), factorCol2)) {
            				String err = "Factor 2 Column has non-numeric value.";
                            addErrorMessage(err);
                            logger.info("TransformColumnMunging is aborted: " + err);
                            reqsMet = false;
            			}
            		}
            	}
        	}
        	//if colValType3 = "A constant", factorConst3 is required
        	//if addFactor3 = "Yes" and operation2 != "Concatenate", factorConst2, factorConst3, factorCol2 and factorCol3 have to be numeric
        	String addFactor3 = this.getOptionAsString("addFactor3");
        	if (addFactor3.equals("Yes")) {
        		String colValType2 = this.getOptionAsString("colValType2");
            	String factorConst2 = this.getOptionAsString("factorConst2");
        		String colValType3 = this.getOptionAsString("colValType3");
            	String factorConst3 = this.getOptionAsString("factorConst3");
            	/*if (colValType3.equals("A constant") && 
            			(factorConst3 == null || factorConst3.trim().equals(""))) {
            		String err = "Factor 3 Value is a required field.";
                    addErrorMessage(err);
                    logger.info("TransformColumnMunging is aborted: " + err);
                    reqsMet = false;
            	}*/
            	String operation2 = this.getOptionAsString("operation2");
            	if (!operation2.equals("Concatenate")) {
            		//check factorConst2
            		if (colValType2.equals("A constant")) {
            			if (!isNumeric(factorConst2)) {
            				String err = "Factor 2 Value isn't a number.";
                            addErrorMessage(err);
                            logger.info("TransformColumnMunging is aborted: " + err);
                            reqsMet = false;
            			}
            		}
            		//check factorConst3
            		if (colValType3.equals("A constant")) {
            			if (!isNumeric(factorConst3)) {
            				String err = "Factor 3 Value isn't a number.";
                            addErrorMessage(err);
                            logger.info("TransformColumnMunging is aborted: " + err);
                            reqsMet = false;
            			}
            		}
            		//check factorCol2
            		if (colValType2.equals("Value from column")) {
            			String factorCol2 = this.getOptionAsString("factorCol2");
            			if (!isColumnNumeric(inputFile.getAbsolutePath(), factorCol2)) {
            				String err = "Factor 2 Column has non-numeric value.";
                            addErrorMessage(err);
                            logger.info("TransformColumnMunging is aborted: " + err);
                            reqsMet = false;
            			}
            		}
            		//check factorCol3
            		if (colValType3.equals("Value from column")) {
            			String factorCol3 = this.getOptionAsString("factorCol3");
            			if (!isColumnNumeric(inputFile.getAbsolutePath(), factorCol3)) {
            				String err = "Factor 3 Column has non-numeric value.";
                            addErrorMessage(err);
                            logger.info("TransformColumnMunging is aborted: " + err);
                            reqsMet = false;
            			}
            		}
            	}
        	}
        }
        
        if (reqsMet) {
        	// Run the program and return its stdout to a file.
	        File outputDirectory = this.runExternal();
		    if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
		    	logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/column_munging_result.txt");
	            if (file0 != null && file0.exists()) {
                	Integer nodeIndex0 = 0;
                    Integer fileIndex0 = 0;
                    String label0 = "tab-delimited";
                    this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
	            } else {
	            	addErrorMessage("An unknown error has occurred with the TransformColumnMunging component.");
	            }
		    }
        }               
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
    
    //in r col name with space should replace space with a "."
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
    
}
