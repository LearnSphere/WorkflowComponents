package edu.cmu.pslc.learnsphere.exports.zip;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ExportZipMain extends AbstractComponent {

    /**
     * Main method.
     * @param args workflow component arguments (see build.xml's runComponent)
     */
    public static void main(String[] args) {
        // Create the component and start it.
        ExportZipMain tool = new ExportZipMain();
        tool.startComponent(args);

    }

    /** Default constructor.
     */
    public ExportZipMain() {
        super();
    }

    @Override
    protected void processOptions() {

    }

    @Override
    protected void parseOptions() {

    }


    @Override
    /**
     * The run method contains the actual process for the component, unless an
     * external program is being run in its stead.
     */
    protected void runComponent() {
        List<File> allFiles = new ArrayList<File>();


        allFiles.addAll(this.getAttachments(0));


        File zipFile = WorkflowHelper.compressFiles(this.getComponentOutputDir(), allFiles);

        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        String fileLabel = "zip";

        this.addOutputFile(zipFile, nodeIndex, fileIndex, fileLabel);

        System.out.println(this.getOutput());

    }

    /**
     * The test() method is used to test the inputs or options prior to the "run" method call.
     * @return true if passing, false otherwise
     */
    @Override
    protected Boolean test() {
        return true;
    }


}
