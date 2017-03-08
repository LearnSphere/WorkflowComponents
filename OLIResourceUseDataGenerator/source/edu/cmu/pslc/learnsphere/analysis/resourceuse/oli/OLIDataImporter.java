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
        private static final String SUCCESS_PREFIX = "SUCCESS: " + TOOL_NAME + " - ";
        /** Warning prefix string. */
        private static final String WARN_PREFIX = "WARN: " + TOOL_NAME + " - ";
        /** Error prefix string. */
        private static final String ERROR_PREFIX = "ERROR: " + TOOL_NAME + " - ";
        
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
                //verify both files: replace slashes, check headers and check exist
                validateUserSessFile();
                validateTransactionFile();
                resourceUseOliUserSessFileItem = saveUserSessFile();
                if (resourceUseOliUserSessFileItem != null 
                                && resourceUseOliUserSessFileItem.getId() != null)
                        logger.info(SUCCESS_PREFIX + "Save user-sess file: " + userSessFileName + ", with new ID: " + resourceUseOliUserSessFileItem.getId());
                else {
                        logger.info(ERROR_PREFIX + "Failed saving user-sess file: " + userSessFileName);
                        throw ResourceUseOliException.userSessFileSaveFailException(userSessFileName);
                }
                resourceUseOliTransactionFileItem = saveTransactionFile();
                if (resourceUseOliTransactionFileItem != null 
                                && resourceUseOliTransactionFileItem.getId() != null)
                        logger.info(SUCCESS_PREFIX + "Save transaction file: " + transactionFileName + ", with new ID: " + resourceUseOliTransactionFileItem.getId());
                else {
                        logger.info(ERROR_PREFIX + "Failed saving transaction file: " + transactionFileName);
                        throw ResourceUseOliException.transactionFileSaveFailException(transactionFileName);
                }
                runLoadData(resourceUseOliUserSessFileItem.getFileName(), IMPORT_FILE_TYPE_USER_SESS, (Integer)resourceUseOliUserSessFileItem.getId());
                runLoadData(resourceUseOliTransactionFileItem.getFileName(), IMPORT_FILE_TYPE_TRANSACTION, (Integer)resourceUseOliTransactionFileItem.getId());
        }
                    
        //verify transaction files: replace slashes in file name, check headers and check file exist
        private void validateTransactionFile() throws ResourceUseOliException {
                // if windows operation system, replace slashes in path
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.indexOf("win") >= 0) {
                        transactionFileName = transactionFileName.replaceAll("\\\\", "\\/");
                        logger.info("Transaction file name is changed to: " + transactionFileName);
                }
                File txnFile =  new File(transactionFileName);
                if (!txnFile.exists()) {
                        throw ResourceUseOliException.fileNotFoundException(txnFile);
                }
                String[] headers = helper.getHeaderFromFile(txnFile, DEFAULT_DELIMITER);
                for (int i = 0; i < headers.length; i++) {
                        if (!headers[i].equals(ACTION_LOG_COLUMN_NAMES[i])) 
                                throw ResourceUseOliException.wrongHeaderFormatException(txnFile, IMPORT_FILE_TYPE_TRANSACTION);
                }
        }

        //verify user_sess files: replace slashes in file name, check headers and check file exist
        private void validateUserSessFile() throws ResourceUseOliException {
                // if windows operation system, replace slashes in path
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.indexOf("win") >= 0) {
                        userSessFileName = userSessFileName.replaceAll("\\\\", "\\/");
                        logger.info("User-sess file name is: " + userSessFileName);
                }
                File userSessFile =  new File(userSessFileName);
                if (!userSessFile.exists()) {
                        throw ResourceUseOliException.fileNotFoundException(userSessFile);
                }
                String[] headers = helper.getHeaderFromFile(userSessFile, DEFAULT_DELIMITER);
                for (int i = 0; i < headers.length; i++) {
                        if (!headers[i].equals(USER_SESS_COLUMN_NAMES[i])) 
                                throw ResourceUseOliException.wrongHeaderFormatException(userSessFile, IMPORT_FILE_TYPE_USER_SESS);
                }
        }
        
        /**
         * Load the data from the files into table, and save the file to the system
         * @param String fileName including full path
         * @param importFileInfoItem the import file info item
         * @param importFileType either transaction or user_sess
         * @param toBeSavedFilePath the system file path where the file will be saved to
         * @param toBeSaveFileName the file name which the file will be saved to
         * @return true if successful, false otherwise
         */
        private int runLoadData(String fileName, String importFileType, int refId) 
                                        throws ResourceUseOliException {
                logger.info("Loading data in file " + fileName);
                File theOriginalFile = new File(fileName);
                String lineTerminator = getLineTerminator(theOriginalFile);
                ResourceUseOliImporterDao riDao = DaoFactory.DEFAULT.getResourceUseOliImporterDao();
                int numRows = 0;
                try {
                        if (importFileType.equals(IMPORT_FILE_TYPE_TRANSACTION))
                                numRows = riDao.loadTransactionData(fileName, refId, lineTerminator, ACTION_LOG_TABLE_COLUMN_NAMES, ACTION_LOG_TABLE_NULLABLE_DATETIME_COLUMN_NAMES);
                        else if (importFileType.equals(IMPORT_FILE_TYPE_USER_SESS))
                                numRows = riDao.loadUserSessData(fileName, refId, lineTerminator, USER_SESS_TABLE_COLUMN_NAMES);
                } catch (SQLException exception) {
                        throw ResourceUseOliException.loadingSQLException(fileName, importFileType, refId, exception);
                }
                if (numRows <= 0)
                        throw ResourceUseOliException.noRowsLoadedException(fileName, importFileType, refId);
                else
                        logger.info("Number of rows loaded: " + numRows); 
                return numRows;
        }
        
        //save to resource_use_oli_transaction_file
        private ResourceUseOliTransactionFileItem saveTransactionFile() {
                //insert resource_use_oli_transaction_file
                ResourceUseOliTransactionFileItem resourceUseOliTransactionFileItem = new ResourceUseOliTransactionFileItem();
                resourceUseOliTransactionFileItem.setFileName(transactionFileName);
                ResourceUseOliTransactionFileDao resourceUseOliTransactionFileDao = DaoFactory.DEFAULT.getResourceUseOliTransactionFileDao();
                resourceUseOliTransactionFileDao.saveOrUpdate(resourceUseOliTransactionFileItem);
                return resourceUseOliTransactionFileItem;
        }
        
        //save to resource_use_oli_user_sess_file
        private ResourceUseOliUserSessFileItem saveUserSessFile() {
                ResourceUseOliUserSessFileItem resourceUseOliUserSessFileItem = new ResourceUseOliUserSessFileItem();
                resourceUseOliUserSessFileItem.setFileName(userSessFileName);
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
                                deletedTxnFileRowCnt + " records are deleted from resource_use_oli_transaction_file for transactionFile: " + resourceUseOliTransactionFileId);
                
        }
        
       //delete all resource_use_oli_user_sess and resource_use_oli_user_sess_file with this resource_use_oli_user_sess_file_id
        private void deleteUserSessAndUserSessFile(Integer resourceUseOliUserSessFileId) {
                ResourceUseOliUserSessDao resourceUseOliUserSessDao = DaoFactory.DEFAULT.getResourceUseOliUserSessDao();
                int deletedUserSessCnt = resourceUseOliUserSessDao.clear(resourceUseOliUserSessFileId);
                ResourceUseOliUserSessFileDao resourceUseOliUserSessFileDao = DaoFactory.DEFAULT.getResourceUseOliUserSessFileDao();
                int deletedUserSessFileCnt = resourceUseOliUserSessFileDao.clear(resourceUseOliUserSessFileId);
                helper.logInfo(WARN_PREFIX, deletedUserSessCnt + " records are deleted from resource_use_oli_user_sess; and " +
                                deletedUserSessFileCnt + " records are deleted from resource_use_oli_user_sess_file for userSessFile: " + resourceUseOliUserSessFileId);
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
