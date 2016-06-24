package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Represent an OLI intermediate data object. Fields are
 *      Course
 *      Student
 *      actionCount
 *      actionTime (in seconds)
 *      pageViewCount
 *      pageViewTime (in seconds)
 *      actionToViewPageCount
 *      actionToViewPageTime (in seconds)
 *      viewPageToActionCount
 *      viewPageToActionTime (in seconds)
 *  
 */

public class OLIViewActionDataObject{
        private String student;
        private Integer actionCount;
        private Integer actionTime;
        private Integer pageViewCount;
        private Integer pageViewTime;
        private Integer actionToPageViewCount;
        private Integer actionToPageViewTime;
        private Integer pageViewToActionCount;
        private Integer pageViewToActionTime;
        private Integer endOfMediaPlayToViewPageCount;
        private Integer endOfMediaPlayToViewPageTime;
        private OLIIntermediateDataObject lastViewActionOLIIntermediateDataObject;
        private OLIIntermediateDataObject lastProcessedOLIIntermediateDataObject;
        private String currContextMessageId;
        
        public OLIViewActionDataObject () {}
        
        public void setLastViewActionOLIIntermediateDataObject (OLIIntermediateDataObject lastViewActionOLIIntermediateDataObject) {
                this.lastViewActionOLIIntermediateDataObject = lastViewActionOLIIntermediateDataObject;
        }
        public OLIIntermediateDataObject getLastViewActionOLIIntermediateDataObject () {
                return this.lastViewActionOLIIntermediateDataObject;
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

        public void setCurrContextMessageId (String currContextMessageId) {
                this.currContextMessageId = currContextMessageId;
        }
        public String getCurrContextMessageId () {
                return this.currContextMessageId;
        }
        
        public void setActionCount (Integer actionCount) {
                this.actionCount = actionCount;
        }
        public Integer getActionCount () {
                return this.actionCount;
        }
        public String getActionCountForDisplay () {
                if (actionCount == null)
                        return "0";
                else 
                        return actionCount.toString();
        }
        public void incrementActionCount () {
                if (actionCount == null)
                        actionCount = 0;
                actionCount++;
        }
        public void addToActionCount (int cnt) {
                if (actionCount == null)
                        actionCount = 0;
                actionCount += cnt;
        }
        
        public void setActionTime (Integer actionTime) {
                this.actionTime = actionTime;
        }
        public Integer getActionTime () {
                return this.actionTime;
        }
        public String getActionTimeForDisplay () {
                if (actionTime == null)
                        return "0";
                else 
                        return actionTime.toString();
        }
        public void addToActionTime (int time) {
                if (actionTime == null)
                        actionTime = 0;
                actionTime += time;
        }
        
        public void setPageViewCount (Integer pageViewCount) {
                this.pageViewCount = pageViewCount;
        }
        public Integer getPageViewCount () {
                return this.pageViewCount;
        }
        public String getPageViewCountForDisplay () {
                if (pageViewCount == null)
                        return "0";
                else 
                        return pageViewCount.toString();
        }
        public void incrementPageViewCount () {
                if (pageViewCount == null)
                        pageViewCount = 0;
                pageViewCount++;
        }
        public void addToPageViewCount (int cnt) {
                if (pageViewCount == null)
                        pageViewCount = 0;
                pageViewCount += cnt;
        }
        public void decrementPageViewCount () {
                if (pageViewCount == null)
                        return;
                pageViewCount--;
        }
        
        public void setPageViewTime (Integer pageViewTime) {
                this.pageViewTime = pageViewTime;
        }
        public Integer getPageViewTime () {
                return this.pageViewTime;
        }
        public String getPageViewTimeForDisplay () {
                if (pageViewTime == null)
                        return "0";
                else 
                        return pageViewTime.toString();
        }
        public void addToPageViewTime (int time) {
                if (pageViewTime == null)
                        pageViewTime = 0;
                pageViewTime += time;
        }
        public void subtractFromPageViewTime (int time) {
                if (pageViewTime == null)
                        return;
                pageViewTime -= time;
        }
        
        public void setActionToPageViewCount (Integer actionToPageViewCount) {
                this.actionToPageViewCount = actionToPageViewCount;
        }
        public Integer getActionToPageViewCount () {
                return this.actionToPageViewCount;
        }
        public String getActionToPageViewCountForDisplay () {
                if (actionToPageViewCount == null)
                        return "0";
                else 
                        return actionToPageViewCount.toString();
        }
        public void incrementActionToPageViewCount () {
                if (actionToPageViewCount == null)
                        actionToPageViewCount = 0;
                actionToPageViewCount++;
        }
        public void addToActionToPageViewCount (int cnt) {
                if (actionToPageViewCount == null)
                        actionToPageViewCount = 0;
                actionToPageViewCount += cnt;
        }
        
        public void setActionToPageViewTime (Integer actionToPageViewTime) {
                this.actionToPageViewTime = actionToPageViewTime;
        }
        public Integer getActionToPageViewTime () {
                return this.actionToPageViewTime;
        }
        public String getActionToPageViewTimeForDisplay () {
                if (actionToPageViewTime == null)
                        return "0";
                else 
                        return actionToPageViewTime.toString();
        }
        public void addToActionToPageViewTime (int time) {
                if (actionToPageViewTime == null)
                        actionToPageViewTime = 0;
                actionToPageViewTime += time;
        }
        
        public void setPageViewToActionCount (Integer pageViewToActionCount) {
                this.pageViewToActionCount = pageViewToActionCount;
        }
        public Integer getPageViewToActionCount () {
                return this.pageViewToActionCount;
        }
        public String getPageViewToActionCountForDisplay () {
                if (pageViewToActionCount == null)
                        return "0";
                else 
                        return pageViewToActionCount.toString();
        }
        public void incrementPageViewToActionCount () {
                if (pageViewToActionCount == null)
                        pageViewToActionCount = 0;
                pageViewToActionCount++;
        }
        public void addToPageViewToActionCount (int cnt) {
                if (pageViewToActionCount == null)
                        pageViewToActionCount = 0;
                pageViewToActionCount += cnt;
        }
        
        public void setPageViewToActionTime (Integer pageViewToActionTime) {
                this.pageViewToActionTime = pageViewToActionTime;
        }
        public Integer getPageViewToActionTime () {
                return this.pageViewToActionTime;
        }
        public String getPageViewToActionTimeForDisplay () {
                if (pageViewToActionTime == null)
                        return "0";
                else 
                        return pageViewToActionTime.toString();
        }
        public void addToPageViewToActionTime (int time) {
                if (pageViewToActionTime == null)
                        pageViewToActionTime = 0;
                pageViewToActionTime += time;
        }
        
        public void setEndOfMediaPlayToViewPageCount (Integer endOfMediaPlayToViewPageCount) {
                this.endOfMediaPlayToViewPageCount = endOfMediaPlayToViewPageCount;
        }
        public Integer getEndOfMediaPlayToViewPageCount () {
                return this.endOfMediaPlayToViewPageCount;
        }
        public String getEndOfMediaPlayToViewPageCountForDisplay () {
                if (endOfMediaPlayToViewPageCount == null)
                        return "0";
                else 
                        return endOfMediaPlayToViewPageCount.toString();
        }
        public void incrementEndOfMediaPlayToViewPageCount () {
                if (endOfMediaPlayToViewPageCount == null)
                        endOfMediaPlayToViewPageCount = 0;
                endOfMediaPlayToViewPageCount++;
        }
        public void addToEndOfMediaPlayToViewPageCount (int cnt) {
                if (endOfMediaPlayToViewPageCount == null)
                        endOfMediaPlayToViewPageCount = 0;
                endOfMediaPlayToViewPageCount += cnt;
        }
        
        public void setEndOfMediaPlayToViewPageTime (Integer endOfMediaPlayToViewPageTime) {
                this.endOfMediaPlayToViewPageTime = endOfMediaPlayToViewPageTime;
        }
        public Integer getEndOfMediaPlayToViewPageTime () {
                return this.endOfMediaPlayToViewPageTime;
        }
        public String getEndOfMediaPlayToViewPageTimeForDisplay () {
                if (endOfMediaPlayToViewPageTime == null)
                        return "0";
                else 
                        return endOfMediaPlayToViewPageTime.toString();
        }
        public void addToEndOfMediaPlayToViewPageTime (int time) {
                if (endOfMediaPlayToViewPageTime == null)
                        endOfMediaPlayToViewPageTime = 0;
                endOfMediaPlayToViewPageTime += time;
        }
        
        public boolean dataIsEmpty() {
                if (actionCount == null && actionTime == null &&
                                pageViewCount == null && pageViewTime == null &&
                                actionToPageViewCount == null && actionToPageViewTime == null &&
                                pageViewToActionCount == null && pageViewToActionTime == null &&
                                endOfMediaPlayToViewPageCount == null && endOfMediaPlayToViewPageTime == null)
                        return true;
                else return false;
        }
        
        public void processOLIIntermediateDataObject (OLIIntermediateDataObject currViewActionOLIIntermediateDataObject) {
                //for view page: add nextTime to veiw page time and count
                //if previous action is action, add prevTime to action_to_view and count
                //for action: if previous is view_page, add preTime to view_to_action and count; subtract prevTime from view time and count
                //for just action: add prevTime to action; add 1 to count only when it's first try in context block or start_attempt
                if (currViewActionOLIIntermediateDataObject.isViewPage()) {
                        Integer nextTime = currViewActionOLIIntermediateDataObject.getNextTimeDiff();
                        if (nextTime != null && nextTime.intValue() != 0) {
                                addToPageViewTime(nextTime);
                                incrementPageViewCount();
                        }
                        Integer prevTime = currViewActionOLIIntermediateDataObject.getPrevTimeDiff();
                        if (prevTime != null && prevTime.intValue() != 0) {
                                if (lastProcessedOLIIntermediateDataObject != null) {
                                        if (lastProcessedOLIIntermediateDataObject.isPlainAction() || lastProcessedOLIIntermediateDataObject.isXMLAction()) {
                                                addToActionToPageViewTime(prevTime);
                                                incrementActionToPageViewCount();
                                        } else if (lastProcessedOLIIntermediateDataObject.isPlainMediaStopAction() || lastProcessedOLIIntermediateDataObject.isXMLMediaStopAction()) {
                                                addToEndOfMediaPlayToViewPageTime(prevTime);
                                                incrementEndOfMediaPlayToViewPageCount();
                                        }
                                }
                        }
                } else if (currViewActionOLIIntermediateDataObject.isPlainAction() ||
                                currViewActionOLIIntermediateDataObject.isXMLAction()) {
                        if (lastProcessedOLIIntermediateDataObject != null && lastProcessedOLIIntermediateDataObject.isViewPage()) {
                                Integer prevTime = currViewActionOLIIntermediateDataObject.getPrevTimeDiff();
                                if (prevTime != null && prevTime.intValue() != 0) {
                                        addToPageViewToActionTime(prevTime);
                                        incrementPageViewToActionCount();
                                        //subtract prevTime from viewPage
                                        if (pageViewTime >= prevTime) {
                                                subtractFromPageViewTime(prevTime);
                                        }
                                        if (pageViewCount >= 1) {
                                                decrementPageViewCount();
                                        }
                                }
                        } else {
                                Integer prevTime = currViewActionOLIIntermediateDataObject.getPrevTimeDiff();
                                if (prevTime != null && prevTime.intValue() != 0) {
                                        addToActionTime(prevTime);
                                        if (currViewActionOLIIntermediateDataObject.isPlainAction() &&
                                                        currViewActionOLIIntermediateDataObject.isPlainActionStartSessionOrAttempt()) {
                                                /*if (lastProcessedOLIIntermediateDataObject == null || lastProcessedOLIIntermediateDataObject.isPlainMediaAction() || lastProcessedOLIIntermediateDataObject.isXMLMediaAction()) {
                                                        incrementActionCount();
                                                } else if (lastProcessedOLIIntermediateDataObject != null &&
                                                                currViewActionOLIIntermediateDataObject.getInfo() != null &&
                                                                lastProcessedOLIIntermediateDataObject.getInfo() != null &&
                                                                !currViewActionOLIIntermediateDataObject.getInfo().equals(lastProcessedOLIIntermediateDataObject.getInfo())) {
                                                        incrementActionCount();
                                                } else if (lastViewActionOLIIntermediateDataObject == null)
                                                        incrementActionCount();*/
                                                incrementActionCount();
                                        } else if (currViewActionOLIIntermediateDataObject.isXMLAction()){
                                                //increment only once for the entire context block
                                                if (currContextMessageId == null || !currContextMessageId.equals(currViewActionOLIIntermediateDataObject.getXmlExtractedDataObject().getContextMessageId()))
                                                        incrementActionCount();
                                        }
                                }
                        }
                }
                setLastViewActionOLIIntermediateDataObject(currViewActionOLIIntermediateDataObject);
                if (currViewActionOLIIntermediateDataObject.getXmlExtractedDataObject() != null &&
                                currViewActionOLIIntermediateDataObject.getXmlExtractedDataObject().getContextMessageId() != null)
                        setCurrContextMessageId(currViewActionOLIIntermediateDataObject.getXmlExtractedDataObject().getContextMessageId());
        }

        public String toString() {
                StringBuffer sb = new StringBuffer();
                sb.append("student: " + student + "\t");
                sb.append("actionCount: " + actionCount + "\t");
                sb.append("actionTime: " + actionTime);
                sb.append("pageViewCount: " + pageViewCount + "\t");
                sb.append("pageViewTime: " + pageViewTime + "\t");
                sb.append("actionToPageViewCount: " + actionToPageViewCount + "\t");
                sb.append("actionToPageViewTime: " + actionToPageViewTime + "\t");
                sb.append("pageViewToActionCount: " + pageViewToActionCount + "\t");
                sb.append("pageViewToActionTime: " + pageViewToActionTime + "\t");
                sb.append("endOfMediaPlayToViewPageCount: " + endOfMediaPlayToViewPageCount + "\t");
                sb.append("endOfMediaPlayToViewPageTime: " + endOfMediaPlayToViewPageTime + "\n");
                sb.append("lastViewActionOLIIntermediateDataObject: " + lastViewActionOLIIntermediateDataObject + "\n");
                sb.append("lastProcessedOLIIntermediateDataObject: " + lastProcessedOLIIntermediateDataObject);
                
                return sb.toString();
        }
}
