package edu.cmu.pslc.learnsphere.transform.sensordataalign;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

import java.util.ArrayList;
import java.util.List;



public class SensorDataAlignMain extends AbstractComponent {

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        SensorDataAlignMain tool = new SensorDataAlignMain();
        tool.startComponent(args);

    }

    /**
     * This class runs Sensor Data Align on two files.
     */
    public SensorDataAlignMain() {
        super();


    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("tab-delimited", 0, 0, ".*");
        this.addMetaDataFromInput("tab-delimited", 1, 0, ".*");

    }

    @Override
    public Boolean test() {
        Boolean passing = true;
        // The first index is the input node index of this component.
        // The second index is the file index for that node.
        return passing;
    }

    /**
     * Sensor Data Align for the two files and adds the resulting file to the component output.
     */
    List<Integer> listTime1=new ArrayList<>();
    List<Integer> listSensorData1=new ArrayList<>();
    List<Integer> listTime2=new ArrayList<>();
    List<Integer> listSensorData2=new ArrayList<>();
    
    @Override
    protected void runComponent() {  
        File outputDirectory = this.runExternal();
        //String dir= this.getToolPath();
    
        //.. Attach the output files to the component output with addOutputFile...//
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsoluteFile() + "/SensorDataAligned.txt");
            
            if (file0 != null && file0.exists() ) {

                Integer nodeIndex = 0;
                Integer fileIndex = 0;      
                String fileLabel = "tab-delimited";  
                this.addOutputFile(file0, nodeIndex, fileIndex, fileLabel);

            } else {
                this.addErrorMessage("An unknown error has occurred with the SensordataAlign component.");
            }

        }

        // Send the component output back to the workflow.
       System.out.println(this.getOutput());
        
    }


}


/*
package edu.cmu.pslc.learnsphere.generate.pfafeatures;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class PFAMain extends AbstractComponent {

    /** Component option (model). */
/*
    String modelName = null;

    public static void main(String[] args) {

        PFAMain tool = new PFAMain();
        tool.startComponent(args);
    }

    public PFAMain() {
        super();
    }


    @Override
    protected void parseOptions() {

        if (this.getOptionAsString("model") != null) {
            modelName = this.getOptionAsString("model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
        }

    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // Add the column headers from our input file to this component's output metadata,
        // plus one extra column for Predicted Error Rate, if it doesn't already exist.

        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("transaction", 0, 0, ".*");
        this.addMetaData("transaction", 0, META_DATA_LABEL, "label0", 0, "KC (" + modelName + ")");
    }

    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternalMultipleFileOuput();
        // Attach the output files to the component output: file_type = "analysis-summary", label = ""
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File outputFile = new File(outputDirectory.getAbsoluteFile() + "/transaction file with added features.txt");
            Integer nodeIndex0 = 0;
            Integer fileIndex0 = 0;
            String label0 = "transaction";
            logger.info("Added file: " + outputFile.getAbsolutePath());
            this.addOutputFile(outputFile, nodeIndex0, fileIndex0, label0);
        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
}


*/