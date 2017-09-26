package edu.cmu.pslc.learnsphere.analysis.Rglm;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class RglmMain extends AbstractComponent {
        private static String[] R_REQUIRED_OPTIONS= {"modelingType", "family", "response", "terms", "fixedEffects", "randomEffects"}; 

    public static void main(String[] args) {

        RglmMain tool = new RglmMain();
        tool.startComponent(args);
    }

    public RglmMain() {
        super();
    }

    @Override
    protected void runComponent() {
            String fileName = this.getAttachment(0, 0).getAbsolutePath();
            String modelingTypeOpt = this.getOptionAsString("modelingType");
            String familyOpt = this.getOptionAsString("family");
            String responseOpt = this.getOptionAsString("response");
            String response = responseOpt.replaceAll("[\\(\\[\\]\\)\\-\\s]", ".");
            String formula = response + " ~ ";
            String fixedEffectsOpt = this.getOptionAsString("fixedEffects");
            String randomEffectsOpt = this.getOptionAsString("randomEffects");
            
            //handle errors due to required field empty
            if (modelingTypeOpt.equals("glm") || modelingTypeOpt.equals("lm")) {
                    if (fixedEffectsOpt == null || fixedEffectsOpt.equals("")) {
                            String errMsg = "Fixed effects is required for lm or glm function.";
                            addErrorMessage(errMsg);
                            logger.info(errMsg);
                            System.err.println(errMsg);
                            return;
                    }
            } else if (modelingTypeOpt.equals("glmer") || modelingTypeOpt.equals("lmer")) {
                    if ((randomEffectsOpt == null || randomEffectsOpt.equals(""))){
                            String errMsg = "Random effects is required for lmer or glmer function.";
                            addErrorMessage(errMsg);
                            logger.info(errMsg);
                            System.err.println(errMsg);
                            return;
                    }
            }
            
            //handle formula
            String fixedEffectsForFormula = "";
            String randomEffectsForFormula = "";
            if (fixedEffectsOpt != null && !fixedEffectsOpt.equals("")) {
                    List<String> fixedEffectsList = Arrays.asList(fixedEffectsOpt.split("\\s*,\\s*"));
                    if (fixedEffectsList != null) {
                            int cnt = 0;
                            for (String fixedEffectTerm : fixedEffectsList) {
                                  //replace [ ()-] with .
                                    fixedEffectsForFormula += fixedEffectTerm.replaceAll("[\\(\\[\\]\\)\\-\\s]", ".");
                                   if (cnt < fixedEffectsList.size() - 1) {
                                           fixedEffectsForFormula += " + ";  
                                   }
                                   cnt ++;
                            }
                    }
            }
            if ((modelingTypeOpt.equals("glmer") || modelingTypeOpt.equals("lmer")) 
                            && randomEffectsOpt != null && !randomEffectsOpt.equals("")) {
                    List<String> randomEffectsList = Arrays.asList(randomEffectsOpt.split("\\s*,\\s*"));
                    if (randomEffectsList != null) {
                            int cnt = 0;
                            for (String randomEffect : randomEffectsList) {
                                    //make sure the space around "|" is deleted
                                    String validPattern =  "\\s*\\|\\s*";
                                    Pattern pattern = Pattern.compile(validPattern);
                                    Matcher matcher = pattern.matcher(randomEffect);
                                    randomEffect = matcher.replaceAll("|");
                                    //make sure all space around "+" is removed
                                    validPattern =  "\\s*\\+\\s*";
                                    pattern = Pattern.compile(validPattern);
                                    matcher = pattern.matcher(randomEffect);
                                    randomEffect = matcher.replaceAll("+");
                                    randomEffectsForFormula += "(" + randomEffect.replaceAll("[\\(\\[\\]\\)\\-\\s]", ".") + ")";
                                    if (cnt < randomEffectsList.size() - 1) {
                                            randomEffectsForFormula += " + ";  
                                    }
                                    cnt ++;  
                            }
                    }
            }
            if (!fixedEffectsForFormula.equals("")) {
                    formula += fixedEffectsForFormula;
                    if (!randomEffectsForFormula.equals(""))
                            formula += " + " + randomEffectsForFormula;  
            } else {
                    formula += randomEffectsForFormula;
            }
            logger.info("Run R-glm, data file: " + fileName);
            logger.info("Run R-glm, family: " + familyOpt);
            logger.info("Run R-glm, formula: " + formula);
            logger.info("Run R-glm, modelingType: " + modelingTypeOpt);
            this.componentOptions.addContent(0, new Element("formula").setText(formula));
            this.componentOptions.addContent(0, new Element("responseCol").setText(response));
            //delete randomEffects content bc | in the option can choke the command line
            if (this.componentOptions!= null && this.componentOptions.getChildren() != null) {
                    for (Element optionElementInner : (List<Element>) this.componentOptions.getChildren()) {
                            logger.info(optionElementInner.getName());
                            if (optionElementInner.getName().equals("randomEffects")) {
                                    String text = optionElementInner.getText();
                                    String validPattern =  "\\s*\\|\\s*";
                                    Pattern pattern = Pattern.compile(validPattern);
                                    Matcher matcher = pattern.matcher(text);
                                    text = matcher.replaceAll("^|");
                                    optionElementInner.setText(text);
                                    
                                    break;
                            }
                    }
            }
            
            //do data format checking:
            //1.binomial must be 0/1 or correct/incorrect/hint when glm/glmer
            //2.numeric
            int responseColInd = getColInd(fileName, responseOpt);
            if (responseColInd == -1) {
                    String errMsg = "Response column not found in headers.";
                    addErrorMessage(errMsg);
                    logger.info(errMsg);
                    System.err.println(errMsg);
                    return;
            }
            if (modelingTypeOpt.equals("glm") || modelingTypeOpt.equals("glmer")) {
                    if (familyOpt.indexOf("binomial") != -1 && !getColBinaryConvertible(fileName, responseColInd)) {
                            String errMsg = "Values for response column must be 0 or 1 for binomial function.";
                            addErrorMessage(errMsg);
                            logger.info(errMsg);
                            System.err.println(errMsg);
                            return;
                    } else if (familyOpt.indexOf("binomial") == -1 && !isColNumeric(fileName, responseColInd) && !getColBinaryConvertible(fileName, responseColInd)) {
                            String errMsg = "Response column must be numeric.";
                            addErrorMessage(errMsg);
                            logger.info(errMsg);
                            System.err.println(errMsg);
                            return;
                    }
            } else if (!isColNumeric(fileName, responseColInd) && !getColBinaryConvertible(fileName, responseColInd)) {
                    String errMsg = "Response column must be numeric.";
                    addErrorMessage(errMsg);
                    logger.info(errMsg);
                    System.err.println(errMsg);
                    return;
                    
            }
            
            File outputDirectory = this.runExternalMultipleFileOuput();
            if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                    logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                    File file0 = new File(outputDirectory.getAbsolutePath() + "/R output model summary.txt");
                    if (file0 != null && file0.exists()) {
                            Integer nodeIndex0 = 0;
                            Integer fileIndex0 = 0;
                            String label0 = "text";
                            this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
                    } else {
                            this.addErrorMessage("An unknown error has occurred with the Rglm component.");
                    }
            }
            
            // Send the component output back to the workflow.
            System.out.println(this.getOutput());
            
            
    }
    
    private int getColInd(String filePathName, String colName) {
            String[][] fContent = IOUtil.read2DStringArray(filePathName);
            String[] headers = fContent[0];
            int colInd = 0;
            for (String colHeader : headers) {
                    if (colName.equals(colHeader))
                            return colInd;
                    colInd++;
            }
            return -1;
    }
    
    private boolean isColNumeric(String filePathName, int colInd) {
            String[][] fContent = IOUtil.read2DStringArray(filePathName);
            for (int rowCnt = 1; rowCnt < fContent.length; rowCnt++) {
                    String[] row = fContent[rowCnt];
                    if (colInd >= row.length)
                            continue;
                    if (!StringUtils.isNumeric(row[colInd]))
                            return false;
            }
            return true;
    }

    //return false if this column is 0 or 1, true if correct/incorrect/hint or true/false
    //return null of other values
    private boolean getColBinaryConvertible(String filePathName, int colInd) {
            String[][] fContent = IOUtil.read2DStringArray(filePathName);
            for (int rowCnt = 1; rowCnt < fContent.length; rowCnt++) {
                    String[] row = fContent[rowCnt];
                    if (colInd >= row.length)
                            return false;
                    String colValue = row[colInd];
                    boolean colNeedConvert = false;
                    if (!colValue.equals("0") && !colValue.equals("1") &&
                                    !colValue.equalsIgnoreCase("hint") && !colValue.equalsIgnoreCase("correct") && !colValue.equalsIgnoreCase("incorrect") &&
                                    !colValue.equalsIgnoreCase("true") && !colValue.equalsIgnoreCase("false")) {
                            return false;
                    }
            }
            return true;
    }
}
