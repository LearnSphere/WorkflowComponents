package edu.cmu.pslc.learnsphere.transform.classifylightside;

import java.io.File;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class TransformClassifyLightsideMain extends AbstractComponent {

	public static void main(String[] args) {

        TransformClassifyLightsideMain tool = new TransformClassifyLightsideMain();
        tool.startComponent(args);
    }

    public TransformClassifyLightsideMain() {
        super();
    }

    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternal();
        // Attach the output files to the component output with addOutputFile(..>)
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsolutePath() + "/output.txt");

            if (file0 != null && file0.exists() ) {

                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "tab-delimited";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

            } else {
                this.addErrorMessage("The expected output files could not be found.");
            }

        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
    
    
   
    
}
