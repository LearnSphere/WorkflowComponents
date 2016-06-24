package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

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
        private OLIIntermediateDataObject lastMediaPlayOLIIntermediateDataObject;
        private OLIIntermediateDataObject lastProcessedOLIIntermediateDataObject;
        
        public OLIMediaDataObject () {}
        
        public void setLastMediaPlayOLIIntermediateDataObject (OLIIntermediateDataObject lastMediaPlayOLIIntermediateDataObject) {
                this.lastMediaPlayOLIIntermediateDataObject = lastMediaPlayOLIIntermediateDataObject;
        }
        public OLIIntermediateDataObject getLastMediaPlayOLIIntermediateDataObject () {
                return this.lastMediaPlayOLIIntermediateDataObject;
        }
        
        public void setLastProcessedOLIIntermediateDataObject (OLIIntermediateDataObject lastProcessedOLIIntermediateDataObject) {
                this.lastProcessedOLIIntermediateDataObject = lastProcessedOLIIntermediateDataObject;
        }
        public OLIIntermediateDataObject getLastProcessedOLIIntermediateDataObject () {
                return this.lastProcessedOLIIntermediateDataObject;
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
        
        public void processOLIIntermediateDataObject (OLIIntermediateDataObject currMediaPlayOLIIntermediateDataObject) {
                //when action is "PLAY", or "MUTE", or "UNMUTE"
                if (currMediaPlayOLIIntermediateDataObject.isPlainMediaAction()) {
                        Integer nextTime = currMediaPlayOLIIntermediateDataObject.getNextTimeDiff();
                        if (nextTime != null && nextTime.intValue() != 0) {
                                addToMediaTime(nextTime);
                                //add to count as long as it is a play action
                                if (currMediaPlayOLIIntermediateDataObject.isPlainMediaPlayAction())
                                        incrementMediaCount();
                        }
                        /*if (lastProcessedOLIIntermediateDataObject == null || 
                                        (!lastProcessedOLIIntermediateDataObject.isPlainMediaAction() && !lastProcessedOLIIntermediateDataObject.isXMLMediaAction()))
                                incrementMediaCount();
                        else if (lastMediaPlayOLIIntermediateDataObject != null &&
                                        currMediaPlayOLIIntermediateDataObject.getInfoFileName() != null &&
                                        lastMediaPlayOLIIntermediateDataObject.getInfoFileName() != null &&
                                        !currMediaPlayOLIIntermediateDataObject.getInfoFileName().equals(lastMediaPlayOLIIntermediateDataObject.getInfoFileName())) {
                                incrementMediaCount();
                        } else if (lastMediaPlayOLIIntermediateDataObject == null)
                                incrementMediaCount();*/
                } else if (currMediaPlayOLIIntermediateDataObject.isXMLMediaAction()) {
                        if (currMediaPlayOLIIntermediateDataObject.isMediaPlayRecordableActionForXML()) {
                                Integer nextTime = currMediaPlayOLIIntermediateDataObject.getNextTimeDiff();
                                if (nextTime != null && nextTime.intValue() != 0) {
                                        addToMediaTime(nextTime);
                                        if (currMediaPlayOLIIntermediateDataObject.isMediaPlayActionForXML())
                                                incrementMediaCount();
                                }
                                /*if (lastProcessedOLIIntermediateDataObject == null ||
                                                (!lastProcessedOLIIntermediateDataObject.isPlainMediaAction() && !lastProcessedOLIIntermediateDataObject.isXMLMediaAction())) {
                                        incrementMediaCount();
                                } else if (lastMediaPlayOLIIntermediateDataObject != null && !lastMediaPlayOLIIntermediateDataObject.isXMLMediaAction()) {
                                        incrementMediaCount();
                                } else if (lastMediaPlayOLIIntermediateDataObject != null &&
                                                currMediaPlayOLIIntermediateDataObject.getXmlExtractedDataObject().getMediaFileName() != null &&
                                                lastMediaPlayOLIIntermediateDataObject.getXmlExtractedDataObject().getMediaFileName() != null &&
                                                !currMediaPlayOLIIntermediateDataObject.getXmlExtractedDataObject().getMediaFileName().equals(lastMediaPlayOLIIntermediateDataObject.getXmlExtractedDataObject().getMediaFileName())) {
                                        incrementMediaCount();
                                } else if (lastMediaPlayOLIIntermediateDataObject == null) {
                                        incrementMediaCount();
                                }*/
                        }
                }

                Integer prevTime = currMediaPlayOLIIntermediateDataObject.getPrevTimeDiff();
                if (prevTime != null && prevTime.intValue() != 0) {
                        if (lastProcessedOLIIntermediateDataObject != null) {
                                if (lastProcessedOLIIntermediateDataObject.isPlainAction() || lastProcessedOLIIntermediateDataObject.isXMLAction()) {
                                        addToActionToMediaPlayTime(prevTime);
                                        incrementActionToMediaPlayCount();
                                }
                        }
                }
                this.setLastMediaPlayOLIIntermediateDataObject(currMediaPlayOLIIntermediateDataObject);
        }
        
        public String toString() {
                StringBuffer sb = new StringBuffer();
                sb.append("student: " + student + "\t");
                sb.append("mediaCount: " + mediaCount + "\t");
                sb.append("mediaTime: " + mediaTime + "\t");
                sb.append("actionToMediaPlayCount: " + actionToMediaPlayCount + "\t");
                sb.append("actionToMediaPlayTime: " + actionToMediaPlayTime + "\n");
                sb.append("lastMediaPlayOLIIntermediateDataObject: " + lastMediaPlayOLIIntermediateDataObject + "\n");
                sb.append("lastProcessedOLIIntermediateDataObject: " + lastProcessedOLIIntermediateDataObject);
                
                return sb.toString();
        }
}
