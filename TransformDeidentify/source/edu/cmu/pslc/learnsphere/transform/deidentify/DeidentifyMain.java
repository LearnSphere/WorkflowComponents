package edu.cmu.pslc.learnsphere.transform.deidentify;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;





import org.apache.commons.codec.digest.DigestUtils;

import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.dao.DaoFactory;
import edu.cmu.pslc.datashop.dao.StudentDao;
import edu.cmu.pslc.datashop.item.StudentItem;

public class DeidentifyMain extends AbstractComponent {

    private static final String FILE_TYPE_USER_MAP = "user-map";
    private static final String FILE_TYPE_TAB_DELIMITED = "tab-delimited";
    private static final String DEFAULT_DELIMITER = "\t";

    /** File for user map*/
    private File userMapFile = null;
    /** File to be deidentified */
    private File fileToDeidentify = null;
    //options
    private String[] defaultMapHeaders = {"Actual ID", "Anon ID"};
    private int mapAnonIdColInd = 1;
    private int mapActualIdColInd = 0;
    private int fileActualIdColInd = 0;
    private Boolean caseSensitive = false;
    private String delimiter = DEFAULT_DELIMITER;
    private String outputDelimiter = DEFAULT_DELIMITER;


    String mapAnonIdColName = null;
    String mapActualIdColName = null;
    String fileActualIdColName = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

            DeidentifyMain tool = new DeidentifyMain();
            tool.startComponent(args);
    }
    /**
     * This class deidentify student and generate user map
     */
    public DeidentifyMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("user-map", 0, 0, ".*");
        this.addMetaDataFromInput("transaction", 1, 0, ".*");

    }

    /**
     * Parse the options list.
     */
    @Override protected void parseOptions() {
        if (this.getOptionAsString("mapAnonIdColumnName") != null) {
            mapAnonIdColName = this.getOptionAsString("mapAnonIdColumnName").replaceAll("(?i)\\s*(.*)\\s*", "$1");
        }
        if (this.getOptionAsString("mapActualIdColumnName") != null) {
            mapActualIdColName = this.getOptionAsString("mapActualIdColumnName").replaceAll("(?i)\\s*(.*)\\s*", "$1");
        }
        if (this.getOptionAsString("fileActualIdColumnName") != null) {
            fileActualIdColName = this.getOptionAsString("fileActualIdColumnName").replaceAll("(?i)\\s*(.*)\\s*", "$1");
        }
        if (this.getOptionAsBoolean("caseSensitive") != null) {
            caseSensitive = this.getOptionAsBoolean("caseSensitive");
        }
        if (this.getOptionAsString("delimiterPattern") != null) {
            delimiter = this.getOptionAsString("delimiterPattern");
        }
    }

    //set/get methods
    public void setUserMapFile(File userMapFile) {
            this.userMapFile = userMapFile;
    }
    public File getUserMapFile() {
            return this.userMapFile;
    }

    public void setFileToDeidentify(File fileToDeidentify) {
            this.fileToDeidentify = fileToDeidentify;
    }
    public File getFileToDeidentify() {
            return this.fileToDeidentify;
    }

    public void setMapAnonIdColInd(int mapAnonIdColInd) {
            this.mapAnonIdColInd = mapAnonIdColInd;
    }
    public int getMapAnonIdColInd() {
            return this.mapAnonIdColInd;
    }

    public void setMapActualIdColInd(int mapActualIdColInd) {
            this.mapActualIdColInd = mapActualIdColInd;
    }
    public int getMapActualIdColInd() {
            return this.mapActualIdColInd;
    }

    public void setFileActualIdColInd(int fileActualIdColInd) {
            this.fileActualIdColInd = fileActualIdColInd;
    }
    public int getFileActualIdColInd() {
            return this.fileActualIdColInd;
    }

    public void setCaseSensitive(Boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
    }
    public Boolean getCaseSensitive() {
            return this.caseSensitive;
    }

    public void setDelimiter(String delimiter) {
            this.delimiter = delimiter;
    }
    public String getDelimiter() {
            return this.delimiter;
    }

    /**
     * Joins the two files and adds the resulting file to the component output.
     */
    @Override
    protected void runComponent() {
            String appContextPath = this.getApplicationContextPath();
            logger.info("appContextPath: " + appContextPath);
            //System.out.println("appContextPath: " + appContextPath);

            // Do not follow symbolic links so we can prevent unwanted directory traversals if someone
            // does manage to create a symlink to somewhere dangerous (like /datashop/deploy/)
            if (Files.exists(Paths.get(appContextPath), LinkOption.NOFOLLOW_LINKS)) {
                /** Initialize the Spring Framework application context. */
                SpringContext.getApplicationContext(appContextPath);
            }
            // Input files


            userMapFile = this.getAttachment(0, 0);
            logger.info("user map file: " + userMapFile.getAbsolutePath());

            fileToDeidentify = this.getAttachment(1, 0);
            logger.info("file to be mapped: " + fileToDeidentify.getAbsolutePath());


        // Output files
        File deidentifiedFile = this.createFile("Deidentified", ".txt");
        File newUserMapFile = this.createFile("newUserMap", ".txt");
        // Options


        //set column index for actual id and anon id for map and fileToDeidentify
        if (mapAnonIdColName != null && !mapAnonIdColName.equals("")) {
                mapAnonIdColInd = getColumnIndex(mapAnonIdColName, userMapFile);
                if (mapAnonIdColInd == -1) {
                        String errorMsg = "Anonymous id column name: " + mapAnonIdColName + " in user map is invalid.";
                        logger.error(errorMsg);
                        this.addErrorMessage(errorMsg);
                        return;
                }
        }
        if (mapActualIdColName != null && !mapActualIdColName.equals("")) {
                mapActualIdColInd = getColumnIndex(mapActualIdColName, userMapFile);
                if (mapActualIdColInd == -1) {
                        String errorMsg = "Actual id column name: " + mapActualIdColName + " in user map is invalid.";
                        logger.error(errorMsg);
                        this.addErrorMessage(errorMsg);
                        return;
                }
        }
        if (fileActualIdColName != null && !fileActualIdColName.equals("")) {
                fileActualIdColInd = getColumnIndex(fileActualIdColName, fileToDeidentify);
                if (fileActualIdColInd == -1) {
                        String errorMsg = "Actual id column name: " + fileActualIdColName + " in data file is invalid.";
                        logger.error(errorMsg);
                        this.addErrorMessage(errorMsg);
                        return;
                }
        }

        // Processing
        //hashmap that stores actual id as the key and anonymous id as the value
        Map<String, String> studentIdPairsOfMap = new HashMap<String, String>();
        Map<String, String> studentIdPairsOfFile = new HashMap<String, String>();

        //load map file into studentIdPairsOfMap
        if (this.userMapFile != null)
                studentIdPairsOfMap = getStudentIdPairsFromMap();
        //get all distinct students in toBeDeidentifiedFile into studentIdPairsOfFile
        studentIdPairsOfFile = getStudentIdsFromFile();
        //loop thru studentIdPairsOfFile, for each student check:
        for (String thisStudentActualId  : studentIdPairsOfFile.keySet()) {
                //check if thisStudentActualId is in map
                String anonIdInMap = null;
                String actualIdInMap = null;
                for (Map.Entry<String, String> entry : studentIdPairsOfMap.entrySet()) {
                        String curActualId = entry.getKey();
                        String curAnonId = entry.getValue();
                        if ((caseSensitive && curActualId.equals(thisStudentActualId)) ||
                                        (!caseSensitive && curActualId.equalsIgnoreCase(thisStudentActualId))) {
                                anonIdInMap = curAnonId;
                                actualIdInMap = curActualId;
                                break;
                        }
                }
                //if in map, update studentIdPairsOfFile with map's anon student id
                //if not, check if the id is in DS database
                if (actualIdInMap != null && anonIdInMap != null && !anonIdInMap.trim().equals("")) {
                        studentIdPairsOfFile.put(thisStudentActualId, anonIdInMap);
                        continue;
                } else {
                        String anonIdInDS = getDSAnonStudentId(thisStudentActualId);
                        if (anonIdInDS != null && anonIdInDS.trim().equals("")) {
                                studentIdPairsOfFile.put(thisStudentActualId, anonIdInDS);
                                studentIdPairsOfMap.put(thisStudentActualId, anonIdInDS);
                                continue;
                        }
                }
                //if not found in DS
                String newAnonId = getStudentAnonId(thisStudentActualId);
                studentIdPairsOfFile.put(thisStudentActualId, newAnonId);
                studentIdPairsOfMap.put(thisStudentActualId, newAnonId);
        }

        //make the new map file
        writeMapToFile(studentIdPairsOfMap, newUserMapFile);
        //deidentify file
        deidentifyFile(studentIdPairsOfFile, deidentifiedFile);

        Integer fileIndex = 0;
        Integer nodeIndex = 0;
        this.addOutputFile(newUserMapFile, nodeIndex, fileIndex, "user-map");
        nodeIndex = 1;
        this.addOutputFile(deidentifiedFile, nodeIndex, fileIndex, "tab-delimited");

        System.out.println(this.getOutput());
    }

    private int getColumnIndex(String columnName, File file) {
            String[] fileHeaders = getHeaderFromFile(file, delimiter);
            int colCnt = 0;
            for (String fileHeader : fileHeaders) {
                    if (fileHeader.equals(columnName))
                            return colCnt;
                    colCnt ++;
            }
            return -1;
    }

    //load map file into studentIdPairsOfMap
    private Map<String, String> getStudentIdPairsFromMap() {
            Map<String, String> studentIdPairs = new HashMap<String, String>();
            List<String[]> data = getListOfStringArrayFromFile(userMapFile, delimiter, false);
            boolean firstRow = true;
            for (String[] row : data) {
                    if (firstRow) {
                            firstRow = false;
                            continue;
                    }
                    int maxCol = (mapActualIdColInd > mapAnonIdColInd) ? mapActualIdColInd : mapAnonIdColInd;
                    if (row != null && row.length > maxCol) {
                            //has to be a real actual id
                            String actualId = row[this.mapActualIdColInd];
                            if (actualId != null && !actualId.trim().equals(""))
                                    studentIdPairs.put(actualId, row[this.mapAnonIdColInd]);
                    }
            }
            return studentIdPairs;
    }

    //get all distinct students in toBeDeidentifiedFile into studentIdPairsOfFile
    private Map<String, String> getStudentIdsFromFile() {
            Map<String, String> studentIdPairs = new HashMap<String, String>();
            List<String[]> data = getListOfStringArrayFromFile(fileToDeidentify, delimiter, false);
            boolean firstRow = true;
            for (String[] row : data) {
                    if (firstRow) {
                            firstRow = false;
                            continue;
                    }
                    //has to be real actual id
                    String actualId = row[this.fileActualIdColInd];
                    if (actualId != null && !actualId.trim().equals(""))
                            studentIdPairs.put(actualId, "");
            }
            return studentIdPairs;
    }

    private String getDSAnonStudentId(String studentActualId) {
        // Have to go to mapping_db for actual student info
        edu.cmu.pslc.datashop.mapping.dao.StudentDao mappedStudentDao =
            edu.cmu.pslc.datashop.mapping.dao.DaoFactory.HIBERNATE.getStudentDao();

        Collection<edu.cmu.pslc.datashop.mapping.item.StudentItem> students = null;
        if (this.caseSensitive) {
            students = mappedStudentDao.find(studentActualId);
        } else {
            students = mappedStudentDao.findIgnoreCase(studentActualId);
        }
        if (students.size() > 0) {
            Iterator<edu.cmu.pslc.datashop.mapping.item.StudentItem> it = students.iterator();
            edu.cmu.pslc.datashop.mapping.item.StudentItem student = it.next();
            return student.getAnonymousUserId();
        }
        return null;
    }

    private void writeMapToFile(Map<String, String> studentIdPairs, File outputFile) {
            BufferedWriter bw = null;
            try {
                FileWriter fstream = new FileWriter(outputFile);
                bw = new BufferedWriter(fstream);
                outputFile.createNewFile();
                String osName = System.getProperty("os.name").toLowerCase();
                String newLine = "\n";
                if (osName.indexOf("win") >= 0) {
                        newLine = "\r\n";
                }
                //get headers
                String[] headers = null;
                if (userMapFile != null) {
                        headers = getHeaderFromFile(userMapFile, delimiter);
                } else {
                        headers = defaultMapHeaders;
                }
                //write headers to new file
                bw.append(concatenateStringArray(headers, outputDelimiter) + newLine);
                List<String> actulIdsInMapFile = new ArrayList<String>();
                int colCnt = headers.length;
                //current data in map file
                if (userMapFile != null) {
                        List<String[]> rows = this.getListOfStringArrayFromFile(userMapFile, delimiter, false);
                        for (int i = 1; i < rows.size(); i++) {
                                String[] row = rows.get(i);
                                String currentMapFileActualId = row[this.mapActualIdColInd];
                                if (studentIdPairs.containsKey(currentMapFileActualId)
                                                && studentIdPairs.get(currentMapFileActualId) != null
                                                && !studentIdPairs.get(currentMapFileActualId).trim().equals("")) {
                                        row[this.mapAnonIdColInd] = studentIdPairs.get(currentMapFileActualId);
                                        actulIdsInMapFile.add(currentMapFileActualId);
                                }
                                //write this row to new map file
                                bw.append(concatenateStringArray(row, outputDelimiter) + newLine);
                        }
                }
                //write new mapping in studentIdPairs to new map file
                for (Map.Entry<String, String> entry : studentIdPairs.entrySet()) {
                        String curActualId = entry.getKey();
                        String curAnonId = entry.getValue();
                        if (!actulIdsInMapFile.contains(curActualId)) {
                                String[] newRow = new String[colCnt];
                                for (int i = 0; i < colCnt; i++){
                                        if (i == this.mapActualIdColInd)
                                                newRow[i] = curActualId;
                                        else if (i == this.mapAnonIdColInd)
                                                newRow[i] = curAnonId;
                                        else
                                                newRow[i] = "";
                                }
                                bw.append(concatenateStringArray(newRow, outputDelimiter) + newLine);
                        }
                }
            } catch (Exception e) {
                    this.addErrorMessage(e.getMessage());
            } finally {
                try {
                    bw.flush();
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    private void deidentifyFile(Map<String, String> studentIdPairs, File outputFile) {
            BufferedWriter bw = null;
            try {
                FileWriter fstream = new FileWriter(outputFile);
                bw = new BufferedWriter(fstream);
                outputFile.createNewFile();
                String osName = System.getProperty("os.name").toLowerCase();
                String newLine = "\n";
                if (osName.indexOf("win") >= 0) {
                        newLine = "\r\n";
                }
                List<String[]> rows = this.getListOfStringArrayFromFile(this.fileToDeidentify, delimiter, false);
                boolean firstRow = true;
                for (String[] row : rows) {
                        if (firstRow) {
                                firstRow = false;
                        } else {
                                String currentDataFileActualId = row[this.fileActualIdColInd];
                                String currentDataFileAnonId = studentIdPairs.get(currentDataFileActualId);
                                if (currentDataFileAnonId != null && !currentDataFileAnonId.trim().equals(""))
                                        row[this.fileActualIdColInd] = currentDataFileAnonId;
                        }
                        bw.append(this.concatenateStringArray(row, outputDelimiter) + newLine);
                }
            } catch (Exception e) {
                    this.addErrorMessage(e.getMessage());
            } finally {
                try {
                    bw.flush();
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    /**
     *  Encrypts the current actual studentId.
     */
    private String getStudentAnonId(String studentId) {
        String prefix = "stu_";
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[20];
        random.nextBytes(bytes);
        String salt = bytes.toString();
        return (prefix + DigestUtils.md5Hex(studentId + salt));

    }

    /**
     * Given a File, read and return the first line.
     * @param file the File to read
     * @param field delimiter
     * @return String[] first line of the file which is hopefully the column headings
     * @throws ResourceUseException could occur while opening the file
     */
    public String[] getHeaderFromFile(File file, String delimiter) {
        String[] headers = null;
        try {
                Scanner sc = new Scanner(file);
                headers = sc.nextLine().split(delimiter, -1);
                if (headers.length == 1 && headers[0].length() == 1)
                        headers = new String[0];
                sc.close();
        } catch (FileNotFoundException fEx) {
                this.addErrorMessage(fEx.getMessage());
        }
        return headers;
    }


    /**
     * Turn File into Map structure. The column index is the key. The rest are put in a LinkedList.
     * @param File file whose content to be extracted
     * @param String delimiter field delimiter
     * @param boolean skipEmptyLine skip empty line or not
     * @param boolean includeFirstRow whether to include first row. When first row is header, use false
     * @param colIndex the column index to join on
     * @return Map<String, List<String>> first column becomes the key, turn the rest columns into List<String>
     * @throws ResourceUseException */
    public Map<String, List<String>> turnFileContentToMap(File file, String delimiter,
            boolean skipEmptyLine, boolean includeFirstRow, int colIndex) {

            Map<String, List<String>> dataInMap = new HashMap<String, List<String>>();
            List<String[]> data = getListOfStringArrayFromFile(file, delimiter, skipEmptyLine);
            boolean firstRow = true;
            for (String[] row : data) {
                    if (!includeFirstRow && firstRow) {
                            firstRow = false;
                            continue;
                    }
                    String key = row[colIndex];
                    List<String> rowList = new LinkedList<String>(Arrays.asList(row));
                    rowList.remove(colIndex);
                    dataInMap.put(key, rowList);
                    //System.warn.println(key +" " + Arrays.asList(rowList).toString());
                    firstRow = false;
            }
            return dataInMap;
    }


    /**
     * Given a File, read and return list of string array. Skip empty line.
     * @param file the File to read
     * @param field delimiter
     * @param boolean skipEmptyLine skip empty line or not
     * @return List<String[]> the content in List of string array
     * @throws ResourceUseException could occur while opening the file
     */
    private List<String[]> getListOfStringArrayFromFile(File file, String delimiter, boolean skipEmptyLine) {
            List<String[]> output = new ArrayList<String[]>();
            try {
                    Scanner sc = new Scanner(file);
                    while (sc.hasNextLine()) {
                            String[] cols = sc.nextLine().split(delimiter, -1);
                            if (skipEmptyLine) {
                                    if (cols.length == 0 || (cols.length == 1 && cols[0].length() == 1))
                                            continue;
                            }
                            output.add(cols);
                    }
                    sc.close();
            } catch (FileNotFoundException fEx) {
                    this.addErrorMessage("File: " + file.getAbsolutePath() + ", not found. " + fEx.getMessage());
            }
            return output;
    }

    private String concatenateStringArray (String[] strArray, String delimiter) {
            StringBuffer sb = new StringBuffer();
            if (strArray == null) {
                    return null;
            }
            for (int i = 0; i < strArray.length; i++) {
                    if (strArray[i] != null)
                            sb.append(strArray[i]);
                    if (i < strArray.length -1)
                            sb.append(delimiter);
            }
            return sb.toString();
    }

}
