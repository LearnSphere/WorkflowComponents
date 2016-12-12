package edu.cmu.pslc.learnsphere.transform.appendCF;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;






import org.apache.commons.lang.StringUtils;

import edu.cmu.pslc.datashop.item.CfTxLevelItem;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class AppendCFMain extends AbstractComponent {

    private static final String DEFAULT_DELIMITER = "\t";
    private static final String OUTPUT_DELIMITER = "\t";
    private static final String STUDENT_STEP_TYPE = "student-step";
    private static final String TRANSACTION_TYPE = "transaction";
   
    private static final String CF_PREFIX = "CF (";
    private static final String CF_SUFFIX = ")";
    private static final String TXN_LEVEL_PREFIX = "Level (";
    private static final String TXN_LEVEL_SUFFIX = ")";
    private static final String STUDENT_STEP_PROBLEM_HIERARCHY = "Problem Hierarchy";
    private static final String PROBLEM_NAME = "Problem Name";
    private static final String STEP_NAME = "Step Name";
    private static final String PROBLEM_VIEW = "Problem View";
    private static final String STUDENT_NAME = "Anon Student Id";
    
    private static final String APPEND_BY_PROBLEM = "Student-problem Level";
    private static final String APPEND_BY_STEP = "Student-step Level";
    private static final String APPEND_BY_STEP_AND_PROBLEM_VIEW = "Student-step-problem view Level";
    
    private int studentStepFileColLength;
    
    
    private List<String> cfColumnNames = new ArrayList<String>();
    private List<Integer> cfColumnIndices = new ArrayList<Integer>();
    private List<Integer> txnLevelIndices = new ArrayList<Integer>();
    private List<String> txnLevelNames = new ArrayList<String>();
    private int studentStepProblemHierarchyIndex;
    private int txnStudentNameIndex;
    private int studentStepStudentNameIndex;
    private int txnProblemNameIndex;
    private int studentStepProblemNameIndex;
    private int txnStepNameIndex;
    private int studentStepStepNameIndex;
    private int txnProblemViewIndex;
    private int studentStepProblemViewIndex;
    
    private List<StudentStepCFObject> studentStepCFObjs = new ArrayList<StudentStepCFObject>();
    
    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
        AppendCFMain tool = new AppendCFMain();
        tool.startComponent(args);

    }

    /**
     * This class runs Join on two files.
     */
    public AppendCFMain() {
        super();


    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("student-step", 0, 0, ".*");
        this.addMetaDataFromInput("transaction", 1, 0, ".*");

    }

    @Override
    public Boolean test() {
        Boolean passing = true;
        // The first index is the input node index of this component.
        // The second index is the file index for that node.
        return passing;
    }

    /**
     * Joins the two files and adds the resulting file to the component output.
     */
    @Override
    protected void runComponent() {

        // Input files
        // The first index is the input node index of this component.
        // The second index is the file index for that node.
        String file1Type = this.getAttachmentType(0);    
        File file1 = this.getAttachment(0, 0);
        String file2Type = this.getAttachmentType(1);
        File file2 = this.getAttachment(1, 0);
        
        
        if (file1 == null || file2 == null) {
            System.err.println("The Transform -> AppendCF component requires two input files.");
            return;
        }
        
        if (!(isStudentStepFile(file1Type) && isTransactionFile(file2Type)) &&
                        !(isStudentStepFile(file2Type) && isTransactionFile(file1Type))){
                System.err.println("The Transform -> AppendCF component requires one transaction and one student-step file.");
                return;
        }
        
        File studentStepFile = isStudentStepFile(file1Type)
                        ? file1 : file2;
        
        File transactionFile = isTransactionFile(file1Type)
                        ? file1 : file2;
        
        // Options
        String cfColumnName = this.getOptionAsString("cfColumnName");
        String cfAppendMode = this.getOptionAsString("cfAppendMode");
        String appendAllCFString = this.getOptionAsString("appendAllCF");
        
        logger.debug("studentStepFile: " + studentStepFile);
        logger.debug("transactionFile:" + transactionFile);
        logger.debug("cfColumnName:" + cfColumnName);
        logger.debug("cfAppendMode:" + cfAppendMode);
        logger.debug("appendAllCFString:" + appendAllCFString);
        
        Boolean appendAllCF = appendAllCFString.equalsIgnoreCase("true")
            ? true : false;
        
        //get all CF columns that need to be appended
        //get transaction file headers
        processTxnFileHeaders(getHeaderFromFile(transactionFile, DEFAULT_DELIMITER), appendAllCF, cfColumnName);
        String[] studentStepHeaders = getHeaderFromFile(studentStepFile, DEFAULT_DELIMITER);
        studentStepFileColLength = studentStepHeaders.length;
        processStudentStepFileHeaders(studentStepHeaders);
        
        
        //process transaction file to get all combination of CF
        processTxnFile(transactionFile, cfAppendMode);

        for (StudentStepCFObject obj : studentStepCFObjs)
                logger.debug(obj);

        File generatedFile = processStudentStepFile(studentStepFile, cfAppendMode);
        
        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        String fileLabel = "tab-delimited";

        this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileLabel);

        System.out.println(this.getOutput());
    }
    
    private boolean isStudentStepFile(String fileType) {
            if (fileType.equals(STUDENT_STEP_TYPE))
                    return true;
            else
                    return false;
    }
    
    private boolean isTransactionFile(String fileType) {
            if (fileType.equals(TRANSACTION_TYPE))
                    return true;
            else
                    return false;
    }
    
    private boolean isCFField(String columnName) {
            if (columnName.startsWith(CF_PREFIX) && columnName.endsWith(CF_SUFFIX))
                    return true;
            else
                    return false;
    }
    
    private boolean isTxnLevelField(String columnName) {
            if (columnName.startsWith(TXN_LEVEL_PREFIX) && columnName.endsWith(TXN_LEVEL_SUFFIX))
                    return true;
            else
                    return false;
    }
    
    private String trimTxnLevel(String columnName) {
            return columnName.substring(TXN_LEVEL_PREFIX.length(), columnName.length()-1);
    }
    
    /**
     * Given a File, read and return the first line.
     * @param file the File to read
     * @param field delimiter
     * @return String[] first line of the file which is hopefully the column headings
     * @throws ResourceUseException could occur while opening the file
     */
    private String[] getHeaderFromFile(File file, String delimiter) {
        String[] headers = null;
        try {
                Scanner sc = new Scanner(file);
                headers = sc.nextLine().split(delimiter, -1);
                if (headers.length == 1 && headers[0].length() == 1)
                        headers = new String[0];
                sc.close();
        } catch (FileNotFoundException fEx) {
                this.addErrorMessage(fEx.getMessage());
        }
        return headers;
    }
    
    private void processTxnFileHeaders (String[] headers, boolean appendAllCF, String cfColumnName) {
            if (!appendAllCF && cfColumnName != null && cfColumnName.length() != 0) {
                    for (int i = 0; i < headers.length; i++){
                            String thisHeader = headers[i];
                            if (thisHeader.equals(STUDENT_NAME)) {
                                    txnStudentNameIndex = i;
                            } else if (thisHeader.equals(PROBLEM_NAME)) {
                                    txnProblemNameIndex = i;
                            } else if (thisHeader.equals(STEP_NAME)) {
                                    txnStepNameIndex = i;
                            } else if (isTxnLevelField(thisHeader)) {
                                    txnLevelNames.add(trimTxnLevel(thisHeader));
                                    txnLevelIndices.add(i);
                            } else if (thisHeader.equals(PROBLEM_VIEW)) {
                                    txnProblemViewIndex = i;
                            } else if (thisHeader.equals(cfColumnName)) {
                                    cfColumnNames.add(cfColumnName);
                                    cfColumnIndices.add(i);
                            }
                    }
            } else {
                    boolean cfDone = false;
                    for (int i = 0; i < headers.length; i++){
                            String thisHeader = headers[i];
                            if (thisHeader.equals(STUDENT_NAME)) {
                                    txnStudentNameIndex = i;
                            } else if (thisHeader.equals(PROBLEM_NAME)) {
                                    txnProblemNameIndex = i;
                            } else if (thisHeader.equals(STEP_NAME)) {
                                    txnStepNameIndex = i;
                            } else if (isTxnLevelField(thisHeader)) {
                                    txnLevelNames.add(trimTxnLevel(thisHeader));
                                    txnLevelIndices.add(i);
                            } else if (thisHeader.equals(PROBLEM_VIEW)) {
                                    this.txnProblemViewIndex = i;
                            } else if (isCFField(thisHeader)) {
                                    if (appendAllCF || !cfDone) {
                                            cfColumnNames.add(thisHeader);
                                            cfColumnIndices.add(i);
                                    }
                                    cfDone = true;
                            }      
                    }
            }
    }
    
    private void processStudentStepFileHeaders (String[] headers) {
            for (int i = 0; i < headers.length; i++){
                    String thisHeader = headers[i];
                    if (thisHeader.equals(STUDENT_NAME)) {
                            studentStepStudentNameIndex = i;
                    } else if (thisHeader.equals(PROBLEM_NAME)) {
                            studentStepProblemNameIndex = i;
                    } else if (thisHeader.equals(STEP_NAME)) {
                            studentStepStepNameIndex = i;
                    } else if (thisHeader.equals(STUDENT_STEP_PROBLEM_HIERARCHY)) {
                            studentStepProblemHierarchyIndex = i;
                    } else if (thisHeader.equals(PROBLEM_VIEW)) {
                            this.studentStepProblemViewIndex = i;
                    }      
            }
    }
    
    private void processTxnFile(File txnFile, String cfAppendMode) {
            try (BufferedReader bReader = new BufferedReader(new FileReader(txnFile));) {
                    String line = bReader.readLine();
                    int lineCnt = 0;
                    while (line != null) {
                            if (lineCnt == 0) {
                                    line = bReader.readLine();
                                    lineCnt++;
                                    continue;
                            }
                            String row[] = line.split(DEFAULT_DELIMITER, -1);
                            String student = row[txnStudentNameIndex];
                            String levelsProblem = "";
                            for (int levelInd = 0; levelInd < txnLevelIndices.size(); levelInd++) {
                                   //append to level column names
                                    String levelVal = row[txnLevelIndices.get(levelInd)];
                                    if (levelVal != null && !levelVal.trim().equals(""))
                                            levelsProblem += txnLevelNames.get(levelInd) + " " + row[txnLevelIndices.get(levelInd)] + ", ";
                            }
                            levelsProblem += row[txnProblemNameIndex];
                            //get lower case ad delete quotes
                            levelsProblem = levelsProblem.replace("\"", "").toLowerCase();

                            String step = "";
                            if (cfAppendMode.equals(APPEND_BY_STEP) || cfAppendMode.equals(APPEND_BY_STEP_AND_PROBLEM_VIEW))
                                    step = row[txnStepNameIndex];
                            String problemView = "";
                            if (cfAppendMode.equals(APPEND_BY_STEP_AND_PROBLEM_VIEW))
                                    problemView = row[txnProblemViewIndex];

                            StudentStepCFObject thisStudentStepCFObject = new StudentStepCFObject(student,
                                                                                                    levelsProblem,
                                                                                                    step,
                                                                                                    problemView);
                            boolean found = false;
                            for (StudentStepCFObject thisObj : studentStepCFObjs) {
                                    if (thisObj.equals(thisStudentStepCFObject)) {
                                            thisStudentStepCFObject = thisObj;
                                            found = true;
                                            break;
                                    }
                            }
                            if (!found) {
                                    studentStepCFObjs.add(thisStudentStepCFObject);
                            }
                            
                            //add/set the CF values
                            for (int i = 0; i < cfColumnIndices.size(); i++) {
                                    int thisCFInd = cfColumnIndices.get(i);
                                    thisStudentStepCFObject.setCFValue(row[thisCFInd], i);
                            }
                            line = bReader.readLine();
                            lineCnt++;
                    }
                    bReader.close();

            } catch (Exception e) {
                    this.addErrorMessage(e.getMessage());
            }
    }
    
    private File processStudentStepFile (File studentStepFile, String cfAppendMode) {
            // Output file
            File generatedFile = this.createFile("AppendCF", ".txt");
            BufferedWriter bw = null;
            try {
                    FileWriter fstream = new FileWriter(generatedFile);
                    bw = new BufferedWriter(fstream);
                    generatedFile.createNewFile();
                    int lineCnt = 0;
                    try (BufferedReader bReader = new BufferedReader(new FileReader(studentStepFile));) {
                            String line = bReader.readLine();
                            String studentStepHeaders = null;
                            if (line != null) {
                                    // write header
                                    studentStepHeaders = line.replaceAll("[\r\n]", "") + "\t";
                                    for (int i = 0; i < cfColumnNames.size(); i++) {
                                            String thisCFColName = cfColumnNames.get(i);
                                            studentStepHeaders += thisCFColName;
                                            if (i < cfColumnNames.size()-1)
                                                    studentStepHeaders += "\t";
                                    }
                                    bw.append(studentStepHeaders + "\n");
                                    line = bReader.readLine();
                            }
                            lineCnt++;
                            while (line != null) {
                                line = line.trim();
                                String row[] = line.split(DEFAULT_DELIMITER, -1);
                                String studentStepRow = line.replaceAll("[\r\n]", "");
                                //when a row is missing last column(s), fill with tab
                                logger.debug("row.length: " + row.length);
                                logger.debug("studentStepFileColLength: " + studentStepFileColLength);
                                if (row.length < studentStepFileColLength) {
                                        for (int i = 0; i < (studentStepFileColLength - row.length); i++ ) {
                                                studentStepRow += "\t";
                                        }
                                }
                                studentStepRow += "\t";
                                String student = row[studentStepStudentNameIndex];
                                String levelsProblem = row[this.studentStepProblemHierarchyIndex] + ", " + row[studentStepProblemNameIndex];
                                levelsProblem = levelsProblem.replace("\"", "").toLowerCase();
                                
                                String step = "";
                                if (cfAppendMode.equals(APPEND_BY_STEP) || cfAppendMode.equals(APPEND_BY_STEP_AND_PROBLEM_VIEW))
                                        step = row[studentStepStepNameIndex];
                                String problemView = "";
                                if (cfAppendMode.equals(APPEND_BY_STEP_AND_PROBLEM_VIEW))
                                        problemView = row[studentStepProblemViewIndex];
                                
                                StudentStepCFObject thisStudentStepCFObject = new StudentStepCFObject(student,
                                                                                                        levelsProblem,
                                                                                                        step,
                                                                                                        problemView);
                                boolean found = false;
                                for (StudentStepCFObject thisObj : studentStepCFObjs) {
                                        if (thisObj.equals(thisStudentStepCFObject)) {
                                                found = true;
                                                List<String> cfValues = thisObj.getCFValues();
                                                for (int i = 0; i < cfValues.size(); i++) {
                                                        String thisCFVal = cfValues.get(i);
                                                        studentStepRow += thisCFVal;
                                                        if (i < cfValues.size()-1)
                                                                studentStepRow += "\t";
                                                }
                                                break;
                                        }
                                }
                                //not found, add empty tab
                                if (!found) {
                                        for (int i = 0; i < cfColumnIndices.size(); i++) {
                                                if (i < cfColumnIndices.size()-1)
                                                        studentStepRow += "\t";
                                        }
                                }
                                bw.append(studentStepRow + "\n");
                                line = bReader.readLine(); 
                                lineCnt++;
                            }
                            bReader.close();
                        } catch (Exception ex) {
                                this.addErrorMessage(ex.getMessage());
                        }
                } catch (Exception e) {
                    this.addErrorMessage(e.getMessage());
                } finally {
                    try {
                        bw.flush();
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            return generatedFile;
    }

    //this class represents a unique row in student_step file: combination of student, levels, problem, step and problem view 
    public class StudentStepCFObject extends Exception {
            private String student = "";
            private String levelsProblem = "";
            private String step = "";
            private String problemView = "";
            private List<String> cfValues = new ArrayList<String>();
            
            public StudentStepCFObject() {}
            
            public StudentStepCFObject(String student,
                                            String levelsProblem,
                                            String step,
                                            String problemView) {
                    this.student = student;
                    this.levelsProblem = levelsProblem;
                    this.step = step;
                    this.problemView = problemView;
            }
            
            public String getStudent() {return student;}
            public void setStudent(String student) {this.student = student;}
            
            public String getLevelsProblem() {return levelsProblem;}
            public void setLevelsProblem(String levelsProblem) {this.levelsProblem = levelsProblem;}
            
            public String getStep() {return step;}
            public void setStep(String step) {this.step = step;}
            
            public String getProblemView() {return problemView;}
            public void setProblemView(String problemView) {this.problemView = problemView;}
            
            public List<String> getCFValues() {return cfValues;}
            public void setCFValue(String cfValue, int pos) {
                    if (pos >= cfValues.size())
                            cfValues.add(cfValue);
                    else if ((cfValues.get(pos) == null || cfValues.get(pos).equals("")) && 
                                    (cfValue != null && !cfValue.equals("")))
                            cfValues.set(pos, cfValue);
            }
            
            public String toString() {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(getClass().getName());
                    buffer.append("@");
                    buffer.append(Integer.toHexString(hashCode()));
                    buffer.append(" [");
                    buffer.append("student:" + student + "\t");
                    buffer.append("levelsProblem:" + levelsProblem + "\t");
                    buffer.append("step:" + step + "\t");
                    buffer.append("problemView:" + problemView + "\t");
                    buffer.append("cfValues:");
                    for (String cfValue : cfValues) {
                            buffer.append(cfValue + ",");
                    }
                    buffer.append("]");
                    return buffer.toString();
            }
            
            /**
             * Determines whether another object is equal to this one.
             * @param obj the object to test equality with this one
             * @return true if the items are equal, false otherwise
             */
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj instanceof StudentStepCFObject) {
                        StudentStepCFObject otherItem = (StudentStepCFObject)obj;
                        if (!student.equals(otherItem.getStudent()))
                                return false;
                        if (!levelsProblem.equals(otherItem.getLevelsProblem()))
                                return false;
                        if (!step.equals(otherItem.getStep()))
                                return false;
                        if (!problemView.equals(otherItem.getProblemView()))
                                return false;
                    return true;
                }
                return false;
            }
    }

}
