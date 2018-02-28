package %COMPONENT_PKG%;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.OutputStream;

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

    /**
     * The test() method is used to test the known inputs prior to running.
     * @return true if passing, false otherwise
     */
    @Override
    protected Boolean test() {
        Boolean passing = true;
        return passing;
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

    // Constant
    private static final String NEW_LINE_CHAR = "\n";

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

%INPUT_DEFN_BLOCK%
        File outputDirectory = new File(this.getComponentOutputDir());
%OUTPUT_DEFN_BLOCK%

        // Light-weight example of using the input and output files.
        try {
            // Process input file as necessary.
            int lineCount = 0;
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(inputFile0));
                while (in.readLine() != null) { lineCount++; }
            } finally {
                if (in != null) { in.close(); }
            }
            
            OutputStream outputStream = null;
            try {
                byte[] label = null;
                byte[] value = null;
                
                outputStream = new FileOutputStream(outputFile0);
                
                label = ("This file has the %OPT_NAME% value: ").getBytes("UTF-8");
                value = %OPT_NAME%.getBytes("UTF-8");
                
                outputStream.write(label);
                outputStream.write(value);
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));
                
                outputStream = new FileOutputStream(outputFile0);
                
                label = ("This file has the number of input lines: " + lineCount).getBytes("UTF-8");
                
                outputStream.write(label);
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));
                
            } catch (Exception e) {
                // This will be picked up by the workflows platform and relayed to the user.
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (Exception e) {
            // This will be picked up by the workflows platform and relayed to the user.
            e.printStackTrace();
        }

        if (this.isCancelled()) {
            this.addErrorMessage("Cancelled workflow during component execution.");
        } else {
%OUTPUT_USAGE_BLOCK%
        }

        System.out.println(this.getOutput());

        for (String err : this.errorMessages) {
            // These will also be picked up by the workflows platform and relayed to the user.
            System.err.println(err);
        }
    }
}
