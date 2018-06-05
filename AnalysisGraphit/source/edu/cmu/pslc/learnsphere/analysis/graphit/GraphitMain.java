package edu.cmu.pslc.learnsphere.analysis.graphit;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphitMain extends AbstractComponent {
private static void copyFileUsingFileChannels(File source, File dest)

        throws IOException {

    FileChannel inputChannel = null;

    FileChannel outputChannel = null;

    try {

        inputChannel = new FileInputStream(source).getChannel();

        outputChannel = new FileOutputStream(dest).getChannel();

        outputChannel.transferFrom(inputChannel, 0, inputChannel.size());

    } finally {

        inputChannel.close();

        outputChannel.close();

    }

}

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
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternal();
        // Attach the output files to the component output with addOutputFile(..>)
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsolutePath() + "/Statistics Practice - SamSam Spacing Effects Full Model.html");
            File file1 = new File(outputDirectory.getAbsolutePath() + "/R_output_model_summary.txt");
          File file2 = new File(outputDirectory.getAbsolutePath() + "/Posttest - SamSam Spacing Effects Full Model.html");
   
            if (file0 != null && file0.exists()&& file1 != null && file1.exists() && file2 != null && file2.exists()) {
               

           File source = new File("plotly-latest.min.js");
            File dest = new File(outputDirectory.getAbsolutePath() + "/plotly-latest.min.js");
                try {
                    copyFileUsingFileChannels(source, dest);
                } catch (IOException ex) {
                    Logger.getLogger(GraphitMain.class.getName()).log(Level.SEVERE, null, ex);
                }
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

            } else {
                this.addErrorMessage("An unknown error has occurred with the Graphit component.");
            }

        }

        // Send the component output back to the workflow.
       System.out.println(this.getOutput());
    }

}
