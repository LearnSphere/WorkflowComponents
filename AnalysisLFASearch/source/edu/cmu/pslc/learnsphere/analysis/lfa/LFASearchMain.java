package edu.cmu.pslc.learnsphere.analysis.lfa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.util.FileUtils;

import edu.cmu.pslc.statisticalCorrectnessModeling.utils.FileHelper;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.dataStructure.NamedMatrix;
// The PenalizedAFMTransferModel applies the AFM to the student-step data.
import edu.cmu.pslc.afm.transferModel.AFMTransferModel;
import edu.cmu.pslc.afm.transferModel.PenalizedAFMTransferModel;
// The AbstractComponent class is required by each component.
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.workflows.InputHeaderOption;
import edu.cmu.pslc.lfa.dataStructures.AICHeuristic;
import edu.cmu.pslc.lfa.dataStructures.BICHeuristic;
import edu.cmu.pslc.lfa.dataStructures.Heuristic;
import edu.cmu.pslc.lfa.dataStructures.Problem;
import edu.cmu.pslc.lfa.dataStructures.ProblemTranferModelSplit;
import edu.cmu.pslc.lfa.searchFunctions.LFASearch;
/**
 * Workflow component: LFA search main class.
 */
public class LFASearchMain extends AbstractComponent {
        private static final Integer EXPANDED_STATE_SIZE = 1000;
        private static final Integer unEXPANDED_STATE_SIZE = 1000;
        private static final boolean MULTI_CORE = false;
        private static final Integer NUMBER_OF_CORE = 1;
        private static final int MAX_ROW_NUMBER = 10000;
        private static final int MAX_STUDENT_NUMBER = 100;
        private static final int MAX_P_SKILL_NUMBER = 100;
        

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

            LFASearchMain tool = new LFASearchMain();
        tool.startComponent(args);
    }

    /**
     * This class runs LFA.
     */
    public LFASearchMain() {
        super();
    }


    @Override
    protected void processOptions() {
        logger.info("Processing Options");
    }

    
    @Override
    protected void runComponent() {
            int nodeIndex = 0;
        String heuristic = this.getOptionAsString("heuristic");
        //Maximum Search Iteration: search stops after reaching this number of iterations; max allowed is 200.
        int maxIter = this.getOptionAsInteger("maxIter");
        if (maxIter >= 200) {
                String errMsgForUI = "Maximum iteration can't be over 200.";
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        } else if (maxIter <= 0) {
                String errMsgForUI = "Maximum iteration can't be 0 or negative.";
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        }
        //Number of Top Models to be Outputed, 
        int numOfOutputModels  = this.getOptionAsInteger("numOfOutputModels");
        if (numOfOutputModels >= 200 ) {
                String errMsgForUI = "Number of top models to be output can't be over 200.";
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        } else if (numOfOutputModels <= 0) {
                String errMsgForUI = "Number of top models to be output can't be 0 or negative.";
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        }
        //Stop Search When Heuristic is Repeated This Many Times
        int stopRepCnt  = this.getOptionAsInteger("stopRepCnt");
        if (stopRepCnt <= 0) {
                stopRepCnt = 0;
        }
        //KC Models to be Used for Difficulty Factors: 
        //and multi-skill models are not allowed, 
        //models should have the same number of observations
        List<InputHeaderOption> inputHeaderOptionsForPMat = this.getInputHeaderOption("modelPMatrix", nodeIndex);
        List<String> modelsForPMat = new ArrayList<String>();
        for (InputHeaderOption headerOpt : inputHeaderOptionsForPMat) {
                modelsForPMat.add(headerOpt.getOptionValue());
        }
        File inputFile = this.getAttachments(nodeIndex).get(0);
        logger.info("inputFile: " + inputFile.getAbsolutePath());
        //at least two models should be selected
        if (modelsForPMat.size()<2) {
                String errMsgForUI = "At least two models must be selected for difficulty factors.";
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        }
        //and multi-skill models are not allowed
        //models should have the same number of observations
        String delim = "\t";
        String[] headers = null;
        List<Integer> modelColIndices = new ArrayList<Integer>();
        try (BufferedReader bReader = new BufferedReader(new FileReader(inputFile));) {
                String line = bReader.readLine();
                headers = line.split(delim, -1);
                for (String model : modelsForPMat) {
                        for (int i = 0; i < headers.length; i++) {
                                if (headers[i].equals(model)) {
                                        modelColIndices.add(i);
                                        break;
                                }
                        } 
                }
                //keep observation number for each models
                List<Integer> modelObsNumbers = new ArrayList<Integer>();
                for (int i = 0; i < modelColIndices.size(); i++) {
                        modelObsNumbers.add(0);
                }
                line = bReader.readLine();
                while (line != null) {
                        String row[] = line.split(delim, -1);
                        if (row.length != headers.length) {
                                String errMsgForUI = "Import file is not in correct format. Some rows are missing columns.";
                                String errMsgForLog = errMsgForUI;
                                handleAbortingError (errMsgForUI, errMsgForLog);
                                bReader.close();
                                return;
                        }
                        for (int i = 0; i < modelColIndices.size(); i++) {
                                int modelColIndex = modelColIndices.get(i);
                                String modelCellValue = row[modelColIndex];
                                if (modelCellValue.indexOf("~~") != -1) {
                                        String errMsgForUI = "Multi-skilled models are not allowed.";
                                        String errMsgForLog = errMsgForUI;
                                        handleAbortingError (errMsgForUI, errMsgForLog);
                                        bReader.close();
                                        return;
                                }
                                if (modelCellValue != null && !modelCellValue.trim().equals("")) {
                                        if (modelObsNumbers.get(i) == null) {
                                                modelObsNumbers.set(i, 1);
                                        } else {
                                                modelObsNumbers.set(i, modelObsNumbers.get(i)+1);
                                        }
                                }
                        }
                        line = bReader.readLine();
                }
                bReader.close();
                int obsNum = 0;
                for (int modelObsNumber : modelObsNumbers) {
                        if (obsNum == 0)
                                obsNum = modelObsNumber;
                        else if (obsNum != modelObsNumber) {
                                String errMsgForUI = "Models picked for difficulty factor can not have different number of observations.";
                                String errMsgForLog = errMsgForUI;
                                handleAbortingError (errMsgForUI, errMsgForLog);
                                bReader.close();
                                return;
                        }
                }
                //input file can't have more than MAX_ROW_NUMBER rows
                if (obsNum > MAX_ROW_NUMBER) {
                        String errMsgForUI = "Number of data rows exceeds limit. Allowed: " + MAX_ROW_NUMBER + "; found: " + obsNum + ".";
                        String errMsgForLog = errMsgForUI;
                        handleAbortingError (errMsgForUI, errMsgForLog);
                        return;
                }
        } catch (IOException ioe) {
                String errMsgForUI = "Error reading input file: " + inputFile.getAbsolutePath();
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        }
        //first clean up all exsiting files and dir
        logger.info(componentOutputDir);
        File outputDir = new File(componentOutputDir);
        if (outputDir != null && outputDir.isDirectory()) {
                File[] files = outputDir.listFiles();
                        if(files!=null) { 
                            for(File f: files) {
                                if(f.isDirectory()) {
                                        try {
                                                FileUtils.forceDelete(f);
                                        } catch (IOException ioe) {
                                                String errMsgForUI = "Error deleting file/dir in current working directory: " + componentOutputDir;
                                                String errMsgForLog = errMsgForUI;
                                                handleAbortingError (errMsgForUI, errMsgForLog);
                                                return;
                                        }
                                } else {
                                    f.delete();
                                }
                            }
                        }
        }
        //make sssvs and  matrix files
        final int FIRST_KNOWLEDGE_COMPONENT_EXPORT_COLUMN = 19;
        final int COLUMNS_IN_MODEL = 3;
        PenalizedAFMTransferModel model = new PenalizedAFMTransferModel();
        String matricesNames = "";
        String matricesFileNames = "";
        String SSSFile = "";
        for (int i = 0; i < modelColIndices.size(); i++) {
                int modelIndex = (modelColIndices.get(i) - FIRST_KNOWLEDGE_COMPONENT_EXPORT_COLUMN)/COLUMNS_IN_MODEL;
                String[][] sssvs =  FileHelper.getSSSVSFromDatashopExportV7(inputFile.getAbsolutePath(), modelIndex, false);
                if (sssvs == null || sssvs.length == 0)
                        continue;
                model.init(sssvs, false);
                String modelName = headers[modelColIndices.get(i)];
                modelName = modelName.substring(modelName.indexOf("(") + 1, modelName.indexOf(")"));
                //delete special character
                modelName.replaceAll("\\\\", "bslash");
                modelName.replaceAll("/", "fslash");
                modelName.replaceAll("|", "pipe");
                modelName.replaceAll(":", "colons");
                modelName.replaceAll("\\*", "star");
                modelName.replaceAll("\"", "quote");
                modelName.replaceAll("\\?", "question");
                modelName.replaceAll("<", "lt");
                modelName.replaceAll(">", "gt");
                IOUtil.writeString2DArray(model.getQ().toString2D(), componentOutputDir + modelName + "Q.txt");
                if (matricesNames.equals(""))
                        matricesNames = modelName;
                else
                        matricesNames += "," + modelName;
                if (matricesFileNames.equals(""))
                        matricesFileNames = modelName + "Q.txt";
                else
                        matricesFileNames += "," + modelName + "Q.txt";
                if (i == 0) {
                        IOUtil.writeString2DArray(model.getAFMDataObject().computeMinSSS(), componentOutputDir + "SSS.txt");
                        SSSFile = "SSS.txt";
                }
        }
        LFASearch lfaSearch = new LFASearch();
        AFMTransferModel initModel = lfaSearch.setupSearchWithDatashopExportFiles(componentOutputDir, componentOutputDir, matricesNames, matricesFileNames, SSSFile);
        if (initModel == null) {
                String errMsgForUI = "Error found for initial model ";
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        }
        if (initModel.getStudentParameters() != null && initModel.getStudentParameters().length > MAX_STUDENT_NUMBER) {
                String errMsgForUI = "Number of students exceeds limit. Allowed: " + MAX_STUDENT_NUMBER + ", found: " + initModel.getStudentParameters().length + ".";
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        }
        //p matrix can't have more than MAX_P_SKILL_NUMBER skills
        NamedMatrix pMat = new NamedMatrix(IOUtil.read2DStringArray(componentOutputDir + "MultiModelCombinedPMatrix.txt"));
        if (pMat.columns() > MAX_P_SKILL_NUMBER) {
                String errMsgForUI = "Number of skills in P matrix exceeds limit. Allowed: " + MAX_P_SKILL_NUMBER + ", found: " + pMat.columns() + ".";
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        }
        Heuristic h = null;
        if (heuristic != null && heuristic.equalsIgnoreCase("BIC"))
                h = new BICHeuristic();
        else
                h = new AICHeuristic();
        try {
                logger.info("LFA search starts: " + (new Date()));
                lfaSearch.init(maxIter, EXPANDED_STATE_SIZE, unEXPANDED_STATE_SIZE, h, initModel);
                if (MULTI_CORE) {
                        lfaSearch.setNumberOfThreads(NUMBER_OF_CORE);
                        lfaSearch.setOnMultiCore(true);
                } else
                        lfaSearch.setOnMultiCore(false);
                //lfaSearch.setOutputTopStateCount(6);
                //lfaSearch.setOutputTopStateInterval(1);
                
                lfaSearch.setOutputFinalStateCount(numOfOutputModels);
                lfaSearch.setStopRepetitionCount(stopRepCnt);
                Problem p = new ProblemTranferModelSplit(h, 100);
                lfaSearch.search(p);
                lfaSearch.outputTopStates(-1, true);
        } catch (Exception ex) {
                String errMsgForUI = "Error found during search: " + ex.getMessage();
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        }
        
        File allFile = this.createFile("all", ".txt");
        File multiModelCombinedPMatrixFile = this.createFile("MultiModelCombinedPMatrix", ".txt");
        File allModelsTxtFile = this.createFile("allModels", ".txt");
        File allModelsZipFile = this.createFile("allModels", ".zip");
        String finalResultFolder = componentOutputDir + File.separator + "final";
         
        //copy the allModels.txt from final folder to top working folder
        File fileIn = new File(finalResultFolder + File.separator + "allModels.txt");
        if (fileIn != null && fileIn.exists()) {
                try {
                        org.apache.commons.io.FileUtils.copyFile(fileIn, allModelsTxtFile);
                } catch (IOException ioe) {
                        String errMsgForUI = "Error coping allModels.txt file " + componentOutputDir;
                        String errMsgForLog = errMsgForUI;
                        handleAbortingError (errMsgForUI, errMsgForLog);
                        return;
                }
        }
        //zip final folder into allModels.zip
        try {
                compress(finalResultFolder, componentOutputDir + File.separator + "allModels.zip");
                
        } catch (IOException ioe) {
                String errMsgForUI = "Error zipping files in final folder for folder: " + componentOutputDir;
                String errMsgForLog = errMsgForUI;
                handleAbortingError (errMsgForUI, errMsgForLog);
                return;
        }
        /*nodeIndex = 0;
        Integer fileIndex = 0;
        String label = "tab-delimited";
        this.addOutputFile(allModelsTxtFile, nodeIndex, fileIndex, label);
        */
        
        nodeIndex = 0;
        Integer fileIndex = 0;
        String label = "analysis-summary";
        this.addOutputFile(allModelsTxtFile, nodeIndex, fileIndex, label);
        label = "tab-delimited";
        nodeIndex = 1;
        this.addOutputFile(allFile, nodeIndex, fileIndex, label);
        nodeIndex = 2;
        this.addOutputFile(multiModelCombinedPMatrixFile, nodeIndex, fileIndex, label);
        nodeIndex = 3;
        label = "zip";
        this.addOutputFile(allModelsZipFile, nodeIndex, fileIndex, label);
        
        System.out.println(this.getOutput());
    }

    private void handleAbortingError (String errMsgForUI, String errMsgForLog) {
            addErrorMessage(errMsgForUI);
            logger.info("AnalysisLFASearch aborted: " + errMsgForLog );
            System.out.println(getOutput());
            return; 
    }
    
    private static void compress(String dirPath, String zipFileName) throws IOException {
            final Path sourceDir = Paths.get(dirPath);
            final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                        Path targetFile = sourceDir.relativize(file);
                        outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                        byte[] bytes = Files.readAllBytes(file);
                        outputStream.write(bytes, 0, bytes.length);
                        outputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
            outputStream.close();
            
        }
    
    
}
