/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2015
 * All Rights Reserved
 *
 * Java wrapper for a component that converts an OLI skill model to a KC model file
 * -Peter
 */

package edu.cmu.pslc.learnsphere.oli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.cmu.pslc.datashop.dao.AuthorizationDao;
import edu.cmu.pslc.datashop.dao.CustomFieldDao;
import edu.cmu.pslc.datashop.dao.DaoFactory;
import edu.cmu.pslc.datashop.dao.DatasetDao;
import edu.cmu.pslc.datashop.dao.StepRollupDao;
import edu.cmu.pslc.datashop.dao.UserDao;
import edu.cmu.pslc.datashop.item.AuthorizationItem;
import edu.cmu.pslc.datashop.item.CustomFieldItem;
import edu.cmu.pslc.datashop.item.DatasetItem;
import edu.cmu.pslc.datashop.item.SampleItem;
import edu.cmu.pslc.datashop.item.SkillModelItem;
import edu.cmu.pslc.datashop.item.UserItem;
import edu.cmu.pslc.datashop.util.DataShopInstance;
import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.webservices.DatashopClient;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class OliToRISE extends AbstractComponent {

    /** Decimal format used for error rates. */
    private DecimalFormat decimalFormat = new DecimalFormat("0.000#");

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
        OliToRISE tool = new OliToRISE();
        tool.startComponent(args);
    }

    /**
     * Default constructor.
     */
    public OliToRISE() { super(); }

    @Override
    protected void runComponent() {

        Integer datasetId = this.getOptionAsInteger("dataset");
        if (datasetId == null) {
            addErrorMessage("Please specify a valid datasetId.");
            System.out.println(this.getOutput());
            return;
        }
        if (!initializeDao()) {
            addErrorMessage("Failed to initialize DAO; couldn't find applicationContext.xml.");
            System.out.println(this.getOutput());
            return;
        }

        // Confirm that user has access (at least 'view') to the specified dataset.
        UserItem user = getUser();
        DatasetItem dataset = DaoFactory.DEFAULT.getDatasetDao().get(datasetId);
        if (dataset == null) {
            addErrorMessage("Please specify a valid datasetId.");
            System.out.println(this.getOutput());
            return;
        }

        Boolean hasAccess = hasAccess(user, dataset, false);
        if (!hasAccess) {
            addErrorMessage("User does not have access to dataset: " + datasetId);
            System.out.println(this.getOutput());
            return;
        }

        // Confirm the dataset has high-stakes information.
        if (!getIsHighStakesAvailable(dataset)) {
            addErrorMessage("Specified dataset (" + datasetId + ") does not have high-stakes data.");
            System.out.println(this.getOutput());
            return;
        }

        String kcModel = this.getOptionAsString("kcModel");
        Map<String, Double[]> errorMap = getSkillErrorMap(dataset, kcModel);

        File outputFile = this.createFile("errorRates_output", ".txt");
        outputFile = populateOutputFile(outputFile, errorMap);
        this.addOutputFile(outputFile, 0, 0, "tab-delimited");

        System.out.println(this.getOutput());
    }

    /**
     * Helper method to initialize applicationContext for DAO access.
     *
     * @return flag inidicating success
     */
    private Boolean initializeDao() {

        Boolean result = false;

        // Dao-enabled components require an applicationContext.xml in the component directory,

        String appContextPath = this.getApplicationContextPath();

        // Do not follow symbolic links so we can prevent unwanted directory traversals if someone
        // does manage to create a symlink to somewhere dangerous (like /datashop/deploy/)
        if (Files.exists(Paths.get(appContextPath), LinkOption.NOFOLLOW_LINKS)) {
            /** Initialize the Spring Framework application context. */
            SpringContext.getApplicationContext(appContextPath);
            DataShopInstance.initialize();
            result = true;
        }

        return result;
    }

    /**
     * Helper method to get UserItem for specified userId.
     *
     * @return UserItem null if user not found
     */
    private UserItem getUser() {

        String userId = this.getUserId();
        if (userId == null) { return null; }

        UserDao userDao = DaoFactory.DEFAULT.getUserDao();
        UserItem user = userDao.get(userId);

        return user;
    }

    /**
     * Helper method to determine if user has access to specified dataset.
     * @param dataset the DatasetItem for the specified dataset
     * @param editAccess flag indicating to check for edit access. Default is view access.
     * @return Boolean result. True iff user has access to dataset
     */
    private Boolean hasAccess(UserItem user, DatasetItem dataset, Boolean editAccess) {

        if (user == null) { return false; }

        if (user.getAdminFlag()) { return true; }

        // Dataset not found means no access.
        if (dataset == null) { return false; }

        AuthorizationDao authorizationDao = DaoFactory.DEFAULT.getAuthorizationDao();
        String authLevel = authorizationDao.getAuthLevel(user, dataset);

        if (authLevel == null) { return false; }

        if (editAccess && (authLevel.equals(AuthorizationItem.LEVEL_VIEW))) {
            return false;
        }

        // At this point, any non-null value of authLevel means VIEW access is good
        return true;
    }

    /**
     * Helper to determine if high-stakes data is present for the specified dataset.
     * @param dataset the DatasetItem for the specified dataset
     * @return Boolean flag indicating presence of high-stakes CustomField
     */
    private Boolean getIsHighStakesAvailable(DatasetItem dataset) {

        CustomFieldDao cfDao = DaoFactory.DEFAULT.getCustomFieldDao();
        List<CustomFieldItem> cfList =
            cfDao.findMatchingByName("highStakes", dataset, true);
        return (cfList.size() > 0);
    }


    /**
     * Helper method to get map of skill names and high- and low-stakes error rates for
     * the sepcified dataset and KC model.
     * @param dataset the DatasetItem for the specified dataset
     * @param kcModel name of the specified KC model
     * @return Map with skill name as key and array of error rates as value
     */
    private Map<String, Double[]> getSkillErrorMap(DatasetItem dataset, String kcModel) {
        Map<String, Double[]> result = new HashMap<String, Double[]>();

        SampleItem sample = DaoFactory.DEFAULT.getSampleDao().findOrCreateDefaultSample(dataset);
        SkillModelItem skillModel = DaoFactory.DEFAULT.getSkillModelDao().findByName(dataset, kcModel);
        if (skillModel == null) {
            addErrorMessage("Failed to find specified KC model: " + kcModel);
            return null;
        }

        StepRollupDao stepRollupDao = DaoFactory.DEFAULT.getStepRollupDao();
        Map<String, Double> hsErrorMap = stepRollupDao.getHighStakesError((Integer)sample.getId(), (Long)skillModel.getId());
        if (hsErrorMap == null) {
            addErrorMessage("Failed to get high-stakes error rates for KC model: " + kcModel);
            return null;
        }
        for (String s : hsErrorMap.keySet()) {
            Double[] errorRates = new Double[2];
            if (hsErrorMap.get(s) != null) {
                errorRates[0] = hsErrorMap.get(s);
                result.put(s, errorRates);
            }
        }

        Map<String, Double> lsErrorMap = stepRollupDao.getLowStakesError((Integer)sample.getId(), (Long)skillModel.getId());
        if (lsErrorMap == null) {
            addErrorMessage("Failed to get low-stakes error rates for KC model: " + kcModel);
            return null;
        }
        for (String s : lsErrorMap.keySet()) {
            Double[] errorRates = result.get(s);
            if (errorRates == null) {
                errorRates = new Double[2];
                errorRates[0] = null;
            }
            errorRates[1] = lsErrorMap.get(s);
            result.put(s, errorRates);
        }

        return result;
    }

    // Constant
    private static final String NEW_LINE_CHAR = "\n";

    // Constant
    private static final String TAB_CHAR = "\t";

    /**
     * Write the results to an output file.
     * @param theFile the File to write to
     * @param map the error rates by skill
     * @return the populated file
     */
    private File populateOutputFile(File theFile, Map<String, Double[]> errorRateMap) {

        if (errorRateMap == null) { return theFile; }

        // Java try-with-resources
        OutputStream outputStream = null;
        try {
            // header
            outputStream = new FileOutputStream(theFile);
            outputStream.write("skill".getBytes("UTF-8"));
            outputStream.write(TAB_CHAR.getBytes("UTF-8"));
            outputStream.write("high-stakes error".getBytes("UTF-8"));
            outputStream.write(TAB_CHAR.getBytes("UTF-8"));
            outputStream.write("low-stakes error".getBytes("UTF-8"));
            outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));            

            for (String s : errorRateMap.keySet()) {
                Double[] errorRates = errorRateMap.get(s);
                outputStream.write(s.getBytes("UTF-8"));
                outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                String errRateStr = "";
                if (errorRates[0] != null) {
                    errRateStr = decimalFormat.format(errorRates[0]);
                }
                outputStream.write(errRateStr.getBytes("UTF-8"));
                outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                if (errorRates[1] != null) {
                    errRateStr = decimalFormat.format(errorRates[1]);
                } else {
                    errRateStr = "";
                }
                outputStream.write(errRateStr.getBytes("UTF-8"));
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));            
            }

        } catch (Exception e) {
            // This will be picked up by the workflows platform and relayed to the user.
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) { outputStream.close(); }
            } catch (IOException e) {
            }
        }

        return theFile;
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
        logger.info("Processing options.");
    }
}

