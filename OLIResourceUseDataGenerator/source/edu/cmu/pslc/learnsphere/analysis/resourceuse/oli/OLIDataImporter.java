/*
 * Carnegie Mellon University, Human Computer Interaction Institute
 * Copyright 2015
 * All Rights Reserved
 */

package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.cmu.pslc.datashop.extractors.AbstractExtractor;
import edu.cmu.pslc.datashop.importdb.dao.ImportDbDaoFactory;
import edu.cmu.pslc.datashop.importdb.dao.ImportFileInfoDao;
import edu.cmu.pslc.datashop.importdb.dao.ImportStatusDao;
import edu.cmu.pslc.datashop.importdb.item.ImportFileInfoItem;
import edu.cmu.pslc.datashop.importdb.item.ImportStatusItem;
import edu.cmu.pslc.datashop.item.FileItem;
import edu.cmu.pslc.datashop.util.FileUtils;
import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.util.VersionInformation;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dao.DaoFactory;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dao.ResourceUseOliImporterDao;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dao.ResourceUseOliTransactionDao;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dao.ResourceUseOliTransactionFileDao;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dao.ResourceUseOliUserSessDao;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dao.ResourceUseOliUserSessFileDao;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.item.ResourceUseOliTransactionFileItem;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.item.ResourceUseOliUserSessFileItem;

/**
 * This tool is used to read a tab-delimited OLI transaction import file,
 * create a resourceUseOliTransactionFile and many resourceuserTransactionItem.
 * It calls sql "load data infile" to do this.
 *
 * @author Hui Cheng
 * @version $Revision: 12897 $
 * <BR>Last modified by: $Author: hcheng $
 * <BR>Last modified on: $Date: 2016-02-02 00:28:49 -0500 (Tue, 02 Feb 2016) $
 * <!-- $KeyWordsOff: $ -->
 */
public class OLIDataImporter {
        /** Debug logging. */
        private Logger logger = Logger.getLogger(getClass().getName());

        /** The name of this tool, used in displayUsage method. */
        private static final String TOOL_NAME = OLIDataImporter.class.getSimpleName();
        /** Success prefix string. */
        private static final String SUCCESS_PREFIX = TOOL_NAME + " - ";
        /** Warning prefix string. */
        private static final String WARN_PREFIX = "WARN " + TOOL_NAME + " - ";
        /** Error prefix string. */
        private static final String ERROR_PREFIX = "ERROR " + TOOL_NAME + " - ";
        
        /** Default Delimiter */
        private static final String DEFAULT_DELIMITER = "\\t";
        /** Name of transaction file to import */
        private String transactionFileName = null;
        /** Name of user session file to import */
        private String userSessFileName = null;
        
        //result
        private ResourceUseOliTransactionFileItem resourceUseOliTransactionFileItem;
        private ResourceUseOliUserSessFileItem resourceUseOliUserSessFileItem;
        
        ResourceUseOliHelper helper;
        
        public static String IMPORT_FILE_TYPE_TRANSACTION = "transaction";
        public static String IMPORT_FILE_TYPE_USER_SESS = "user_sess";
        
        private String[] ACTION_LOG_COLUMN_NAMES = {"guid", "sess_ref", "source", "time",
                                                        "timezone", "action", "external_object_id", "container",
                                                        "concept_this", "concept_req", "eastern_time", "server_receipt_time",
                                                        "info_type", "info"};
        private String[] ACTION_LOG_TABLE_COLUMN_NAMES = {"guid", "user_sess", "source", "transaction_time",
                                                        "time_zone", "action", "external_object_id", "container",
                                                        "concept_this", "concept_req", "eastern_time", "server_receipt_time",
                                                        "info_type", "info"};
        private String[] ACTION_LOG_TABLE_NULLABLE_DATETIME_COLUMN_NAMES = {"eastern_time", "server_receipt_time"};
        private String[] USER_SESS_COLUMN_NAMES = {"user_sess", "user_id"};
        private String[] USER_SESS_TABLE_COLUMN_NAMES = {"user_sess", "anon_student_id"};

        protected OLIDataImporter() {
                helper = new ResourceUseOliHelper();
        }
        
        /**
         * Display the usage of this utility.
         */
        protected void displayUsage() {
            System.err.println("\nUSAGE: java -classpath ..."
                + " OLIDataImporter -transaction_file name"
                + " -user_sess_file name");
            System.err.println("Option descriptions:");
            System.err.println("\t-transaction_file name \t\t\t transaction file name");
            System.err.println("\t-user_sess_file name \t\t\t user sess file name");
        }
        
        /**
         * Handle the command line arguments.
         * @param args - command line arguments passed into main
         * @return returns null if no exit is required,
           * 0 if exiting successfully (as in case of -help),
           * or any other number to exit with an error status
         */
        protected void handleOptions(String[] args) {
          // The value is null if no exit is required,
            // 0 if exiting successfully (as in case of -help),
            // or any other number to exit with an error status
            String exitError = null;
            if (args != null && args.length != 0) {
                java.util.ArrayList argsList = new java.util.ArrayList();
                for (int i = 0; i < args.length; i++) {
                    argsList.add(args[i]);
                }
                // loop through the arguments
                for (int i = 0; i < args.length; i++) {
                        if (args[i].equals("-h")) {
                                displayUsage();
                                exitError = "Help wanted";
                        } else if (args[i].equals("-help")) {
                                displayUsage();
                                exitError = "Help wanted";
                        } else if (args[i].equals("-transaction_file")) {
                                if (++i < args.length) {
                                        setTransactionFileName(args[i]);
                                        logger.debug("transaction file name: " + getTransactionFileName());
                                } else {
                                        System.err.println(
                                                 "Error: a transaction file name must be specified with this argument");
                                        displayUsage();
                                        exitError = "Error: a transaction file name must be specified with this argument";
                                }
                         } else if (args[i].equals("-user_sess_file")) {
                                if (++i < args.length) {
                                         setUserSessFileName(args[i]);
                                         logger.debug("user sess file name: " + getUserSessFileName());
                                } else {
                                         System.err.println(
                                                  "Error: a user sess file name must be specified with this argument");
                                         displayUsage();
                                         exitError = "Error: a user sess file name must be specified with this argument";
                                }
                         }
                         // If the exitError is set, then break out of the loop
                         if (exitError != null) {
                                break;
                         }
                } // end if then else
            } // end for loop
            
            //transaction file is required
            if (getTransactionFileName() == null) {
                    System.err.println("Error: transaction file is required. ");
                    displayUsage();
                    exitError = "Error: transaction file is required. ";
            }
            //user-sess file is required
            if (getUserSessFileName() == null) {
                    System.err.println("Error: user sess file is required. ");
                    displayUsage();
                    exitError = "Error: user sess file is required. ";
            }
            
            if (exitError != null)
                    System.exit(1);
        } // end handleOptions
               
        /**
         * This is the main method for running OLI data import and aggregator
         * @param args command line arguments
         */
        public static void main(String[] args) {          
          Logger logger = Logger.getLogger("OLIDataImporter.main");
          String version = VersionInformation.getReleaseString();
          logger.info("OLIDataImporter starting (" + version + ")...");
          OLIDataImporter oliImporter = new OLIDataImporter();
          OLIDataAggregator oliDataAggregator = new OLIDataAggregator();
          try {
                  // handle command line arguments, exit if something is amiss
                  oliImporter.handleOptions(args);
                  // do the work
                  oliImporter.importData();
                  oliDataAggregator.setResourceUseOliTransactionFileId(oliImporter.getResourceUseOliTransactionFileId());
                  oliDataAggregator.setResourceUseOliUserSessFileId(oliImporter.getResourceUseOliUserSessFileId());
                  System.out.println(oliDataAggregator.aggregateData());
                  oliImporter.clearData();
                  
          } catch (ResourceUseOliException exception) {
                  logger.error("OLIDataImporter/Aggregator exception: " + exception.getErrorMessage());
          } catch (Throwable throwable) {
              logger.error("Unknown error in main method.", throwable);
          } finally {
              logger.info("OLIDataImporter done.");
          }
        }

        /**
         * This is where the control of the overall process to import OLI resource use data.
         */
        public void importData() throws ResourceUseOliException {
                //record import info in import db
                ImportStatusItem importStatusItem = null;
                ImportFileInfoItem txnImportFileInfo = null;
                ImportFileInfoItem userSessImportFileInfo = null;
                // create a row in the import_db.import_status table
                importStatusItem = createEntryInImportStatusTable();
                      
                // process transaction file
                txnImportFileInfo = getImportFileInfoItem(importStatusItem, transactionFileName);
                File txnFile = new File(transactionFileName);
                //validate transaction file headers
                if (txnImportFileInfo.getStatus().equals(ImportFileInfoItem.STATUS_QUEUED))
                        validateTransactionImportFileHeaders(txnFile, txnImportFileInfo);
                txnImportFileInfo = getImportFileInfoItem((Integer)txnImportFileInfo.getId());
                importStatusItem = getImportStatusItem((Integer)importStatusItem.getId());
                if (txnImportFileInfo.getStatus().equals(ImportFileInfoItem.STATUS_QUEUED)) {
                        // change status to LOADING
                        txnImportFileInfo.setStatus(ImportStatusItem.STATUS_LOADING);
                        updateImportFileInfo(txnImportFileInfo);
                        importStatusItem.setStatus(ImportStatusItem.STATUS_LOADING);
                        updateImportStatus(importStatusItem);
                        
                        //save to resource_use_oli_transaction_file
                        resourceUseOliTransactionFileItem = saveTransactionFile();
                        //System.out.print("resourceUseOliTransactionFileItem:" + resourceUseOliTransactionFileItem);
                        //load data
                        txnImportFileInfo = getImportFileInfoItem((Integer)txnImportFileInfo.getId());
                        if (!txnImportFileInfo.getStatus().equals(ImportFileInfoItem.STATUS_ERROR)) {
                                runLoadData(txnImportFileInfo, IMPORT_FILE_TYPE_TRANSACTION, (Integer)resourceUseOliTransactionFileItem.getId());
                                txnImportFileInfo = getImportFileInfoItem((Integer)txnImportFileInfo.getId());
                                txnImportFileInfo.setTimeEnd(new Date());
                                txnImportFileInfo.setStatus(ImportFileInfoItem.STATUS_LOADED);
                                this.updateImportFileInfo(txnImportFileInfo);
                        }
                }
                
                // process user_sess file
                userSessImportFileInfo = getImportFileInfoItem(importStatusItem, userSessFileName);
                File userSessFile = new File(userSessFileName);
                //validate userSess file headers
                if (userSessImportFileInfo.getStatus().equals(ImportFileInfoItem.STATUS_QUEUED))
                        validateUserSessImportFileHeaders(userSessFile, userSessImportFileInfo);
                userSessImportFileInfo = getImportFileInfoItem((Integer)userSessImportFileInfo.getId());
                importStatusItem = getImportStatusItem((Integer)importStatusItem.getId());
                if (userSessImportFileInfo.getStatus().equals(ImportFileInfoItem.STATUS_QUEUED)) {
                        // change status to LOADING
                        userSessImportFileInfo.setStatus(ImportStatusItem.STATUS_LOADING);
                        updateImportFileInfo(txnImportFileInfo);
                        importStatusItem.setStatus(ImportStatusItem.STATUS_LOADING);
                        updateImportStatus(importStatusItem);
                        
                        //save to resource_use_oli_user_sess_file
                        resourceUseOliUserSessFileItem = saveUserSessFile();
                        //System.out.print("resourceUseOliUserSessFileItem:" + resourceUseOliUserSessFileItem);
                        //load data
                        userSessImportFileInfo = getImportFileInfoItem((Integer)userSessImportFileInfo.getId());
                        if (!userSessImportFileInfo.getStatus().equals(ImportFileInfoItem.STATUS_ERROR)) {
                                runLoadData(userSessImportFileInfo, IMPORT_FILE_TYPE_USER_SESS, (Integer)resourceUseOliUserSessFileItem.getId());
                                userSessImportFileInfo = getImportFileInfoItem((Integer)userSessImportFileInfo.getId());
                                userSessImportFileInfo.setTimeEnd(new Date());
                                userSessImportFileInfo.setStatus(ImportFileInfoItem.STATUS_LOADED);
                                this.updateImportFileInfo(userSessImportFileInfo);
                        }
                }
                
                //update import status
                String msg = "Importing files ";
                if (resourceUseOliTransactionFileItem != null)
                        msg += "for resource use OLI transaction file id: " + resourceUseOliTransactionFileItem.getId() + "; ";
                if (resourceUseOliUserSessFileItem != null)
                        msg += "for resource use OLI user-sess file id: " + resourceUseOliUserSessFileItem.getId() + ". ";
                ImportStatusDao importStatusDao = ImportDbDaoFactory.DEFAULT.getImportStatusDao();
                boolean txnHasError = false;
                if (txnImportFileInfo != null) {
                        if (txnImportFileInfo.getStatus().equals(ImportFileInfoItem.STATUS_ERROR)) {
                                msg += "Error found in loading transaction file: " + txnImportFileInfo.getFileName() + ". ";
                                txnHasError = true;
                        } else {
                                msg += "Successful loading for transaction file: " + txnImportFileInfo.getFileName() + ". ";                                 
                        }
                }
                boolean userSessHasError = false;
                if (userSessImportFileInfo != null) {
                        if (userSessImportFileInfo.getStatus().equals(ImportFileInfoItem.STATUS_ERROR)) {
                                msg += "Error found in loading user_sess file: " + userSessImportFileInfo.getFileName() + ". ";
                                userSessHasError = true;
                        } else {
                                msg += "Successful loading for user_sess file: " + userSessImportFileInfo.getFileName() + ". ";                                 
                        }
                }
                
                if(txnImportFileInfo != null || userSessImportFileInfo != null){
                        importStatusItem.setTimeEnd(new Date());
                        if (txnHasError || userSessHasError) {
                                reportImportStatusError(importStatusItem, msg);
                        } else {
                                importStatusItem.setStatus(ImportStatusItem.STATUS_IMPORTED);
                                importStatusItem.setWarningMessage(msg);
                                importStatusDao.saveOrUpdate(importStatusItem);
                        }
                }
        }

        
        /**
         * Load the data from the files into table, and save the file to the system
         * @param importStatusItem the given import status item
         * @param importFileInfoItem the import file info item
         * @param importFileType either transaction or user_sess
         * @param toBeSavedFilePath the system file path where the file will be saved to
         * @param toBeSaveFileName the file name which the file will be saved to
         * @return true if successful, false otherwise
         */
        private boolean runLoadData(ImportFileInfoItem importFileInfoItem, String importFileType, int refId ) 
                                        throws ResourceUseOliException {
                boolean successFlag = true;
                String importedFileName = importFileInfoItem.getFileName();
                //don't replace back splash. because the import file has it in info field
                //successFlag = processBackslashInFile(importedFileName);
                if (!successFlag) {
                        String errorMessage = ERROR_PREFIX + "Failed to process backslash for file " + importedFileName;
                        importFileInfoItem.setTimeEnd(new Date());
                        reportImportFileInfoError(importFileInfoItem, errorMessage);
                        successFlag = false;
                } else {
                        helper.logInfo(SUCCESS_PREFIX, "Loading data in file ", importedFileName);
                        File theOriginalFile = new File(importedFileName);
                        String lineTerminator = getLineTerminator(theOriginalFile);
                        ResourceUseOliImporterDao riDao = DaoFactory.DEFAULT.getResourceUseOliImporterDao();
                int numRows = 0;
                try {
                        if (importFileType.equals(IMPORT_FILE_TYPE_TRANSACTION))
                                numRows = riDao.loadTransactionData(importedFileName, refId, lineTerminator, ACTION_LOG_TABLE_COLUMN_NAMES, ACTION_LOG_TABLE_NULLABLE_DATETIME_COLUMN_NAMES);
                        else if (importFileType.equals(IMPORT_FILE_TYPE_USER_SESS))
                                numRows = riDao.loadUserSessData(importedFileName, refId, lineTerminator, USER_SESS_TABLE_COLUMN_NAMES);
                } catch (SQLException exception) {
                    String errorMessage = ERROR_PREFIX + "Exception caught in loading data for file " + importedFileName;
                    importFileInfoItem.setTimeEnd(new Date());
                    reportImportFileInfoError(importFileInfoItem, errorMessage, exception);
                    successFlag = false;
                }
                
                if (numRows <= 0) {
                    String errorMessage = ERROR_PREFIX + "Zero rows loaded for file " + importedFileName;
                    importFileInfoItem.setTimeEnd(new Date());
                    reportImportFileInfoError(importFileInfoItem, errorMessage);
                    successFlag = false;
                } else {
                    importFileInfoItem.setTimeEnd(new Date());
                    importFileInfoItem.setStatus(ImportFileInfoItem.STATUS_LOADED);
                    updateImportFileInfo(importFileInfoItem);
                }
            }

            if (successFlag) {
                helper.logInfo(SUCCESS_PREFIX, "Data loaded successfully.");
            } else {
                helper.logInfo(ERROR_PREFIX, "Data loading has error.");
            }

            helper.logDebug("runLoadData is done: ", successFlag);
            return successFlag;
        }
        
        //save to resource_use_oli_transaction_file
        private ResourceUseOliTransactionFileItem saveTransactionFile() {
                //insert resource_use_oli_transaction_file
                ResourceUseOliTransactionFileItem resourceUseOliTransactionFileItem = new ResourceUseOliTransactionFileItem();
                ResourceUseOliTransactionFileDao resourceUseOliTransactionFileDao = DaoFactory.DEFAULT.getResourceUseOliTransactionFileDao();
                resourceUseOliTransactionFileDao.saveOrUpdate(resourceUseOliTransactionFileItem);
                return resourceUseOliTransactionFileItem;
        }
        
        //save to resource_use_oli_user_sess_file
        private ResourceUseOliUserSessFileItem saveUserSessFile() {
                ResourceUseOliUserSessFileItem resourceUseOliUserSessFileItem = new ResourceUseOliUserSessFileItem();
                ResourceUseOliUserSessFileDao resourceUseOliUserSessFileDao = DaoFactory.DEFAULT.getResourceUseOliUserSessFileDao();
                resourceUseOliUserSessFileDao.saveOrUpdate(resourceUseOliUserSessFileItem);
                return resourceUseOliUserSessFileItem;
        }
        
        public void clearData() {
                deleteTransactionAndTransactionFile(getResourceUseOliTransactionFileId());
                deleteUserSessAndUserSessFile(getResourceUseOliUserSessFileId()); 
        }
        
        //delete all resource_use_oli_transaction and resource_use_oli_transaction_file with this resource_use_oli_transaction_file_id
        private void deleteTransactionAndTransactionFile(Integer resourceUseOliTransactionFileId) {
                ResourceUseOliTransactionDao resourceUseOliTransactionDao = DaoFactory.DEFAULT.getResourceUseOliTransactionDao();
                int deletedTxnRowCnt = resourceUseOliTransactionDao.clear(resourceUseOliTransactionFileId);
                ResourceUseOliTransactionFileDao resourceUseOliTransactionFileDao = DaoFactory.DEFAULT.getResourceUseOliTransactionFileDao();
                int deletedTxnFileRowCnt = resourceUseOliTransactionFileDao.clear(resourceUseOliTransactionFileId);
                helper.logInfo(WARN_PREFIX, deletedTxnRowCnt + " records are deleted from resource_use_oli_transaction; and " +
                                deletedTxnFileRowCnt + " records are deleted from resource_use_oli_transaction_file.");
                
        }
        
       //delete all resource_use_oli_user_sess and resource_use_oli_user_sess_file with this resource_use_oli_user_sess_file_id
        private void deleteUserSessAndUserSessFile(Integer resourceUseOliUserSessFileId) {
                ResourceUseOliUserSessDao resourceUseOliUserSessDao = DaoFactory.DEFAULT.getResourceUseOliUserSessDao();
                int deletedUserSessCnt = resourceUseOliUserSessDao.clear(resourceUseOliUserSessFileId);
                ResourceUseOliUserSessFileDao resourceUseOliUserSessFileDao = DaoFactory.DEFAULT.getResourceUseOliUserSessFileDao();
                int deletedUserSessFileCnt = resourceUseOliUserSessFileDao.clear(resourceUseOliUserSessFileId);
                helper.logInfo(WARN_PREFIX, deletedUserSessCnt + " records are deleted from resource_use_oli_user_sess; and " +
                                deletedUserSessFileCnt + " records are deleted from resource_use_oli_user_sess_file.");
        }

        /**
         * Replace all single backslash in a file to prepare the loading.
         * And a .bk file is created
         * And make a .bk file for the original file
         * @param fileName the name of the file to be processed
         * @return true if process is successful, otherwise false
         */
        private boolean processBackslashInFile(String fileName) {
            String backUpFileName = fileName + ".bk";
            File file = new File(fileName);
            String[] grepCmd = {"grep", "\\\\", fileName};
            String[] sedCmd = {"sed", "-e", "s/\\\\/\\\\\\\\/g", backUpFileName};
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.indexOf("win") >= 0) {
                grepCmd[1] = "\"\\\\\\\\\"";
                grepCmd[2] = "\"" + fileName + "\"";
                sedCmd[2] = "\'s/\\\\\\\\/\\\\\\\\\\\\\\\\/g\'";
                sedCmd[3] = "\"" + backUpFileName + "\"";
            }
            InputStream ins = null;
            InputStreamReader isr = null;
            BufferedReader bufferReader = null;
            BufferedWriter output = null;
            try {
                helper.logDebug("Running GREP:", Arrays.toString(grepCmd));
                Process process = new ProcessBuilder(grepCmd).start();
                ins = process.getInputStream();
                isr = new InputStreamReader(ins);
                bufferReader = new BufferedReader(isr);
                String line;
                boolean found = false;
                while ((line = bufferReader.readLine()) != null) {
                  found = true;
                  logger.debug("Found single back slash in the file.");
                  break;
                }
                process.destroy();
                if (bufferReader != null) {
                   bufferReader.close();
                }
                if (isr != null) {
                   isr.close();
                }
                if (ins != null) {
                    ins.close();
                }
                if (found) {
                  if (FileUtils.copyFile(file, new File(backUpFileName))) {
                        helper.logDebug("Running SED:", Arrays.toString(sedCmd));
                        process = new ProcessBuilder(sedCmd).start();
                        ins = process.getInputStream();
                        isr = new InputStreamReader(ins);
                        bufferReader = new BufferedReader(isr);

                        output = new BufferedWriter(new FileWriter(file));

                        line = "";
                        output.write("");
                        while ((line = bufferReader.readLine()) != null) {
                          output.append(line + "\n");
                        }
                        process.waitFor();
                    }
                }
            } catch (IOException exception) {
                logger.error("processBackslashInFile:IOException occurred.", exception);
                return false;
            } catch (InterruptedException exception) {
                logger.error("processBackslashInFile:IOException occurred.", exception);
                return false;
            } finally {
                try {
                    if (bufferReader != null) {
                      bufferReader.close();
                    }
                    if (isr != null) {
                        isr.close();
                    }
                    if (ins != null) {
                        ins.close();
                    }
                    if (output != null) {
                        output.close();
                    }
                } catch (IOException exception) {
                    logger.error("processBackslashInFile:IOException occurred.", exception);
                    return false;
                }

                // call garbage collection explicitly
                // so that rename and delete file would work later in the process.
                System.gc();
            }
            return true;
        }
        
        /**
         * Given a File, find the line terminator.
         * @param importFileInfo the import file info items
         * @return the line terminator
         */
        private String getLineTerminator(File theFile) {
            String lineTerminator = "";
            BufferedReader bufferReader = null;
            FileReader reader = null;
            try {
                reader = new FileReader(theFile);
                bufferReader = new BufferedReader(reader);
                int intChar;
                while (((intChar = bufferReader.read()) != -1)) {
                    char lastChar = (char) intChar;
                    if (lastChar == '\r') {
                        intChar = bufferReader.read();
                        if (intChar != -1) {
                            lastChar = (char) intChar;
                            if (lastChar == '\n') {
                                lineTerminator = "\r\n";
                                break;
                            }
                        }
                        lineTerminator = "\r";
                        break;
                    } else  if (lastChar == '\n') {
                        lineTerminator = "\n";
                        break;
                    }
                }
            } catch (IOException exception) {
                logger.error("getLineTerminator:IOException occurred.", exception);
            } finally {
                try {
                    if (bufferReader != null) {
                        bufferReader.close();
                        bufferReader = null;
                    }
                    if (reader != null) {
                        reader.close();
                        reader = null;
                    }
                } catch (IOException exception) {
                    logger.error("getLineTerminator:IOException occurred.", exception);
                } finally {
                    System.gc();
                }
            }
            return lineTerminator;
        }
             
        private ImportFileInfoItem getImportFileInfoItem(int importFileInfoId) {
                ImportFileInfoDao importFileInfoDao = ImportDbDaoFactory.DEFAULT.getImportFileInfoDao();
                return importFileInfoDao.get(importFileInfoId);
        }
        
        private ImportStatusItem getImportStatusItem(int importStatusId) {
                ImportStatusDao importStatusDao = ImportDbDaoFactory.DEFAULT.getImportStatusDao();
                return importStatusDao.get(importStatusId);
        }

        private void validateTransactionImportFileHeaders (File txnFile, ImportFileInfoItem txnImportFileInfo)  {
                boolean valid = true;
                String errorMsg = "";
                try {
                        String[] headers = helper.getHeaderFromFile(txnFile, DEFAULT_DELIMITER);
                        for (int i = 0; i < headers.length; i++) {
                                if (!headers[i].equals(ACTION_LOG_COLUMN_NAMES[i])) {
                                        valid = false;
                                        errorMsg = "validateTransactionImportFileHeaders found error for column: "
                                                        + headers[i] + " in " + txnFile.getName();
                                        break;
                                }
                        }
                        
                    } catch (ResourceUseOliException exception) {
                            valid = false;
                            errorMsg = "validateTransactionImportFileHeaders:IOException occurred. "
                                            + txnFile.getName() + exception.getMessage();
                    }
                if (!valid) {
                        txnImportFileInfo.setTimeEnd(new Date());
                        reportImportFileInfoError(txnImportFileInfo, errorMsg);
                }
        }
        
        private void validateUserSessImportFileHeaders (File userSessFile, ImportFileInfoItem userSessImportFileInfo) 
                                throws ResourceUseOliException {
                boolean valid = true;
                String errorMsg = "";
                try {
                        String[] headers = helper.getHeaderFromFile(userSessFile, DEFAULT_DELIMITER);
                        for (int i = 0; i < headers.length; i++) {
                                if (!headers[i].equals(USER_SESS_COLUMN_NAMES[i])) {
                                        valid = false;
                                        errorMsg = "validateTransactionImportFileHeaders found error for column: "
                                                        + headers[i] + " in " + userSessFile.getName();
                                        break;
                                }
                        }
                        
                    } catch (ResourceUseOliException exception) {
                            valid = false;
                            errorMsg = "validateTransactionImportFileHeaders:IOException occurred. "
                                            + userSessFile.getName() + exception.getMessage();
                    }
                if (!valid) {
                        userSessImportFileInfo.setTimeEnd(new Date());
                        reportImportFileInfoError(userSessImportFileInfo, errorMsg); 
                }
        }
         
        /**
         * Create a row in the import_db.import_status table for this run.
         * Set the time_start to now and the status to 'QUEUED'.
         *  
         * @return the status item just created if success, exception otherwise
         */
        private ImportStatusItem createEntryInImportStatusTable() {
            ImportStatusDao importStatusDao = ImportDbDaoFactory.DEFAULT.getImportStatusDao();
            ImportStatusItem statusItem = new ImportStatusItem();
            String tmpDatasetName = FileUtils.removePathFromFileName(this.transactionFileName) + "; " + FileUtils.removePathFromFileName(this.userSessFileName);
            if (tmpDatasetName.length() > 100)
                    tmpDatasetName = tmpDatasetName.substring(0, 100);
            statusItem.setDatasetName(tmpDatasetName);
            statusItem.setDomainName(TOOL_NAME);
            statusItem.setTimeStart(new Date());
            statusItem.setStatus(ImportStatusItem.STATUS_QUEUED);
            importStatusDao.saveOrUpdate(statusItem);
            return statusItem;
        }

        /**
         * Create a row in the import_db.import_file_info table for the given file.
         * Set the file name, status, as well as,
         * the time_start to now.
         * @param importStatusItem the high level status row in the database (FK)
         * @param fileName the name of file to load
         * @param status expecting ERROR or QUEUED
         * @return the status item just created if success, exception otherwise
         */
        private ImportFileInfoItem createEntryInImportFileInfoTable(
                ImportStatusItem importStatusItem, String fileName, String status, String errorMsg) {
            ImportFileInfoDao importFileInfoDao = ImportDbDaoFactory.DEFAULT.getImportFileInfoDao();
            ImportFileInfoItem importFileInfoItem = new ImportFileInfoItem();
            importFileInfoItem.setImportStatus(importStatusItem);
            importFileInfoItem.setFileName(fileName);
            Date now = new Date();
            importFileInfoItem.setTimeStart(now);
            if (status.equals(ImportFileInfoItem.STATUS_ERROR)) {
                    importFileInfoItem.setTimeEnd(now);
                    if (errorMsg != null)
                            importFileInfoItem.setErrorMessage(errorMsg);
            }
            importFileInfoItem.setStatus(status);
            importFileInfoDao.saveOrUpdate(importFileInfoItem);
            return importFileInfoItem;
        }
        
        /**
         * Returns a ImportFileInfoItem object for the specified file name.
         * @param importStatusItem the given status item
         * @param fileName the file name
         * @return an importFileInfoItem if the file is valid, null otherwise
         */
        private ImportFileInfoItem getImportFileInfoItem(ImportStatusItem importStatusItem, String fileName) {
            ImportFileInfoItem importFileInfoItem = null;
            // if windows operation system, replace slashes in path
            boolean winFlag = false;
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.indexOf("win") >= 0) {
                winFlag = true;
                helper.logInfo(SUCCESS_PREFIX,
                        "Replacing slashes path of file name, os.name: ",
                        osName);
            } else {
                    helper.logInfo(SUCCESS_PREFIX,
                        "NOT Replacing slashes path of file name, os.name: ",
                        osName);
            }

            if (fileName != null) {
                if (winFlag) {
                    fileName = fileName.replaceAll("\\\\", "\\/");
                }

                File theFile =  new File(fileName);

                if (!theFile.exists()) {
                    String fixedName = FileUtils.removePathFromFileName(theFile.getName());
                    String errorMsg = "File " + fixedName + " does not exist.";
                    reportImportStatusError(importStatusItem, errorMsg);
                    importFileInfoItem = createEntryInImportFileInfoTable(importStatusItem,
                                                                      fileName,
                                                                      ImportFileInfoItem.STATUS_ERROR, errorMsg);
                } else {
                    if (theFile.getName().endsWith(".txt")) {
                            helper.logInfo(SUCCESS_PREFIX, "File ", fileName, " found.");
                        importFileInfoItem =
                            createEntryInImportFileInfoTable(importStatusItem,
                                                             fileName,
                                                             ImportFileInfoItem.STATUS_QUEUED, null);
                    } else {
                        String fixedName = FileUtils.removePathFromFileName(theFile.getName());
                        String errorMsg = "File " + fixedName + " does not have a .txt extension.";
                        reportImportStatusError(importStatusItem, errorMsg);
                        importFileInfoItem =
                            createEntryInImportFileInfoTable(importStatusItem,
                                                             fileName,
                                                             ImportFileInfoItem.STATUS_ERROR, errorMsg);
                    }
                }
            } else {
                    String errorMsg = "File name is null";
                    importFileInfoItem =
                                    createEntryInImportFileInfoTable(importStatusItem,
                                                                     fileName,
                                                                     ImportFileInfoItem.STATUS_ERROR, errorMsg);
            }

            return importFileInfoItem;
        }

        
        /**
         * Report the current error on the import_file_info.
         *  
         * @param importFileInfoItem the import file level status row in the database (FK)
         * @param message the new message
         * @param exception if there is one so that it can be put in the debug logging
         */
        private void reportImportFileInfoError(ImportFileInfoItem importFileInfoItem,
                                 String message, Exception exception) {
            ImportFileInfoDao importFileInfoDao = ImportDbDaoFactory.DEFAULT.getImportFileInfoDao();
            importFileInfoDao.saveErrorMessage(importFileInfoItem, message);
            if (exception == null) {
                    helper.logError(ERROR_PREFIX, message);
            } else {
                    helper.logError(ERROR_PREFIX + message, exception);
            }
        }
        /**
         * Calls the other report error method without an exception.
         * @param importFileInfoItem the file level status row in the database (FK)
         * @param message the new message
         */
        private void reportImportFileInfoError(ImportFileInfoItem importFileInfoItem, String message) {
                reportImportFileInfoError(importFileInfoItem, message, null);
        }

        /**
         * Report the current error on the import_status and write error message/exception to error log
         *  
         * @param importStatusItem the high level status row in the database (FK)
         * @param message the new message
         * @param exception if there is one so that it can be put in the debug logging
         */
        private void reportImportStatusError(ImportStatusItem importStatusItem,
                                 String message, Exception exception) {
            ImportStatusDao importStatusDao = ImportDbDaoFactory.DEFAULT.getImportStatusDao();
            importStatusDao.saveErrorMessage(importStatusItem, message);

            if (exception == null) {
                    helper.logError(ERROR_PREFIX, message);
            } else {
                    helper.logError(ERROR_PREFIX + message, exception);
            }
        }
        /**
         * Calls the other report error method without an exception.
         * @param importStatusItem the high level status row in the database (FK)
         * @param message the new message
         */
        private void reportImportStatusError(ImportStatusItem importStatusItem, String message) {
                reportImportStatusError(importStatusItem, message, null);
        }
        
        /**
         * update importStatusItem.
         * @param importStatusItem the high level status row in the database
         * @param message the new message
         */
        private void updateImportStatus(ImportStatusItem importStatusItem) {
                ImportStatusDao importStatusDao = ImportDbDaoFactory.DEFAULT.getImportStatusDao();
                importStatusDao.saveOrUpdate(importStatusItem);
                
        }
        
        /**
         * update importFileInfoItem.
         * @param importStatusItem the high level status row in the database
         * @param message the new message
         */
        private void updateImportFileInfo(ImportFileInfoItem importFileInfoItem) {
                ImportFileInfoDao importFileInfoDao = ImportDbDaoFactory.DEFAULT.getImportFileInfoDao();
                importFileInfoDao.saveOrUpdate(importFileInfoItem);
        }

        public String getTransactionFileName () {
                return transactionFileName;
        }
        
        public void setTransactionFileName (String transactionFileName) {
                this.transactionFileName = transactionFileName;
        }
        
        public String getUserSessFileName () {
                return userSessFileName;
        }
        
        public void setUserSessFileName (String userSessFileName) {
                this.userSessFileName = userSessFileName;
        }
        
        public Integer getResourceUseOliTransactionFileId () {
                if (resourceUseOliTransactionFileItem != null)
                        return (Integer)resourceUseOliTransactionFileItem.getId();
                else
                        return null;
        }
        
        public Integer getResourceUseOliUserSessFileId () {
                if (resourceUseOliUserSessFileItem != null)
                        return (Integer)resourceUseOliUserSessFileItem.getId();
                else
                        return null;
        }
}
