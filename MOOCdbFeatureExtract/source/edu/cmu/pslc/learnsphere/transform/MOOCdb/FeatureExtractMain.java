package edu.cmu.pslc.learnsphere.transform.MOOCdb;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;

import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.DaoFactory;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.FeatureExtractionDao;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.MOOCdbDao;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.hibernate.HibernateDaoFactory;
import edu.cmu.pslc.learnsphere.analysis.moocdb.item.MOOCdbItem;
import edu.cmu.pslc.learnsphere.analysis.moocdb.item.FeatureExtractionItem;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class FeatureExtractMain extends AbstractComponent {

    /** Component option (dataset). */
    String courseName = null;
    
    private int MAX_FEATURE_TO_EXTRACT = 15;

    public static void main(String[] args) {
            FeatureExtractMain tool = new FeatureExtractMain();
            tool.startComponent(args);
    }

    public FeatureExtractMain() {
            super();
    }

    @Override
    protected void runComponent() {
            // Dao-enabled components require an applicationContext.xml in the component directory,

            String appContextPath = this.getApplicationContextPath();
            logger.info("appContextPath: " + appContextPath);

            // Do not follow symbolic links so we can prevent unwanted directory traversals if someone
            // does manage to create a symlink to somewhere dangerous (like /datashop/deploy/)
            if (Files.exists(Paths.get(appContextPath), LinkOption.NOFOLLOW_LINKS)) {
                /** Initialize the Spring Framework application context. */
                SpringContext.getApplicationContext(appContextPath);
            }
            //get MOOCdb name from MOOCdb file
            String MOOCdbName = null;
            File MOOCdbFile = getAttachment(0, 0);
            logger.info("MOOCdbFile: " + MOOCdbFile);
            //File MOOCdbFeaturesFile = getAttachment(1, 0);
            if (MOOCdbFile != null) {
                            MOOCdbName = getMOOCdbNameFromFile(MOOCdbFile);
            }
            if (MOOCdbName == null) {
                    //send error message
                    String err = "MOOCdb name is not provided.";
                    addErrorMessage(err);
                    logger.info("MOOCdbFeatureExtraction aborted: " + err);
                    System.err.println(err);
                    return;
            }
            //make sure this MOOCdb exists
            MOOCdbItem currMOOCdbItem = findMOOCdb(MOOCdbName);
            logger.info("MOOCdbItem found: " + currMOOCdbItem);
            if (currMOOCdbItem == null) {
                    //send error message
                    String err = "MOOCdb " + MOOCdbName + " doesn't exist. ";
                    addErrorMessage(err);
                    logger.info("MOOCdbFeatureExtraction aborted: " + err);
                    System.err.println(err);
                    return;  
            }
            
            String progress = currMOOCdbItem.getCurrentProgress();
            String username = currMOOCdbItem.getUsername();
            String password = currMOOCdbItem.getPassword();
            Date endTimestamp = currMOOCdbItem.getEndTimestamp();
            Date earliestSubmissionDate = currMOOCdbItem.getEarliestSubmissionTimestamp();
            if (earliestSubmissionDate == null)
                    earliestSubmissionDate = this.getEarliestSubmissionTime(MOOCdbName);
            //process startDate
            String opStartDate = getOptionAsString("startDate");
            Date startDate = null;
            //correct format is yyyy-mm-dd
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            if(opStartDate != null && !opStartDate.equals("") && !opStartDate.equalsIgnoreCase("yyyy-mm-dd")) {
                    try {
                            startDate = format.parse(opStartDate);
                    } catch (ParseException ex) {
                            String errMsg = "Date format for startDate is wrong. startDate: " + opStartDate;
                            addErrorMessage(errMsg);
                            logger.info("MOOCdbFeatureExtraction aborted: " + errMsg);
                            System.err.println(errMsg);
                            return;
                    }
            } else {
                    startDate = earliestSubmissionDate;
            }
            
            
            if (progress != null && !progress.equals("") && !progress.equals(MOOCdbItem.PROGRESS_DONE)) {
                    String errMsg = "MOOCdb " + MOOCdbName + " is currently undergoing " + progress + " by another process.";
                    addErrorMessage(errMsg + " You can either wait till it's done or start process with a new custom MOOCdb name.");
                    logger.info("MOOCdbFeatureExtraction aborted: " + errMsg);
                    System.err.println(errMsg + " You can either wait till it's done or start process with a new custom MOOCdb name.");
                    return;
                    
            } else if (progress != null && progress.equals(MOOCdbItem.PROGRESS_DONE) && endTimestamp != null) {
                    logger.info("Feature extract for MOOCdb: " + MOOCdbName);
            } else {
                    String errMsg = "MOOCdb " + MOOCdbName + " isn't ready for feature extraction due to unknown error.";
                    addErrorMessage(errMsg);
                    logger.info("MOOCdbFeatureExtraction aborted: " + errMsg + " currMOOCdbItem: " + currMOOCdbItem);
                    System.err.println(errMsg);
                    return;
            }
            //process feature to extract
            List<String> featuresToExtractList = this.getMultiOptionAsString("featuresToExtract");
            if (featuresToExtractList.size() > MAX_FEATURE_TO_EXTRACT) {
                    String errMsg = "Too many features are selected. Extract less than " + MAX_FEATURE_TO_EXTRACT + " features at a time. ";
                    addErrorMessage(errMsg);
                    logger.info("MOOCdbFeatureExtraction aborted: " + errMsg + " currMOOCdbItem: " + currMOOCdbItem);
                    System.err.println(errMsg);
                    return;
            }
            String featuresToExtract = "";
            Map<Integer, String> availFeatures = getAllFeatures(MOOCdbName);
            String[] values = null;
            if (featuresToExtractList != null && featuresToExtractList.size() != 0) 
                    values = (String[]) featuresToExtractList.toArray(new String[0]);
            if (values == null || values.length == 0) {
                    for (int key : availFeatures.keySet())
                            featuresToExtract += key + ",";
            } else {
                    for (String value : values) {
                            if (value.matches("[0-9]+")) {
                                    //check this feature id is valid
                                    if (!availFeatures.containsKey(Integer.parseInt(value))) {
                                            //send out error message
                                            String err = "Feature: " + value + " is not available. ";
                                            addErrorMessage(err);
                                            logger.info(err);
                                            System.err.println(err);
                                            return;
                                    } else
                                            featuresToExtract += value + ","; 
                            } else {
                                    if (!availFeatures.containsValue(value)) {
                                          //send out error message
                                            String err = "Feature: " + value + " is not available. ";
                                            addErrorMessage(err);
                                            logger.info(err);
                                            System.err.println(err);
                                            return;
                                    } else {
                                            for (Entry<Integer, String> entry : availFeatures.entrySet()) {
                                                    if (entry.getValue().equals(value)) {
                                                            featuresToExtract += entry.getKey() + ","; 
                                                    }
                                            }
                                    } 
                            }
                    }
            }
            if (featuresToExtract.equals("")) {
                    for (int key : availFeatures.keySet())
                            featuresToExtract += key + ",";
            }
            //delete the last comma
            if (featuresToExtract.lastIndexOf(",") == featuresToExtract.length()-1) {
                    featuresToExtract = featuresToExtract.substring(0, featuresToExtract.length()-1);
            }
            //order the list
            List<String> items = Arrays.asList(featuresToExtract.split("\\s*,\\s*"));
            List<Integer> intList = new ArrayList<Integer>();
            for(String s : items) 
                    intList.add(Integer.valueOf(s));
            Collections.sort(intList);
            featuresToExtract = "";
            for (int key : intList)
                    featuresToExtract += key + ",";
            //delete the last comma
            if (featuresToExtract.lastIndexOf(",") == featuresToExtract.length()-1) {
                    featuresToExtract = featuresToExtract.substring(0, featuresToExtract.length()-1);
            }
            logger.info("Feature to extract: " + featuresToExtract);
            //process num_of_week
            String numberWeeks = getOptionAsString("numberWeeks");
            if (numberWeeks == null || numberWeeks.equals(""))
                    numberWeeks = "10";
            if (!numberWeeks.matches("[0-9]+")) {
                    //send out error message
                    addErrorMessage("Invalid number of week: " + numberWeeks + ". ");
                    logger.info("MOOCdbFeatureExtraction aborted: Invalid number of week: " + numberWeeks + ". ");
                    System.err.println("Invalid number of week: " + numberWeeks + ". ");
                    return;
            }
            int iNumOfWeek = Integer.parseInt(numberWeeks);
            logger.info("Number of weeks for extraction: " + iNumOfWeek);
            
            //find out if someone has already done feature extraction for this specification
            FeatureExtractionItem featureExtractionItem = findAFeatureExtraction(MOOCdbName, startDate, iNumOfWeek, featuresToExtract);
            boolean newFeatureExtraction = false;
            if (featureExtractionItem != null) {
                    logger.info("Found featureExtractionItem: " + featureExtractionItem);
                    if (featureExtractionItem.getEndTimestamp() != null) {
                            this.componentOptions.addContent(0, new Element("runExtraction").setText("false"));
                    } else {
                            String errMsg = "MOOCdb " + MOOCdbName + " is currently undergoing feature extraction by another process.";
                            this.addErrorMessage(errMsg + " You have to wait till it's done.");
                            logger.info("MOOCdbFeatureExtraction aborted: " + errMsg + " Features are: featuresToExtract: " + featuresToExtract + 
                                            "; numberOfWeeks: " + numberWeeks + "; startDate: " + opStartDate);
                            
                            System.err.println(errMsg + " Please wait till it's done.");
                            return;
                    }
            } else {
                    newFeatureExtraction = true;
                    featureExtractionItem = new FeatureExtractionItem();
                    //featureExtractionItem.setCreatedBy();
                    featureExtractionItem.setStartTimestamp(new Date());
                    featureExtractionItem.setStartDate(startDate);
                    featureExtractionItem.setFeaturesList(featuresToExtract);
                    featureExtractionItem.setNumOfWeek(Integer.parseInt(numberWeeks));
                    try {
                            saveOrUpdateFeatureExtractionItem(MOOCdbName, featureExtractionItem);
                    } catch (Exception ex) {
                            String errMsg = "Error found while saving featureExtractionItem: " + featureExtractionItem + " for MOOCdb: " + MOOCdbName + "; exception: " + ex.getMessage();
                            this.addErrorMessage(errMsg);
                            logger.info("MOOCdbFeatureExtraction aborted: " + errMsg);
                            System.err.println(errMsg);
                            return;
                    }
                    logger.info("Saved new featureExtractionItem: " + featureExtractionItem);
                    this.componentOptions.addContent(0, new Element("runExtraction").setText("true"));
            }    
            Map<String, String> dbConfig = HibernateDaoFactory.DEFAULT.getAnalysisDatabaseHostPort();
            this.componentOptions.addContent(0, new Element("MOOCdbName").setText(MOOCdbName));
            this.componentOptions.addContent(0, new Element("un").setText(username));
            this.componentOptions.addContent(0, new Element("p").setText(password));
            //this.componentOptions.addContent(0, new Element("dbHost").setText(dbConfig.get("host")));
            //this.componentOptions.addContent(0, new Element("dbPort").setText(dbConfig.get("port")));
            this.componentOptions.addContent(0, new Element("earliestSubmissionDate").setText(format.format(earliestSubmissionDate)));
            this.componentOptions.addContent(0, new Element("featureExtractionId").setText("" + featureExtractionItem.getId()));
            this.componentOptions.addContent(0, new Element("startDateWF").setText(format.format(startDate)));
            this.componentOptions.addContent(0, new Element("featuresToExtractWF").setText(featuresToExtract));
            this.componentOptions.addContent(0, new Element("numberWeeksWF").setText(numberWeeks));
            this.componentOptions.addContent(0, new Element("exportFormatWF").setText("tall"));
            
            // Run the program and return its stdout to a file.
            File outputDirectory = this.runExternalMultipleFileOuput();
            
            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileLabel = "longitudinal-features";
            String featureFilePath = outputDirectory.getAbsolutePath() + "/moocdb_features.txt";
            File longitudinalFile = new File(featureFilePath);
            
            this.addOutputFile(longitudinalFile, nodeIndex, fileIndex, fileLabel);

            nodeIndex = 1;
            fileIndex = 0;
            fileLabel = "moocdb-feature-description";
            File featuresFile = new File(outputDirectory.getAbsolutePath() + "/feature_descriptions.txt");
            this.addOutputFile(featuresFile, nodeIndex, fileIndex, fileLabel);

            // Send the component output bakc to the workflow.
            System.out.println(this.getOutput());
            //update the featureExtractionItem if new feature extraction
            if (newFeatureExtraction) {
                    featureExtractionItem.setEndTimestamp(new Date());
                    try {
                            saveOrUpdateFeatureExtractionItem(MOOCdbName, featureExtractionItem);
                            logger.info("Updated featureExtractionItem: " + featureExtractionItem);
                    } catch (Exception ex) {
                            String errMsg = "Error found while updating featureExtractionItem after feature extraction: " + featureExtractionItem + " for MOOCdb: " + MOOCdbName + "; exception: " + ex.getMessage();
                            this.addErrorMessage(errMsg);
                            logger.info(errMsg);
                            System.err.println(errMsg);
                            return;
                    }
            }        
    }

    //check if a MOOCdb already exists
    private MOOCdbItem findMOOCdb(String MOOCdbName) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            MOOCdbItem item = dbDao.getMOOCdbByName(MOOCdbName);
            return item;
    }
    
    private Map<Integer, String> getAllFeatures (String MOOCdbName) {
            FeatureExtractionDao feDao = DaoFactory.DEFAULT.getFeatureExtractionDao();
            return feDao.getAllFeatures(MOOCdbName);
    }
    
    private FeatureExtractionItem findAFeatureExtraction(String MOOCdbName,  Date startDate, 
                                    int numberWeeks, String featuresToExtract) {
            FeatureExtractionDao feDao = DaoFactory.DEFAULT.getFeatureExtractionDao();
            return feDao.findAFeatureExtraction(MOOCdbName, startDate, numberWeeks, featuresToExtract);
    }
    
    //save or update a featureExtractionItem
    private void saveOrUpdateFeatureExtractionItem(String MOOCdbName, FeatureExtractionItem featureExtractionItem) 
                    throws Exception {
            FeatureExtractionDao feDao = DaoFactory.DEFAULT.getFeatureExtractionDao();
            feDao.saveOrUpdateFeatureExtractionItem(MOOCdbName, featureExtractionItem);
    }
    
    private String getMOOCdbNameFromFile(File dbPointerFile) {
            String fileContent = IOUtil.readString(dbPointerFile.getAbsolutePath());
            //the first property name currently is MOOCdbName 
            if (fileContent.trim().indexOf(MOOCdbItem.MOOCdb_PROPERTY_NAME) == 0) {
                    String[] tokens = fileContent.trim().split("\\=");
                    if (tokens[0].trim().equals(MOOCdbItem.MOOCdb_PROPERTY_NAME)){
                            return tokens[1].trim();
                    } else
                            return null;
            } else
                    return null;
    }
    
    private Date getEarliestSubmissionTime (String MOOCdbName) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            return dbDao.getEarliestSubmissionTime(MOOCdbName);
    }
}
