package edu.cmu.pslc.learnsphere.imports.gradebook;

import edu.cmu.datalab.util.Gradebook;
import edu.cmu.datalab.util.GradebookUtils;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ImportGradebookMain extends AbstractComponent {

    public static void main(String[] args) {

        ImportGradebookMain tool = new ImportGradebookMain();
        tool.startComponent(args);
    }

    public ImportGradebookMain() {
        super();
    }

    /**
     * Processes the gradebook file.
     * Serves as a means for ensuring that the file follows allowed Gradebook format.
     */
    @Override
    protected void runComponent() {

        try {
            Gradebook gradebook = GradebookUtils.readFile(this.getAttachment(0, 0));
        } catch (Exception e) {
            String msg = "Failed to parse gradebook file. " + e; 
            logger.info(msg);
            this.addErrorMessage(msg);
        }

        System.out.println(this.getOutput());

        for (String err : this.errorMessages) {
            // These will also be picked up by the workflows platform and relayed to the user.
            System.err.println(err);
        }
    }
}
