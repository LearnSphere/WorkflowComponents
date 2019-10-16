package edu.cmu.pslc.learnsphere.transform.multiselectionConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class MultiselectionConverterMain extends AbstractComponent {

    public static void main(String[] args) {

            MultiselectionConverterMain tool = new MultiselectionConverterMain();
        tool.startComponent(args);
    }

    public MultiselectionConverterMain() {
        super();
    }

    @Override
    protected void runComponent() {
        //get/set -f option
        File dataFile = getAttachment(0, 0);
        logger.info("MultiselectionConverter dataFile: " + dataFile.getAbsolutePath());
        File mapFile = getAttachment(1, 0);
        logger.info("MultiselectionConverter mapFile: " + mapFile.getAbsolutePath());
        
        this.addPrivateOption("dataFile", dataFile.getAbsolutePath());
        this.addPrivateOption("mapFile", mapFile.getAbsolutePath());
        
        String outputFileName = "";
        String fileNameWithOutExt = FilenameUtils.removeExtension(dataFile.getName());
        String fileExtension = FilenameUtils.getExtension(dataFile.getName());
        outputFileName = "/" + fileNameWithOutExt + "_converted." + fileExtension;
        logger.info("MultiselectionConverter outputFileName: " + outputFileName);
        
        
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
