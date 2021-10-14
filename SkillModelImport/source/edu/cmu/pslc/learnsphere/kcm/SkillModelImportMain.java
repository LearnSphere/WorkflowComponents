/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2021
 * All Rights Reserved
 *
 * Java wrapper for a component that imports a skill model to a specified DataShop dataset.
 */

package edu.cmu.pslc.learnsphere.kcm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URLEncoder;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.cmu.pslc.datashop.dao.AuthorizationDao;
import edu.cmu.pslc.datashop.dao.DatasetDao;
import edu.cmu.pslc.datashop.dao.DaoFactory;
import edu.cmu.pslc.datashop.dao.UserDao;
import edu.cmu.pslc.datashop.item.AuthorizationItem;
import edu.cmu.pslc.datashop.item.DatasetItem;
import edu.cmu.pslc.datashop.item.UserItem;
import edu.cmu.pslc.datashop.util.DataShopInstance;
import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.webservices.DatashopClient;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

import static edu.cmu.pslc.datashop.util.FileUtils.cleanForFileSystem;

public class SkillModelImportMain extends AbstractComponent {

    private Transformer transformer = null;

    private List<File> modelValueFiles = new ArrayList<File>();
    private List<File> kcmExportFiles = new ArrayList<File>();

    private Boolean outputZip = false;
    
    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
        SkillModelImportMain tool = new SkillModelImportMain();
        
        tool.startComponent(args);
    }

    public SkillModelImportMain() { super(); }

    @Override
    protected void runComponent() {

        Integer datasetId = this.getOptionAsInteger("datasetId");
        if (datasetId != null) {
            logger.debug("Specified datasetId = " + datasetId);

            // Confirm user has access and can import new KC models.
            UserItem user = getUser();
            Boolean hasAccess = hasAccess(user, datasetId, true);

            File inputFile = this.getAttachment(0, 0);

            // All models included in the file will be imported. If so, the newModelName
            // string returned will be a comma-separated list of kcm names.
            String newModelName = null;
            
            if ((inputFile != null) && (hasAccess)) {
                newModelName = importNewKCModel(datasetId, user, inputFile);
            } else if (inputFile == null) {
                addErrorMessage("Unable to find input file.");
            } else {
                addErrorMessage("User does not have the access required to import a new KC model.");
            }

            // Note: if more than one model was in the file, all will be imported!
            // This means the newModelName string may be a comma-separated string of names.
            if (newModelName != null) {
                getKCModelResults(datasetId, user, newModelName);

                if (outputZip) {
                    this.addOutputFile(getZippedFiles(modelValueFiles, "model_values.zip"), 0, 0, "file");
                    this.addOutputFile(getZippedFiles(kcmExportFiles, "kcm_exports.zip"), 1, 0, "file");
                } else {
                    this.addOutputFile(modelValueFiles.get(0), 0, 0, "file");
                    this.addOutputFile(kcmExportFiles.get(0), 1, 0, "file");
                }
            }
        } else {
            addErrorMessage("Please specify a valid datasetId.");
        }

        System.out.println(this.getOutput());
    }

    private File getZippedFiles(List<File> fileList, String zipFileName) {

        File zipFile = this.createFile(zipFileName);
        
        ZipOutputStream zip = null;
        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            zip = new ZipOutputStream(fos);
            
            for (File f : fileList) {
                byte[] buf = new byte[1024];
                int len;
                FileInputStream in = new FileInputStream(f);
                zip.putNextEntry(new ZipEntry(f.getName()));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }

                // Delete files now that they've been zipped.
                f.delete();
            }

            if (zip != null) {
                zip.flush();
                zip.close();
            }
            
        } catch (Exception e) {
            logger.debug("Failed to create zip file. " + e);
            addErrorMessage("Failed to create zip file with results.");
        }

        return zipFile;
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

        // If unable to initialize appContext, return null.
        if (!initializeDao()) { return null; }
        
        String userId = this.getUserId();
        if (userId == null) { return null; }

        UserDao userDao = DaoFactory.DEFAULT.getUserDao();
        UserItem user = userDao.get(userId);

        return user;
    }

    /**
     * Helper method to determine if user has access to specified dataset.
     * 
     * @param datasetId the db id of the dataset. TBD: allow user to pick dataset from list in UI
     * @param editAccess flag indicating to check for edit access. Default is view access.
     * @return Boolean result. True iff user has access to dataset
     */
    private Boolean hasAccess(UserItem user, Integer datasetId, Boolean editAccess) {

        if (user == null) { return false; }

        if (user.getAdminFlag()) { return true; }

        DatasetDao dsDao = DaoFactory.DEFAULT.getDatasetDao();
        DatasetItem dataset = dsDao.get(datasetId);

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
     * Helper method to initialize XML transformer.
     */
    private void initializeTransformer() {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            transformer = tf.newTransformer();
            transformer.setOutputProperty(INDENT, "yes");
            transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } catch (Exception e) {
            logger.debug("Failed to initialize XML transformer: " + e);
        }
    }
    
    /**
     * Helper method to import KC model specified in file. Assumption is the specified
     * user has the necessary dataset access.
     *
     * NOTE: the webservice will attempt to import all models specified in the file!
     * 
     * @param datasetId the dataset id
     * @param user the logged in user
     * @param theFile the new KC model
     * @return name of newly-imported model, null if import failed
     */
    private String importNewKCModel(Integer datasetId, UserItem user, File theFile) {

        String modelNameStr = null;
        
        StringBuffer path = new StringBuffer();
        path.append("/datasets/" + datasetId).append("/importkcm/");

        File exportKcmFile = null;
        try {
            String localUrl = DataShopInstance.getDatashopUrl();
            String apiToken = user.getApiToken();
            String secret = user.getSecret();
            DatashopClient client = new DatashopClient(localUrl, apiToken, secret);
            if (client == null) { return null; }

            String contents = FileUtils.readFileToString(theFile, null);
            String resultXml = client.getPostService(path.toString(), contents, "text/plain");
            String message = getErrorMessage(resultXml);
            if (message != null) {
                addErrorMessage(message);
            } else {
                // Success. Get the name of the newly-imported KC model(s).
                modelNameStr = getModelName(resultXml);
            }

        } catch (Exception e) {
            addErrorMessage("Failed to import KC model file. " + e);
        }

        return modelNameStr;
    }

    /** Constant. */
    private static final String SUCCESS_CODE_STR = "0";

    /**
     * Helper method for parsing web service result for message.
     *
     * @param resultXml the string output
     * @return the message, null if call succeeded
     *
     */
    private String getErrorMessage(String resultXml) {

        if (resultXml == null) { return null; }

        // Not a web-service formatted response.
        if (resultXml.indexOf("pslc_datashop_message") == -1) { return null; }

        String result = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
             
            //Parse the content to Document object
            InputStream inputStream = new ByteArrayInputStream(resultXml.replaceAll("[\r\n]+", "").getBytes());
            Document doc = builder.parse(inputStream);
            Element rootEle = doc.getDocumentElement();
            String code = rootEle.getAttribute("result_code");
            // If an error is returned, return the message.
            if (!code.equals(SUCCESS_CODE_STR)) {
                result = rootEle.getAttribute("result_message");
            }
        } catch (Exception e) {
            logger.warn("Failed to parse results XML file. " + e);
        }

        return result;
    }

    private static final String IMPORT_SUCCESS_MSG_START = "Success. KCM(s): ";
    private static final String IMPORT_SUCCESS_MSG_END = " saved successfully.";
    private static final Integer successStartLen = IMPORT_SUCCESS_MSG_START.length();
    
    /**
     * Helper method for parsing web service result for name(s) of new KC model(s).
     *
     * If more than one model was imported, it will be a comma-separated list of names.
     *
     * @param resultXml the string output
     * @return the name, null if call succeeded
     *
     */
    private String getModelName(String resultXml) {

        if (resultXml == null) { return null; }

        // Not a web-service formatted response.
        if (resultXml.indexOf("pslc_datashop_message") == -1) { return null; }

        String result = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
             
            //Parse the content to Document object
            InputStream inputStream = new ByteArrayInputStream(resultXml.replaceAll("[\r\n]+", "").getBytes());
            Document doc = builder.parse(inputStream);
            Element rootEle = doc.getDocumentElement();
            String code = rootEle.getAttribute("result_code");
            // If success, parse the message to get the KC model name.
            if (code.equals(SUCCESS_CODE_STR)) {
                String msg = rootEle.getAttribute("result_message");
                Integer idx1 = msg.indexOf(IMPORT_SUCCESS_MSG_START);
                Integer idx2 = msg.indexOf(IMPORT_SUCCESS_MSG_END);
                result = msg.substring(idx1 + successStartLen, idx2);
            }
        } catch (Exception e) {
            logger.debug("Failed to parse results XML file. " + e);
            addErrorMessage("Failed to get results for new skill model(s).");
        }

        return result;
    }
    
    /**
     * Helper method to get KC model export, model values and write these to files.
     * @param datasetId the dataset id
     * @param user the logged in user
     * @param modelName
     */
    private void getKCModelResults(Integer datasetId, UserItem user, String modelName) {

        logger.debug("getKCModelResults: modelName = " + modelName);
        
        try {
            String localUrl = DataShopInstance.getDatashopUrl();
            String apiToken = user.getApiToken();
            String secret = user.getSecret();
            DatashopClient client = new DatashopClient(localUrl, apiToken, secret);
            if (client == null) { return; }

            String[] modelNames = modelName.split(",");

            // If more than one model was added, the output files are zip files.
            outputZip = (modelNames.length > 1);
            
            for (String s : modelNames) {
                int count = 0;
                while (modelNotAvailable(client, datasetId, s.trim()) && (count < 6)) {
                    logger.debug(count + ": modelNotAvailable for: " + s.trim());
                    Thread.sleep(10000); // sleep for 10 seconds and try again
                    count++;
                }
                if (count == 6) {
                    logger.debug("Failed to get updated dataset.");
                    addErrorMessage("Failed to get results for new skill model(s).");
                    break;
                }

                String cleanedModelName = cleanForFileSystem(s.trim());
                
                String modelValues = getModelValues(client, datasetId, s.trim());
                if (modelValues != null) {
                    File modelValuesFile = this.createFile("model_values_" + cleanedModelName + ".xml");
                    FileUtils.writeStringToFile(modelValuesFile, modelValues, Charset.defaultCharset().toString());
                    modelValueFiles.add(modelValuesFile);
                }
                
                String kcmExport = getKCModelExport(client, datasetId, s.trim());
                if (kcmExport != null) {
                    File kcmExportFile = this.createFile("kcm_" + cleanedModelName + "_export.txt");
                    FileUtils.writeStringToFile(kcmExportFile, kcmExport, Charset.defaultCharset().toString());
                    kcmExportFiles.add(kcmExportFile);
                }
            }

        } catch (Exception e) {
            addErrorMessage("Failed to get KC model results. " + e);
        }
        
    }

    /**
     * Helper method to query the server for the named KC model and return the
     * model values for the model as an XML string.
     */
    private String getModelValues(DatashopClient client, Integer datasetId, String modelName) {

        StringBuffer path = new StringBuffer();
        path.append("/datasets/" + datasetId).append("?verbose=true");

        try {
            String result = client.getService(path.toString(), "text/xml");

            if (result.indexOf(modelName) != -1) {
                // Model exists in dataset output; get it.
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
             
                //Parse the content to Document object
                InputStream inputStream = new ByteArrayInputStream(result.replaceAll("[\r\n]+", "").getBytes());
                Document doc = builder.parse(inputStream);

                // Get <dataset> element.
                Element dsEle = (Element)doc.getDocumentElement().getFirstChild();
                // Get <kc_model> elements.
                NodeList kcmNodes = dsEle.getElementsByTagName("kc_model");
                for (int i = 0; i < kcmNodes.getLength(); i++) {
                    Element element = (Element)kcmNodes.item(i);

                    // Get the <name> element.
                    Element nameEle = (Element)element.getChildNodes().item(0);

                    // The content of the <name> element is doc node.
                    Node nameNode = nameEle.getFirstChild();
                    String kcmNodeName = nameNode.getNodeValue();
                    if (kcmNodeName.equals(modelName)) {
                        return xmlToString(element);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get model values for new model: " + modelName + ". " + e);
            return null;
        }

        return null;
    }

    private String xmlToString(Element ele) {
        String result = null;

        if (transformer == null) { initializeTransformer(); }
        
        try {
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(ele), new StreamResult(writer));
            result = writer.getBuffer().toString();
        } catch (Exception e) {
            logger.debug("Failed to convert XML to string: " + e);
        }

        return result;
    }
    
    private String getKCModelExport(DatashopClient client, Integer datasetId, String modelName) {

        String result = null;
        try {
            String encodedName = URLEncoder.encode(modelName, "UTF-8");
            StringBuffer path = new StringBuffer();
            path.append("/datasets/" + datasetId).append("/exportkcm?kc_model=").append(encodedName);
            result = client.getService(path.toString(), "text/plain");
        
            // If successful, the 'result' is the contents of the export. Otherwise, there is an error message.
            String message = getErrorMessage(result);
            if (message != null) {
                addErrorMessage(message);
                result = null;
            }
        } catch (Exception e) {
            addErrorMessage("Failed to export KC model. " + e);
            result = null;
        }

        return result;
    }
    
    private Boolean modelNotAvailable(DatashopClient client, Integer datasetId, String modelName) {

        StringBuffer path = new StringBuffer();
        path.append("/datasets/" + datasetId).append("?verbose=true");

        try {
            String result = client.getService(path.toString(), "text/xml");
            return (result.indexOf(modelName) == -1);
        } catch (Exception e) {
            return false;
        }
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
    }

    @Override
    protected void processOptions() {
    }
}

