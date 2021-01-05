package edu.cmu.pslc.learnsphere.transform.multiskillConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class MultiskillConverterMain extends AbstractComponent {

    public static void main(String[] args) {

            MultiskillConverterMain tool = new MultiskillConverterMain();
        tool.startComponent(args);
    }

    public MultiskillConverterMain() {
        super();
    }

    @Override
    protected void runComponent() {
        File inputFile = getAttachment(0, 0);
        this.addPrivateOption("inputFile", inputFile.getAbsolutePath());
        
        logger.info("MultiskillConverter inputFile: " + inputFile.getAbsolutePath());
        String multiskillConversionMethod = this.getOptionAsString("multiskillConversionMethod");
        //for multiskillConversionMethod = Concatenate
        List<String> KCModels = this.getMultiOptionAsString("kcModelsToConvert");
        //for multiskillConversionMethod = Split to Multiple Rows
        String KCModel = this.getOptionAsString("kcModelToConvert");
        List<String> valuesToBeSplit = this.getMultiOptionAsString("valuesToBeSplit");
        
        String outputFileName = "/multiskill_converted_" + inputFile.getName();
        if (multiskillConversionMethod.equals("Split to Multiple Rows")) {
        	//need to make sure the columns for valuesToBeSplit are all numbers
        }
                
        logger.info("MultiskillConverter outputFileName: " + outputFileName);
        File outputDirectory = this.runExternal();
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                File file0 = new File(outputDirectory.getAbsolutePath() + outputFileName);
                if (file0 != null && file0.exists()) {
                        Integer nodeIndex0 = 0;
                        Integer fileIndex0 = 0;
                        String label0 = "student-step";
                        this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
                } else {
                        addErrorMessage("An unknown error has occurred with the TransactionAggregator component.");
                }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());

    }
    
}
