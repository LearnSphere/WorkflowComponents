package edu.cmu.pslc.learnsphere.analysis.analysisFTest5X2;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class AnalysisFTest5X2 extends AbstractComponent {


    public static void main(String[] args) {

        AnalysisFTest5X2 tool = new AnalysisFTest5X2();
        tool.startComponent(args);
    }

    public AnalysisFTest5X2() {
        super();
    }

    @Override
    protected void parseOptions() {

    }

    @Override
    protected void processOptions() {

    }

    @Override
    protected void runComponent() {

      File outputDirectory = this.runExternal();
      if (outputDirectory.isDirectory() && outputDirectory.canRead()) {

        File file0 = new File(outputDirectory.getAbsolutePath() + "/test.txt");
        File file1 = new File(outputDirectory.getAbsolutePath() + "/R_output_model_summary.txt");
        if (file0 != null && file0.exists() && file1 != null && file1.exists()) {

          Integer nodeIndex = 0;
          Integer fileIndex = 0;
          String fileLabel = "text";
          this.addOutputFile(file0, nodeIndex, fileIndex, fileLabel);

          Integer nodeIndex1 = 1;
          Integer fileIndex1 = 0;
          String fileLabel1 = "text";
          this.addOutputFile(file1, nodeIndex1, fileIndex1, fileLabel1);

        } else {
          this.addErrorMessage("Files missing.");
        }
      
      }
      // Send the component output back to the workflow.
      System.out.println(this.getOutput());
    }
}
