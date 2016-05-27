package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.cmu.pslc.datashop.item.ActionItem;
import edu.cmu.pslc.datashop.item.AttemptActionItem;
import edu.cmu.pslc.datashop.item.AttemptSelectionItem;
import edu.cmu.pslc.datashop.item.DatasetLevelItem;
import edu.cmu.pslc.datashop.item.MessageItem;
import edu.cmu.pslc.datashop.item.ProblemItem;
import edu.cmu.pslc.datashop.item.SelectionItem;
import edu.cmu.pslc.datashop.xml.ContextMessage;
import edu.cmu.pslc.datashop.xml.ContextMessageParser;
import edu.cmu.pslc.datashop.xml.ContextMessageParserFactory;
import edu.cmu.pslc.datashop.xml.EventDescriptor;
import edu.cmu.pslc.datashop.xml.SemanticEvent;
import edu.cmu.pslc.datashop.xml.ToolMessage;
import edu.cmu.pslc.datashop.xml.ToolMessageParser;
import edu.cmu.pslc.datashop.xml.ToolMessageParserFactory;
import edu.cmu.pslc.datashop.xml.TutorMessage;
import edu.cmu.pslc.datashop.xml.TutorMessageParser;
import edu.cmu.pslc.datashop.xml.TutorMessageParserFactory;
import edu.cmu.pslc.datashop.xml.XmlParser;
import edu.cmu.pslc.datashop.xml.XmlParserFactory;

/**
 * Represent fields extracted from tutor_message xml. Fields are
 *      log_act_id
 *      message_type
 *      context_message_id 
 *      transaction_id
 *      semantic_event 
 *      xml_action 
 *      media_file 
 *      problem 
 *  
 */

public class XMLExtractedDataObject{
        private long resourceUseTransactionId;
        private String messageType;
        private String contextMessageId;
        private String contextMessageName;
        private String transactionId;
        private String problemName;
        private String semanticEventName;
        private String action;
        private String mediaFileName;
        
        public XMLExtractedDataObject() {}
        
        public void initiate(String infoXMLStr) throws JDOMException,
                                                        IOException, 
                                                        TooManyProblemsExistException,
                                                        MoreThanOneElementException {
                SAXBuilder builder = new SAXBuilder();
                Document xmlDocument = builder.build(new StringReader(infoXMLStr));
                XmlParser parser = XmlParserFactory.getInstance().get(xmlDocument);
                List<MessageItem> msgList = parser.getMessageItems();
                for (int i = 0; i < msgList.size(); i++) {
                        //parse the message top level data
                        MessageItem mItem = msgList.get(i);
                        String thisMessageType = mItem.getMessageType();
                        if (thisMessageType.equals(MessageItem.MSG_TYPE_CONTEXT)) {
                                contextMessageId = mItem.getContextMessageId();
                                if (messageType == null)
                                        messageType = MessageItem.MSG_TYPE_CONTEXT;
                                ContextMessageParser contextParser = ContextMessageParserFactory
                                                .getInstance().get(mItem);
                                ContextMessage currentContextMessage = contextParser.getContextMessage();
                                contextMessageName = currentContextMessage.getName();
                                //get problem from context message
                                if (this.problemName == null && currentContextMessage.getDatasetItem() != null) {
                                        List<DatasetLevelItem> datasetLevels = currentContextMessage.getDatasetItem().getDatasetLevelsExternal();
                                        //check dataset level
                                        for (DatasetLevelItem dslItem : datasetLevels ) {
                                                //in case when top level has problems
                                                List<ProblemItem> problems = dslItem.getProblemsExternal();
                                                if (problems != null && problems.size() > 0) {
                                                        if (problems.size() > 1)
                                                                throw new TooManyProblemsExistException("ResourceUseTransactionId: " + resourceUseTransactionId);
                                                        ProblemItem problemItem = problems.get(0);
                                                        problemName = problemItem.getProblemName();
                                                }
                                                //dslItem's child dataset level
                                                List<DatasetLevelItem> childrenDatasetLevel = dslItem.getChildrenExternal();
                                                for (DatasetLevelItem child : childrenDatasetLevel) {
                                                        List<ProblemItem> childProblems = child.getProblemsExternal();
                                                        if (childProblems != null && childProblems.size() > 0) {
                                                                if (childProblems.size() > 1)
                                                                        throw new TooManyProblemsExistException("ResourceUseTransactionId: " + resourceUseTransactionId);
                                                                ProblemItem problemItem = childProblems.get(0);
                                                                problemName = problemItem.getProblemName();
                                                        }
                                                }
                                        }
                                }
                        } else if (thisMessageType.equals(MessageItem.MSG_TYPE_TOOL)) {
                                if (contextMessageId == null)
                                        this.contextMessageId = mItem.getContextMessageId();
                                messageType = MessageItem.MSG_TYPE_TOOL;
                                transactionId = mItem.getTransactionId();
                                ToolMessageParser toolParser =
                                                ToolMessageParserFactory.getInstance().get(mItem);
                                ToolMessage newToolMsg = toolParser.getToolMessage();
                                //problem
                                ProblemItem problemItem = newToolMsg.getProblemItem();
                                if (problemItem != null) {
                                        problemName = problemItem.getProblemName();
                                }
                                //there should be only one semantic event
                                List<SemanticEvent> semanticEvents = newToolMsg.getSemanticEventsExternal();
                                if (semanticEvents != null && semanticEvents.size() > 0) {
                                        if (semanticEvents.size() > 1)
                                                throw new MoreThanOneElementException("More than one Semantic Event found for resourceUseTransactionId: " + resourceUseTransactionId);  
                                        semanticEventName = semanticEvents.get(0).getName();
                                }
                                List<EventDescriptor> eventDescriptors = newToolMsg.getEventDescriptorsExternal();
                                if (eventDescriptors != null && eventDescriptors.size() > 0) {
                                        if (eventDescriptors.size() != 1)
                                                throw new MoreThanOneElementException("More than one Event Descriptor found for resourceUseTransactionId: " + resourceUseTransactionId); 
                                        List<AttemptActionItem> actions = eventDescriptors.get(0).getActionsExternal();
                                        if (actions != null && actions.size() > 0) {
                                                if (actions.size() != 1)
                                                        throw new MoreThanOneElementException("More than one Action found for resourceUseTransactionId: " + resourceUseTransactionId); 
                                                action = actions.get(0).getAction();
                                        }
                                        List<AttemptSelectionItem> selections = eventDescriptors.get(0).getSelectionsExternal();
                                        if (selections != null && selections.size() > 0) {
                                                for (AttemptSelectionItem selItem : selections) {
                                                        //selection exist
                                                        if (selItem.getType() != null && selItem.getType().equals(OLIIntermediateDataObject.XML_SELECTION_TYPE_MEDIA_FILE))
                                                                mediaFileName = selItem.getSelection();
                                                }
                                        }
                                }
                        } else if (thisMessageType.equals(MessageItem.MSG_TYPE_TUTOR)) {
                                if (contextMessageId == null)
                                        this.contextMessageId = mItem.getContextMessageId();
                                messageType = MessageItem.MSG_TYPE_TUTOR;
                                transactionId = mItem.getTransactionId();
                                TutorMessageParser tutorParser =
                                                TutorMessageParserFactory.getInstance().get(mItem);
                                TutorMessage newTutorMsg = tutorParser.getTutorMessage();
                                //problem
                                ProblemItem problemItem = newTutorMsg.getProblemItem();
                                if (problemItem != null) {
                                        problemName = problemItem.getProblemName();
                                }
                                //there should be only one semantic event
                                List<SemanticEvent> semanticEvents = newTutorMsg.getSemanticEventsExternal();
                                if (semanticEvents != null && semanticEvents.size() > 0) {
                                        if (semanticEvents.size() != 1)
                                                throw new MoreThanOneElementException("More than one Semantic Event found for resourceUseTransactionId: " + resourceUseTransactionId);  
                                        semanticEventName = semanticEvents.get(0).getName();
                                }
                                List<EventDescriptor> eventDescriptors = newTutorMsg.getEventDescriptorsExternal();
                                if (eventDescriptors != null && eventDescriptors.size() > 0) {
                                        if (eventDescriptors.size() != 1)
                                                throw new MoreThanOneElementException("More than one Event Descriptor found for resourceUseTransactionId: " + resourceUseTransactionId); 
                                        List<ActionItem> actions = eventDescriptors.get(0).getActionsExternal();
                                        if (actions != null && actions.size() > 0) {
                                                if (actions.size() != 1)
                                                        throw new MoreThanOneElementException("More than one Action found for resourceUseTransactionId: " + resourceUseTransactionId); 
                                                action = actions.get(0).getAction();
                                        }
                                        List<SelectionItem> selections = eventDescriptors.get(0).getSelectionsExternal();
                                        if (selections != null && selections.size() > 0) {
                                                for (SelectionItem selItem : selections) {
                                                        //selection exist
                                                        if (selItem.getType() != null && selItem.getType().equals(OLIIntermediateDataObject.XML_SELECTION_TYPE_MEDIA_FILE))
                                                                mediaFileName = selItem.getSelection();
                                                        
                                                }
                                        }
                                }
                        }
                }
        }
        
        public void setResourceUseTransactionId (long resourceUseTransactionId) {
                this.resourceUseTransactionId = resourceUseTransactionId;
        }
        public long getResourceUseTransactionId () {
                return this.resourceUseTransactionId;
        }
        
        public void setMessageType (String messageType) {
                this.messageType = messageType;
        }
        public String getMessageType () {
                return this.messageType;
        }

        public void setContextMessageId (String contextMessageId) {
                this.contextMessageId = contextMessageId;
        }
        public String getContextMessageId () {
                return this.contextMessageId;
        }
        
        public void setContextMessageName (String contextMessageName) {
                this.contextMessageName = contextMessageName;
        }
        public String getContextMessageName () {
                return this.contextMessageName;
        }
        
        public void setTransactionId (String transactionId) {
                this.transactionId = transactionId;
        }
        public String getTransactionId () {
                return this.transactionId;
        }

        public void setProblemName (String problemName) {
                this.problemName = problemName;
        }
        public String getProblemName () {
                return this.problemName;
        }

        public void setSemanticEventName (String semanticEventName) {
                this.semanticEventName = semanticEventName;
        }
        public String getSemanticEventName () {
                return this.semanticEventName;
        }

        public void setAction (String action) {
                this.action = action;
        }
        public String getAction () {
                return this.action;
        }
        
        public void setMediaFileName (String mediaFileName) {
                this.mediaFileName = mediaFileName;
        }
        public String getMediaFileName () {
                return this.mediaFileName;
        }
        
        public String toString() {
                StringBuffer sb = new StringBuffer();
                sb.append("resourceUseTransactionId: " + resourceUseTransactionId + "\t");
                sb.append("messageType: " + messageType + "\t");
                sb.append("contextMessageId: " + contextMessageId + "\t");
                sb.append("contextMessageName: " + contextMessageName + "\t");
                sb.append("transactionId: " + transactionId + "\t");
                sb.append("problemName: " + problemName + "\t");
                sb.append("semanticEventName: " + semanticEventName + "\t");
                sb.append("action: " + action + "\t");
                sb.append("mediaFileName: " + mediaFileName);
                return sb.toString();
                
        }
        
        
        /**
         * Exception for XML tutor_message when more than one problem found in context message */
        public class TooManyProblemsExistException extends Exception {
                private String msgHeader = "ProblemNotExistException found ";

                public TooManyProblemsExistException () {
                }
                
                public TooManyProblemsExistException (String msg) {
                        msgHeader += msg;
                }

                public String getMessage() {
                        return msgHeader;
                }

                public String getMessage(String msg) {
                        return msgHeader + msg;
                }
        }
        
        /**
         * Exception for OLI_db_umuc database when more than one element found in tutor or tool message */
        public class MoreThanOneElementException extends Exception {
                private String msgHeader = "MoreThanOneElementException found ";

                public MoreThanOneElementException () {
                }
                
                public MoreThanOneElementException (String msg) {
                        msgHeader += msg;
                }

                public String getMessage() {
                        return msgHeader;
                }

                public String getMessage(String msg) {
                        return msgHeader + msg;
                }
        }
}
