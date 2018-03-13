package %COMPONENT_PKG%;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class %COMPONENT_NAME%Main extends AbstractComponent {

%OPTIONS_DEFN_BLOCK%
    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        %COMPONENT_NAME%Main tool = new %COMPONENT_NAME%Main();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public %COMPONENT_NAME%Main() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

%PROCESS_OPTIONS_BLOCK%
    }

    @Override
    protected void parseOptions() {

%PARSE_OPTIONS_BLOCK%
    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

%RUN_EXTERNAL_BLOCK%

%OUTPUT_DEFN_BLOCK%
        if (this.isCancelled()) {
            this.addErrorMessage("Cancelled workflow during component execution.");
        } else {
%OUTPUT_USAGE_BLOCK%
        }

        System.out.println(this.getOutput());

    }
}
