package edu.cmu.pslc.learnsphere.analysis.resourceuse.oli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.cmu.pslc.datashop.item.SampleItem;
import edu.cmu.pslc.datashop.item.UserItem;
import edu.cmu.pslc.datashop.servlet.export.StepRollupExportHelper;

/* If we want access to dao and helpers, simply enable the hibernate/spring class paths in the build.xml
   and include the following libraries. */
import edu.cmu.pslc.datashop.dao.DaoFactory;
import edu.cmu.pslc.datashop.dao.SampleDao;
import edu.cmu.pslc.datashop.dao.UserDao;
import edu.cmu.pslc.datashop.servlet.HelperFactory;

/* Workflow includes. */
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.util.FileUtils;
import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.util.VersionInformation;



public class OLIDataGenerator extends AbstractComponent {
    private OLIDataImporter oliImporter;
    private OLIDataAggregator oliDataAggregator;

    private static String FILE_TYPE_TRANSACTION = "tab-delimited";
    private static String FILE_TYPE_USER_SESS_MAP = "user-sess-map";
    private static String FILE_TYPE_RESOURCE_USE = "resource-use";

    public static void main(String[] args) {

        OLIDataGenerator tool = new OLIDataGenerator();
        tool.startComponent(args);

    }

    public OLIDataGenerator() {
        super();

        oliImporter = new OLIDataImporter();
        oliDataAggregator = new OLIDataAggregator();
    }


    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // If you want to add all headers from a previous component, try one of these:
        //this.addMetaDataFromInput("transaction", 0, 0, ".*");
        //this.addMetaDataFromInput("user-session-map", 1, 0, ".*");
        
        // Instead, we already know the headers we plan to create
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header0", 0, "student");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header1", 1, "student");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header2", 2, "action count");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header3", 3, "action time");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header4", 4, "page view count");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header5", 5, "page view time");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header6", 6, "action to page view count");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header7", 7, "action to page view time");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header8", 8, "page view to action count");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header9", 9, "page view to action time");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header10", 10, "media play count");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header11", 11, "media play time");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header12", 12, "media play to view page count");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header13", 13, "media play to view page time");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header14", 14, "action to media play count");
        this.addMetaData(FILE_TYPE_RESOURCE_USE, 0, META_DATA_HEADER, "header15", 15, "action to media play time");
    }

    @Override
    protected void runComponent() {

        // Dao-enabled components require an applicationContext.xml in the component directory,

        String appContextPath = this.getApplicationContextPath();
        logger.info("appContextPath: " + appContextPath);

        // Do not follow symbolic links so we can prevent unwanted directory traversals if someone
        // does manage to create a symlink to somewhere dangerous (like /datashop/deploy/)
        if (Files.exists(Paths.get(appContextPath), LinkOption.NOFOLLOW_LINKS)) {
            /** Initialize the Spring Framework application context. */
            SpringContext.getApplicationContext(appContextPath);
        }
        //in the test xml file, the transaction file (node 0, file 0) is specified first
        // and user-sess (node 1, file 0) is specified second
        oliImporter.setTransactionFileName(this.getAttachment(0, 0).getAbsolutePath());
        logger.info("Transaction file: " + oliImporter.getTransactionFileName());
        oliImporter.setUserSessFileName(this.getAttachment(1, 0).getAbsolutePath());
        logger.info("User sess file: " + oliImporter.getUserSessFileName());

        try {
                // do the work
                oliImporter.importData();
                logger.info("OLI importer imported data, transaction file id: " +
                                oliImporter.getResourceUseOliTransactionFileId() + "; user-sess file id: " +
                                oliImporter.getResourceUseOliUserSessFileId());
                logger.info("OLI data generator, aggregation started...");
                //System.out.println("OLI data generator, aggregation started...");
                oliDataAggregator.setResourceUseOliTransactionFileId(oliImporter.getResourceUseOliTransactionFileId());
                oliDataAggregator.setResourceUseOliUserSessFileId(oliImporter.getResourceUseOliUserSessFileId());
                String resourceUseAggregatedData = oliDataAggregator.aggregateData();
                logger.info("OLI data generator, aggregation ended...");
                //make File and write content
                File componentOutputFile = this.createFile(FILE_TYPE_RESOURCE_USE, ".txt");
                FileUtils.dumpToFile(resourceUseAggregatedData, componentOutputFile, true);
                logger.info("OLI importer, file created: " + componentOutputFile.getAbsolutePath());
                Integer nodeIndex = 0;
                Integer fileIndex = 0;
                String fileLabel = FILE_TYPE_RESOURCE_USE;
                this.addOutputFile(componentOutputFile, nodeIndex, fileIndex, fileLabel);
                oliImporter.clearData();
                System.out.println(this.getOutput());
        } catch (ResourceUseOliException exception) {
                String errorMsg = "OLIDataImporter/Aggregator exception caught: " + exception.getErrorMessage();
                logger.error(errorMsg);
                this.addErrorMessage(errorMsg);
        } catch (Throwable throwable) {
            logger.error("Unknown error in main method.", throwable);
            this.addErrorMessage("Unknown error in main method." + throwable);
        } finally {
            logger.info("OLIDataGenerator done.");
        }

        // WorkflowHelper can also be used for a lot of convenient java methods dealing with xml and json.
        // WorkflowHelper workflowHelper = HelperFactory.DEFAULT.getWorkflowHelper();
    }

}
