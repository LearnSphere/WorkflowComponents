package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;

import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dao.DaoFactory;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dao.ResourceUseOliTransactionDao;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OliResourceUseDTOInterface;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OliUserTransactionDTO;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dataobject.OliUserTransactionWithXmlDTO;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dataobject.OLIMediaDataObject;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dataobject.OLIViewActionDataObject;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dataobject.XMLExtractedDataObject;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dataobject.XMLExtractedDataObject.MoreThanOneElementException;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dataobject.XMLExtractedDataObject.TooManyProblemsExistException;

/**
 * This class is the driver for the aggregation of OLI log_act data. It does the following:
 * 1. get all student transaction data by joining user-sess and log tables ordering by student and time
 * 2. for each student, assign one OLIViewActionDataObject and one OLIMediaDataObject
 * 3. for each row of data from resource_use_oli_transaction, 
 *      computing prevTime (null if new session starts) and nextTime, extracting data
 *      tutor_message XML, and combining rows with the same transaction id into one row
 * 5. pass the result to OILViewActionDataObject or OLIMediaDataObject 
 * 6. output result into a file   
 */
public class OLIDataAggregator {
        /** Debug logging. */
        private Logger logger = Logger.getLogger(getClass().getName());

        /** The name of this tool, used in displayUsage method. */
        private static final String TOOL_NAME = OLIDataAggregator.class.getSimpleName();
        /** Error prefix string. */
        private static final String ERROR_PREFIX = "ERROR " + TOOL_NAME + " - ";
        /** Warning prefix string. */
        private static final String WARN_PREFIX = "WARN " + TOOL_NAME + " - ";

    private String javaDateFormatStr = "yyyy-MM-dd HH:mm:ss";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(javaDateFormatStr);
    
    private Integer resourceUseOliTransactionFileId = null;
    private Integer resourceUseOliUserSessFileId = null;
    
    private HashMap<String, OLIViewActionDataObject> allStudentViewActionDataObjects;
    private HashMap<String, OLIMediaDataObject> allStudentMediaDataObjects;
    private String DEFAULT_OUTPUT_FILE = "OLIStudentResourceUse.txt";
    private String outputFileName = DEFAULT_OUTPUT_FILE;

    public OLIDataAggregator() {
            allStudentViewActionDataObjects = new HashMap<String, OLIViewActionDataObject>() ;
            allStudentMediaDataObjects = new HashMap<String, OLIMediaDataObject>() ;
    }

    public Integer getResourceUseOliTransactionFileId () {
            return this.resourceUseOliTransactionFileId;
    }
    
    public void setResourceUseOliTransactionFileId (int id) {
            this.resourceUseOliTransactionFileId = id;
    }
    
    public Integer getResourceUseOliUserSessFileId () {
            return this.resourceUseOliUserSessFileId;
    }
    
    public void setResourceUseOliUserSessFileId (int id) {
            this.resourceUseOliUserSessFileId = id;
    }

    public String aggregateData() throws ResourceUseOliException {
            //get all data ordered by student and time
            ResourceUseOliTransactionDao resourceUseOliTransactionDao = DaoFactory.DEFAULT.getResourceUseOliTransactionDao();
            List<OliUserTransactionDTO> studentTransactions = resourceUseOliTransactionDao.getAllTransactions(resourceUseOliUserSessFileId, resourceUseOliTransactionFileId);
            logger.info("All student transaction data: " + studentTransactions.size() + " rows.");
            
            if (studentTransactions == null || studentTransactions.size() == 0) {
                    throw ResourceUseOliException.noUserTransactionFoundException(resourceUseOliUserSessFileId, resourceUseOliTransactionFileId);
            }
            
            String lastStudent = "";
            OLIViewActionDataObject viewActionDataObject = null;
            OLIMediaDataObject mediaDataObject = null;
            int rowCount = 0;
            OliResourceUseDTOInterface lastObj = null;
            OliResourceUseDTOInterface currObj = null;
            OliResourceUseDTOInterface nextObj = null;
            OliResourceUseDTOInterface lastProcessedObj = null;
            String lastContextMessageId = null;
            String lastProblemName = null;
            int studentCnt = 0;
            
            for (OliUserTransactionDTO transactionItem : studentTransactions) {
                    String anonStudentId = transactionItem.getStudent();
                    //logger.info(transactionItem);
                    if (!anonStudentId.equals(lastStudent)) {
                            logger.info("Aggregating data for student: " + anonStudentId);
                            studentCnt++;
                            //process the last two rows of the last student
                            OliResourceUseDTOInterface newDataObject = null;
                            if (currObj != null && lastObj != null) {
                                    newDataObject = currObj.combineTwoOLIIntermediateDataObject(lastObj);
                            }
                            if (newDataObject == null) {
                                    if (lastObj != null) {
                                            callProcessOLIDataObject(lastObj, viewActionDataObject, mediaDataObject);
                                    }
                                    if (currObj != null) {
                                            callProcessOLIDataObject(currObj, viewActionDataObject, mediaDataObject);
                                    }
                            } else {
                                    callProcessOLIDataObject(newDataObject, viewActionDataObject, mediaDataObject);
                            }
                            
                            //refresh all variables
                            viewActionDataObject = new OLIViewActionDataObject();
                            allStudentViewActionDataObjects.put(anonStudentId, viewActionDataObject);
                            viewActionDataObject.setStudent(anonStudentId);
                            mediaDataObject = new OLIMediaDataObject();
                            allStudentMediaDataObjects.put(anonStudentId, mediaDataObject);
                            mediaDataObject.setStudent(anonStudentId);
                            rowCount = 1;
                            lastObj = null;
                            currObj = null;
                            nextObj = null;
                            lastProcessedObj = null;
                            lastContextMessageId = null;
                            lastProblemName = null;
                    }
                    long thisTransactionId = (Long)transactionItem.getResourceUseTransactionId();
                    String thisAction = transactionItem.getAction();
                    String thisInfoType = transactionItem.getInfoType();
                    String thisInfo = transactionItem.getInfo();
                    //process xml and get xml data
                    XMLExtractedDataObject xmlExtractedDataObject = null;
                    if (OliUserTransactionWithXmlDTO.actionIsTutorMessage(thisAction) && OliUserTransactionWithXmlDTO.infoTypeIsTutorMessage(thisInfoType)) {
                            try {
                                    xmlExtractedDataObject = new XMLExtractedDataObject();
                                    xmlExtractedDataObject.setResourceUseTransactionId(thisTransactionId);
                                    xmlExtractedDataObject.initiate(thisInfo);
                            } catch (JDOMException e) {
                                    //LogUtils.logErr(logger, ERROR_PREFIX, "extractInfoFromXML found JDOMException", e);
                                    logger.debug(ERROR_PREFIX + "extractInfoFromXML found JDOMException: " + e.getMessage());
                                    continue;
                            } catch (IOException e) {
                                    //LogUtils.logErr(logger, ERROR_PREFIX, "extractInfoFromXML found IOException", e);
                                    logger.debug(ERROR_PREFIX + "extractInfoFromXML found IOException: " + e.getMessage());
                                    continue;
                            } catch (TooManyProblemsExistException e) {
                                    //LogUtils.logErr(logger, ERROR_PREFIX, "extractInfoFromXML found TooManyProblemsExistException found IOException", e);
                                    logger.debug(ERROR_PREFIX + "extractInfoFromXML found TooManyProblemsExistException: " + e.getMessage());
                                    continue;
                            } catch (MoreThanOneElementException e) {
                                    //LogUtils.logErr(logger, ERROR_PREFIX, "extractInfoFromXML found MoreThanOneElementException found IOException", e);
                                    logger.debug(ERROR_PREFIX + "extractInfoFromXML found MoreThanOneElementException: " + e.getMessage());
                                    continue;
                            }
                    }
                    if (xmlExtractedDataObject != null && !xmlExtractedDataObject.getContextMessageId().equals(lastContextMessageId)){
                            //logger.info("xmlExtractedDataObject: " + xmlExtractedDataObject);
                            lastContextMessageId = xmlExtractedDataObject.getContextMessageId();
                            lastProblemName = xmlExtractedDataObject.getProblemName();
                    } else if (xmlExtractedDataObject != null && xmlExtractedDataObject.getContextMessageId().equals(lastContextMessageId) &&
                                    xmlExtractedDataObject.getProblemName() != null) {
                            lastProblemName = xmlExtractedDataObject.getProblemName();
                    }
                    if (rowCount == 1) {
                            if (xmlExtractedDataObject == null)
                                    lastObj = transactionItem;
                            else {
                                    lastObj = new OliUserTransactionWithXmlDTO(transactionItem);
                                    ((OliUserTransactionWithXmlDTO)lastObj).setXmlExtractedDataObject(xmlExtractedDataObject);
                            }
                            rowCount++;
                            lastStudent = anonStudentId;
                            continue;
                    } else if (rowCount == 2) {
                            if (xmlExtractedDataObject == null)
                                    currObj = transactionItem;
                            else {
                                    currObj = new OliUserTransactionWithXmlDTO(transactionItem);
                                    ((OliUserTransactionWithXmlDTO)currObj).setXmlExtractedDataObject(xmlExtractedDataObject);
                            }
                            lastObj.calculateNextTimeDiff(currObj);
                            currObj.calculatePrevTimeDiff(lastObj);
                            rowCount++;
                            lastStudent = anonStudentId;
                            continue;
                    } else {
                            if (xmlExtractedDataObject == null)
                                    nextObj = transactionItem;
                            else {
                                    nextObj = new OliUserTransactionWithXmlDTO(transactionItem);
                                    ((OliUserTransactionWithXmlDTO)nextObj).setXmlExtractedDataObject(xmlExtractedDataObject);
                            }
                            
                            currObj.calculateNextTimeDiff(nextObj);
                            nextObj.calculatePrevTimeDiff(currObj);
                            /*
                            logger.info(rowCount + ", lastProcessedObj: " + lastProcessedObj);
                            logger.info(rowCount + ", lastObj: " + lastObj);
                            logger.info(rowCount + ", currObj: " + currObj);
                            logger.info(rowCount + ", nextObj : " + nextObj);
                            */
                            //see if prevObj and currObj can be combined
                            OliResourceUseDTOInterface newDataObject = currObj.combineTwoOLIIntermediateDataObject(lastObj);
                            //logger.info(rowCount + ", after combined : " + newDataObject);
                            if (newDataObject == null) {
                                    //logger.info("Process lastObj: " + lastObj);
                                    callProcessOLIDataObject(lastObj, viewActionDataObject, mediaDataObject);
                            } else {
                                    currObj = newDataObject;
                                    lastObj = lastProcessedObj;
                            }
                            viewActionDataObject.setLastProcessedDataObject(lastObj);
                            mediaDataObject.setLastProcessedDataObject(lastObj);
                            //logger.info("viewActionDataObject: " + viewActionDataObject);
                            //logger.info("mediaDataObject: " + mediaDataObject);
                            rowCount++;
                    }
                    //System.out.println("end of loop, viewActionDataObject : " + viewActionDataObject);
                    //System.out.println("end of loop, mediaDataObject: " + mediaDataObject);
                    //before going to next row, reset objects
                    lastProcessedObj = lastObj;
                    lastObj = currObj;
                    currObj = nextObj;
                    lastStudent = anonStudentId;
            }//end of each transaction row
            
            //process the last two rows
            OliResourceUseDTOInterface newDataObject = null;
            if (currObj != null && lastObj != null) {
                    newDataObject = currObj.combineTwoOLIIntermediateDataObject(lastObj);
            }
            if (newDataObject == null) {
                    if (lastObj != null) {
                            callProcessOLIDataObject(lastObj, viewActionDataObject, mediaDataObject);
                    }
                    if (currObj != null) {
                            callProcessOLIDataObject(currObj, viewActionDataObject, mediaDataObject);
                    }
            } else {
                    callProcessOLIDataObject(newDataObject, viewActionDataObject, mediaDataObject);
            }
            
            logger.info("Total student processed: " + studentCnt);
            if ((allStudentMediaDataObjects != null && allStudentMediaDataObjects.size() != 0)
                            || (allStudentViewActionDataObjects != null && allStudentViewActionDataObjects.size() != 0)) {
                    return outputToString();
            } else
                    throw ResourceUseOliException.noDataFoundException();
    }

    public String outputToString() throws ResourceUseOliException{
            StringBuffer sb = new StringBuffer();
            String newLine = "";
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.indexOf("win") >= 0)
                    newLine = "\r\n";
            else
                    newLine = "\n";
            sb.append("student\taction count\taction time\tpage view count\tpage view time\t" +
                            "action to page view count\taction to page view time\t" +
                            "page view to action count\tpage view to action time\t" +
                            "media play count\tmedia play time\t" +
                            "media play to view page count\tmedia play to view page time\t" +
                            "action to media play count\taction to media play time" + newLine);
            Iterator it = allStudentViewActionDataObjects.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                String student = (String) pair.getKey();
                OLIViewActionDataObject viewActionDataObj = (OLIViewActionDataObject) pair.getValue();
                OLIMediaDataObject mediaDataObj = (OLIMediaDataObject)allStudentMediaDataObjects.get(student);
                if (!viewActionDataObj.dataIsEmpty() || !mediaDataObj.dataIsEmpty()) {
                        sb.append(student + "\t" +
                                viewActionDataObj.getActionCountForDisplay() + "\t" + viewActionDataObj.getActionTimeForDisplay() + "\t" + 
                                viewActionDataObj.getPageViewCountForDisplay() + "\t" + viewActionDataObj.getPageViewTimeForDisplay() + "\t" + 
                                viewActionDataObj.getActionToPageViewCountForDisplay() + "\t" + viewActionDataObj.getActionToPageViewTimeForDisplay() + "\t" + 
                                viewActionDataObj.getPageViewToActionCountForDisplay() + "\t" + viewActionDataObj.getPageViewToActionTimeForDisplay() + "\t" +
                                mediaDataObj.getMediaCountForDisplay() + "\t" + mediaDataObj.getMediaTimeForDisplay() + "\t" + 
                                viewActionDataObj.getEndOfMediaPlayToViewPageCountForDisplay() + "\t" + viewActionDataObj.getEndOfMediaPlayToViewPageTimeForDisplay() + "\t" +
                                mediaDataObj.getActionToMediaPlayCountForDisplay() + "\t" + mediaDataObj.getActionToMediaPlayTimeForDisplay() + newLine);
                }
            }
            return sb.toString();
     }
    
    private void callProcessOLIDataObject (OliResourceUseDTOInterface oliResourceUseDTO,
                                                    OLIViewActionDataObject viewActionDataObject,
                                                    OLIMediaDataObject mediaDataObject) {
            if (oliResourceUseDTO instanceof OliUserTransactionDTO) {
                    OliUserTransactionDTO dtoObj = (OliUserTransactionDTO)oliResourceUseDTO;
                    if (dtoObj.isPlainAction() || dtoObj.isViewPage() || dtoObj.isCombinedViewSaveAttemptAction()) {
                            viewActionDataObject.processOLIDataObject(oliResourceUseDTO);
                    } else if (dtoObj.isPlainMediaAction()) {
                            mediaDataObject.processOLIDataObject(oliResourceUseDTO);
                    }
            } else if (oliResourceUseDTO instanceof OliUserTransactionWithXmlDTO) {
                    OliUserTransactionWithXmlDTO dtoObj = (OliUserTransactionWithXmlDTO)oliResourceUseDTO;
                    if (dtoObj.isXMLAction()) {
                            viewActionDataObject.processOLIDataObject(oliResourceUseDTO);
                    } else if (dtoObj.isXMLMediaAction()) {
                            mediaDataObject.processOLIDataObject(oliResourceUseDTO);
                    }
            }
    }

}