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
    public static final int WRONG_FILE_FORMAT = -3;
    /**SQL exception caught*/
    public static final int SQL_EXCEPTION_CAUGHT = -4;
    /**exception when no user_sess found via user_sess file*/
    public static final int NO_USER_SESS_FOUND_VIA_USER_SESS = -5;
    /**exception when no user_sess found via transaction file*/
    public static final int NO_USER_SESS_FOUND_VIA_TXN = -6;
    /**No data found exception*/
    public static final int NO_DATA_FOUND_EXCEPTION = -7;

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
     * Exception indicating the file doesn't exist.
     * @param File file
     * @return exception indicating file not found
     */
    public static ResourceUseOliException fileNotFoundException(File file) {
        return new ResourceUseOliException(FILE_NOT_FOUND, "File is not found: " + file.getAbsolutePath() + ".");
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
     * Exception indicating the file format is wrong
     * @param File file to be used
     * @return exception indicating file not found
     */
    public static ResourceUseOliException wrongFileFormatException(File file) {
        return new ResourceUseOliException(WRONG_FILE_FORMAT, "Wrong file format in file: " + file.getAbsolutePath() + ".");
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