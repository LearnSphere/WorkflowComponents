package edu.cmu.pslc.learnsphere.transform.sensordatainterpolate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class SensorDataInterpolateMain extends AbstractComponent {

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        SensorDataInterpolateMain tool = new SensorDataInterpolateMain();
        tool.startComponent(args);

    }

    /**
     * This class runs Sensor Data Interpolation on one file.
     */
    public SensorDataInterpolateMain() {
        super();


    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("tab-delimited", 0, 0, ".*");
        

    }

    @Override
    public Boolean test() {
        Boolean passing = true;
        // The first index is the input node index of this component.
        // The second index is the file index for that node.
        return passing;
    }

    /**
     * Sensor Data Interpolation for the two files and adds the resulting file to the component output.
     */
    List<Integer> listTime1=new ArrayList<>();
    List<Integer> listSensorData1=new ArrayList<>();
   
    
    @Override
    protected void runComponent() {  
        File outputDirectory = this.runExternal();
        String dir= this.getToolPath();
    
        //.. Attach the output files to the component output with addOutputFile...//
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsoluteFile() + "/SensorDataInterpolate.txt");
            
            if (file0 != null && file0.exists() ) {

                Integer nodeIndex = 0;
                Integer fileIndex = 0;      
                String fileLabel = "tab-delimited";  
                this.addOutputFile(file0, nodeIndex, fileIndex, fileLabel);

            } else {
                this.addErrorMessage("An unknown error has occurred with the SensordataInterpolate component.");
            }

        }

        // Send the component output back to the workflow.
       System.out.println(this.getOutput());
        
    }


}
