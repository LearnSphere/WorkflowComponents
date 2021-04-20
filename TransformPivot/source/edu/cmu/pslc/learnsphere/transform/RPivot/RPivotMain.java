package edu.cmu.pslc.learnsphere.transform.RPivot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class RPivotMain extends AbstractComponent {

    public static void main(String[] args) {

        RPivotMain tool = new RPivotMain();
        tool.startComponent(args);
    }

    public RPivotMain() {
        super();
    }

    @Override
    protected void runComponent() {
        //get/set -f option
        File inputFile = getAttachment(0, 0);
        logger.info("RPivot inputFile: " + inputFile.getAbsolutePath());
        this.componentOptions.addContent(0, new Element("f").setText(inputFile.getAbsolutePath()));
        
        String moreFactors = this.getOptionAsString("moreFactors");
        String aggMethodName = this.getOptionAsString("aWF");
        if (aggMethodName == null)
                aggMethodName = "length";
        else if (aggMethodName.equalsIgnoreCase("count"))
                aggMethodName = "length";
        //check if measurement (-m) column are all double
        List<String> measurementColNames = this.getMultiOptionAsString("mWF");
        String[][] allCells = IOUtil.read2DRuggedStringArray(inputFile.getAbsolutePath(), false);
        String[] headers = allCells[0];
        List<Integer> measurementColInds = new ArrayList<Integer>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            for (String measurementColName : measurementColNames) {
                    if (header.equals(measurementColName)) {
                        measurementColInds.add(i);
                    }
            }
        }

        if (measurementColInds.size() == -1) {
                //send error message
                String err = "The measurement column is not found in data.";
                addErrorMessage(err);
                logger.info("RPivot is aborted: " + err);
        } else {
        	Boolean reqsMet = true;
        	if (!aggMethodName.equalsIgnoreCase("length")) {
        			int nonNumericCount = 0;
                    for (int i = 1; i < allCells.length; i++) {
                            for (int j = 0; j < measurementColInds.size(); j++) {
                            	try {
                                	Double.parseDouble(allCells[i][measurementColInds.get(j)]);
                                } catch (NumberFormatException e) {
                                	nonNumericCount++;
                                }
                            }
                    }
                    if (nonNumericCount == allCells.length -1) {
	                    //send error message
	                    String err = "The measurement column is a non-numeric column.";
	                    addErrorMessage(err);
	                    logger.info("RPivot is aborted: " + err);
	                    reqsMet = false;
                    }
                    
        	}

            if (reqsMet) {
                    String mStr = "";
                    String origMeaNames = "";
                    for (int i = 0; i < measurementColNames.size(); i++) {
                            if (i < measurementColNames.size()-1) {
                                    origMeaNames += measurementColNames.get(i)  + ",";
                                    mStr += removeSpace(measurementColNames.get(i)) + ",";
                            } else {
                                    origMeaNames += measurementColNames.get(i);
                                    mStr += removeSpace(measurementColNames.get(i));
                            }
                    }
                    
	            this.componentOptions.addContent(0, new Element("m").setText(mStr));
	            this.componentOptions.addContent(0, new Element("origm").setText(origMeaNames));
	            
	            
	            this.componentOptions.addContent(0, new Element("a").setText(aggMethodName));
	            List<String> pivotColNames = this.getMultiOptionAsString("cWF");
	            if (pivotColNames == null) {
                    // send error message
                    String err = "Column for pivot can't be null.";
                    addErrorMessage(err);
                    logger.info("RPivot is aborted: " + err);
	            } else {
	            	// else continue with processing
                    String colNames = "";
                    String origColNames = "";
                    for (int i = 0; i < pivotColNames.size(); i++) {
                            if (i < pivotColNames.size()-1) {
                                    origColNames += pivotColNames.get(i)  + ",";
                                    colNames += removeSpace(pivotColNames.get(i)) + ",";
                            } else {
                                    origColNames += pivotColNames.get(i);
                                    colNames += removeSpace(pivotColNames.get(i));
                            }
                    }
                    if (moreFactors.equalsIgnoreCase("no"))
                            colNames = "";
                    this.componentOptions.addContent(0, new Element("c").setText(colNames));
                    this.componentOptions.addContent(0, new Element("origc").setText(origColNames));
                    
		            List<String> pivotRowNames = this.getMultiOptionAsString("rWF");

		            if (pivotRowNames == null) {
	                    //send error message
	                    String err = "Row for pivot can't be null.";
	                    addErrorMessage(err);
	                    logger.info("RPivot is aborted: " + err);
	                    reqsMet = false;
		            } else {
	                    String rowNames = "";
	                    String origRowNames = "";
	                    for (int i = 0; i < pivotRowNames.size(); i++) {
	                        if (i < pivotRowNames.size()-1) {
	                            origRowNames += pivotRowNames.get(i)  + ",";
	                            rowNames += removeSpace(pivotRowNames.get(i)) + ",";
	                        } else {
	                                origRowNames += pivotRowNames.get(i);
	                            rowNames += removeSpace(pivotRowNames.get(i));
	                        }
	                    }

	                    this.componentOptions.addContent(0, new Element("r").setText(rowNames));
	                    this.componentOptions.addContent(0, new Element("origr").setText(origRowNames));
	                    
		            }

		            if (reqsMet) {
				        // Run the program and return its stdout to a file.
			            File outputDirectory = this.runExternal();
				        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
			                logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
			                File file0 = new File(outputDirectory.getAbsolutePath() + "/pivot_result.txt");
			                if (file0 != null && file0.exists()) {
		                        Integer nodeIndex0 = 0;
		                        Integer fileIndex0 = 0;
		                        String label0 = "tab-delimited";
		                        this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
			                } else {
		                        addErrorMessage("An unknown error has occurred with the RPivot component.");
			                }
				        }
		            }
	            }
            }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());

    }
    //in r col name with space should replace space with a "."
    private String removeSpace(String aColName) {
            //return aColName.replaceAll("\\s+",".");
            return aColName.replaceAll("[\\(\\[\\]\\)\\-\\s]",".");
    }

}
