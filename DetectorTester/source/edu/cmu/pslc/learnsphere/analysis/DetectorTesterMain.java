package edu.cmu.pslc.learnsphere.analysis;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.dao.*;
import edu.cmu.pslc.datashop.dao.hibernate.*;
import edu.cmu.pslc.datashop.item.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import edu.cmu.pslc.datashop.util.SpringContext;

public class DetectorTesterMain extends AbstractComponent {

    public static void main(String[] args) {

        DetectorTesterMain tool = new DetectorTesterMain();
        tool.startComponent(args);
    }

    public DetectorTesterMain() {
        super();
    }

    /**
   * The test() method is used to test the known inputs prior to running.
   * @return true if passing, false otherwise
   */
      @Override
      protected Boolean test() {
        Boolean passing = true;
        return passing;
      }

      /**
       * Parse the options list.
       */
      @Override
      protected void parseOptions() {
        logger.info("Parsing options.");
      }

      @Override
      protected void processOptions() {

      }


    @Override
    protected void runComponent() {
        boolean access = hasAccess();
        if (!access) {
          logger.error("User does not have access to the DetectorTesterAccess project." +
              "  Please request access to that project to be able to use this component.");
          System.err.println("User does not have access to the DetectorTesterAccess project." +
              "  Please request access to that project to be able to use this component.");
        }

        File outputDirectory = null;
        if (access) {
          // Run the program and return its stdout to a file.
          outputDirectory = this.runExternalMultipleFileOuput();
        }
        if (outputDirectory != null) {
            if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                logger.debug(outputDirectory.getAbsolutePath() + "/output.txt");
                File outputFile = new File(outputDirectory.getAbsolutePath() + "\\output.txt");

                if (outputFile != null && outputFile.exists()) {
                    Integer nodeIndex = 0;
                    Integer fileIndex = 0;
                    String fileLabel = "tab-delimited";
                    logger.debug(outputDirectory.getAbsolutePath() + "\\output.txt"); // different slash for windows machines

                    this.addOutputFile(outputFile, nodeIndex, fileIndex, fileLabel);
                } else {
                    errorMessages.add("cannot add output files");
                }
            } else {
                errorMessages.add("Issue with output directory");
            }
        }

        for (String err : errorMessages) {
            logger.error(err);
        }

        // Send the component output bakc to the workflow.
        System.out.println(this.getOutput());
    }
    /**
     * Return true if the user has access to the DetectorTesterAccess project
     */
    private boolean hasAccess() {
      String appContextPath = this.getApplicationContextPath();
      logger.info("appContextPath: " + appContextPath);

      // Do not follow symbolic links so we can prevent unwanted directory traversals if someone
      // does manage to create a symlink to somewhere dangerous (like /datashop/deploy/)
      if (Files.exists(Paths.get(appContextPath), LinkOption.NOFOLLOW_LINKS)) {
          /** Initialize the Spring Framework application context. */
          SpringContext.getApplicationContext(appContextPath);
      }

      UserDao userDao = DaoFactory.DEFAULT.getUserDao();
      
      String userId = this.getUserId();
      if (userId == null) { //for use with ant runComponent
        userId = "peterschaldenbrand@gmail.com";
      }

      UserItem userItem = userDao.get(userId);

      ProjectDao projectDao = DaoFactory.DEFAULT.getProjectDao();

      String accessProject = getAccessProjectName();
      if (accessProject == null) {
        logger.error("Couldn't find access project");
      }
      logger.debug("Access project name: " + accessProject);

      ArrayList<ProjectItem> projectsWithName = (ArrayList<ProjectItem>)projectDao.find(accessProject);
      
      logger.debug("projectsWithName " + projectsWithName);

      if (projectsWithName.size() != 1) {
        logger.error("No projects found with name " + accessProject + 
            ". Unable to grant access to component.");
      }

      ProjectItem detectorProject = projectsWithName.get(0);

      AuthorizationDao authorizationDao = DaoFactory.DEFAULT.getAuthorizationDao();

      Integer projectId = (Integer)detectorProject.getId();
      logger.debug("Project ID : " + projectId);

      String authorization = authorizationDao.getAuthorization(userId, projectId);
      logger.debug("Authorization level: " + authorization);
      if (authorization == null) {
        System.err.println("Could not find authorization data on user for project: " + accessProject);
        return false;
      }

      if (authorization.equals(AuthorizationItem.LEVEL_EDIT) 
          || authorization.equals(AuthorizationItem.LEVEL_ADMIN)) {
        return true;
      } else {
        System.err.println("User does not have edit or admin authorization to project " + accessProject +
            ".  Get access to this project to be able to run written code.");
        return false;
      }

    }

    private String getAccessProjectName() {
      String filename = this.getToolDir() + "build.properties";
      try {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        while(br.ready()) {
          String line = br.readLine();

          String [] toks = line.split("=");

          if (toks.length != 2) {
            continue;
          }

          if (toks[0].equals("authorizationProject")) {
            return toks[1];
          }
        }
      } catch (IOException e) {
        logger.error("Couldn't get authorizationProject from build properties. " + e.toString());
      }
      return null;
    }

}
