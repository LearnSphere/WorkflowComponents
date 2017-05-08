package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dataobject;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.cmu.pslc.datashop.item.MessageItem;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.AbstractOliResourceUseDTO;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OliResourceUseDTOInterface;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OliUserTransactionDTO;

/**
 *Similar to edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OliUserTransactionDTO
 * except it has XML 
 */

public class OliUserTransactionWithXmlDTO extends AbstractOliResourceUseDTO{
        private XMLExtractedDataObject xmlExtractedDataObject;

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
        
        public OliUserTransactionWithXmlDTO () {}
        public OliUserTransactionWithXmlDTO (OliUserTransactionDTO baseObj) {
                this.resourceUseTransactionId = baseObj.getResourceUseTransactionId();
                this.student = baseObj.getStudent();
                this.session = baseObj.getSession();
                this.action = baseObj.getAction();
                this.info = baseObj.getInfo();
                this.infoType = baseObj.getInfoType();
                this.UTCTime = baseObj.getUTCTime();
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
        
        public boolean isXMLMediaStopAction () {
                if (action.equals(XML_ACTION_STOP) ||
                                action.equals(XML_ACTION_END))
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

        public OliUserTransactionWithXmlDTO combineTwoOLIIntermediateDataObject (OliResourceUseDTOInterface anotherObj) {
                if (!session.equals(anotherObj.getSession()))
                        return null;
                if (!(anotherObj instanceof OliUserTransactionWithXmlDTO))
                        return null;
                OliUserTransactionWithXmlDTO newObj = null;
                OliUserTransactionWithXmlDTO anotherObjCasted = (OliUserTransactionWithXmlDTO)anotherObj;
                if (xmlExtractedDataObject != null && anotherObjCasted.getXmlExtractedDataObject() != null) {
                        if (xmlExtractedDataObject.getTransactionId() != null && anotherObjCasted.getXmlExtractedDataObject().getTransactionId() != null &&
                                        xmlExtractedDataObject.getTransactionId().equals(anotherObjCasted.getXmlExtractedDataObject().getTransactionId()))  {
                                newObj = new OliUserTransactionWithXmlDTO();
                                newObj.setStudent(student);
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
                                if (isToolAttemptOrTutorResult() && anotherObjCasted.isToolAttemptOrTutorResult()) {
                                        newObj.setAction(COMBINED_ATTEMPT_RESULT);
                                        newXMLObj.setSemanticEventName(COMBINED_ATTEMPT_RESULT);
                                } else if (isToolHintRequestOrTutorHintMsg() && anotherObjCasted.isToolHintRequestOrTutorHintMsg()) {
                                        newObj.setAction(COMBINED_HINT_REQUEST_MSG);
                                        newXMLObj.setSemanticEventName(COMBINED_HINT_REQUEST_MSG);
                                } else {
                                        newObj.setAction(action);
                                        newXMLObj.setSemanticEventName(xmlExtractedDataObject.getSemanticEventName());
                                }
                        } //when this is a context with other xml action which is not a context 
                        else if ((isXMLContextMessage() || anotherObjCasted.isXMLContextMessage()) && 
                                                !(isXMLContextMessage() && anotherObjCasted.isXMLContextMessage())){
                                if (!xmlExtractedDataObject.getContextMessageId().equals(anotherObjCasted.getXmlExtractedDataObject().getContextMessageId()))
                                        return null;
                                if (isCombinedXMLAction() || anotherObjCasted.isCombinedXMLAction())
                                        return null;
                                if ((xmlExtractedDataObject.getContextMessageName() != null && xmlExtractedDataObject.getContextMessageName().equals(XML_CONTEXT_MESSAGE_TUTOR_COMPLETE)) ||
                                        (anotherObjCasted.getXmlExtractedDataObject().getContextMessageName() != null && anotherObjCasted.getXmlExtractedDataObject().getContextMessageName().equals(XML_CONTEXT_MESSAGE_TUTOR_COMPLETE)))
                                        return null;
                                OliUserTransactionWithXmlDTO tempObj = isXMLContextMessage() ? anotherObjCasted : this;
                                newObj = new OliUserTransactionWithXmlDTO();
                                newObj.setStudent(tempObj.getStudent());
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
