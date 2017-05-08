package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dataobject;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OliResourceUseDTOInterface;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OliUserTransactionDTO;

/**
 * Represent an OLI media data object. Fields are
 *      Course
 *      Student
 *      mediaCount
 *      mediaTime (in seconds)
 * 
 *  
 */

public class OLIMediaDataObject{
        private String student;
        private Integer mediaCount;
        private Integer mediaTime;
        private Integer actionToMediaPlayCount;
        private Integer actionToMediaPlayTime;
        private OliResourceUseDTOInterface lastMediaPlayDataObject;
        private OliResourceUseDTOInterface lastProcessedDataObject;
        
        public OLIMediaDataObject () {}
        
        public void setLastMediaPlayDataObject (OliResourceUseDTOInterface lastMediaPlayDataObject) {
                this.lastMediaPlayDataObject = lastMediaPlayDataObject;
        }
        public OliResourceUseDTOInterface getLastMediaPlayDataObject () {
                return this.lastMediaPlayDataObject;
        }
        
        public void setLastProcessedDataObject (OliResourceUseDTOInterface lastProcessedDataObject) {
                this.lastProcessedDataObject = lastProcessedDataObject;
        }
        public OliResourceUseDTOInterface getLastProcessedDataObject () {
                return this.lastProcessedDataObject;
        }
        
        public void setStudent (String student) {
                this.student = student;
        }
        public String getStudent () {
                return this.student;
        }
        
        public void setMediaCount (Integer mediaCount) {
                this.mediaCount = mediaCount;
        }
        public Integer getMediaCount () {
                return mediaCount;
        }
        public String getMediaCountForDisplay () {
                if (mediaCount == null)
                        return "0";
                else 
                        return mediaCount.toString();
        }
        public void incrementMediaCount () {
                if (mediaCount == null)
                        mediaCount = 0;
                mediaCount++;
        }
        public void addToMediaCount (int cnt) {
                if (mediaCount == null)
                        mediaCount = 0;
                mediaCount += cnt;
        }
        
        public void setMediaTime (Integer mediaTime) {
                this.mediaTime = mediaTime;
        }
        public Integer getMediaTime () {
                return mediaTime;
        }
        public String getMediaTimeForDisplay () {
                if (mediaTime == null)
                        return "0";
                else 
                        return mediaTime.toString();
        }
        public void addToMediaTime (int time) {
                if (mediaTime == null)
                        mediaTime = 0;
                mediaTime += time;
        }
        
        public void setActionToMediaPlayCount (Integer actionToMediaPlayCount) {
                this.actionToMediaPlayCount = actionToMediaPlayCount;
        }
        public Integer getActionToMediaPlayCount () {
                return this.actionToMediaPlayCount;
        }
        public String getActionToMediaPlayCountForDisplay () {
                if (actionToMediaPlayCount == null)
                        return "0";
                else 
                        return actionToMediaPlayCount.toString();
        }
        public void incrementActionToMediaPlayCount () {
                if (actionToMediaPlayCount == null)
                        actionToMediaPlayCount = 0;
                actionToMediaPlayCount++;
        }
        public void addToActionToMediaPlayCount (int cnt) {
                if (actionToMediaPlayCount == null)
                        actionToMediaPlayCount = 0;
                actionToMediaPlayCount += cnt;
        }
        
        public void setActionToMediaPlayTime (Integer actionToMediaPlayTime) {
                this.actionToMediaPlayTime = actionToMediaPlayTime;
        }
        public Integer getActionToMediaPlayTime () {
                return this.actionToMediaPlayTime;
        }
        public String getActionToMediaPlayTimeForDisplay () {
                if (actionToMediaPlayTime == null)
                        return "0";
                else 
                        return actionToMediaPlayTime.toString();
        }
        public void addToActionToMediaPlayTime (int time) {
                if (actionToMediaPlayTime == null)
                        actionToMediaPlayTime = 0;
                actionToMediaPlayTime += time;
        }

        public boolean dataIsEmpty() {
               if (mediaCount == null && mediaTime == null &&
                               actionToMediaPlayCount == null && actionToMediaPlayTime == null)
                       return true;
               else
                       return false;
        }
        
        public void processOLIDataObject (OliResourceUseDTOInterface oliDataObject) {
                //when action is "PLAY", or "MUTE", or "UNMUTE"
                //current obj is media play without XML
                if ((oliDataObject instanceof OliUserTransactionDTO) 
                                && ((OliUserTransactionDTO)oliDataObject).isPlainMediaAction()){
                        Integer nextTime = oliDataObject.getNextTimeDiff();
                        if (nextTime != null && nextTime.intValue() != 0) {
                                addToMediaTime(nextTime);
                                //add to count as long as it is a play action
                                if (((OliUserTransactionDTO)oliDataObject).isPlainMediaPlayAction())
                                        incrementMediaCount();
                        }
                } //current obj is media play with xml
                else if ((oliDataObject instanceof OliUserTransactionWithXmlDTO)
                                && ((OliUserTransactionWithXmlDTO)oliDataObject).isXMLMediaAction()) {
                        if (((OliUserTransactionWithXmlDTO)oliDataObject).isMediaPlayRecordableActionForXML()) {
                                Integer nextTime = oliDataObject.getNextTimeDiff();
                                if (nextTime != null && nextTime.intValue() != 0) {
                                        //add time for any media related
                                        addToMediaTime(nextTime);
                                        //add count only for play action
                                        if (((OliUserTransactionWithXmlDTO)oliDataObject).isMediaPlayActionForXML())
                                                incrementMediaCount();
                                }
                        }
                }

                Integer prevTime = oliDataObject.getPrevTimeDiff();
                if (prevTime != null && prevTime.intValue() != 0) {
                        if (lastProcessedDataObject != null) {
                                if ((lastProcessedDataObject instanceof OliUserTransactionDTO && ((OliUserTransactionDTO)lastProcessedDataObject).isPlainAction())
                                                || (lastProcessedDataObject instanceof OliUserTransactionWithXmlDTO && ((OliUserTransactionWithXmlDTO)lastProcessedDataObject).isXMLAction())) {
                                        addToActionToMediaPlayTime(prevTime);
                                        incrementActionToMediaPlayCount();
                                }
                        }
                }
                this.setLastMediaPlayDataObject(oliDataObject);
        }
        
        public String toString() {
                StringBuffer sb = new StringBuffer();
                sb.append("student: " + student + "\t");
                sb.append("mediaCount: " + mediaCount + "\t");
                sb.append("mediaTime: " + mediaTime + "\t");
                sb.append("actionToMediaPlayCount: " + actionToMediaPlayCount + "\t");
                sb.append("actionToMediaPlayTime: " + actionToMediaPlayTime + "\n");
                sb.append("lastMediaPlayDataObject: " + lastMediaPlayDataObject + "\n");
                sb.append("lastProcessedDataObject: " + lastProcessedDataObject);
                
                return sb.toString();
        }
}
