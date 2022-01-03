package edu.cmu.pslc.learnsphere.transform.rowMunging;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class TransformRowMungingMain extends AbstractComponent {

    public static void main(String[] args) {

        TransformRowMungingMain tool = new TransformRowMungingMain();
        tool.startComponent(args);
    }

    public TransformRowMungingMain() {
        super();
    }

    @Override
    protected void runComponent() {
        //get/set -f option
    	boolean reqsMet = true;
        File inputFile = getAttachment(0, 0);
        logger.info("TransformRowMunging inputFile: " + inputFile.getAbsolutePath());
        //check the first condition
        String condType1 = this.getOptionAsString("condType1");
        String condCompOper1 = this.getOptionAsString("condCompOper1");
        String condConst1 = this.getOptionAsString("condConst1");
        if (condType1.equals("Constants")) {
			//remove quotes
        	String modCondConst1 = modConstantString(condConst1);
        	logger.info("modified condConst1 is:" + modCondConst1);
        	this.setOption("condConst1", modCondConst1);
        	//"" can't be in contain
        	if (condCompOper1.equals("Contain") && cotainEmptyString(modCondConst1)) {
        		//send error message
                String err = "Condition 1 Comparison has an empty string for Contain operation.";
                addErrorMessage(err);
                logger.info("TransformRowMunging is aborted: " + err); 
                reqsMet = false;
        	}
        }
        //NA value can only be equal
        if (condType1.equals("NA")
        		&& !condCompOper1.equals("Equal to")) {
        	//send error message
            String err = "Condition 1 Comparison Operator can only be Equal to when Condition 1 is compared to null value.";
            addErrorMessage(err);
            logger.info("TransformRowMunging is aborted: " + err); 
            reqsMet = false;
        }
        //condCompOper can't be Contain when condType is Value from column	
        if (condType1.equals("Value from column")
        		&& condCompOper1.equals("Contain")) {
        	//send error message
        	String err = "Condition 1 Comparison Operator can't be Contain when Condition 1 is compared to a column.";
            addErrorMessage(err);
            logger.info("TransformRowMunging is aborted: " + err); 
            reqsMet = false;
        }
        //if operator is numeric, all operants have to be numeric
        if (condCompOper1.equals("Greater than") || condCompOper1.equals("Greater than or equal to")
        		|| condCompOper1.equals("Less than") || condCompOper1.equals("Less than or equal to")) {
        	//check condition1 col is numeric
        	String condition1 = this.getOptionAsString("condition1");
        	if (!isColumnNumeric(inputFile.getAbsolutePath(), condition1)) {
    			//send error message
                String err = "Condition 1 Column has non-numeric value.";
                addErrorMessage(err);
                logger.info("TransformColumnMunging is aborted: " + err);
                reqsMet = false;
    		}
        	if (condType1.equals("Value from column")) {
        		String condCol1 = this.getOptionAsString("condCol1");
            	if (!isColumnNumeric(inputFile.getAbsolutePath(), condCol1)) {
        			//send error message
                    String err = "Condition 1 Compare to Column has non-numeric value.";
                    addErrorMessage(err);
                    logger.info("TransformColumnMunging is aborted: " + err);
                    reqsMet = false;
        		}
        	} else if (condType1.equals("Constants")) {
        		//check if condConst1 are all numeric
        		condConst1 = this.getOptionAsString("condConst1");
                boolean allNumeric = true;
                List<String> items = Arrays.asList(condConst1.split("\\s*,\\s*"));
                for (String item : items) {
                	if (!isNumeric(item.trim())) {
                		allNumeric = false;
                		break;
                	}
                }
                if (!allNumeric) {
        			//send error message
                    String err = "Condition 1 Compare to Value has non-numeric value.";
                    addErrorMessage(err);
                    logger.info("TransformColumnMunging is aborted: " + err);
                    reqsMet = false;
        		}
        	}
        }
        
        //check the second condition
        String addCond2 = this.getOptionAsString("addCond2");
        if (addCond2.equals("Yes")) {
	        String condType2 = this.getOptionAsString("condType2");
	        String condCompOper2 = this.getOptionAsString("condCompOper2");
	        String condConst2 = this.getOptionAsString("condConst2");
	        //replace all quotes
	        if (condType2.equals("Constants")) {
	        	String modCondConst2 = modConstantString(condConst2);
	        	logger.info("modified condConst2 is:" + modCondConst2);
	        	this.setOption("condConst2", modCondConst2);
	        	//"" can't be in contain
	        	if (condCompOper2.equals("Contain") && cotainEmptyString(modCondConst2)) {
	        		//send error message
	                String err = "Condition 2 Comparison has an empty string for Contain operation.";
	                addErrorMessage(err);
	                logger.info("TransformRowMunging is aborted: " + err); 
	                reqsMet = false;
	        	}
	        }
	        //NA value can only be equal
	        if (condType2.equals("NA")
	        		&& !condCompOper2.equals("Equal to")) {
	        	//send error message
	            String err = "Condition 2 Comparison Operator can only be Equal to when Condition 2 is compared to null value.";
	            addErrorMessage(err);
	            logger.info("TransformRowMunging is aborted: " + err); 
	            reqsMet = false;
	        }
	        //condCompOper can't be Contain when condType is Value from column	
	        if (condType2.equals("Value from column")
	        		&& condCompOper2.equals("Contain")) {
	        	//send error message
	        	String err = "Condition 2 Comparison Operator can't be Contain when Condition 2 is compared to a column.";
	            addErrorMessage(err);
	            logger.info("TransformRowMunging is aborted: " + err); 
	            reqsMet = false;
	        }
	        //if operator is numeric, all operants have to be numeric
	        if (condCompOper2.equals("Greater than") || condCompOper2.equals("Greater than or equal to")
	        		|| condCompOper2.equals("Less than") || condCompOper2.equals("Less than or equal to")) {
	        	//check condition2 col is numeric
	        	String condition2 = this.getOptionAsString("condition2");
	        	if (!isColumnNumeric(inputFile.getAbsolutePath(), condition2)) {
	    			//send error message
	                String err = "Condition 2 Column has non-numeric value.";
	                addErrorMessage(err);
	                logger.info("TransformColumnMunging is aborted: " + err);
	                reqsMet = false;
	    		}
	        	if (condType2.equals("Value from column")) {
	        		String condCol2 = this.getOptionAsString("condCol2");
	            	if (!isColumnNumeric(inputFile.getAbsolutePath(), condCol2)) {
	        			//send error message
	                    String err = "Condition 2 Compare to Column has non-numeric value.";
	                    addErrorMessage(err);
	                    logger.info("TransformColumnMunging is aborted: " + err);
	                    reqsMet = false;
	        		}
	        	} else if (condType2.equals("Constants")) {
	        		//check if condConst2 are all numeric
	        		condConst2 = this.getOptionAsString("condConst2");
	                boolean allNumeric = true;
	                List<String> items = Arrays.asList(condConst2.split("\\s*,\\s*"));
	                for (String item : items) {
	                	if (!isNumeric(item.trim())) {
	                		allNumeric = false;
	                		break;
	                	}
	                }
	                if (!allNumeric) {
	        			//send error message
	                    String err = "Condition 2 Compare to Value has non-numeric value.";
	                    addErrorMessage(err);
	                    logger.info("TransformColumnMunging is aborted: " + err);
	                    reqsMet = false;
	        		}
	        	}
	        }
        }
        //check the third condition
        String addCond3 = this.getOptionAsString("addCond3");
        if (addCond2.equals("Yes") && addCond3.equals("Yes")) {
	        String condType3 = this.getOptionAsString("condType3");
	        String condCompOper3 = this.getOptionAsString("condCompOper3");
	        String condConst3 = this.getOptionAsString("condConst3");
	        //replace all quotes
	        if (condType3.equals("Constants")) {
	        	String modCondConst3 = modConstantString(condConst3);
	        	this.setOption("condConst3", modCondConst3);
	        	logger.info("modified condConst2 is:" + modCondConst3);
	        	//"" can't be in contain
	        	if (condCompOper3.equals("Contain") && cotainEmptyString(modCondConst3)) {
	        		//send error message
	                String err = "Condition 3 Comparison has an empty string for Contain operation.";
	                addErrorMessage(err);
	                logger.info("TransformRowMunging is aborted: " + err); 
	                reqsMet = false;
	        	}
	        }
	        //NA value can only be equal
	        if (condType3.equals("NA")
	        		&& !condCompOper3.equals("Equal to")) {
	        	//send error message
	            String err = "Condition 3 Comparison Operator can only be Equal to when Condition 3 is compared to null value.";
	            addErrorMessage(err);
	            logger.info("TransformRowMunging is aborted: " + err); 
	            reqsMet = false;
	        }
	        //condCompOper can't be Contain when condType is Value from column	
	        if (condType3.equals("Value from column")
	        		&& condCompOper3.equals("Contain")) {
	        	//send error message
	        	String err = "Condition 3 Comparison Operator can't be Contain when Condition 3 is compared to a column.";
	            addErrorMessage(err);
	            logger.info("TransformRowMunging is aborted: " + err); 
	            reqsMet = false;
	        }
	        //if operator is numeric, all operants have to be numeric
	        if (condCompOper3.equals("Greater than") || condCompOper3.equals("Greater than or equal to")
	        		|| condCompOper3.equals("Less than") || condCompOper3.equals("Less than or equal to")) {
	        	//check condition3 col is numeric
	        	String condition3 = this.getOptionAsString("condition3");
	        	if (!isColumnNumeric(inputFile.getAbsolutePath(), condition3)) {
	    			//send error message
	                String err = "Condition 3 Column has non-numeric value.";
	                addErrorMessage(err);
	                logger.info("TransformColumnMunging is aborted: " + err);
	                reqsMet = false;
	    		}
	        	if (condType3.equals("Value from column")) {
	        		String condCol3 = this.getOptionAsString("condCol3");
	            	if (!isColumnNumeric(inputFile.getAbsolutePath(), condCol3)) {
	        			//send error message
	                    String err = "Condition 3 Compare to Column has non-numeric value.";
	                    addErrorMessage(err);
	                    logger.info("TransformColumnMunging is aborted: " + err);
	                    reqsMet = false;
	        		}
	        	} else if (condType3.equals("Constants")) {
	        		//check if condConst3 are all numeric
	        		condConst3 = this.getOptionAsString("condConst3");
	                boolean allNumeric = true;
	                List<String> items = Arrays.asList(condConst3.split("\\s*,\\s*"));
	                for (String item : items) {
	                	if (!isNumeric(item.trim())) {
	                		allNumeric = false;
	                		break;
	                	}
	                }
	                if (!allNumeric) {
	        			//send error message
	                    String err = "Condition 3 Compare to Value has non-numeric value.";
	                    addErrorMessage(err);
	                    logger.info("TransformColumnMunging is aborted: " + err);
	                    reqsMet = false;
	        		}
	        	}
	        }
        }
        
        if (reqsMet) {
        	// Run the program and return its stdout to a file.
	        File outputDirectory = this.runExternal();
		    if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
		    	logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/row_munging_result.txt");
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
    
    //remove double quote or single quote, and the spaces before and after them
    private String modConstantString(String strVal) {
    	String newStrVal = "";
    	boolean started = false;
    	List<String> tempWds = Arrays.asList(strVal.split(","));
    	for (String tempWd : tempWds) {
    		if (tempWd.matches("\\s*\".*\"\\s*") || tempWd.matches("\\s*'.*'\\s*")) {
    			tempWd = tempWd.trim();
    			tempWd = tempWd.substring(1, tempWd.length()-1);
    		}
    		if (!started) {
    			newStrVal = tempWd;
    			started = true;
    		} else {
    			newStrVal += "," + tempWd;
    		}
    	}
    	return newStrVal;
    }
    
    //check if a comma separate string contains an empty string
    private boolean cotainEmptyString(String strVal) {
    	boolean containEmptyString = false;
    	//if strVal is empty
    	if (strVal.length() == 0)
    		return true;
    	//if last char is a comma, it contains an empty string
    	if (strVal.substring(strVal.length() - 1).equals(","))
    		return true;
    	List<String> tempWds = Arrays.asList(strVal.split(","));
    	for (String tempWd : tempWds) {
    		if (tempWd.equals("")) {
    			containEmptyString = true;
    			break;
    		}
    	}
    	return containEmptyString;
    }
    
    
}
