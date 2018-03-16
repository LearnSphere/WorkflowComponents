package edu.cmu.pslc.learnsphere.analysis.datalab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

import edu.cmu.datalab.util.Gradebook;
import edu.cmu.datalab.util.GradebookUtils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 * Workflow component: Datalab Correlation tool.
 */
public class CorrelationMain extends AbstractComponent {

    private String[] headers;
    private String[] students;
    private int numItems = 0;
    private int numStudents = 0;

    public static void main(String[] args) {

        CorrelationMain tool = new CorrelationMain();
        tool.startComponent(args);
    }

    public CorrelationMain() {
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

    /**
     * Processes the gradebook file.
     */
    @Override
    protected void runComponent() {

        Array2DRowRealMatrix data = null;

        try {
            Gradebook gradebook = GradebookUtils.readFile(this.getAttachment(0, 0));

            data = gradebook.getData();
            headers = gradebook.getHeaders();
            students = gradebook.getStudents();
            numItems = headers.length - 1;  // don't include student column
            numStudents = students.length;

            Boolean summaryColPresent =
                this.getOptionAsString("summary_column_present").equalsIgnoreCase("true");

        } catch (Exception e) {
            String msg = "Failed to parse gradebook and compute correlation. " + e;
            logger.info(msg);
            this.addErrorMessage(msg);
        }

        // Write the correlation file.
        File correlationFile = populateCorrelationFile(data);

        // If we haven't seen any errors yet...
        if (this.errorMessages.size() == 0) {
            if (correlationFile == null) {
                this.addErrorMessage("Failed to create output correlation file.");
            } else {
                Integer nodeIndex = 0;
                Integer fileIndex = 0;
                String fileType = "correlation";
                this.addOutputFile(correlationFile, nodeIndex, fileIndex, fileType);
            }
        }

        System.out.println(this.getOutput());

        for (String err : this.errorMessages) {
            // These will also be picked up by the workflows platform and relayed to the user.
            System.err.println(err);
        }
    }

    // Constant
    private static final String NEW_LINE_CHAR = "\n";

    // Constant
    private static final String TAB_CHAR = "\t";

    /**
     * Write the Correlation values to a file.
     * @param data the matrix of data
     * @return the populated file
     */
    private File populateCorrelationFile(Array2DRowRealMatrix data) {

        if (data == null) { return null; }

        File correlationFile = this.createFile("Correlation", ".txt");

        // Java try-with-resources
        try (OutputStream outputStream = new FileOutputStream(correlationFile)) {

                // Write header to export
                for (int i = 0; i < headers.length; i++) {
                    if (i != 0) { outputStream.write(headers[i].getBytes("UTF-8")); }
                    if (i < headers.length - 1) { outputStream.write(TAB_CHAR.getBytes("UTF-8")); }
                }
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                // Write values to export
                double[][] theData = data.getData();
                for (int i = 0; i < numItems; i++) {
                    outputStream.write(headers[i+1].getBytes("UTF-8"));
                    outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                    for (int j = 0; j < numItems; j++) {
                        if (j > i) { continue; }
                        double correlationValue = getPearsonsCorrelation(data.getColumn(i),
                                                                         data.getColumn(j));
                        outputStream.write(String.valueOf(correlationValue).getBytes("UTF-8"));
                        if (j < (numItems - 1)) { outputStream.write(TAB_CHAR.getBytes("UTF-8")); }
                    }

                    outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                // This will be picked up by the workflows platform and relayed to the user.
                e.printStackTrace();
            }

        return correlationFile;
    }

    /**
     * Compute Pearson's correlation
     * @param var1 the first value
     * @param var2 the second value
     * @return double the correlation
     */
    private double getPearsonsCorrelation (double[] var1, double[] var2) {
        PearsonsCorrelation pc = new PearsonsCorrelation();
        ArrayList<double[]> cleaned = GradebookUtils.cleanNullValue(var1, var2);
        double[] temp_var1 = cleaned.get(0);
        double[] temp_var2 = cleaned.get(1);
        double correlationVal = Double.NaN;
        try {
            correlationVal = pc.correlation(temp_var1, temp_var2);
        } catch (DimensionMismatchException dex) {
            logger.error("Correlation error: items' length do not match.");
        } catch (MathIllegalArgumentException dex) {
            logger.error("Correlation error: not enough data for items.");
        }
        return correlationVal;
    }
}
