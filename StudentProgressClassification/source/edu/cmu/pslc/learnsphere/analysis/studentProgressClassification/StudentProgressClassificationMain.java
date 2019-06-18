package edu.cmu.pslc.learnsphere.analysis.studentProgressClassification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import java.util.Hashtable;
import java.util.LinkedHashMap;

import edu.cmu.pl2.item.GoalItem;
import edu.cmu.pl2.item.ResourceUseItem;
import edu.cmu.pl2.item.StudentStatusItem;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.servlet.workflows.WorkflowImportHelper;



public class StudentProgressClassificationMain extends AbstractComponent {
    private final static String GOAL_STUDENT_ID = "student_id";
    private final static String GOAL_OBJECTIVE_UNITS = "objective_units";
    private final static String GOAL_OBJECTIVE_TIME = "objective_time";
    private final static String GOAL_OBJECTIVE_PROP_CORRECT = "objective_prop_correct";
    private final static String GOAL_PROLEMS_COMPLETED = "objective_problems_completed";
    private final static String RESOURCE_USE_ANON_STUDENT_ID = "Anon Student Id";
    private final static String RESOURCE_USE_PROP_CORRECT_STEPS = "propCorrectSteps";
    private final static String RESOURCE_USE_TIME = "time";
    private final static String RESOURCE_USE_PROBLEMS = "problems";
    private final static String RESOURCE_USE_TIME_FRAME_START = "time_frame_start";
    private final static String RESOURCE_USE_TIME_FRAME_END = "time_frame_end";
    
    private final List<String> requiredResourceUseHeaders = new ArrayList<>(Arrays.asList(RESOURCE_USE_ANON_STUDENT_ID, RESOURCE_USE_PROP_CORRECT_STEPS, RESOURCE_USE_TIME, RESOURCE_USE_PROBLEMS, RESOURCE_USE_TIME_FRAME_START, RESOURCE_USE_TIME_FRAME_END)); 
    private final List<String> requiredGoalHeaders = new ArrayList<>(Arrays.asList(GOAL_STUDENT_ID, GOAL_OBJECTIVE_UNITS, GOAL_OBJECTIVE_TIME, GOAL_OBJECTIVE_PROP_CORRECT, GOAL_PROLEMS_COMPLETED)); 
    
    public static void main(String[] args) {

        StudentProgressClassificationMain tool = new StudentProgressClassificationMain();
        tool.startComponent(args);

    }

    public StudentProgressClassificationMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        this.addMetaDataFromInput("tab-delimited", 0, 0, ".*");
    }

    @Override
    protected void runComponent() {
            String s_wowFactor  = this.getOptionAsString("wow_factor");
            Integer wowFactor = null;
            try {
                    wowFactor = Integer.parseInt(s_wowFactor);
            } catch (NumberFormatException nfe) {
                    //in case the input is number with a % sign
                    s_wowFactor = s_wowFactor.substring(0, s_wowFactor.length() - 1);
                    try {
                            wowFactor = Integer.parseInt(s_wowFactor);
                    } catch (NumberFormatException nfe2) {}
            }
            
            if (wowFactor == null) {
                    //send error message
                    String errMsgForUI = "Threshhod for Wow Status is in wrong format. Use a number or number with percent.";
                    String errMsgForLog = errMsgForUI;
                    handleAbortingError (errMsgForUI, errMsgForLog);
            }
            //process files
            File resourceUseFile = this.getAttachment(0, 0);
            logger.info("resourceUseFile: " + resourceUseFile.getAbsolutePath());
            File goalFile = this.getAttachment(1, 0);
            logger.info("goalFile: " + goalFile.getAbsolutePath());
            
            //process files, acceptale delimiters are , or \t
            char delim = ',';
            String fileExt = ".csv";
            LinkedHashMap<String, Integer> resourceUseColumnHeaders = WorkflowImportHelper.getColumnHeaders(resourceUseFile, delim);
            if (resourceUseColumnHeaders.size() <= 1) {
                    delim = '\t';
                    fileExt = ".txt";
                    resourceUseColumnHeaders = WorkflowImportHelper.getColumnHeaders(resourceUseFile, delim);
                    if (resourceUseColumnHeaders.size() <= 1) {
                            //tried comma and tab for delimiter. neither works. throw error
                            //send error message
                            String errMsgForUI = "Resource use file should be either tab-delimited or csv text file.";
                            String errMsgForLog = errMsgForUI + " File: " + resourceUseFile.getAbsolutePath();
                            handleAbortingError (errMsgForUI, errMsgForLog);
                    }
            }
            char goalDelim = ',';
            LinkedHashMap<String, Integer> goalColumnHeaders = WorkflowImportHelper.getColumnHeaders(goalFile, goalDelim);
            if (goalColumnHeaders.size() <= 1) {
                    goalDelim = '\t';
                    goalColumnHeaders = WorkflowImportHelper.getColumnHeaders(goalFile, goalDelim);
                    if (goalColumnHeaders.size() <= 1) {
                            //tried , and \t for delimiter. neither works. throw error
                            //send error message
                            String errMsgForUI = "Student goal file should be either tab-delimited or csv text file.";
                            String errMsgForLog = errMsgForUI + " File: " + goalFile.getAbsolutePath();
                            handleAbortingError (errMsgForUI, errMsgForLog);
                    }
            }
            //make sure all required columns are found
            for (String goalHeader : requiredGoalHeaders) {
                    if (!goalColumnHeaders.containsKey(goalHeader)) {
                            String errMsgForUI = "Required header: " + goalHeader + " is missing from student goal file.";
                            String errMsgForLog = errMsgForUI + " File: " + goalFile.getAbsolutePath();
                            handleAbortingError (errMsgForUI, errMsgForLog);
                    }
            }
            for (String resourceUseHeader : requiredResourceUseHeaders) {
                    if (!resourceUseColumnHeaders.containsKey(resourceUseHeader)) {
                            String errMsgForUI = "Required header: " + resourceUseHeader + " is missing from resource use file.";
                            String errMsgForLog = errMsgForUI + " File: " + resourceUseFile.getAbsolutePath();
                            handleAbortingError (errMsgForUI, errMsgForLog);
                    }
            }
            //process goal. studentGoalItems has student id as key and goalItem as value
            Hashtable<String, GoalItem> studentGoalItems = new Hashtable<String, GoalItem>();
            String s_delim = Character.toString(delim);
            try (BufferedReader bReader = new BufferedReader(new FileReader(goalFile));) {
                    //skip header line
                    String line = bReader.readLine();
                    line = bReader.readLine();
                    while (line != null) {
                            String goalRow[] = line.split(s_delim, -1);
                            //GOAL_STUDENT_ID
                            String student = goalRow[goalColumnHeaders.get(GOAL_STUDENT_ID)];
                            GoalItem goalItem = new GoalItem();
                            //GOAL_OBJECTIVE_UNITS
                            goalItem.setObjectiveUnits(goalRow[goalColumnHeaders.get(GOAL_OBJECTIVE_UNITS)]);
                            //GOAL_OBJECTIVE_TIME
                            String tempVal = goalRow[goalColumnHeaders.get(GOAL_OBJECTIVE_TIME)];
                            try {
                                    goalItem.setObjectiveTime(Double.parseDouble(tempVal));
                            } catch (NumberFormatException nfe) {
                                    String errMsgForUI = "Goal file has non-number data for objective time.";
                                    String errMsgForLog = errMsgForUI + " File: " + goalFile.getAbsolutePath();
                                    handleAbortingError (errMsgForUI, errMsgForLog);
                            }
                            //GOAL_OBJECTIVE_PROP_CORRECT
                            tempVal = goalRow[goalColumnHeaders.get(GOAL_OBJECTIVE_PROP_CORRECT)];
                            try {
                                    goalItem.setObjectivePropCorrect(Double.parseDouble(tempVal));
                            } catch (NumberFormatException nfe) {
                                    String errMsgForUI = "Goal file has non-number data for objective prop correct.";
                                    String errMsgForLog = errMsgForUI + " File: " + goalFile.getAbsolutePath();
                                    handleAbortingError (errMsgForUI, errMsgForLog);
                            }
                            //GOAL_PROLEMS_COMPLETED
                            tempVal = goalRow[goalColumnHeaders.get(GOAL_PROLEMS_COMPLETED)];
                            try {
                                    goalItem.setObjectiveProblemsCompleted(Integer.parseInt(tempVal));
                            } catch (NumberFormatException nfe) {
                                    String errMsgForUI = "Goal file has non-number data for objective problem completed.";
                                    String errMsgForLog = errMsgForUI + " File: " + goalFile.getAbsolutePath();
                                    handleAbortingError (errMsgForUI, errMsgForLog);
                            }
                            studentGoalItems.put(student, goalItem);
                            line = bReader.readLine();
                    }
                    bReader.close();
            } catch (IOException ioe) {
                    String errMsgForUI = "Error reading goal file: " + goalFile.getAbsolutePath();
                    String errMsgForLog = errMsgForUI;
                    handleAbortingError (errMsgForUI, errMsgForLog);
            }
            //from output file, decide if time_frame_end is empty
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date lastDateInTimeFrameStart = null;
            //read the resource use file line by line
            try (BufferedReader bReader = new BufferedReader(new FileReader(resourceUseFile));) {
                    String line = bReader.readLine();
                    line = bReader.readLine();
                    while (line != null) {
                            String resourceUseRow[] = line.split(s_delim, -1);
                            //RESOURCE_USE_TIME_FRAME_START
                            String tempVal = resourceUseRow[resourceUseColumnHeaders.get(RESOURCE_USE_TIME_FRAME_START)];
                            if (tempVal != null && !tempVal.trim().equals("")) {
                                    try {
                                            Date thisTimeFrameStart = sdf.parse(tempVal);
                                            if (lastDateInTimeFrameStart == null)
                                                    lastDateInTimeFrameStart = thisTimeFrameStart;
                                            else if (thisTimeFrameStart.after(lastDateInTimeFrameStart))
                                                    lastDateInTimeFrameStart = thisTimeFrameStart;
                                                
                                    } catch (ParseException nfe) {
                                            String errMsgForUI = "Resource use file has non-date data for time_frame_start column. Acceptable date format is yyyy-MM-dd (e.g. 2012-09-31). Empty value is allowed.";
                                            String errMsgForLog = "Resource use file has non-date data for time_frame_start column. File: " + resourceUseFile.getAbsolutePath();
                                            handleAbortingError (errMsgForUI, errMsgForLog);
                                    }
                            }
                            line = bReader.readLine();
                    }
                    bReader.close();
            } catch (IOException ioe) {
                    String errMsgForUI = "Error reading resource use file: " + resourceUseFile.getAbsolutePath();
                    String errMsgForLog = errMsgForUI;
                    handleAbortingError (errMsgForUI, errMsgForLog);
            }
            // process output file
            File outputFile = this.createFile("StudentProgressClassification", fileExt);
            BufferedWriter bw = null;
            try {
                FileWriter fstream = new FileWriter(outputFile);
                bw = new BufferedWriter(fstream);
                outputFile.createNewFile();
                //read the resource use file line by line
                try (BufferedReader bReader = new BufferedReader(new FileReader(resourceUseFile));) {
                        //skip headers
                        String headerLine = bReader.readLine();
                        bw.append(headerLine + delim + "status\n");
                        String line = bReader.readLine();
                        while (line != null) {
                                String resourceUseRow[] = line.split(s_delim, -1);
                                //RESOURCE_USE_ANON_STUDENT_ID
                                String student = resourceUseRow[resourceUseColumnHeaders.get(RESOURCE_USE_ANON_STUDENT_ID)];
                                GoalItem goalItem = studentGoalItems.get(student);
                                if (goalItem == null) {
                                        //write to output file with empty student prgress classification
                                        bw.append(line + delim + "\n");
                                        line = bReader.readLine();
                                        continue;
                                }
                                ResourceUseItem resourceUseItem = new ResourceUseItem();
                                //RESOURCE_USE_PROP_CORRECT_STEPS
                                String tempVal = resourceUseRow[resourceUseColumnHeaders.get(RESOURCE_USE_PROP_CORRECT_STEPS)];
                                if (tempVal != null && !tempVal.trim().equals("")) {
                                        try {
                                                resourceUseItem.setPropCorrectSteps(Double.parseDouble(tempVal));
                                        } catch (NumberFormatException nfe) {
                                                String errMsgForUI = "Resource use file has non-number data for propCorrectSteps column. Empty value is allowed.";
                                                String errMsgForLog = "Resource use file has non-number data for propCorrectSteps column.  File: " + resourceUseFile.getAbsolutePath();
                                                handleAbortingError (errMsgForUI, errMsgForLog);
                                        }
                                }
                                //RESOURCE_USE_PROBLEMS
                                tempVal = resourceUseRow[resourceUseColumnHeaders.get(RESOURCE_USE_PROBLEMS)];
                                if (tempVal != null && !tempVal.trim().equals("")) {
                                        try {
                                                resourceUseItem.setProblemsCompleted(Integer.parseInt(tempVal));
                                        } catch (NumberFormatException nfe) {
                                                String errMsgForUI = "Resource use file has non-number data for problems column. Empty value is allowed.";
                                                String errMsgForLog = "Resource use file has non-number data for problems column. File: " + resourceUseFile.getAbsolutePath();
                                                handleAbortingError (errMsgForUI, errMsgForLog);
                                        }
                                }
                                //RESOURCE_USE_TIME
                                tempVal = resourceUseRow[resourceUseColumnHeaders.get(RESOURCE_USE_TIME)];
                                if (tempVal != null && !tempVal.trim().equals("")) {
                                        try {
                                                resourceUseItem.setMinutesUsed(Double.parseDouble(tempVal));
                                        } catch (NumberFormatException nfe) {
                                                String errMsgForUI = "Resource use file has non-number data for time column. Empty value is allowed.";
                                                String errMsgForLog = "Resource use file has non-number data for time column. File: " + resourceUseFile.getAbsolutePath();
                                                handleAbortingError (errMsgForUI, errMsgForLog);
                                        }
                                }
                                //RESOURCE_USE_TIME_FRAME_START
                                tempVal = resourceUseRow[resourceUseColumnHeaders.get(RESOURCE_USE_TIME_FRAME_START)];
                                if (tempVal != null && !tempVal.trim().equals("")) {
                                        try {
                                                Date thisTimeFrameStart = sdf.parse(tempVal);
                                                resourceUseItem.setTimeFrameStart(thisTimeFrameStart);
                                                if (lastDateInTimeFrameStart == null)
                                                        lastDateInTimeFrameStart = thisTimeFrameStart;
                                                else if (thisTimeFrameStart.after(lastDateInTimeFrameStart))
                                                        lastDateInTimeFrameStart = thisTimeFrameStart;
                                                    
                                        } catch (ParseException nfe) {
                                                String errMsgForUI = "Resource use file has non-date data for time_frame_start column. Acceptable date format is yyyy-MM-dd (e.g. 2012-09-31). Empty value is allowed.";
                                                String errMsgForLog = "Resource use file has non-date data for time_frame_start column. File: " + resourceUseFile.getAbsolutePath();
                                                handleAbortingError (errMsgForUI, errMsgForLog);
                                        }
                                }
                                //RESOURCE_USE_TIME_FRAME_END
                                tempVal = resourceUseRow[resourceUseColumnHeaders.get(RESOURCE_USE_TIME_FRAME_END)];
                                boolean hasEmptyTimeFrameEnd = false;
                                if (tempVal != null && !tempVal.trim().equals("")) {
                                        try {
                                                resourceUseItem.setTimeFrameEnd(sdf.parse(tempVal));
                                        } catch (NumberFormatException nfe) {
                                                String errMsgForUI = "Resource use file has non-date data for time_frame_end column. Acceptable date format is yyyy-MM-dd (e.g. 2012-09-31). Empty value is allowed.";
                                                String errMsgForLog = "Resource use file has non-date data for time_frame_end column. File: " + resourceUseFile.getAbsolutePath();
                                                handleAbortingError (errMsgForUI, errMsgForLog);
                                        }
                                } else {
                                        resourceUseItem.setTimeFrameEnd(lastDateInTimeFrameStart);
                                        hasEmptyTimeFrameEnd = true;
                                }
                                        
                                String status = StudentStatusItem.computeProgressStatus(resourceUseItem, goalItem, (double)wowFactor/100);
                                
                                if (status == null)
                                        status = "";
                                
                                //write to output file with empty student progress classification
                                if (!hasEmptyTimeFrameEnd)
                                        bw.append(line + delim + status + "\n");
                                else {
                                        bw.append(line + sdf.format(lastDateInTimeFrameStart) + delim + status + "\n");
                                }
                                line = bReader.readLine();
                        }
                        bReader.close();
                } catch (IOException ioe) {
                        String errMsgForUI = "Error reading resource use file: " + resourceUseFile.getAbsolutePath();
                        String errMsgForLog = errMsgForUI;
                        handleAbortingError (errMsgForUI, errMsgForLog);
                }
                
            } catch (Exception e) {
                    String errMsgForUI = "Error writing output file: " + outputFile.getAbsolutePath() + ". Error: " + e.getMessage();
                    String errMsgForLog = "Error writing output file: " + outputFile.getAbsolutePath() + ". Error: " + e.getStackTrace();
                    handleAbortingError (errMsgForUI, errMsgForLog);
            } finally {
                    try {
                            bw.flush();
                            bw.close();
                    } catch (IOException e) {
                            String errMsgForUI = "Error closing output file: " + outputFile.getAbsolutePath() + ". Error: " + e.getMessage();
                            String errMsgForLog = "Error closing output file: " + outputFile.getAbsolutePath() + ". Error: " + e.getStackTrace();
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            e.printStackTrace();
                    }
            }
            
            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileLabel = "resource-use";
            this.addOutputFile(outputFile, nodeIndex, fileIndex, fileLabel);
            System.out.println(getOutput());
    }
    
    private void handleAbortingError (String errMsgForUI, String errMsgForLog) {
            addErrorMessage(errMsgForUI);
            logger.info("StudentProgressClassification aborted: " + errMsgForLog );
            System.out.println(getOutput());
            return; 
    }
    
    

}
