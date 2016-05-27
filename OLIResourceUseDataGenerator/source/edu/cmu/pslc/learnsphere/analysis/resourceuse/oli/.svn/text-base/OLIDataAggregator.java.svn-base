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
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OLIIntermediateDataObject;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OLIMediaDataObject;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OLIViewActionDataObject;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.XMLExtractedDataObject;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.XMLExtractedDataObject.MoreThanOneElementException;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.XMLExtractedDataObject.TooManyProblemsExistException;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.item.ResourceUseOliTransactionItem;

/**
 * This class is the driver for the aggregation of OLI log_act data. It does the following:
 * 1. get all unique students from resource_use_oli_user_sess table for a resource_use_oli_transaction_file
 * 2. for each student, get data from resource_use_oli_tansaction table, ordered by time
 * 3. for each student, assign one OLIViewActionDataObject and one OLIMediaDataObject
 * 4. for each row of data from resource_use_oli_transaction, convert it to OLIIntermediateDataObject by
 *      computing prevTime (null if new session starts) and nextTime, extracting data
 *      tutor_message XML, and combining rows with the same transaction id into one row
 * 5. pass the resulting OLIIntermediateDataObject to OILViewActionDataObject or OLIMediaDataObject 
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
    
    private List<String> uniqueStudents;
    private HashMap<String, OLIViewActionDataObject> allStudentViewActionDataObjects;
    private HashMap<String, OLIMediaDataObject> allStudentMediaDataObjects;
    private String DEFAULT_OUTPUT_FILE = "OLIStudentResourceUse.txt";
    private String outputFileName = DEFAULT_OUTPUT_FILE;
    
    private ResourceUseOliHelper helper;

    public OLIDataAggregator() {
            helper = new ResourceUseOliHelper();
            uniqueStudents = new ArrayList<String>();
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
            //get the all related students
            uniqueStudents = helper.getUniqueStudents(resourceUseOliUserSessFileId, resourceUseOliTransactionFileId);
            if (uniqueStudents == null || uniqueStudents.size() == 0) {
                    if (resourceUseOliUserSessFileId != null)
                            throw ResourceUseOliException.noUserSessionFoundViaUserSessFileException(resourceUseOliUserSessFileId);
                    else if (resourceUseOliTransactionFileId != null)
                            throw ResourceUseOliException.noUserSessionFoundViaTransactionFileException(resourceUseOliTransactionFileId);
            }
            //process data for each student
            for (String anonStudentId : uniqueStudents) {
                    getStudentData(anonStudentId);
            }
            if ((allStudentMediaDataObjects != null && allStudentMediaDataObjects.size() != 0)
                            || (allStudentViewActionDataObjects != null && allStudentViewActionDataObjects.size() != 0))
                    return outputToString();
            else
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

    private void getStudentData(String anonStudentId) {
            ResourceUseOliTransactionDao resourceUseOliTransactionDao = DaoFactory.DEFAULT.getResourceUseOliTransactionDao();
            List<ResourceUseOliTransactionItem> studentTransactions = resourceUseOliTransactionDao.getTransactionByAnonStudentId(resourceUseOliTransactionFileId, anonStudentId);
           
            OLIViewActionDataObject viewActionDataObject = new OLIViewActionDataObject();
            allStudentViewActionDataObjects.put(anonStudentId, viewActionDataObject);
            viewActionDataObject.setStudent(anonStudentId);
            OLIMediaDataObject mediaDataObject = new OLIMediaDataObject();
            allStudentMediaDataObjects.put(anonStudentId, mediaDataObject);
            mediaDataObject.setStudent(anonStudentId);
            OLIIntermediateDataObject lastObj = null;
            OLIIntermediateDataObject currObj = null;
            OLIIntermediateDataObject nextObj = null;
            OLIIntermediateDataObject lastProcessedObj = null;
            int rowCount = 1;
            String lastContextMessageId = null;
            String lastProblemName = null;
            
            for (ResourceUseOliTransactionItem transactionItem : studentTransactions) {
                        long thisTransactionId = (Long)transactionItem.getId();
                        String thisSession = transactionItem.getUserSess();
                        Date thisTime = transactionItem.getTransactionTime();
                        String thisAction = transactionItem.getAction();
                        String thisInfoType = transactionItem.getInfoType();
                        String thisInfo = transactionItem.getInfo();
                        //process xml and get xml data
                        XMLExtractedDataObject xmlExtractedDataObject = null;
                        if (OLIIntermediateDataObject.actionIsTutorMessage(thisAction) && OLIIntermediateDataObject.infoTypeIsTutorMessage(thisInfoType)) {
                                try {
                                        xmlExtractedDataObject = new XMLExtractedDataObject();
                                        xmlExtractedDataObject.setResourceUseTransactionId(thisTransactionId);
                                        xmlExtractedDataObject.initiate(thisInfo);
                                } catch (JDOMException e) {
                                        //LogUtils.logErr(logger, ERROR_PREFIX, "extractInfoFromXML found JDOMException", e);
                                        continue;
                                } catch (IOException e) {
                                        //LogUtils.logErr(logger, ERROR_PREFIX, "extractInfoFromXML found IOException", e);
                                        continue;
                                } catch (TooManyProblemsExistException e) {
                                        //LogUtils.logErr(logger, ERROR_PREFIX, "extractInfoFromXML found TooManyProblemsExistException found IOException", e);
                                        continue;
                                } catch (MoreThanOneElementException e) {
                                        //LogUtils.logErr(logger, ERROR_PREFIX, "extractInfoFromXML found MoreThanOneElementException found IOException", e);
                                        continue;
                                }
                        }
                        //boolean resourceFound = resourceFoundInCourse(thisInfo, thisInfoType, xmlExtractedDataObject, lastContextMessageId, lastProblemName);
                        if (xmlExtractedDataObject != null && !xmlExtractedDataObject.getContextMessageId().equals(lastContextMessageId)){
                                lastContextMessageId = xmlExtractedDataObject.getContextMessageId();
                                lastProblemName = xmlExtractedDataObject.getProblemName();
                        } else if (xmlExtractedDataObject != null && xmlExtractedDataObject.getContextMessageId().equals(lastContextMessageId) &&
                                        xmlExtractedDataObject.getProblemName() != null) {
                                lastProblemName = xmlExtractedDataObject.getProblemName();
                        }
                        /*if (!resourceFound)
                                continue;*/
                                
                        if (rowCount == 1) {
                                lastObj = new OLIIntermediateDataObject();
                                lastObj.setResourceUseTransactionId(thisTransactionId);
                                lastObj.setStudent(anonStudentId);
                                lastObj.setSession(thisSession);
                                lastObj.setUTCTime(thisTime);
                                lastObj.setAction(thisAction);
                                lastObj.setInfoType(thisInfoType);
                                if (xmlExtractedDataObject == null)
                                        lastObj.setInfo(thisInfo);
                                else {
                                        lastObj.setXmlExtractedDataObject(xmlExtractedDataObject);
                                }
                                
                                rowCount++;
                                continue;
                        } else if (rowCount == 2) {
                                currObj = new OLIIntermediateDataObject();
                                currObj.setResourceUseTransactionId(thisTransactionId);
                                currObj.setStudent(anonStudentId);
                                currObj.setSession(thisSession);
                                currObj.setUTCTime(thisTime);
                                currObj.setAction(thisAction);
                                currObj.setInfoType(thisInfoType);
                                if (xmlExtractedDataObject == null)
                                        currObj.setInfo(thisInfo);
                                else {
                                        currObj.setXmlExtractedDataObject(xmlExtractedDataObject);
                                }
                                lastObj.calculateNextTimeDiff(currObj);
                                currObj.calculatePrevTimeDiff(lastObj);
                                rowCount++;
                                continue;
                        } else {
                                nextObj = new OLIIntermediateDataObject();
                                nextObj.setResourceUseTransactionId(thisTransactionId);
                                nextObj.setStudent(anonStudentId);
                                nextObj.setSession(thisSession);
                                nextObj.setUTCTime(thisTime);
                                nextObj.setAction(thisAction);
                                nextObj.setInfoType(thisInfoType);
                                if (xmlExtractedDataObject == null)
                                        nextObj.setInfo(thisInfo);
                                else {
                                        nextObj.setXmlExtractedDataObject(xmlExtractedDataObject);
                                }
                                currObj.calculateNextTimeDiff(nextObj);
                                nextObj.calculatePrevTimeDiff(currObj);
                                //System.out.println(rowCount + ", lastProcessedObj: " + lastProcessedObj);
                                //System.out.println(rowCount + ", lastObj: " + lastObj);
                                //System.out.println(rowCount + ", currObj: " + currObj);
                                //System.out.println(rowCount + ", nextObj : " + nextObj);
                                //see if prevObj and currObj can be combined
                                OLIIntermediateDataObject newOLIIntermediateDataObject = currObj.combineTwoOLIIntermediateDataObject(lastObj);
                                //System.out.println(rowCount + ", after combined : " + newOLIIntermediateDataObject);
                                if (newOLIIntermediateDataObject == null) {
                                        callProcessOLIIntermediateDataObject(lastObj, viewActionDataObject, mediaDataObject);
                                } else {
                                        currObj = newOLIIntermediateDataObject;
                                        lastObj = lastProcessedObj;
                                }
                                viewActionDataObject.setLastProcessedOLIIntermediateDataObject(lastObj);
                                mediaDataObject.setLastProcessedOLIIntermediateDataObject(lastObj);
                                rowCount++;
                                
                        }
                        //System.out.println("end of loop, viewActionDataObject : " + viewActionDataObject);
                        //System.out.println("end of loop, mediaDataObject: " + mediaDataObject);
                        //before going to next row, reset objects
                        lastProcessedObj = lastObj;
                        lastObj = currObj;
                        currObj = nextObj;
                }
                //process the last two
                OLIIntermediateDataObject newOLIIntermediateDataObject = null;
                if (currObj != null)
                        currObj.combineTwoOLIIntermediateDataObject(lastObj);
                if (newOLIIntermediateDataObject == null) {
                        if (lastObj != null)
                                callProcessOLIIntermediateDataObject(lastObj, viewActionDataObject, mediaDataObject);
                        if (currObj != null)
                                callProcessOLIIntermediateDataObject(currObj, viewActionDataObject, mediaDataObject);
                } else {
                        callProcessOLIIntermediateDataObject(newOLIIntermediateDataObject, viewActionDataObject, mediaDataObject);
                }
            
    }
    
    private void callProcessOLIIntermediateDataObject (OLIIntermediateDataObject oliIntermediateDataObject,
                                                    OLIViewActionDataObject viewActionDataObject,
                                                    OLIMediaDataObject mediaDataObject) {
            if (oliIntermediateDataObject.isPlainAction() ||
                            oliIntermediateDataObject.isXMLAction() ||
                            oliIntermediateDataObject.isViewPage()) {
                    viewActionDataObject.processOLIIntermediateDataObject(oliIntermediateDataObject);
            } else if (oliIntermediateDataObject.isPlainMediaAction() ||
                            oliIntermediateDataObject.isXMLMediaAction()) {
                    mediaDataObject.processOLIIntermediateDataObject(oliIntermediateDataObject);
            }
    }

}