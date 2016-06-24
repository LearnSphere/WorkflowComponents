package edu.cmu.pslc.learnsphere.analysis.resourceuse.coursera.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.pslc.learnsphere.analysis.resourceuse.coursera.item.CourseraClickstreamItem;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.coursera.item.CourseraClickstreamVideoItem;

/**
 * Represent an coursera video data object. 
 *  
 */

public class CourseraVideoActionDataObject{
        private String student;
        private HashMap<String, Integer> studentVideoActionCount;
        private HashMap<String, Double> studentVideoActionTime;
        private DecimalFormat myFormatter = new DecimalFormat("###.##");
        
        public CourseraVideoActionDataObject () {
                studentVideoActionCount = new HashMap<String, Integer>();
                studentVideoActionTime = new HashMap<String, Double>();
        }
        
        public void setStudent (String student) {
                this.student = student;
        }
        public String getStudent () {
                return this.student;
        }
        
        private void setVideoActionCount (String actionType, Integer actionCount) {
                studentVideoActionCount.put(actionType, actionCount);
        }
        private void setVideoActionTime (String actionType, Double timeInSecond) {
                studentVideoActionTime.put(actionType, timeInSecond);
        }
        
        private Integer getVideoActionCount (String actionType) {
                return studentVideoActionCount.get(actionType);
        }
        private Double getVideoActionTime (String actionType) {
                return studentVideoActionTime.get(actionType);
        }
        
        private String getVideoActionCountForDisplay (String actionType) {
                Integer count = getVideoActionCount(actionType);
                if (count == null)
                        return "0";
                else 
                        return count.toString();
        }
        private String getVideoActionTimeForDisplay (String actionType) {
                Double timeInSecond = getVideoActionTime(actionType);
                if (timeInSecond == null)
                        return "0";
                else { 
                        
                        return myFormatter.format(timeInSecond);
                }
        }
        
        private void incrementVideoActionCount (String actionType) {
                Integer count = getVideoActionCount(actionType);
                if (count == null)
                        count = 0;
                count++;
                setVideoActionCount(actionType, count);
        }
        private void addToVideoActionCount (String actionType, int cnt) {
                Integer count = getVideoActionCount(actionType);
                if (count == null)
                        count = 0;
                count += cnt;
                setVideoActionCount(actionType, count);
        }
        private void addToVideoActionTime (String actionType, double seconds) {
                Double timeInSeconds = getVideoActionTime(actionType);
                if (timeInSeconds == null)
                        timeInSeconds = 0.0;
                timeInSeconds += seconds;
                setVideoActionTime(actionType, timeInSeconds);
        }
        
        public void aggregateStudentVideoAction (CourseraClickstreamItem courseraClickstreamItem) {
                CourseraClickstreamVideoItem videoItem = courseraClickstreamItem.getValue();
                if (videoItem != null && videoItem.getType() != null ) {
                        if (courseraClickstreamItem.getNextTimeDiff() != null)
                                addToVideoActionTime(videoItem.getType(), courseraClickstreamItem.getNextTimeDiff());
                        incrementVideoActionCount(videoItem.getType());
                }
        }
        
        public String outputHeader(String[] actionTypes) {
                StringBuffer sb = new StringBuffer();
                sb.append("Student\t");
                int cnt = 0;
                String osName = System.getProperty("os.name").toLowerCase();
                String newLine = "";
                for (String actionType : actionTypes) {
                        sb.append(actionType + " count\t" + actionType + " time in sec");
                        if (cnt < actionTypes.length - 1)
                                sb.append("\t");
                        else {
                                if (osName.indexOf("win") >= 0)
                                        newLine = "\r\n";
                                else
                                        newLine = "\n";
                                sb.append(newLine);
                        }
                        cnt++;
                }
                return sb.toString();
        }

        public String toString(String[] actionTypes) {
                StringBuffer sb = new StringBuffer();
                sb.append(student + "\t");
                int cnt = 0;
                String osName = System.getProperty("os.name").toLowerCase();
                String newLine = "";
                for (String actionType : actionTypes) {
                        sb.append(getVideoActionCountForDisplay(actionType) + "\t" + getVideoActionTimeForDisplay(actionType));
                        if (cnt < actionTypes.length - 1)
                                sb.append("\t");
                        else {
                                if (osName.indexOf("win") >= 0)
                                        newLine = "\r\n";
                                else
                                        newLine = "\n";
                                sb.append(newLine);
                        }
                        cnt++;
                }
                return sb.toString();
        }
        
        public String toString() {
                StringBuffer sb = new StringBuffer();
                sb.append(student + "\t");
                int cnt = 0;
                String osName = System.getProperty("os.name").toLowerCase();
                String newLine = "";
                for (String actionType : studentVideoActionCount.keySet()) {
                        sb.append(getVideoActionCountForDisplay(actionType) + "\t" + getVideoActionTimeForDisplay(actionType));
                        if (cnt < studentVideoActionCount.size() - 1)
                                sb.append("\t");
                        else {
                                if (osName.indexOf("win") >= 0)
                                        newLine = "\r\n";
                                else
                                        newLine = "\n";
                                sb.append(newLine);
                        }
                        cnt++;
                }
                return sb.toString();
        }
}
