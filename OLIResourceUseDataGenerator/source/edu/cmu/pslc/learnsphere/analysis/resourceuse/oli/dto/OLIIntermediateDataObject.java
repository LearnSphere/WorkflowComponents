package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.cmu.pslc.datashop.item.MessageItem;

/**
 * Represent an OLI intermediate data object. Fields are
 *      OLI_log_act_id
 *      Course
 *      Student
 *      Session
 *      Time (UTC time zone)
 *      Action
 *      Page
 *      prevTime
 *      nextTime
 *      context_message_id (when info has tutor message xml)
 *      transaction_id (info has tutor message xml)
 *      semantic_event (info has tutor message xml)
 *      xml_action (info has tutor message xml)
 *      media_file (info has tutor message xml)
 *      problem (info has tutor message xml)
 * 
 *  
 */

public class OLIIntermediateDataObject{
        private long resourceUseTransactionId;
        private String course;
        private String student;
        private String session;
        private Date UTCTime;
        private String action;
        private String info;
        private String infoType;
        private Integer prevTimeDiff;
        private Integer nextTimeDiff;
        private XMLExtractedDataObject xmlExtractedDataObject;

        public static String ACTION_VIEW_PAGE = "VIEW_PAGE";
        public static String ACTION_VIEW_MODULE_PAGE = "VIEW_MODULE_PAGE";
        public static String ACTION_START_SESSION = "START_SESSION";
        public static String ACTION_START_ATTEMPT = "START_ATTEMPT";
        public static String ACTION_EVALUATE_QUESTION = "EVALUATE_QUESTION";
        public static String ACTION_VIEW_HINT = "VIEW_HINT";
        public static String ACTION_TUTOR_HINT_MSG = "TUTOR_ACTION HINT_MSG";
        public static String ACTION_TOOL_HINT_REQUEST = "TOOL_ACTION HINT_REQUEST";
        public static String ACTION_TOOL = "TOOL_ACTION";
        public static String ACTION_TUTOR = "TUTOR_ACTION";
        public static String ACTION_TUTOR_RESULT = "TUTOR_ACTION RESULT";
        public static String ACTION_TOOL_ATTEMPT = "TOOL_ACTION ATTEMPT";
        
        public static String ACTION_CONTEXT_ACTION_LOAD_AUDIO = "CONTEXT_ACTION LOAD_AUDIO";
        public static String ACTION_CONTEXT_ACTION_LOAD_TUTOR = "CONTEXT_ACTION LOAD_TUTOR";
        public static String ACTION_CONTEXT_ACTION_LOAD_VIDEO = "CONTEXT_ACTION LOAD_VIDEO";
        
        public static String COMBINED_ATTEMPT_RESULT = "COMBINED_ATTEMPT_RESULT";
        public static String COMBINED_HINT_REQUEST_MSG = "COMBINED_HINT_REQUEST_MSG";
        public static String COMBINED_CONTEXT_ATTEMPT_RESULT = "COMBINED_CONTEXT_ATTEMPT_RESULT";
        public static String COMBINED_CONTEXT_HINT_REQUEST_MSG = "COMBINED_CONTEXT_HINT_REQUEST_MSG";
        public static String COMBINED_CONTEXT_MEDIA_PLAY = "COMBINED_CONTEXT_MEDIA_PLAY";
        public static String COMBINED_START_SESSION_ATTEMPT = "COMBINED_START_SESSION_ATTEMPT";
        
        public static String ACTION_PLAY = "PLAY";
        public static String ACTION_MUTE = "MUTE";
        public static String ACTION_UNMUTE = "UNMUTE";
        public static String ACTION_STOP = "STOP";
        public static String ACTION_END = "END";
        public static String ACTION_TOOL_ACTION_VIDEO = "TOOL_ACTION VIDEO_ACTION";
        public static String ACTION_TOOL_ACTION_AUDIO = "TOOL_ACTION AUDIO_ACTION";
        
        public static String INFOTYPE_FILE = "file_name";
        public static String INFOTYPE_TUTOR_MESSAGE = "tutor_message.dtd";
        public static String INFOTYPE_TUTOR_MESSAGE_V2 = "tutor_message_v2.dtd";
        public static String INFOTYPE_XML = "xml";
        
        public static String SEMANTIC_EVENT_AUDIO = "AUDIO_ACTION";
        public static String SEMANTIC_EVENT_VIDEO = "VIDEO_ACTION";
        public static String SEMANTIC_ATTEMPT = "ATTEMPT";
        public static String SEMANTIC_HINT_MSG = "HINT_MSG";
        public static String SEMANTIC_HINT_REQUEST = "HINT_REQUEST";
        public static String SEMANTIC_RESULT = "RESULT";

        public static String XML_ACTION_PLAY = "play";
        public static String XML_ACTION_END = "end";
        public static String XML_ACTION_STOP = "stop";
        public static String XML_ACTION_PAUSE = "pause";
        public static String XML_ACTION_MUTE = "mute";
        public static String XML_ACTION_UNMUTE = "unmute";
        
        public static String XML_CONTEXT_MESSAGE_TUTOR_COMPLETE = "TUTOR_COMPLETE";
        public static String XML_SELECTION_TYPE_MEDIA_FILE = "media_file";
        
        private String javaDateFormatStr = "yyyy-MM-dd HH:mm:ss";
        private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(javaDateFormatStr);
        
        public OLIIntermediateDataObject () {}
        
        public void setResourceUseTransactionId (long id) {
                this.resourceUseTransactionId = id;
        }
        public long getResourceUseTransactionId () {
                return this.resourceUseTransactionId;
        }
        
        public void setCourse (String course) {
                this.course = course;
        }
        public String getCourse () {
                return this.course;
        }
        
        public void setStudent (String student) {
                this.student = student;
        }
        public String getStudent () {
                return this.student;
        }
        
        public void setSession (String session) {
                this.session = session;
        }
        public String getSession () {
                return this.session;
        }
        
        public void setUTCTime (Date time) {
                this.UTCTime = time;
        }
        public Date getUTCTime () {
                return this.UTCTime;
        }
        
        public void setAction (String action) {
                this.action = action;
        }
        public String getAction () {
                return this.action;
        }
        
        public void setInfo (String info) {
                this.info = info;
        }
        public String getInfo () {
                return this.info;
        }
        
        public void setInfoType (String infoType) {
                this.infoType = infoType;
        }
        public String getInfoType () {
                return this.infoType;
        }
        
        public void setPrevTimeDiff (Integer prevTime) {
                this.prevTimeDiff = prevTime;
        }
        public Integer getPrevTimeDiff () {
                return this.prevTimeDiff;
        }
        public void calculatePrevTimeDiff (OLIIntermediateDataObject prevObj) {
                if (UTCTime != null && prevObj.getUTCTime() != null && this.session.equals(prevObj.getSession())) {
                        long timeDiffInMilliseconds = this.UTCTime.getTime() - prevObj.getUTCTime().getTime();
                        this.prevTimeDiff = (int)(timeDiffInMilliseconds/1000);
                } else
                        this.prevTimeDiff = null;
        }
        
        public void setNextTimeDiff (Integer nextTime) {
                this.nextTimeDiff = nextTime;
        }
        public Integer getNextTimeDiff () {
                return this.nextTimeDiff;
        }
        public void calculateNextTimeDiff (OLIIntermediateDataObject nextObj) {
                if (UTCTime != null && nextObj.getUTCTime() != null && this.session.equals(nextObj.getSession())) {
                        long timeDiffInMilliseconds = nextObj.getUTCTime().getTime() - this.UTCTime.getTime();
                        this.nextTimeDiff = (int)(timeDiffInMilliseconds/1000);
                } else
                        this.nextTimeDiff = null;
        }
        
        public void setXmlExtractedDataObject (XMLExtractedDataObject xmlExtractedDataObject) {
                this.xmlExtractedDataObject = xmlExtractedDataObject;
        }
        public XMLExtractedDataObject getXmlExtractedDataObject () {
                return this.xmlExtractedDataObject;
        }
        
        public String getInfoFileName () {
                if (infoType.equals(INFOTYPE_FILE))
                        return info;
                else
                        return null;
        }
                
        public boolean isViewPage () {
                if (action.equals(ACTION_VIEW_PAGE) ||
                                action.equals(ACTION_VIEW_MODULE_PAGE))
                        return true;
                else
                        return false;
        }
        
        public boolean isPlainAction () {
                if (action.equals(ACTION_START_SESSION) ||
                                action.equals(ACTION_START_ATTEMPT) ||
                                action.equals(ACTION_EVALUATE_QUESTION) ||
                                action.equals(ACTION_VIEW_HINT) ||
                                action.equals(COMBINED_START_SESSION_ATTEMPT))
                        return true;
                else
                        return false;
        }
        
        public boolean isPlainActionStartSessionOrAttempt () {
                if (action.equals(ACTION_START_SESSION) ||
                                action.equals(ACTION_START_ATTEMPT) ||
                                action.equals(COMBINED_START_SESSION_ATTEMPT))
                        return true;
                else
                        return false;
        }
        
        public boolean isPlainMediaAction () {
                if (action.equals(ACTION_PLAY) ||
                                action.equals(ACTION_MUTE) ||
                                action.equals(ACTION_UNMUTE))
                        return true;
                else
                        return false;
        }
        
        public boolean isPlainMediaStopAction () {
                if (action.equals(ACTION_STOP) ||
                                action.equals(ACTION_END))
                        return true;
                else
                        return false;
        }
        
        public boolean isXMLMediaStopAction () {
                if (action.equals(XML_ACTION_STOP) ||
                                action.equals(XML_ACTION_END))
                        return true;
                else
                        return false;
        }
        
        public boolean isPlainMediaPlayAction () {
                if (action.equals(ACTION_PLAY))
                        return true;
                else
                        return false;
        }
        
        public boolean isXMLMediaAction () {
                if (action.equals(ACTION_TOOL) ||
                                action.equals(ACTION_TOOL_ACTION_AUDIO) ||
                                action.equals(ACTION_TOOL_ACTION_VIDEO) ||
                                action.equals(COMBINED_CONTEXT_MEDIA_PLAY)) {
                        if (xmlExtractedDataObject != null && xmlExtractedDataObject.getSemanticEventName() != null &&
                                        (xmlExtractedDataObject.getSemanticEventName().equals(SEMANTIC_EVENT_AUDIO) ||
                                                        xmlExtractedDataObject.getSemanticEventName().equals(SEMANTIC_EVENT_VIDEO) ||
                                                        xmlExtractedDataObject.getSemanticEventName().equals(COMBINED_CONTEXT_MEDIA_PLAY)))
                                return true;
                        else
                                return false;
                } else
                        return false;
        }
        
        public boolean isCombinedXMLAction() {
                if (action.equals(COMBINED_ATTEMPT_RESULT) ||
                                action.equals(COMBINED_HINT_REQUEST_MSG) ||
                                action.equals(COMBINED_CONTEXT_ATTEMPT_RESULT) ||
                                action.equals(COMBINED_CONTEXT_HINT_REQUEST_MSG) ||
                                action.equals(COMBINED_CONTEXT_MEDIA_PLAY))
                        return true;
                else
                        return false;
        }
        
        public boolean isMediaPlayRecordableActionForXML () {
                if (xmlExtractedDataObject != null && xmlExtractedDataObject.getAction() != null && 
                                (xmlExtractedDataObject.getAction().equals(XML_ACTION_PLAY) ||
                                xmlExtractedDataObject.getAction().equals(XML_ACTION_MUTE) ||
                                xmlExtractedDataObject.getAction().equals(XML_ACTION_UNMUTE))) {
                        return true;
                } else
                        return false;
        }
        
        public boolean isMediaPlayActionForXML () {
                if (xmlExtractedDataObject != null && xmlExtractedDataObject.getAction() != null && 
                                xmlExtractedDataObject.getAction().equals(XML_ACTION_PLAY)) {
                        return true;
                } else
                        return false;
        }

        public boolean isXMLContextMessage () {
                if (action.equals(ACTION_CONTEXT_ACTION_LOAD_TUTOR) ||
                                action.equals(ACTION_CONTEXT_ACTION_LOAD_VIDEO) ||
                                action.equals(ACTION_CONTEXT_ACTION_LOAD_AUDIO) ||
                                action.equals(ACTION_TOOL) ||
                                action.equals(ACTION_TUTOR)) {
                        if (xmlExtractedDataObject != null &&
                                        xmlExtractedDataObject.getMessageType().equals(MessageItem.MSG_TYPE_CONTEXT))
                                return true;
                }
                return false;
        }
        
        public boolean isXMLAction () {
                if (action.equals(ACTION_TUTOR_HINT_MSG) ||
                                action.equals(ACTION_TOOL_HINT_REQUEST) ||
                                action.equals(ACTION_TOOL) ||
                                action.equals(ACTION_TUTOR) || 
                                action.equals(ACTION_TUTOR_RESULT) ||
                                action.equals(ACTION_TOOL_ATTEMPT) ||
                                action.equals(COMBINED_ATTEMPT_RESULT) ||
                                action.equals(COMBINED_HINT_REQUEST_MSG) ||
                                action.equals(COMBINED_CONTEXT_ATTEMPT_RESULT) ||
                                action.equals(COMBINED_CONTEXT_HINT_REQUEST_MSG)) {
                        if (xmlExtractedDataObject != null && xmlExtractedDataObject.getSemanticEventName() != null &&
                                        (xmlExtractedDataObject.getSemanticEventName().equals(SEMANTIC_ATTEMPT) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(SEMANTIC_RESULT) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(SEMANTIC_HINT_MSG) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(SEMANTIC_HINT_REQUEST) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(COMBINED_ATTEMPT_RESULT) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(COMBINED_HINT_REQUEST_MSG) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(COMBINED_CONTEXT_ATTEMPT_RESULT) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(COMBINED_CONTEXT_HINT_REQUEST_MSG)))
                                return true;
                        else
                                return false;
                } else
                        return false;
        }
        
        public boolean isToolAttemptOrTutorResult() {
                if (action.equals(ACTION_TOOL) ||
                                action.equals(ACTION_TUTOR) || 
                                action.equals(ACTION_TUTOR_RESULT) ||
                                action.equals(ACTION_TOOL_ATTEMPT) ||
                                action.equals(COMBINED_ATTEMPT_RESULT) ||
                                action.equals(COMBINED_CONTEXT_ATTEMPT_RESULT)) {
                        if (xmlExtractedDataObject != null && xmlExtractedDataObject.getSemanticEventName() != null &&
                                        (xmlExtractedDataObject.getSemanticEventName().equals(SEMANTIC_ATTEMPT) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(SEMANTIC_RESULT) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(COMBINED_ATTEMPT_RESULT) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(COMBINED_CONTEXT_ATTEMPT_RESULT)))
                                return true;
                        else
                                return false;
                } else
                        return false;
        }
        
        public boolean isToolHintRequestOrTutorHintMsg() {
                if (action.equals(ACTION_TOOL) ||
                                action.equals(ACTION_TUTOR) ||
                                action.equals(ACTION_TUTOR_HINT_MSG) ||
                                action.equals(ACTION_TOOL_HINT_REQUEST) ||
                                action.equals(COMBINED_HINT_REQUEST_MSG) ||
                                action.equals(COMBINED_CONTEXT_HINT_REQUEST_MSG)) {
                        if (xmlExtractedDataObject != null && xmlExtractedDataObject.getSemanticEventName() != null &&
                                        (xmlExtractedDataObject.getSemanticEventName().equals(SEMANTIC_HINT_MSG) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(SEMANTIC_HINT_REQUEST) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(COMBINED_HINT_REQUEST_MSG) ||
                                         xmlExtractedDataObject.getSemanticEventName().equals(COMBINED_CONTEXT_HINT_REQUEST_MSG)))
                                return true;
                        else
                                return false;
                } else
                        return false;
        }
        
        //when action field in log_act is one of the tutor message info
        public static boolean actionIsTutorMessage (String actionStr) {
                if (actionStr.equals(ACTION_TUTOR_HINT_MSG) ||
                                actionStr.equals(ACTION_TOOL_HINT_REQUEST) ||
                                actionStr.equals(ACTION_TOOL) ||
                                actionStr.equals(ACTION_TUTOR) ||
                                actionStr.equals(ACTION_TUTOR_RESULT) ||
                                actionStr.equals(ACTION_TOOL_ATTEMPT) ||
                                actionStr.equals(ACTION_CONTEXT_ACTION_LOAD_AUDIO) ||
                                actionStr.equals(ACTION_CONTEXT_ACTION_LOAD_TUTOR) ||
                                actionStr.equals(ACTION_CONTEXT_ACTION_LOAD_VIDEO) ||
                                actionStr.equals(ACTION_TOOL_ACTION_VIDEO) ||
                                actionStr.equals(ACTION_TOOL_ACTION_AUDIO))
                        return true;
                else return false;
        }
        
        public static boolean infoTypeIsTutorMessage (String infoTypeStr) {
                if (infoTypeStr.equals(INFOTYPE_TUTOR_MESSAGE) || infoTypeStr.equals(INFOTYPE_TUTOR_MESSAGE_V2) || infoTypeStr.equals(INFOTYPE_XML))
                        return true;
                else
                        return false;
        }

        public OLIIntermediateDataObject combineTwoOLIIntermediateDataObject (OLIIntermediateDataObject anotherObj) {
                if (!session.equals(anotherObj.getSession())) {
                        return null;
                } else {
                        OLIIntermediateDataObject newObj = null;
                        //combine Start_attempt and start_session 
                        if (isPlainActionStartSessionOrAttempt() && anotherObj.isPlainActionStartSessionOrAttempt()
                                        && getUTCTime().equals(anotherObj.getUTCTime()) && getInfo().equals(anotherObj.info)) {
                                newObj = new OLIIntermediateDataObject();
                                newObj.setStudent(student);
                                newObj.setCourse(course);
                                newObj.setSession(session);
                                newObj.setUTCTime(UTCTime);
                                newObj.setInfo(info);
                                newObj.setInfoType(infoType);
                                newObj.setAction(COMBINED_START_SESSION_ATTEMPT);
                        }//when this is a pair of hint_request/hint_msg or attempt/attempt
                        else if (xmlExtractedDataObject != null && anotherObj.getXmlExtractedDataObject() != null) {
                                if (xmlExtractedDataObject.getTransactionId() != null && anotherObj.getXmlExtractedDataObject().getTransactionId() != null &&
                                                xmlExtractedDataObject.getTransactionId().equals(anotherObj.getXmlExtractedDataObject().getTransactionId()))  {
                                        newObj = new OLIIntermediateDataObject();
                                        newObj.setStudent(student);
                                        newObj.setCourse(course);
                                        newObj.setSession(session);
                                        newObj.setUTCTime(UTCTime);
                                        newObj.setInfo(info);
                                        newObj.setInfoType(infoType);
                                        XMLExtractedDataObject newXMLObj = new XMLExtractedDataObject();
                                        newObj.setXmlExtractedDataObject(newXMLObj);
                                        newXMLObj.setContextMessageId(xmlExtractedDataObject.getContextMessageId());
                                        newXMLObj.setTransactionId(xmlExtractedDataObject.getTransactionId());
                                        newXMLObj.setMediaFileName(xmlExtractedDataObject.getMediaFileName());
                                        newXMLObj.setProblemName(xmlExtractedDataObject.getProblemName());
                                        newXMLObj.setMessageType(MessageItem.MSG_TYPE_TOOL);
                                        newXMLObj.setAction(xmlExtractedDataObject.getAction());
                                        if (isToolAttemptOrTutorResult() && anotherObj.isToolAttemptOrTutorResult()) {
                                                newObj.setAction(COMBINED_ATTEMPT_RESULT);
                                                newXMLObj.setSemanticEventName(COMBINED_ATTEMPT_RESULT);
                                        } else if (isToolHintRequestOrTutorHintMsg() && anotherObj.isToolHintRequestOrTutorHintMsg()) {
                                                newObj.setAction(COMBINED_HINT_REQUEST_MSG);
                                                newXMLObj.setSemanticEventName(COMBINED_HINT_REQUEST_MSG);
                                        } else {
                                                newObj.setAction(action);
                                                newXMLObj.setSemanticEventName(xmlExtractedDataObject.getSemanticEventName());
                                        }
                                } //when this is a context with other xml action which is not a context 
                                else if ((isXMLContextMessage() || anotherObj.isXMLContextMessage()) && 
                                                !(isXMLContextMessage() && anotherObj.isXMLContextMessage())){
                                        if (!xmlExtractedDataObject.getContextMessageId().equals(anotherObj.getXmlExtractedDataObject().getContextMessageId()))
                                                return null;
                                        if (isCombinedXMLAction() || anotherObj.isCombinedXMLAction())
                                                return null;
                                        if ((xmlExtractedDataObject.getContextMessageName() != null && xmlExtractedDataObject.getContextMessageName().equals(XML_CONTEXT_MESSAGE_TUTOR_COMPLETE)) ||
                                                        (anotherObj.getXmlExtractedDataObject().getContextMessageName() != null && anotherObj.getXmlExtractedDataObject().getContextMessageName().equals(XML_CONTEXT_MESSAGE_TUTOR_COMPLETE)))
                                                return null;
                                        OLIIntermediateDataObject tempObj = isXMLContextMessage() ? anotherObj : this;
                                        newObj = new OLIIntermediateDataObject();
                                        newObj.setStudent(tempObj.getStudent());
                                        newObj.setCourse(tempObj.getCourse());
                                        newObj.setSession(tempObj.getSession());
                                        newObj.setUTCTime(tempObj.getUTCTime());
                                        newObj.setInfo(tempObj.getInfo());
                                        newObj.setInfoType(tempObj.getInfoType());
                                        XMLExtractedDataObject newXMLObj = new XMLExtractedDataObject();
                                        newObj.setXmlExtractedDataObject(newXMLObj);
                                        newXMLObj.setContextMessageId(tempObj.getXmlExtractedDataObject().getContextMessageId());
                                        newXMLObj.setTransactionId(tempObj.getXmlExtractedDataObject().getTransactionId());
                                        newXMLObj.setMediaFileName(tempObj.getXmlExtractedDataObject().getMediaFileName());
                                        newXMLObj.setProblemName(tempObj.getXmlExtractedDataObject().getProblemName());
                                        newXMLObj.setMessageType(MessageItem.MSG_TYPE_TOOL);
                                        newXMLObj.setAction(tempObj.getXmlExtractedDataObject().getAction());
                                        if (tempObj.isToolAttemptOrTutorResult()) {
                                                newObj.setAction(COMBINED_CONTEXT_ATTEMPT_RESULT);
                                                newXMLObj.setSemanticEventName(COMBINED_CONTEXT_ATTEMPT_RESULT);
                                        } else if (tempObj.isToolHintRequestOrTutorHintMsg()) {
                                                newObj.setAction(COMBINED_HINT_REQUEST_MSG);
                                                newXMLObj.setSemanticEventName(COMBINED_HINT_REQUEST_MSG);
                                        } else if (tempObj.isXMLMediaAction()) {
                                                newObj.setAction(COMBINED_CONTEXT_MEDIA_PLAY);
                                                newXMLObj.setSemanticEventName(COMBINED_CONTEXT_MEDIA_PLAY);
                                        } else {
                                                newObj.setAction(tempObj.getAction());
                                                newXMLObj.setSemanticEventName(tempObj.getXmlExtractedDataObject().getSemanticEventName());
                                        }
                                }
                        }
                        if (newObj != null) {
                                if (getPrevTimeDiff() == null || anotherObj.getPrevTimeDiff() == null) {
                                        if (getPrevTimeDiff() != null)
                                                newObj.setPrevTimeDiff(getPrevTimeDiff());
                                        else if (anotherObj.getPrevTimeDiff() != null)
                                                newObj.setPrevTimeDiff(anotherObj.getPrevTimeDiff());
                                } else {
                                        newObj.setPrevTimeDiff(getPrevTimeDiff() >= anotherObj.getPrevTimeDiff() ? getPrevTimeDiff() : anotherObj.getPrevTimeDiff());
                                }
                                if (getNextTimeDiff() == null || anotherObj.getNextTimeDiff() == null) {
                                        if (getNextTimeDiff() != null)
                                                newObj.setNextTimeDiff(getNextTimeDiff());
                                        else if (anotherObj.getNextTimeDiff() != null)
                                                newObj.setNextTimeDiff(anotherObj.getNextTimeDiff());
                                } else {
                                        newObj.setNextTimeDiff(getNextTimeDiff() >= anotherObj.getNextTimeDiff() ? getNextTimeDiff() : anotherObj.getNextTimeDiff());
                                }
                        }
                        return newObj;
                }
        }
        
        public String toString() {
                StringBuffer sb = new StringBuffer();
                sb.append("resourceUseTransactionId: " + resourceUseTransactionId + "\t");
                sb.append("course: " + course + "\t");
                sb.append("student: " + student + "\t");
                sb.append("session: " + session + "\t");
                sb.append("UTCTime: " + simpleDateFormat.format(UTCTime) + "\t");
                sb.append("action: " + action + "\t");
                sb.append("info: " + info + "\t");
                sb.append("infoType: " + infoType + "\t");
                sb.append("prevTimeDiff: " + prevTimeDiff + "\t");
                sb.append("nextTimeDiff: " + nextTimeDiff + "\t");
                sb.append("XMLExtractedDataObject: " + xmlExtractedDataObject);
                return sb.toString();
        }
}
