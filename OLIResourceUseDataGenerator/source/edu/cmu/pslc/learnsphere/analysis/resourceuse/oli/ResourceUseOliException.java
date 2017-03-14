/*
 * Carnegie Mellon University, Human Computer Interaction Institute
 * Copyright 2009
 * All Rights Reserved
 */
package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Collection of all resource use specific exception.
 *
 * @author Hui Cheng
 * @version $Revision: 12897 $
 * <BR>Last modified by: $Author: hcheng $
 * <BR>Last modified on: $Date: 2016-02-02 00:28:49 -0500 (Tue, 02 Feb 2016) $
 * <!-- $KeyWordsOff: $ -->
 */
public class ResourceUseOliException extends Exception {
    /** File doesn't exist exception. */
    public static final int FILE_NOT_FOUND = -1;
    /** IOException. */
    public static final int IO_EXCEPTION_FOUND = -2;
    /** File format error. */
    public static final int WRONG_HEADER_FORMAT = -3;
    /**SQL exception caught*/
    public static final int SQL_EXCEPTION_CAUGHT = -4;
    /**exception when no user_sess found via user_sess file*/
    public static final int NO_USER_SESS_FOUND_VIA_USER_SESS = -5;
    /**exception when no user_sess found via transaction file*/
    public static final int NO_USER_SESS_FOUND_VIA_TXN = -6;
    /**No data found exception*/
    public static final int NO_DATA_FOUND_EXCEPTION = -7;
    /**SQL exception thrown for loading file to DB*/
    public static final int SQL_EXCEPTION_CAUGHT_FOR_LOADING_DATA = -8;
    /**exception thrown for no data loaded to DB*/
    public static final int NO_DATA_LOADED = -9;
    /**exception thrown for failing to save user_sess file*/
    public static final int FAIL_SAVE_USER_SESS_FILE = -10;
    /**exception thrown for failing to save transaction file*/
    public static final int FAIL_SAVE_TRANSACTION_FILE = -11;
    
    

    /** The error code. */
    private int errorCode;
    /** a description of the problem. */
    private String errorMessage;
   
    /**
     * Exception indicating SQLException caught
     * @param SQLException exception
     * @return exception indicating SQLException caught
     */
    public static ResourceUseOliException SQLExceptionCaught(SQLException exception) {
        return new ResourceUseOliException(SQL_EXCEPTION_CAUGHT, "SQL exception caught: " + exception.getMessage() + ".");
    }
    
    /**
     * Exception indicating SQLException caught in loading file
     * @param String fileName
     * @param String loadingType: transaction or user-sess
     * @param int fileID from DB
     * @param SQLException exception
     * @return exception indicating SQLException caught in loading file to DB
     */
    public static ResourceUseOliException loadingSQLException(String fileName, String loadingType, int fileId, SQLException exception) {
            String msg = "SQL exception caught for loading data. " + 
                            "File: " + fileName + "; " +
                            "File ID: " + fileId + "; ";
            if (loadingType.equals(OLIDataImporter.IMPORT_FILE_TYPE_TRANSACTION))
                    msg += "Loading type: transaction; ";
            else if (loadingType.equals(OLIDataImporter.IMPORT_FILE_TYPE_USER_SESS))
                    msg += "Loading type: user sess; ";
            msg += "Exception: " + exception.getMessage() + ".";
        return new ResourceUseOliException(SQL_EXCEPTION_CAUGHT_FOR_LOADING_DATA, msg);
    }
    
    /**
     * Exception indicating 0 rows are loaded
     * @param String fileName
     * @param String loadingType: transaction or user-sess
     * @param int fileID from DB
     * @return exception indicating 0 rows are loaded
     */
    public static ResourceUseOliException noRowsLoadedException(String fileName, String loadingType, int fileId) {
            String msg = "0 rows are loaded. " + 
                            "File: " + fileName + "; " +
                            "File ID: " + fileId + "; ";
            if (loadingType.equals(OLIDataImporter.IMPORT_FILE_TYPE_TRANSACTION))
                    msg += "Loading type: transaction; ";
            else if (loadingType.equals(OLIDataImporter.IMPORT_FILE_TYPE_USER_SESS))
                    msg += "Loading type: user sess; ";
        return new ResourceUseOliException(NO_DATA_LOADED, msg);
    }
    
    /**
     * Exception indicating failure in saving user_sess file
     * @param String userSessFileName
     * @return exception indicating failure in saving user_sess file
     */
    public static ResourceUseOliException userSessFileSaveFailException(String userSessFileName) {
            String msg = "Error caught when saving user-sess file: " + userSessFileName;
        return new ResourceUseOliException(FAIL_SAVE_USER_SESS_FILE, msg);
    }
    
    /**
     * Exception indicating failure in saving transaction file
     * @param String transactionFileName
     * @return exception indicating failure in saving user_sess file
     */
    public static ResourceUseOliException transactionFileSaveFailException(String transactionFileName) {
            String msg = "Error caught when saving transaction file: " + transactionFileName;
        return new ResourceUseOliException(FAIL_SAVE_TRANSACTION_FILE, msg);
    }
    
    /**
     * Exception indicating the file doesn't exist.
     * @param File file
     * @return exception indicating file not found
     */
    public static ResourceUseOliException fileNotFoundException(File file) {
        return new ResourceUseOliException(FILE_NOT_FOUND, "File is not found: " + file.getAbsolutePath() + ".");
    }
    
    /**
     * Exception indicating the file format is wrong
     * @param File file to be used
     * @return exception indicating file not found
     */
    public static ResourceUseOliException wrongHeaderFormatException(File file, String fileType) {
            String msg = "Wrong header format for file: " + file.getAbsolutePath() + "; ";
            if (fileType.equals(OLIDataImporter.IMPORT_FILE_TYPE_TRANSACTION))
                    msg += "File type: transaction.";
            else if (fileType.equals(OLIDataImporter.IMPORT_FILE_TYPE_USER_SESS))
                    msg += "File type: user-sess";
        return new ResourceUseOliException(WRONG_HEADER_FORMAT, msg);
    }
    
    
    /**
     * Exception indicating the file doesn't exist.
     * @param File file to be used
     * @return exception indicating file not found
     */
    public static ResourceUseOliException IOExceptionFoundException(IOException ex) {
        return new ResourceUseOliException(IO_EXCEPTION_FOUND, "IOException found: " + ex.getMessage() + ".");
    }

    /**
     * Exception indicating no user_sess found via resourceUseOliUserSessFileId
     * @param resourceUseOliUserSessFileId resource use OLI user_sess file id
     * @return exception indicating no user_sess found
     */
    public static ResourceUseOliException noUserSessionFoundViaUserSessFileException(Integer resourceUseOliUserSessFileId) {
            
        return new ResourceUseOliException(NO_USER_SESS_FOUND_VIA_USER_SESS, "No user sess found for resourceUseOliUserSessFileId: " + resourceUseOliUserSessFileId + ".");
    }
    
    /**
     * Exception indicating no user_sess found via resourceUseOliTransactionFileId
     * @param resourceUseOliTransactionFileId resource use OLI transaction file id
     * @return exception indicating no user_sess found
     */
    public static ResourceUseOliException noUserSessionFoundViaTransactionFileException(Integer resourceUseOliTransactionFileId) {
            
        return new ResourceUseOliException(NO_USER_SESS_FOUND_VIA_TXN, "No user sess found for resourceUseOliTransactionFileId: " + resourceUseOliTransactionFileId + ".");
    }
    
    
    /**
     * Exception indicating no data found for given user-sess file and transaction file
     * @return exception indicating no data found
     */
    public static ResourceUseOliException noUserTransactionFoundException(Integer resourceUseOliUserSessFileId, Integer resourceUseOliTransactionFileId) {
        return new ResourceUseOliException(NO_DATA_FOUND_EXCEPTION, "No data found for user-sess file ID: " + resourceUseOliUserSessFileId + " and transaction file ID: " + resourceUseOliTransactionFileId + ".");
    }
    
    
    /**
     * Exception indicating no data found
     * @return exception indicating no data found
     */
    public static ResourceUseOliException noDataFoundException() {
        return new ResourceUseOliException(NO_DATA_FOUND_EXCEPTION, "No data found.");
    }
    
    /**
     * Create a new ResourceUseOliException.
     * @param errorCode an error code
     * @param errorMessage a description of the problem.
     */
    public ResourceUseOliException(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     *  The error code.
     *  @return the error code
     */
    public int getErrorCode() { return errorCode; }

    /** A description of the problem. @return a description of the problem */
    public String getErrorMessage() { return errorMessage; }
}