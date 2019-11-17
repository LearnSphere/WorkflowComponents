package edu.cmu.pslc.learnsphere.transform.sensordatajoin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class SensorDataJoinMain extends AbstractComponent {

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        SensorDataJoinMain tool = new SensorDataJoinMain();
        tool.startComponent(args);


    }

    /**
     * This class runs Sensor Data Join on two files.
     */
    public SensorDataJoinMain() {
        super();


    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        //this.addMetaDataFromInput("tab-delimited", 0, 0, ".*");
        //this.addMetaDataFromInput("tab-delimited", 1, 0, ".*");

    }

    @Override
    public Boolean test() {
        Boolean passing = true;
        // The first index is the input node index of this component.
        // The second index is the file index for that node.
        return passing;
    }

    /**
     * Sensor Data Join for the two files and adds the resulting file to the component output.
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
            File file0 = new File(outputDirectory.getAbsoluteFile() + "/segmented_result.csv");
           // File file1 = new File(outputDirectory.getAbsoluteFile() + "/rplot.jpg");
            if (file0 != null && file0.exists() ) {

                Integer nodeIndex = 0;
                Integer fileIndex = 0;      
                String fileLabel = "tab-delimited";  
                
                 Integer nodeIndex1 = 1;
                Integer fileIndex1 = 0;
                String label1 = "image";
                
                this.addOutputFile(file0, nodeIndex, fileIndex, fileLabel);
               // this.addOutputFile(file1, nodeIndex1, fileIndex1, label1);
            } else {
                this.addErrorMessage("An unknown error has occurred with the SensorDatajoin component.");
            }

        }

        // Send the component output back to the workflow.
       System.out.println(this.getOutput());


}
}
