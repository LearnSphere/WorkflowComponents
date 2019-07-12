package edu.cmu.learnsphere.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;

import edu.cmu.pl2.item.MathiaGoalItem;
import edu.cmu.pslc.datashop.servlet.workflows.WorkflowImportHelper;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class PerformanceDifferenceAnalysisMain extends AbstractComponent {
        private String finalGradeColName;
        private String qpaColName;
        private String unitsFactorColName;
        private String anormalyFactorColName;
        private int finalGradeColInd;
        private int qpaColInd;
        private int unitsFactorColInd;
        private int anormalyFactorColInd; 
        private static char delim = ',';
        private static String[] ACCEPTALE_LETTER_GRADES= {"A", "B", "C", "D", "F", "R", "W"};

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        PerformanceDifferenceAnalysisMain tool = new PerformanceDifferenceAnalysisMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public PerformanceDifferenceAnalysisMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

    }

    @Override
    protected void parseOptions() {
        finalGradeColName = this.getOptionAsString("finalGrade");
        qpaColName = this.getOptionAsString("qpa");
        unitsFactorColName = this.getOptionAsString("unitsFactor");
        anormalyFactorColName = this.getOptionAsString("anormalyFactor");
    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {
            //process files
            File dataFile = this.getAttachment(0, 0);
            logger.info("dataFile: " + dataFile.getAbsolutePath());
            
            //process files, make sure delimiter is , 
            LinkedHashMap<String, Integer> columnHeaders = WorkflowImportHelper.getColumnHeaders(dataFile, delim);
            if (columnHeaders.size() <= 1) {
                    //throw error if not CSV, send error message
                    String errMsgForUI = "Input file should be in CSV format.";
                    String errMsgForLog = errMsgForUI + " File: " + dataFile.getAbsolutePath();
                    handleAbortingError (errMsgForUI, errMsgForLog);
                    return;
            }
            finalGradeColInd = columnHeaders.get(finalGradeColName);
            qpaColInd = columnHeaders.get(qpaColName);
            unitsFactorColInd = columnHeaders.get(unitsFactorColName);
            anormalyFactorColInd = columnHeaders.get(anormalyFactorColName);
            try (BufferedReader bReader = new BufferedReader(new FileReader(dataFile));) {
                    //skip header line
                    String line = bReader.readLine();
                    line = bReader.readLine();
                    while (line != null) {
                            String dataRow[] = line.split("" + delim, -1);
                            //make sure finalGradeCol are numbers or 'A', 'B', 'C', 'D', 'F', 'R', 'W'
                            String finalGrade = dataRow[finalGradeColInd];
                            try {  
                                    Double.parseDouble(finalGrade);
                            } catch(NumberFormatException e){
                                    if (!Arrays.asList(ACCEPTALE_LETTER_GRADES).contains(finalGrade)) {
                                            //throw error if not CSV, send error message
                                            String errMsgForUI = "Column Grade for Target Course has invalid value. The acceptable values are numbers or A, B, C, D, F, R, W.";
                                            String errMsgForLog = errMsgForUI + " File: " + dataFile.getAbsolutePath();
                                            handleAbortingError (errMsgForUI, errMsgForLog);
                                            return;
                                    }
                            }
                            //make sure qpaCol are numbers
                            String qpa = dataRow[qpaColInd];
                            try {  
                                    Double.parseDouble(qpa);
                            } catch(NumberFormatException e){
                                    String errMsgForUI = "Column GPA in Other Units has non-numeric value.";
                                    String errMsgForLog = errMsgForUI + " File: " + dataFile.getAbsolutePath();
                                    handleAbortingError (errMsgForUI, errMsgForLog);
                                    return;
                            }
                            //make sure unitsFactorCol are numbers
                            String unitsFactor = dataRow[unitsFactorColInd];
                            try {  
                                    Double.parseDouble(unitsFactor);
                            } catch(NumberFormatException e){
                                    String errMsgForUI = "Column Number of Units Factored into GPA has non-numeric value.";
                                    String errMsgForLog = errMsgForUI + " File: " + dataFile.getAbsolutePath();
                                    handleAbortingError (errMsgForUI, errMsgForLog);
                                    return;
                            }
                            line = bReader.readLine();
                    }
                    bReader.close();
            } catch (IOException ioe) {
                    String errMsgForUI = "Error reading input file: " + dataFile.getAbsolutePath();
                    String errMsgForLog = errMsgForUI;
                    handleAbortingError (errMsgForUI, errMsgForLog);
            }
            // Run the program...
            //Fut first need to make sure there is no /PerformanceDifferenceAnalysis.pdf exists otherwise R will not work
                
            File existingFile = new File(this.getComponentOutputDir() + "/PerformanceDifferenceAnalysis.pdf");
            if (existingFile.exists() && existingFile.isFile()) {
                    existingFile.delete();
                    logger.info("Performance Difference Analysis deleted exisitng " + existingFile.getAbsolutePath() );
            }
            File outputDirectory = this.runExternal();
            File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/PerformanceDifferenceAnalysis.pdf");
            this.addOutputFile(outputFile0, 0, 0, "pdf");
            System.out.println(this.getOutput());
            
    }
    
    private void handleAbortingError (String errMsgForUI, String errMsgForLog) {
            addErrorMessage(errMsgForUI);
            logger.info("Performance Difference Analysis aborted: " + errMsgForLog );
            System.out.println(getOutput());
            return; 
    }
}
