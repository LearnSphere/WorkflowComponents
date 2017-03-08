package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dataobject;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OliResourceUseDTOInterface;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.oli.dto.OliUserTransactionDTO;

/**
 * Represent an OLI summary data object. Fields are
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
        private OliResourceUseDTOInterface lastViewActionDataObject;
        private OliResourceUseDTOInterface lastProcessedDataObject;
        private String currContextMessageId;
        
        public OLIViewActionDataObject () {}
        
        public void setLastViewActionDataObject (OliResourceUseDTOInterface lastViewActionDataObject) {
                this.lastViewActionDataObject = lastViewActionDataObject;
        }
        public OliResourceUseDTOInterface getLastViewActionDataObject () {
                return this.lastViewActionDataObject;
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
                if (actionCount == null || 
                                (actionTime == null || actionTime == 0))
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
                if (actionTime == null 
                                || (actionCount == null || actionCount == 0))
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
                if (pageViewCount == null ||
                                pageViewTime == null || pageViewCount == 0)
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
                if (pageViewTime == null ||
                                pageViewCount == null || pageViewCount == 0)
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
                if (actionToPageViewCount == null
                                || actionToPageViewTime == null || actionToPageViewTime == 0)
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
                if (actionToPageViewTime == null 
                                || actionToPageViewCount == null || actionToPageViewCount == 0)
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
                if (pageViewToActionCount == null 
                                || pageViewToActionTime == null || pageViewToActionTime == 0)
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
                if (pageViewToActionTime == null
                                || pageViewToActionCount == null || pageViewToActionCount == 0)
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
        
        public void processOLIDataObject (OliResourceUseDTOInterface oliDataObject) {
                //for view page: add nextTime to veiw page time and count
                //if previous action is action, add prevTime to action_to_view and count
                //for action: if previous is view_page, add preTime to view_to_action and count; subtract prevTime from view time and count
                //for just action: add prevTime to action; add 1 to count only when it's first try in context block or start_attempt
                
                //when current obj has no XMLin info field 
                if (oliDataObject instanceof OliUserTransactionDTO) {
                        OliUserTransactionDTO currObj = (OliUserTransactionDTO)oliDataObject;
                        //when current action is a view page action
                        if (currObj.isViewPage()) {
                                Integer nextTime = currObj.getNextTimeDiff();
                                if (nextTime != null && nextTime.intValue() != 0) {
                                        addToPageViewTime(nextTime);
                                        incrementPageViewCount();
                                }
                                Integer prevTime = currObj.getPrevTimeDiff();
                                if (prevTime != null && prevTime.intValue() != 0) {
                                        if (lastProcessedDataObject != null) {
                                                //when last process object is action with/or without xml
                                                if (lastProcessedDataObject instanceof OliUserTransactionDTO) {
                                                        //when last processed object is action
                                                        if (((OliUserTransactionDTO)lastProcessedDataObject).isPlainAction()
                                                                        || ((OliUserTransactionDTO)lastProcessedDataObject).isCombinedViewSaveAttemptAction()
                                                                        || ((OliUserTransactionDTO)lastProcessedDataObject).isSubmitAttempt()) {
                                                                addToActionToPageViewTime(prevTime);
                                                                incrementActionToPageViewCount();
                                                        } else if (((OliUserTransactionDTO)lastProcessedDataObject).isPlainMediaStopAction()) {
                                                                addToEndOfMediaPlayToViewPageTime(prevTime);
                                                                incrementEndOfMediaPlayToViewPageCount();
                                                        }
                                                } else if (lastProcessedDataObject instanceof OliUserTransactionWithXmlDTO) {
                                                        if (((OliUserTransactionWithXmlDTO)lastProcessedDataObject).isXMLAction()) {
                                                                addToActionToPageViewTime(prevTime);
                                                                incrementActionToPageViewCount();
                                                        } else if (((OliUserTransactionWithXmlDTO)lastProcessedDataObject).isXMLMediaStopAction()) {
                                                                addToEndOfMediaPlayToViewPageTime(prevTime);
                                                                incrementEndOfMediaPlayToViewPageCount();
                                                        }
                                                }
                                        }
                                }
                        } //when current obj is action or view-saveAttempt aka checkpoint/quiz
                        else if (currObj.isPlainAction() || currObj.isCombinedViewSaveAttemptAction()) {
                                //last action is view page
                                if (lastProcessedDataObject != null 
                                                && (lastProcessedDataObject instanceof OliUserTransactionDTO) 
                                                && (((OliUserTransactionDTO)lastProcessedDataObject).isViewPage() 
                                                                || ((OliUserTransactionDTO)lastProcessedDataObject).isViewPreface())) {
                                        Integer prevTime = currObj.getPrevTimeDiff();
                                        if (prevTime != null && prevTime.intValue() != 0) {
                                                addToPageViewToActionTime(prevTime);
                                                incrementPageViewToActionCount();
                                                //subtract prevTime from viewPage
                                                if (pageViewTime != null && pageViewTime >= prevTime) {
                                                        subtractFromPageViewTime(prevTime);
                                                }
                                                if (pageViewCount != null && pageViewCount >= 1) {
                                                        decrementPageViewCount();
                                                }
                                        }
                                }//last action is an action 
                                else {
                                        Integer prevTime = currObj.getPrevTimeDiff();
                                        if (prevTime != null && prevTime.intValue() != 0) {
                                                addToActionTime(prevTime);
                                                //only add to count of it is start attempt or start session (after combined two rows with the same info and time
                                                if ((currObj.isPlainAction() && currObj.isPlainActionStartSessionOrAttempt())
                                                                || currObj.getAction().equals(OliUserTransactionDTO.COMBINED_START_ATTEMPT_VIEW_SAVE_ATTEMPT)) {
                                                        incrementActionCount();
                                                }//avoid situation action time is not 0 but count is 0
                                                /*else if (actionCount == null || actionCount == 0)
                                                        incrementActionCount();*/
                                        }
                                }
                        }//end of current obj is view/or action/or view-saveAttempt aka checkpoint/quiz
                }//end of oliDataObject instanceof OliUserTransactionDTO
                else if (oliDataObject instanceof OliUserTransactionWithXmlDTO) {
                        OliUserTransactionWithXmlDTO currObj = (OliUserTransactionWithXmlDTO)oliDataObject;
                        //current obj is action with xml
                        if (currObj.isXMLAction()) {
                                //last obj is view without xml
                                if (lastProcessedDataObject != null 
                                                && (lastProcessedDataObject instanceof OliUserTransactionDTO) 
                                                && ((OliUserTransactionDTO)lastProcessedDataObject).isViewPage()) {
                                        Integer prevTime = currObj.getPrevTimeDiff();
                                        if (prevTime != null && prevTime.intValue() != 0) {
                                                addToPageViewToActionTime(prevTime);
                                                incrementPageViewToActionCount();
                                                //subtract prevTime from viewPage
                                                if (pageViewTime != null && pageViewTime >= prevTime) {
                                                        subtractFromPageViewTime(prevTime);
                                                } else
                                                        pageViewTime = 0;
                                                if (pageViewCount != null && pageViewCount >= 1) {
                                                        decrementPageViewCount();
                                                }
                                        }
                                } //last action is an action
                                else {
                                        Integer prevTime = currObj.getPrevTimeDiff();
                                        if (prevTime != null && prevTime.intValue() != 0) {
                                                addToActionTime(prevTime);
                                                if (currObj.isXMLAction()){
                                                        //increment only once for the entire context block
                                                        if (currContextMessageId == null || !currContextMessageId.equals(currObj.getXmlExtractedDataObject().getContextMessageId()))
                                                                incrementActionCount();
                                                        //avoid situation action time is not 0 but count is 0
                                                        /*else if (actionCount == null || actionCount == 0)
                                                                incrementActionCount();*/
                                                }
                                        }
                                }
                        }
                } //end of oliDataObject instanceof OliUserTransactionWithXmlDTO
                setLastViewActionDataObject(oliDataObject);
                if ((oliDataObject instanceof OliUserTransactionWithXmlDTO) 
                                && ((OliUserTransactionWithXmlDTO)oliDataObject).getXmlExtractedDataObject() != null 
                                && ((OliUserTransactionWithXmlDTO)oliDataObject).getXmlExtractedDataObject().getContextMessageId() != null)
                        setCurrContextMessageId(((OliUserTransactionWithXmlDTO)oliDataObject).getXmlExtractedDataObject().getContextMessageId());
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
                sb.append("lastViewActionDataObject: " + lastViewActionDataObject + "\n");
                sb.append("lastProcessedDataObject: " + lastProcessedDataObject);
                
                return sb.toString();
        }
}
