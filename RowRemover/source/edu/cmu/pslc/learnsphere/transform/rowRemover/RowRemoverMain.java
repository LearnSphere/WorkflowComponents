package edu.cmu.pslc.learnsphere.transform.rowRemover;

import java.io.File;
import java.util.List;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class RowRemoverMain extends AbstractComponent {

    public static void main(String[] args) {

        RowRemoverMain tool = new RowRemoverMain();
        tool.startComponent(args);
    }

    public RowRemoverMain() {
        super();
    }

    @Override
    protected void runComponent() {
    	Boolean reqsMet = true;
            String operation = this.getOptionAsString("i_operation");
            if (operation.equalsIgnoreCase("Remove selected rows"))
                    this.componentOptions.addContent(0, new Element("operation").setText("remove"));
            else if (operation.equalsIgnoreCase("Keep selected rows and remove the rest")) {
                    this.componentOptions.addContent(0, new Element("operation").setText("keep"));
            }
            else {
                String exErr = "Wrong row remove operation.";
                addErrorMessage(exErr);
                logger.info(exErr);
                reqsMet = false;
            }

            if (reqsMet) {
	            List<String> valueColumns = this.getMultiOptionAsString("valueColumn");
	            String valueColumnsStr = "";
	            if (valueColumns != null && valueColumns.size() != 0){
	                    for (String key : valueColumns)
	                            valueColumnsStr += key + ",";
	                    //delete the last comma
	                    if (valueColumnsStr.lastIndexOf(",") == valueColumnsStr.length()-1) {
	                            valueColumnsStr = valueColumnsStr.substring(0, valueColumnsStr.length()-1);
	                    }
	            }
	            //add this to parameters
	            this.setOption("valueColumns", valueColumnsStr);
	            String caseSensitive = this.getOptionAsString("caseSensitive");
	            String removeNull = this.getOptionAsString("removeNull");
	            String removeValues = this.getOptionAsString("removeValues");
	            logger.info("RowRemover, operation: " + operation);
	            logger.info("RowRemover, valueColumns: " + valueColumns);
	            logger.info("RowRemover, caseSensitive: " + caseSensitive);
	            logger.info("RowRemover, RowNull: " + removeNull);
	            logger.info("RowRemover, removeValues: " + removeValues);
	            String file0 = this.getAttachment(0, 0).getAbsolutePath();
	            logger.info("RowRemover, file0: " + file0);
	            if (removeValues.trim().equals("")) {
	                    String exErr = "Value to remove is required.";
	                    addErrorMessage(exErr);
	                    logger.info(exErr);
	            } else {
		            // Run the program and return its stdout to a file.
		            //File output = this.runExternal();
		            File outputDirectory = this.runExternal();
		            if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	                    logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	                    File outputFile = new File(outputDirectory.getAbsolutePath() + "/modified_file.txt");
	                    if (outputFile != null && outputFile.exists()) {
                            Integer nodeIndex0 = 0;
                            Integer fileIndex0 = 0;
                            String label0 = "tab-delimited";
                            this.addOutputFile(outputFile, nodeIndex0, fileIndex0, label0);
	                    } else {
                            String exErr = "An unknown error has occurred with the RowRemover component.";
                            addErrorMessage(exErr);
                            logger.info(exErr);
	                    }
		            }
	            }
            }

            // Send the component output back to the workflow.
            System.out.println(this.getOutput());
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        this.addMetaDataFromInput("tab-delimited", 0, 0, ".*");
    }

}
