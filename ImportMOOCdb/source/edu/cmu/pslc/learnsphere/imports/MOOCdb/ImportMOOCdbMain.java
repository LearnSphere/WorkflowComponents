package edu.cmu.pslc.learnsphere.imports.MOOCdb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jdom.Element;
import org.apache.commons.io.FileUtils;

import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.FeatureExtractionDao;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.MOOCdbDao;
import edu.cmu.pslc.learnsphere.analysis.moocdb.item.MOOCdbItem;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.DaoFactory;
import edu.cmu.pslc.datashop.problemcontent.oli.CommonXml;

public class ImportMOOCdbMain extends AbstractComponent {
    private static String MOOCDB_CLEAN = "moocdb_clean";
    private static String MOOCDB_CORE = "moocdb_core";
    

    public static void main(String[] args) {
            ImportMOOCdbMain tool = new ImportMOOCdbMain();
            tool.startComponent(args);
    }

    public ImportMOOCdbMain() {
            super();
    }
    
    protected void processOptions() {
            logger.info("Processing Options");
            // If you want to add all headers from a previous component, try one of these:
            //this.addMetaDataFromInput("transaction", 0, 0, ".*");
            //this.addMetaDataFromInput("user-session-map", 1, 0, ".*");
            
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
            File MOOCdbFile = getAttachment(0, 0);
            String MOOCdbName = null;
            String MOOCdbFileName = MOOCdbFile.getName();
            String MOOCdbFilePath = MOOCdbFile.getAbsolutePath();
            logger.info("MOOCdbFile: " + MOOCdbFilePath);
            String MOOCdbMd5HashValue = CommonXml.md5Hash(MOOCdbFilePath);
            int SQLfileInd = MOOCdbFileName.indexOf(".sql");
            if (SQLfileInd == -1) {
                    MOOCdbName = getMOOCdbNameFromFile(MOOCdbFile);
                    logger.info("MOOCdb name parsed from input file: " + MOOCdbName);
                    if (MOOCdbName == null){
                            //send error message
                            String err = "MOOCdb file has incorrect format: " + MOOCdbFilePath;
                            addErrorMessage(err);
                            logger.info("MOOCdbImport aborted: " + err);
                            System.err.println(err);
                            return;
                    } else {
                            if (!databaseExist(MOOCdbName) || !isMOOCdb(MOOCdbName)) {
                                    //send error message
                                    String err = "MOOCdb doesn't exist: " + MOOCdbName;
                                    addErrorMessage(err);
                                    logger.info("MOOCdbImport aborted: " + err);
                                    System.err.println(err);
                                    return;
                            } else {
                                    //output MOOCdb file and feature name
                                    outputFilesWithExistingDbInfo(MOOCdbName);
                                    return;
                            }
                    }
            } else {//file is a database backup
                    //use backup file name as MOOCdb name
                    //***
                    //database backup command example: mysqldump --user=datashop --password=datashop moocdb_test > moocdb_test_backup.sql
                    //****
                    MOOCdbName = MOOCdbFileName.substring(0, SQLfileInd).replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\?\\*\\+\\.\\>]", "");
                    //just to be sure
                    MOOCdbName = MOOCdbName.replaceAll("\\W", "");
                    MOOCdbName = MOOCdbName.toLowerCase();
                    logger.info("escaped MOOCdb name: " + MOOCdbName);
                    if (MOOCdbName.equals(MOOCDB_CLEAN) || MOOCdbName.equals(MOOCDB_CORE)){
                            //send error message
                            String errMsg = "Wrong MOOCdb name: " + MOOCDB_CLEAN + " or " + MOOCDB_CORE + " is not allowed.";
                            addErrorMessage(errMsg);
                            logger.info("MOOCdbImport aborted: " + errMsg);
                            System.err.println(errMsg);
                            return;
                    }
                    //moocdb exists
                    if (databaseExist(MOOCdbName)) {
                            //is a MOOCdb
                            if (isMOOCdb(MOOCdbName)) {
                                   //if exist in moocdbs table
                                   MOOCdbItem currMOOCdbItem = findMOOCdb(MOOCdbName);
                                   if (currMOOCdbItem != null) {
                                           String progress = currMOOCdbItem.getCurrentProgress();
                                           if (progress != null && !progress.equals("") && !progress.equals(MOOCdbItem.PROGRESS_DONE)) {
                                                   String errMsg = "A MOOCdb " + MOOCdbName + " is currently undergoing recovery by another process.";
                                                   addErrorMessage(errMsg + " You can either wait till it's done or rename your MOOCdb backup file and start a new import process.");
                                                   logger.info("MOOCdbImport aborted: " + errMsg);
                                                   System.err.println(errMsg);
                                                   return;
                                           } else {
                                                   String curItemMOOCdbMd5HashValue = currMOOCdbItem.getMoocdbFileMd5HashValue();
                                                   if (MOOCdbMd5HashValue.equals(curItemMOOCdbMd5HashValue)) {
                                                           //output MOOCdb file and feature name
                                                           outputFilesWithExistingDbInfo(MOOCdbName);
                                                           return;
                                                   } else { //md5Hash is not the same
                                                           String errMsg = "A MOOCdb with the same name already exists but SQL backup file is different. MOOCdb name: " + MOOCdbName + ". ";
                                                           logger.info("MOOCdbImport aborted: " + errMsg);
                                                           errMsg += " You can rename your MOOCdb backup file and start a new import process.";
                                                           addErrorMessage(errMsg);
                                                           System.err.println(errMsg);
                                                           return;
                                                   }
                                           }
                                   } else { //not found in moocdbs table
                                           String errMsg = "A MOOCdb with the same name already exists. MOOCdb name: " + MOOCdbName + ". ";
                                           logger.info("MOOCdbImport aborted: " + errMsg);
                                           errMsg += " Find the existing MOOCdb from Datashop; or rename your MOOCdb backup file and start a new import process.";
                                           addErrorMessage(errMsg);
                                           System.err.println(errMsg);
                                           return;
                                   }
                                   
                            } else {
                                  //db with the same name but it's not a moocdb
                                    String errMsg = "A database with the same name already exists. Database name: " + MOOCdbName + ". ";
                                    logger.info("MOOCdbImport aborted: " + errMsg);
                                    errMsg += " Rename your MOOCdb backup file and start a new import process.";
                                    addErrorMessage(errMsg);
                                    System.err.println(errMsg);
                                    return;
                            }
                    } 
            }
            //possible that moocdb doesn't exist yet but moocdbs table has an record, very unlikely though
            MOOCdbItem currMOOCdbItem = findMOOCdb(MOOCdbName);
            if (currMOOCdbItem != null) {
                    String errMsg = "Inconsistency found between real MOOCdb database and a record in moocdbs table; MOOCdb name: " + MOOCdbName;
                    addErrorMessage(errMsg + " Rename your MOOCdb backup file and start a new import process.");
                    logger.info("MOOCdbImport aborted: " + errMsg);
                    System.err.println(errMsg);
                    return;
            }
            //start a new moocdb
            //save a record in moocdb_courses table
            
            MOOCdbItem moocdbItem = new MOOCdbItem();
            moocdbItem.setMOOCdbName(MOOCdbName);
            moocdbItem.setCurrentProgress(MOOCdbItem.PROGRESS_CREATE_DBS);
            if (this.getUserId() != null)
                    moocdbItem.setCreatedBy(this.getUserId());
            String username = getSaltString();
            while (userExist(username))
                    username = getSaltString();
            moocdbItem.setUsername(username);
            moocdbItem.setPassword(getSaltString());
            moocdbItem.setMoocdbFile(MOOCdbFilePath);
            moocdbItem.setMoocdbFileMd5HashValue(MOOCdbMd5HashValue);
            moocdbItem.setStartTimestamp(new Date());
            saveOrUpdateMOOCdb(moocdbItem);
            logger.info("Saved MOOCdbItem: " + moocdbItem);
            //create moocdb databases
            try {
                    if (userExist(moocdbItem.getUsername())) {
                            deleteMOOCDbItem(moocdbItem);
                            String errMsg = "Error found when creating user for accessing new DB: " + moocdbItem.getUsername();
                            addErrorMessage(errMsg);
                            logger.info("MOOCdbImport aborted: " + errMsg);
                            System.err.println(errMsg);
                            return; 
                    }
                    createDBUser(moocdbItem.getUsername(), moocdbItem.getPassword());
                    createDB(MOOCdbName);
                    addUserToDB(MOOCdbName, moocdbItem.getUsername(), MOOCdbItem.DB_READ_WRITE);
                    addUserToDB(MOOCDB_CORE, moocdbItem.getUsername(), MOOCdbItem.DB_READ);
                    addUserToDB(MOOCDB_CLEAN, moocdbItem.getUsername(), MOOCdbItem.DB_READ);
                    restoreMOOCdb(MOOCdbName, MOOCdbFilePath, moocdbItem.getUsername(), moocdbItem.getPassword());
                    //make sure the resotre db is a MOOCdb
                    if (!isMOOCdb(MOOCdbName)) {
                            deleteMOOCDbItem(moocdbItem);
                            deleteUser(moocdbItem.getUsername());
                            deleteMOOCdb(MOOCdbName);
                            String errMsg = "MOOCdb backup file is not a MOOCdb. MOOCdb file: " +  MOOCdbFilePath;
                            addErrorMessage(errMsg);
                            logger.info("MOOCdbImport aborted: " + errMsg);
                            System.err.println(errMsg);
                            return;
                    }
            } catch (Exception ex) {
                    //send error message
                    this.deleteMOOCDbItem(moocdbItem);
                    try {
                            deleteUser(moocdbItem.getUsername());
                            deleteMOOCdb(MOOCdbName);
                    } catch (Exception innerex) {
                            String errMsg = "Found error deleting database: " +  MOOCdbName +
                                            "; username: " + moocdbItem.getUsername() +
                                            "; Exception: " + innerex.getMessage();
                            addErrorMessage(errMsg);
                            logger.info("MOOCdbImport aborted: " + errMsg);
                            System.err.println(errMsg);
                            return;
                    }
                    String errMsg = "Found error restoring MOOCdb backup file: " + MOOCdbFilePath + "; Exception: " + ex.getMessage();
                    addErrorMessage(errMsg);
                    logger.info("MOOCdbImport aborted: " + errMsg);
                    System.err.println(errMsg);
                    return;
            }
            logger.info("Created MOOCdb: " + MOOCdbName);
            logger.info("Created MOOCdb user: " + moocdbItem.getUsername());
            
            moocdbItem.setCurrentProgress(MOOCdbItem.PROGRESS_DONE);
            moocdbItem.setEndTimestamp(new Date());
            moocdbItem.setLastProgress(MOOCdbItem.PROGRESS_RESTORE_MOOCDB);
            moocdbItem.setLastProgressEndTimestamp(new Date());
            logger.info("Completed restoring MOOCdb: " + MOOCdbName);
            saveOrUpdateMOOCdb(moocdbItem);
            
            //output MOOCdb file and feature name
            outputFilesWithExistingDbInfo(MOOCdbName);
            return;
    }

    
    private String getAllFeatures (String MOOCdbName) {
            FeatureExtractionDao feDao = DaoFactory.DEFAULT.getFeatureExtractionDao();
            Map<Integer, String> featureMap = feDao.getAllFeatures(MOOCdbName);
            Iterator it = featureMap.entrySet().iterator();
            String features = "";
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                features += pair.getValue() + "\t";
            }
            return features.trim();
    }
    

    private boolean isMOOCdb(String dbName) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            return dbDao.isMOOCdb(dbName);
    }
    
    private boolean userExist(String username) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            return dbDao.userExist(username);
    }
    
    private void deleteMOOCDbItem(MOOCdbItem dbItem) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.delete(dbItem);
    }
    
    private void deleteMOOCdb(String MOOCdbName) throws SQLException, Exception {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.deleteMOOCdb(MOOCdbName);
    }
    
    private void deleteUser(String username) throws SQLException {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.deleteUser(username);
    }
    
    private void createDBUser(String username, String passwrod) throws SQLException {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.createDBUser(username, passwrod);
    }
    
    private void createDB(String dbName) throws SQLException {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.createDB(dbName);
    }
    
    private void addUserToDB(String dbName, String username, String accessRights) throws SQLException {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.addUserToDB(dbName, username, accessRights);
    }
    
    //restore MOOCdb
    private void restoreMOOCdb(String DbName, String MOOCdbFileName, String username, String password) 
                    throws SQLException, Exception {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.restoreMOOCdb(DbName, MOOCdbFileName, username, password);
    }
    
    private String getMOOCdbNameFromFile(File dbPointerFile) {
            String fileContent = IOUtil.readString(dbPointerFile.getAbsolutePath());
            //the first property name currently should be MOOCdbName 
            if (fileContent.trim().indexOf(MOOCdbItem.MOOCdb_PROPERTY_NAME) == 0) {
                    String[] tokens = fileContent.trim().split("\\=");
                    if (tokens[0].trim().equals(MOOCdbItem.MOOCdb_PROPERTY_NAME)){
                            return tokens[1].trim();
                    } else
                            return null;
            } else
                    return null;
    }
    
    private boolean databaseExist(String dbName) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            return dbDao.databaseExist(dbName);
    }

    //check if a MOOCdb already exists
    private MOOCdbItem findMOOCdb(String MOOCdbName) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            MOOCdbItem item = dbDao.getMOOCdbByName(MOOCdbName);
            return item;
    }
    
    private void outputFilesWithExistingDbInfo(String MOOCdbName) {
          //output MOOCdb file and feature name
            File dbPointerFile = this.createFile("MOOCdbPointer", ".txt");
            File featuresFile = this.createFile("MOOCdbFeatures", ".txt");
                    
            //write to dbPointerFile
            try (OutputStream outputStream = new FileOutputStream(dbPointerFile)) {
                    // Write header and course name to export
                    byte[] cname = null;
                    cname = (MOOCdbItem.MOOCdb_PROPERTY_NAME + "=" + MOOCdbName).getBytes("UTF-8");
                    outputStream.write(cname);
            } catch (Exception e) {
                    // This will be picked up by the workflows platform and relayed to the user.
                    e.printStackTrace();
            }
                    
            //write one line to featureFile
            try (OutputStream outputStream = new FileOutputStream(featuresFile)) {
                    // Write features to export
                    byte[] features = null;
                    String osName = System.getProperty("os.name").toLowerCase();
                    if (osName.indexOf("win") >= 0) {
                            features = (getAllFeatures(MOOCdbName) + "\r\n").getBytes("UTF-8");
                    } else {
                            features = (getAllFeatures(MOOCdbName) + "\n").getBytes("UTF-8");
                    }
                    outputStream.write(features);
            } catch (Exception e) {
                    // This will be picked up by the workflows platform and relayed to the user.
                    e.printStackTrace();
            }
                            
            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileLabel = "MOOCdb";
            this.addOutputFile(dbPointerFile, nodeIndex, fileIndex, fileLabel);
            nodeIndex = 1;
            fileIndex = 0;
            fileLabel = "MOOCdb-features";
            this.addOutputFile(featuresFile, nodeIndex, fileIndex, fileLabel);

            logger.info("Output MOOCdb to previously existing MOOCdb: " + MOOCdbName);
            // Send the component output back to the workflow.
            System.out.println(this.getOutput());
            return;
    }
    
    private String getSaltString() {
            String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
            StringBuilder salt = new StringBuilder();
            Random rnd = new Random();
            while (salt.length() < 10) { // length of the random string.
                int index = (int) (rnd.nextFloat() * SALTCHARS.length());
                salt.append(SALTCHARS.charAt(index));
            }
            String saltStr = salt.toString();
            return saltStr;
    }
    
    //save or update a MOOCdbITem and return the item
    private void saveOrUpdateMOOCdb(MOOCdbItem dbItem) {
            MOOCdbDao dbDao = DaoFactory.DEFAULT.getMOOCdbDao();
            dbDao.saveOrUpdate(dbItem);
    }
}
