package edu.cmu.pslc.learnsphere.analysis.resourceuse.coursera;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


/* Workflow includes. */
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.util.FileUtils;
import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.coursera.dao.CourseraDbDaoFactory;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.coursera.dao.CourseraClickstreamDao;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.coursera.item.CourseraClickstreamItem;
import edu.cmu.pslc.learnsphere.analysis.resourceuse.coursera.dto.CourseraVideoActionDataObject;



public class CourseraDataGenerator extends AbstractComponent {
    private HashMap<String, CourseraVideoActionDataObject> allStudentVideoActionDataObjects;
    private HashSet<String> actionTypes;

    public static void main(String[] args) {

        CourseraDataGenerator tool = new CourseraDataGenerator();
        tool.startComponent(args);

    }

    public CourseraDataGenerator() {
        super();
        allStudentVideoActionDataObjects = new HashMap<String, CourseraVideoActionDataObject>() ;
        actionTypes = new HashSet();
    }

    @Override
    protected void runComponent() {
        // Dao-enabled components require an applicationContext.xml in the component directory,
        String appContextPath = this.getToolDir() + "/applicationContext.xml";
        logger.info("appContextPath: " + appContextPath);
        //System.out.println("appContextPath: " + appContextPath);

        // Do not follow symbolic links so we can prevent unwanted directory traversals if someone
        // does manage to create a symlink to somewhere dangerous (like /datashop/deploy/)
        if (Files.exists(Paths.get(appContextPath), LinkOption.NOFOLLOW_LINKS)) {
            /** Initialize the Spring Framework application context. */
            SpringContext.getApplicationContext(appContextPath);
        }
        logger.info("Coursera data generator started...");
        try {
                CourseraClickstreamDao courseraClickstreamDao = CourseraDbDaoFactory.DEFAULT.getCourseraClickstreamDao();
                List<CourseraClickstreamItem> courseraClickstreamItems = courseraClickstreamDao.getCourseraClickStream();
                logger.info("Rows retrieved: " + courseraClickstreamItems.size());
                if (courseraClickstreamItems.size() > 0) {
                        CourseraClickstreamItem prevItem = courseraClickstreamItems.get(0);
                        for (int i = 1; i < courseraClickstreamItems.size(); i++){
                                CourseraClickstreamItem currItem = courseraClickstreamItems.get(i);
                                if (prevItem.getUsername().equals(currItem.getUsername())) {
                                        prevItem.computeNextDiff(currItem);
                                        currItem.computePrevDiff(prevItem);
                                }
                                //aggregate for the prevItem
                                if (prevItem.getValue() != null)
                                        actionTypes.add(prevItem.getValue().getType());
                                        aggregateStudentVideoAction(prevItem);
                                prevItem = currItem;
                        }
                        //ensure prevItem is processed if there is only one item and the last item
                        aggregateStudentVideoAction(prevItem);
                }
                StringBuffer outputData = new StringBuffer();
                boolean outputHeader = false;
                for (CourseraVideoActionDataObject studentVideoActionDataObject : allStudentVideoActionDataObjects.values()){
                        if (!outputHeader) {
                                outputData.append(studentVideoActionDataObject.outputHeader(actionTypes.toArray(new String[actionTypes.size()])));
                                outputHeader = true;
                        }
                        outputData.append(studentVideoActionDataObject.toString(actionTypes.toArray(new String[actionTypes.size()])));
                }
                //make File and write content
                File componentOutputFile = this.createFile("Resource-Use", ".txt");
                FileUtils.dumpToFile(outputData, componentOutputFile, true);
                logger.info("Coursera resource use, file created: " + componentOutputFile.getAbsolutePath());

                Integer nodeIndex = 0;
                Integer fileIndex = 0;
                String fileLabel = "resource-use";
                this.addOutputFile(componentOutputFile, nodeIndex, fileIndex, fileLabel);
                System.out.println(this.getOutput());
        } catch (Throwable throwable) {
                logger.error("Unknown error in main method.", throwable);
                this.addErrorMessage("Unknown error in main method." + throwable);
            } finally {
                logger.info("OurseraDataGenerator done.");
            }

    }

    private void aggregateStudentVideoAction(CourseraClickstreamItem item) {
            String username = item.getUsername();
            CourseraVideoActionDataObject studentActionObj = allStudentVideoActionDataObjects.get(username);
            if (studentActionObj == null) {
                    studentActionObj = new CourseraVideoActionDataObject();
                    studentActionObj.setStudent(username);
                    allStudentVideoActionDataObjects.put(username, studentActionObj);
            }
            studentActionObj.aggregateStudentVideoAction(item);
    }

}
