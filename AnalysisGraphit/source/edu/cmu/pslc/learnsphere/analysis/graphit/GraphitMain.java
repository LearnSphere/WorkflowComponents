package edu.cmu.pslc.learnsphere.analysis.graphit;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class GraphitMain extends AbstractComponent {

    public static void main(String[] args) {
           System.out.println("main 1 \n");
           System.out.println("=========================");
           System.out.println(args.length);
          for(String s: args)
              System.out.println(s);
       System.out.println("=========================");
          GraphitMain tool = new GraphitMain();
       tool.startComponent(args);
System.out.println("Main After call");

    }

    public GraphitMain() {
        super();
    }



    @Override
    protected void runComponent() {
        System.out.println("run component\n");
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternal();
        // Attach the output files to the component output with addOutputFile(..>)
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            System.out.println("inside if 1\n");
            File file0 = new File(outputDirectory.getAbsolutePath() + "/Statistics Practice - SamSam Spacing Effects Full Model.html");
            System.out.println(outputDirectory.getAbsolutePath());
            File file1 = new File(outputDirectory.getAbsolutePath() + "/R_output_model_summary.txt");
          File file2 = new File(outputDirectory.getAbsolutePath() + "/Posttest - SamSam Spacing Effects Full Model.html");
            System.out.println("inside if 2\n");
            if ( file1 != null && file1.exists()) {
  System.out.print("inside if 3\n");
               

            
                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "text";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

                Integer nodeIndex1 = 1;
                Integer fileIndex1 = 0;
                String label1 = "text";
                this.addOutputFile(file1, nodeIndex1, fileIndex1, label1);

                Integer nodeIndex2 = 2;
                Integer fileIndex2 = 0;
                String label2 = "text";
                this.addOutputFile(file2, nodeIndex2, fileIndex2, label2);

                
                
                System.out.println("last statemet if \n");
            } else {
                System.out.print("else\n");
                this.addErrorMessage("An unknown error has occurred with the Graphit component.");
            }

        }

        // Send the component output back to the workflow.
        System.out.println("before out\n");
       System.out.println(this.getOutput());
        System.out.println("after out\n");
    }

}
