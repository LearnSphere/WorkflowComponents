package edu.cmu.pslc.learnsphere.imports.ImportEDF;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ImportEDF extends AbstractComponent {


    public static void main(String[] args) {

        ImportEDF tool = new ImportEDF();
        tool.startComponent(args);
    }

    public ImportEDF() {
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

        File file0 = new File(outputDirectory.getAbsolutePath() + "/Results.txt");
        if (file0 != null && file0.exists() ) {

          Integer nodeIndex = 0;
          Integer fileIndex = 0;
          String fileLabel = "text";
          this.addOutputFile(file0, nodeIndex, fileIndex, fileLabel);

  

        } else {
          this.addErrorMessage("Files missing.");
        }

      }
      // Send the component output back to the workflow.
      System.out.println(this.getOutput());
    }
}
