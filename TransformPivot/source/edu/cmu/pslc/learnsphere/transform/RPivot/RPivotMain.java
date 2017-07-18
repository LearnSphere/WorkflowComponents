package edu.cmu.pslc.learnsphere.transform.RPivot;

import java.io.File;
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
            
            //check if measurement (-m) column are all double
            String measurementColName = this.getOptionAsString("mWF");
            String[][] allCells = IOUtil.read2DRuggedStringArray(inputFile.getAbsolutePath(), false);
            String[] headers = allCells[0];
            int measurementColInd = -1;
            for (int i = 0; i < headers.length; i++) {
                    String header = headers[i];
                    if (header.equals(measurementColName)) {
                            measurementColInd = i;   
                    }
            }
            if (measurementColInd == -1) {
                    //send error message
                    String err = "The measurement column is not found in data.";
                    addErrorMessage(err);
                    logger.info("RPivot is aborted: " + err);
                    System.err.println(err);
                    return;
            }
            for (int i = 1; i < allCells.length; i++) {
                    try {
                            Double.parseDouble(allCells[i][measurementColInd]);
                    } catch (NumberFormatException e) {
                            //send error message
                            String err = "The measurement column contains data that is not number.";
                            addErrorMessage(err);
                            logger.info("RPivot is aborted: " + err);
                            System.err.println(err);
                            return;
                    }
            }
            this.componentOptions.addContent(0, new Element("m").setText(removeSpace(measurementColName)));
            String aggMethodName = this.getOptionAsString("aWF");
            if (aggMethodName == null)
                    aggMethodName = "length";
            this.componentOptions.addContent(0, new Element("a").setText(aggMethodName));
            
            List<String> pivotColNames = this.getMultiOptionAsString("cWF");
            if (pivotColNames == null) {
                    //send error message
                    String err = "Column for pivot can't be null.";
                    addErrorMessage(err);
                    logger.info("RPivot is aborted: " + err);
                    System.err.println(err);
                    return;
            } else {
                    String colNames = "";
                    for (int i = 0; i < pivotColNames.size(); i++) {
                            if (i < pivotColNames.size()-1)
                                    colNames += removeSpace(pivotColNames.get(i)) + ",";
                            else
                                    colNames += removeSpace(pivotColNames.get(i));
                    }
                    this.componentOptions.addContent(0, new Element("c").setText(colNames));
            }
            List<String> pivotRowNames = this.getMultiOptionAsString("rWF");
            
            if (pivotRowNames == null) {
                    //send error message
                    String err = "Row for pivot can't be null.";
                    addErrorMessage(err);
                    logger.info("RPivot is aborted: " + err);
                    System.err.println(err);
                    return;
            } else {
                    String rowNames = "";
                    for (int i = 0; i < pivotRowNames.size(); i++) {
                            if (i < pivotRowNames.size()-1)
                                    rowNames += removeSpace(pivotRowNames.get(i)) + ",";
                            else
                                    rowNames += removeSpace(pivotRowNames.get(i));
                    }
                    
                    this.componentOptions.addContent(0, new Element("r").setText(rowNames));
            }
            
        // Run the program and return its stdout to a file.
        File outputDirectory = this.runExternalMultipleFileOuput();
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                File file0 = new File(outputDirectory.getAbsolutePath() + "/pivot_result.txt");
                if (file0 != null && file0.exists()) {
                        Integer nodeIndex0 = 0;
                        Integer fileIndex0 = 0;
                        String label0 = "tab-delimited";
                        this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
                } else {
                        this.addErrorMessage("An unknown error has occurred with the RPivot component.");
                }
        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
    }
    //in r col name with space should replace space with a "." 
    private String removeSpace(String aColName) {
            return aColName.replaceAll("\\s+",".");
    }

}
