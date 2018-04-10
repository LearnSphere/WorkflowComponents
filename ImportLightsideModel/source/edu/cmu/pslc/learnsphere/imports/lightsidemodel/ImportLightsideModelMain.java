package edu.cmu.pslc.learnsphere.imports.lightsidemodel;


import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ImportLightsideModelMain extends AbstractComponent {

    public static void main(String[] args) {

        ImportLightsideModelMain tool = new ImportLightsideModelMain();
        tool.startComponent(args);
    }

    public ImportLightsideModelMain() {
        super();
    }

    /**
     * Blindly imports a lightside model and passes it along to another
     * component that will validate and use it.
     */
    @Override
    protected void runComponent() {

        /*
         * Could check this lightside model file for validity, but for now we assume
         * the subsequent component that uses it will do this work.
         * 
         * Here's how the Gradebook importer does it, for reference
         * 
         * try {
            Gradebook gradebook = GradebookUtils.readFile(this.getAttachment(0, 0));
        } catch (Exception e) {
            String msg = "Failed to parse gradebook file. " + e; 
            logger.info(msg);
            this.addErrorMessage(msg);
        }*/

        System.out.println(this.getOutput());

        for (String err : this.errorMessages) {
            // These will also be picked up by the workflows platform and relayed to the user.
            System.err.println(err);
        }
    }
}
