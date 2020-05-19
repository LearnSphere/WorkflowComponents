package edu.cmu.pslc.learnsphere.analysis.datalab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

import edu.cmu.datalab.util.Gradebook;
import edu.cmu.datalab.util.GradebookUtils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Workflow component: Datalab Cronbach's Alpha tool.
 */
public class CronbachsAlphaMain extends AbstractComponent {

    private String[] headers;
    private String[] students;
    private int numItems = 0;
    private int numStudents = 0;
    private static String htmlTemplateName = "cronbachsAlphaTemplate.html";

    public static void main(String[] args) {

        CronbachsAlphaMain tool = new CronbachsAlphaMain();
        tool.startComponent(args);
    }

    public CronbachsAlphaMain() {
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

        Boolean summaryColPresent =
            this.getOptionAsString("summary_column_present").equalsIgnoreCase("true");
      
        //replace all NA, NONE or NAN with empty string
        File tempFile = replaceNAStr(this.getAttachment(0, 0), "");
        Array2DRowRealMatrix emptyCells = null;
        try {
        	//Gradebook gradebook = GradebookUtils.readFile(this.getAttachment(0, 0));
        	Gradebook gradebook = GradebookUtils.readFile(tempFile);

            data = gradebook.getData();
            headers = gradebook.getHeaders();
            students = gradebook.getStudents();
            numItems = headers.length - 1;  // don't include student column
            numStudents = students.length;
            //mark which cell is empty
            emptyCells = markEmptyCells(numStudents, numItems, tempFile);
            

            if (summaryColPresent) {
                // Remove summary column, this is calculated in the algorithm
                String [] newHeaders = new String [headers.length - 1];
                for (int i = 0; i < headers.length - 1; i++) {
                    newHeaders[i] = headers[i];
                }
                headers = newHeaders;
                data = removeSummaryColumn(data);
                emptyCells = removeSummaryColumn(emptyCells);
            }

        } catch (Exception e) {
            String msg = "Failed to parse gradebook and compute Cronbach's Alpha. " + e;
            logger.info(msg);
            this.addErrorMessage(msg);
        }
        //delete tempFile
        if (tempFile != null && tempFile.exists())
        	tempFile.delete();
        
        // Write the output file.
        File outputFile = populateCronbachsAlphaFile(data, emptyCells, summaryColPresent);

        // If we haven't seen any errors yet...
        if (this.errorMessages.size() == 0) {
            if (outputFile == null) {
                this.addErrorMessage("Failed to create output Cronbach's alpha file.");
            } else {
                // Add the correlation file to the HTML visualization of it
                File htmlFile = addDataReferenceToHtmlFile(outputFile, summaryColPresent);

                if (htmlFile != null) {
                    Integer nodeIndex = 0;
                    Integer fileIndex = 0;
                    String fileType = "inline-html";
                    this.addOutputFile(htmlFile, nodeIndex, fileIndex, fileType);

                    nodeIndex = 1;
                    fileIndex = 0;
                    fileType = "correlation";
                    this.addOutputFile(outputFile, nodeIndex, fileIndex, fileType);
                } else {
                    this.addErrorMessage("Could not create correlation html file");
                }
            }
        }

        System.out.println(this.getOutput());


        for (String err : this.errorMessages) {
            // These will also be picked up by the workflows platform and relayed to the user.
            System.err.println(err);
        }

    }

    /**
     * Removes the last column of the matrix. This is designated as the summary column
     * and does not need to be included with the input file.
     */
    private Array2DRowRealMatrix removeSummaryColumn(Array2DRowRealMatrix data) {
        Array2DRowRealMatrix dataWithoutSummary = new Array2DRowRealMatrix(
            data.getRowDimension(), data.getColumnDimension() - 1);

        for (int i = 0; i < data.getRowDimension(); i++) {
            for (int j = 0; j < data.getColumnDimension() - 1; j++) {
                double cellVal = data.getEntry(i, j);
                dataWithoutSummary.setEntry(i, j, cellVal);
            }
        }

        return dataWithoutSummary;
    }
    
    private Array2DRowRealMatrix markEmptyCells(int studentCnt, int itemCount, File tempFile) throws Exception {

    	Array2DRowRealMatrix emptyCells = new Array2DRowRealMatrix(studentCnt, itemCount);;

        FileInputStream inStream = null;
        BufferedReader bufferedReader = null;

        try {
        	inStream = new FileInputStream(tempFile);
            bufferedReader = new BufferedReader(new InputStreamReader(inStream));
            
            String headerLine = bufferedReader.readLine();
            String delim = null;
            if (headerLine.contains("\t")) {
                delim = "\t";
            } else if (headerLine.contains(",")) {
            	delim = ",";
            }
            if (delim == null)
            	return null;
           
            ArrayList<String> lines = new ArrayList<String>();
            int rowIndex = 0;
            String line = bufferedReader.readLine();
            while (line != null) {
            	lines.add(line);
                line = bufferedReader.readLine();
            }
            for (String s : lines) {
                    String[] rowValues = s.split(delim);
                    int colIndex = -1;
                    for (String valueStr : rowValues) {
                        if (colIndex == -1) {
                            //don't do anything
                        } else if (valueStr.trim().length() == 0) {
                        	emptyCells.setEntry(rowIndex, colIndex, 1);
                        } else {
                        	emptyCells.setEntry(rowIndex, colIndex, 0);
                        }
                        colIndex++;
                    }
                    
                    if (rowValues.length < itemCount + 1) {
                    	for (int i = rowValues.length-1; i <itemCount ; i++)
                    		emptyCells.setEntry(rowIndex, i, 1);
                    }
                    rowIndex++;
                }

            } catch (Exception e) {
                logger.info("Exception: " + e);
                throw e;
            } finally {

                // Don't propagate the exception.
                try {
                    if (inStream != null) { inStream.close(); }
                    if (bufferedReader != null) { bufferedReader.close(); }
                } catch (Exception e) {
                    logger.info("Failed to close resources: " + e);
                }
            }

            return emptyCells;
        }

    // Constant
    private static final String NEW_LINE_CHAR = "\n";

    // Constant
    private static final String TAB_CHAR = "\t";

    /**
     * Write the CronbachsAlpha values to a file.
     * @param data the matrix of data
     * @return the populated file
     */
    private File populateCronbachsAlphaFile(Array2DRowRealMatrix data, Array2DRowRealMatrix emptyCellFlags, boolean summaryColPresent) {

        if (data == null) { return null; }

        File outputFile = this.createFile("CronbachsAlpha", ".txt");

        // Java try-with-resources
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {

            // Write values to export
            Double[] cronbachValues = getCronbachValues(data, emptyCellFlags);
            int iter = numItems;
            if (!summaryColPresent)
            	iter = numItems + 1;
            for (int i = 0; i < iter; i++) {
                String label = "All items";
                if (i > 0) {
                    label = headers[i].trim() + " excluded";
                }
                outputStream.write(label.getBytes("UTF-8"));

                outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                outputStream.write(String.valueOf(cronbachValues[i]).getBytes("UTF-8"));
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));
            }

        } catch (Exception e) {
            // This will be picked up by the workflows platform and relayed to the user.
            e.printStackTrace();
        }

        return outputFile;
    }

    /**
     * Compute Cronbach's alpha for whole set and for eliminating each item
     * @param data Array2DRowRealMatrix
     * @return double[] with n+1 values where n is the number of columns of data.
     *  The first value is for the whole set, the rest is for eliminating each column
     *
     */
    private Double[] getCronbachValues (Array2DRowRealMatrix data, Array2DRowRealMatrix emptyCellFlags) {
    	//delete the values in the cells that are supposed to be empty
    	for (int i = 0; i < data.getRowDimension(); i++) {
    		for (int j = 0; j <data.getColumnDimension(); j++) {
    			if (emptyCellFlags.getEntry(i, j) == 1.0)
    				data.setEntry(i, j, Double.NaN);
    		}
    	}
    	int allRowCnt = data.getRowDimension();
        int rowCnt = allRowCnt;
        int allColumnCnt = data.getColumnDimension();
        int columnCnt = allColumnCnt;
        //keep count of rows for each column
        double[] rowCounts = new double[allColumnCnt];
        //column variance among rows
        double[] varOfColumn = new double[allColumnCnt];
        //keep count of columns for each row
        double[] columnCounts = new double[allRowCnt];
        //total and average for each rows
        double[] rowTotal = new double[allRowCnt];
        double[] rowAverage = new double[allRowCnt];
        double[] rowAvgMultiByColumnCount = new double[allRowCnt];
        //set column count, total, avg*columnCount for each row
        //for row with only one column, set all measures to NaN
        for (int i = 0; i < allRowCnt; i++) {
            double[] thisRow = data.getRow(i);
            ArrayList<Double> thisRowAL = new ArrayList<Double>();
            for (int j = 0; j < thisRow.length; j++) {
                if (!Double.isNaN(thisRow[j])) {
                    thisRowAL.add(thisRow[j]);
                }
            }
            double[] rowValues =  ArrayUtils.toPrimitive(thisRowAL.toArray(new Double[0]));
            if (rowValues == null) {
                columnCounts[i] = Double.NaN;
            } else {
                columnCounts[i] = rowValues.length;
            }
            if (rowValues == null || rowValues.length < 2) {
                rowCnt--;
                rowTotal[i] = Double.NaN;
                rowAverage[i] = Double.NaN;
                rowAvgMultiByColumnCount[i] = Double.NaN;
            } else {
                rowTotal[i] = StatUtils.sum(rowValues);
                rowAverage[i] = StatUtils.mean(rowValues);
            }
        }

        //set row count and variance for each column
        for (int i = 0; i < allColumnCnt; i++) {
            double[] thisCol = data.getColumn(i);
            ArrayList<Double> thisColAL = new ArrayList<Double>();
            for (int j = 0; j < thisCol.length; j++) {
                if (!Double.isNaN(thisCol[j]))
                    thisColAL.add(thisCol[j]);
            }
            double[] colValues = ArrayUtils.toPrimitive(thisColAL.toArray(new Double[0]));
            if (colValues == null) {
                rowCounts[i] = Double.NaN;
            } else {
                rowCounts[i] = colValues.length;
            }
            if (colValues == null || colValues.length < 2) {
                columnCnt--;
                varOfColumn[i] = Double.NaN;
            } else {
                varOfColumn[i] = StatUtils.variance(colValues);
            }
        }

        //rowAvgMultiByColumnCount is (row's mean)*columnCnt
        for (int i = 0; i < allRowCnt; i++) {
            if (!Double.isNaN(rowAverage[i])) {
                rowAvgMultiByColumnCount[i] = rowAverage[i] * (double)columnCnt;
            } else {
                rowAvgMultiByColumnCount[i] = Double.NaN;
            }
        }
        double[] returnValues = new double[1 + allColumnCnt];
        double sumOfColumnVariance = 0.0;
        //compute alpha for whole set
        if (rowCnt < 2 || columnCnt < 2) {
            returnValues[0] = Double.NaN;
            sumOfColumnVariance = Double.NaN;
        } else {
            //compute variance with rowAvgMultiByColumnCount
            //exclude NaN
            ArrayList<Double> rowTotalAL = new ArrayList<Double>();
            for (int i = 0; i < rowAvgMultiByColumnCount.length; i++) {
                if (!Double.isNaN(rowAvgMultiByColumnCount[i]))
                    rowTotalAL.add(rowAvgMultiByColumnCount[i]);
            }
            
            if (rowTotalAL.size() < 2) {
                returnValues[0] = Double.NaN;
            } else {
                double[] rowTotalValues = ArrayUtils.toPrimitive(rowTotalAL.toArray(new Double[0]));
                double varianceOfTotal = StatUtils.variance(rowTotalValues);
                //sum of column variance
                //exclude NaN
                ArrayList<Double> varOfColumnAL = new ArrayList<Double>();
                for (int i = 0; i < varOfColumn.length; i++) {
                    if (!Double.isNaN(varOfColumn[i])) {
                        varOfColumnAL.add(varOfColumn[i]);
                    }
                }
                if (varOfColumnAL.size() < 2) {
                    returnValues[0] = Double.NaN;
                } else {
                    double[] varOfColumnValues =
                        ArrayUtils.toPrimitive(varOfColumnAL.toArray(new Double[0]));
                    sumOfColumnVariance = StatUtils.sum(varOfColumnValues);
                    double value = computeAlphaValue(columnCnt,
                                                     sumOfColumnVariance, varianceOfTotal);
                    if (Double.isInfinite(value) || Double.isNaN(value)) {
                        returnValues[0] = Double.NaN;
                        logger.info("Cronbach's alpha is set to null because alpha "
                                    + "value is computed to Infinite or NaN.");
                    } else {
                        returnValues[0] = value;
                    }
                }
            }
        }

        //compute alpha while eliminating each column
        for (int i = 0; i < allColumnCnt; i++) {
            if (columnCnt < 3 || rowCnt < 2 || Double.isNaN(varOfColumn[i])) {
                returnValues[i + 1] = Double.NaN;
            } else {
                //recompute sum of item variance
                double subsetSumOfColumnVariance = sumOfColumnVariance - varOfColumn[i];
                //recompute variance of total
                double[] totalWithoutThisCol = new double[allRowCnt];
                for (int j = 0; j < allRowCnt; j++) {
                    if (Double.isNaN(rowAvgMultiByColumnCount[j])) {
                        totalWithoutThisCol[j] = Double.NaN;
                    } else {
                        if (Double.isNaN(data.getEntry(j, i))) {
                            totalWithoutThisCol[j] =
                                (rowAvgMultiByColumnCount[j] / (double)columnCnt)
                                * (double)(columnCnt - 1);
                        } else {
                            double sumofThisRow =
                                (rowAvgMultiByColumnCount[j] / (double)columnCnt) * columnCounts[j];
                            totalWithoutThisCol[j] =
                                ((sumofThisRow - data.getEntry(j, i))
                                 / ((double)columnCounts[j] - 1)) * (double)(columnCnt - 1);
                        }
                    }
                }

                //compute variance for row total without this item
                //exclude NaN
                ArrayList<Double> rowTotalAL = new ArrayList<Double>();
                for (int j = 0; j < totalWithoutThisCol.length; j++) {
                    if (!Double.isNaN(totalWithoutThisCol[j])) {
                        rowTotalAL.add(totalWithoutThisCol[j]);
                    }
                }
                if (rowTotalAL.size() < 2) {
                    returnValues[i + 1] = Double.NaN;
                } else {
                    double[] rowTotalValues =
                        ArrayUtils.toPrimitive(rowTotalAL.toArray(new Double[0]));
                    double varianceOfTotal = StatUtils.variance(rowTotalValues);
                    double value = computeAlphaValue(columnCnt - 1,
                                                     subsetSumOfColumnVariance,
                                                     varianceOfTotal);
                    if (Double.isInfinite(value) || Double.isNaN(value)) {
                        returnValues[i + 1] = Double.NaN;
                        logger.info("Cronbach's alpha is set to null because alpha "
                                    + "value is computed to Infinite or NaN.");
                    } else
                        returnValues[i + 1] = value;
                }
            }
        }

        Double[] returnValues_D = new Double[returnValues.length];
        for (int i = 0; i < returnValues.length; i++) {
            if (Double.isNaN(returnValues[i])) {
                returnValues_D[i] = null;
            } else {
                returnValues_D[i] = returnValues[i];
            }
        }

        return returnValues_D;
    }

    private double computeAlphaValue (double count,
                                      double sumOfItemVariance, double varianceOfTotal) {
    	return ((double)(count / (count - 1))) * (1 - sumOfItemVariance / varianceOfTotal);
    }

    private File addDataReferenceToHtmlFile(File correlationFile, boolean summaryColPresent) {
        File htmlTemplateFile = new File(this.getToolDir() + "/program/" + htmlTemplateName);
        File outputFile = null;
        if (htmlTemplateFile.exists() && htmlTemplateFile.isFile() && htmlTemplateFile.canRead()) {
            outputFile = this.createFile("CronbachsAlphaVisualization.html");

            String outputSubpath = this.componentOutputDir
                                   .replaceAll("\\\\", "/")
                                   .replaceAll("^.*/workflows/", "workflows/");
            String dataFilePath = "LearnSphere?htmlPath=" + outputSubpath + correlationFile.getName();

            BufferedReader bReader = null;
            FileReader fReader = null;

            BufferedWriter bWriter = null;
            FileWriter fWriter = null;

            try {

                fReader = new FileReader(htmlTemplateFile);
                bReader = new BufferedReader(fReader);

                fWriter = new FileWriter(outputFile);
                bWriter = new BufferedWriter(fWriter);

                String line = null;
                while ((line = bReader.readLine()) != null) {
                    if (line.contains("${input0}")) {
                        line = line.replaceAll(Pattern.quote("${input0}"),
                                               dataFilePath); // name is data.txt
                    }
                    if (line.contains("${summaryColPresent}")) {
                        if (summaryColPresent) {
                            line = line.replaceAll(Pattern.quote("${summaryColPresent}"),
                                                   "1"); // 1 summary column
                        } else {
                            line = line.replaceAll(Pattern.quote("${summaryColPresent}"),
                                                   "0"); // No summary column
                        }
                    }

                    bWriter.append(line + "\n");
                }
            } catch (IOException e) {
                this.addErrorMessage(e.toString());
            } finally {
                try {
                    if (bReader != null) {
                        bReader.close();
                    }
                    if (bWriter != null) {
                        bWriter.close();
                    }
                } catch (Exception e) {

                }
            }
        }
        return outputFile;
    }
    
    //replace all NA, Nan, None strings in the input file
    private File replaceNAStr(File inputFile, String replaceBy) {
    	String absolutePath = inputFile.getAbsolutePath();
    	String filePath = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
    	String tempFileName = filePath + File.separator + "temp_" + inputFile.getName();
    	File tempFile = new File(tempFileName);
    	if (tempFile.exists()) {
    		tempFile.delete();
    	}
    	BufferedReader bReader = null;
        FileReader fReader = null;

        BufferedWriter bWriter = null;
        FileWriter fWriter = null;

        try {

            fReader = new FileReader(inputFile);
            bReader = new BufferedReader(fReader);

            fWriter = new FileWriter(tempFile);
            bWriter = new BufferedWriter(fWriter);

            String line = null;
            while ((line = bReader.readLine()) != null) {
            	//nan should be in front of na
            	if (line.toLowerCase().contains("nan") ) {
                    line = line.replaceAll("(?i)nan",replaceBy); 
                }
            	if (line.toLowerCase().contains("na") ) {
                    line = line.replaceAll("(?i)na",replaceBy); 
                }
                if (line.toLowerCase().contains("none") ) {
                    line = line.replaceAll("(?i)none",replaceBy); 
                }
                
                bWriter.append(line + "\n");
            }
        } catch (IOException e) {
            this.addErrorMessage(e.toString());
        } finally {
            try {
                if (bReader != null) {
                    bReader.close();
                }
                if (bWriter != null) {
                    bWriter.close();
                }
            } catch (Exception e) {

            }
        }
    	return tempFile;
    }
}
