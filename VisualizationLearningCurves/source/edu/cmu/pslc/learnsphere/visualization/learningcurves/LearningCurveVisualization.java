package edu.cmu.pslc.learnsphere.visualization.learningcurves;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.cmu.pslc.datashop.servlet.learningcurve.LearningCurveImage;
import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.LearningCurveType;

import static edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.ALL_KCS;
import static edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.ALL_STUDENTS;

/**
 * This class is used to generate the Learning Curve data points
 * used to create the Learning Curve graphs and point info. It
 * extracts the necessary values from the student-step export and AFM results file.
 *
 * @author Mike Komisin
 * @version $Revision: $
 * <BR>Last modified by: $Author: $
 * <BR>Last modified on: $Date: $
 * <!-- $KeyWordsOff: $ -->
 */
public class LearningCurveVisualization {

    /** The predicted values provided to this class. */
    List<Double> predictedValuesDouble;

    WorkflowHelper workflowHelper = null;

    /** Debug logging. */
    private Logger logger = Logger.getLogger(getClass().getName());

    public static final String[] resultHeaders = {
            "skillName",
            "opportunity",
            "avgAssistanceScore",
            "avgErrorRate",
            "avgIncorrects",
            "avgHints",
            "avgPredictedErrorRate",
            "avgStepDuration",
            "avgErrorStepDuration",
            "avgCorrectStepDuration",
            "Observations",
            "Steps",
            "Skills",
            "Students",
            "Problems"
    };
    /** Character encoding for input stream readers. */
    public static final String UTF8 = "UTF8";
    /** The buffered reader buffer size. */
    public static final int IS_READER_BUFFER = 8192;
    /** The file delimiter. */
    private static final String STEP_EXPORT_SEPARATOR = "\\t";


    public LearningCurveVisualization() {

        predictedValuesDouble = new ArrayList<Double>();
    }



    /* Java method to replace the sql query built by buildLearningCurveQuery... It is not yet finished but what is here works.
     * Tested using LoggingTest12eMultiSkills, All Data sample, Default skill model.

        SELECT sr.skill_id as typeId, sr.opportunity as oppNum, AVG(sr.total_incorrects + sr.total_hints) as assistanceScore,
        AVG(sr.error_rate) as errorRate, AVG(sr.total_incorrects) as avgIncorrects, AVG(sr.total_hints) as avgHints,
        AVG(sr.step_duration) as stepDuration, AVG(sr.correct_step_duration) as correctStepDuration,
        AVG(sr.error_step_duration) as errorStepDuration, AVG(sr.predicted_error_rate) * 100 as predicted, count(*) as observations,

        SUM(IF(sr.step_duration IS NULL, 0, 1)) as stepDurationObs, SUM(IF(sr.correct_step_duration IS NULL, 0, 1)) as correctStepDurationObs,
        SUM(IF(sr.error_step_duration IS NULL, 0, 1)) as errorStepDurationObs,

        COUNT(DISTINCT if('' is null, null, sr.step_id)) as steps,
        COUNT(DISTINCT if('' is null, null, sr.skill_id)) as skills, COUNT(DISTINCT if('' is null, null, sr.student_id)) as students,
        COUNT(DISTINCT if('' is null, null, sr.problem_id)) as problems FROM step_rollup sr
        WHERE sr.sample_id = 1 AND sr.skill_model_id = 1
        AND sr.first_attempt != '3' AND sr.opportunity IS NOT NULL AND sr.skill_id IN (4, 3, 1, 2) AND sr.student_id IN (1, 2)
        GROUP BY sr.opportunity, sr.skill_id WITH ROLLUP
    */
    private static final Integer NO_PATTERN_LIMIT = -1;
    public Hashtable<String, Vector<LearningCurvePoint>> processStudentStepExportForLearningCurves(
            File stepRollupFile, LearningCurveVisualizationOptions lcOptions) {

        LearningCurveType learningCurveType = lcOptions.getLearningCurveType();

        BufferedReader br = null;
        InputStream inputStream = null;
        Hashtable<String, Vector<LearningCurvePoint>> lcData = new Hashtable<String, Vector<LearningCurvePoint>>();

        // Temporary variable for reading lines
        String line = null;

        // Headings
        Map<String, Integer> headingMap = new Hashtable<String, Integer>();
        Map<String, String> skillNames = new Hashtable<String, String>();
        Map<String, String> opportunities = new Hashtable<String, String>();
        Map<String, Double> avgAssistanceScore = new Hashtable<String, Double>();
        Map<String, Double> avgErrorRate = new Hashtable<String, Double>();
        Map<String, Double> hsErrorRate = new Hashtable<String, Double>();
        Map<String, Double> avgIncorrects = new Hashtable<String, Double>();
        Map<String, Double> avgHints = new Hashtable<String, Double>();
        Map<String, Double> avgStepDuration = new Hashtable<String, Double>();
        Map<String, Double> avgCorrectStepDuration = new Hashtable<String, Double>();
        Map<String, Double> avgErrorStepDuration = new Hashtable<String, Double>();
        Map<String, Double> avgPredictedErrorRate = new Hashtable<String, Double>();
        Map<String, Double> countObservations = new Hashtable<String, Double>();
        Map<String, Double> stepDurationObs = new Hashtable<String, Double>();
        Map<String, Double> correctStepDurationObs = new Hashtable<String, Double>();
        Map<String, Double> errorStepDurationObs = new Hashtable<String, Double>();
        Map<String, HashSet<String>> countSteps = new Hashtable<String, HashSet<String>>();
        Map<String, HashSet<String>> countSkills = new Hashtable<String, HashSet<String>>();
        Map<String, HashSet<String>> countStudents = new Hashtable<String, HashSet<String>>();
        Map<String, HashSet<String>> countProblems = new Hashtable<String, HashSet<String>>();
        Map<String, Integer> maxOpportunities = new Hashtable<String, Integer>();

        // We now support multiple secondary curves so keep a map of them (by model name).
        Map<String, Map<String, Double>> avgSecondaryPERMap = new Hashtable<String, Map<String, Double>>();

        Map<String, String> aggregateBy = new Hashtable<String, String>();

        try {
            // Setup the readers.
            File cachedStepFile = null;
            if (stepRollupFile != null && stepRollupFile.getName().matches(".*\\.txt")) {
                cachedStepFile = stepRollupFile;
            }
            logger.info("Reading file: " + stepRollupFile.getAbsolutePath());

            inputStream = new FileInputStream(cachedStepFile);
            br = new BufferedReader(new InputStreamReader(inputStream, UTF8), IS_READER_BUFFER);

            // Read the headings or exit
            Integer numHeadings = 0;
            if ((line = br.readLine()) != null) {
                String[] fields = line.split(STEP_EXPORT_SEPARATOR);
                numHeadings = fields.length;
                for (int i = 0; i < fields.length; i++) {
                    headingMap.put(fields[i], i);

                }
            } else {
                System.exit(1);
            }

            // Read and process the data
            Hashtable<String, Integer> validRowCounts = new Hashtable<String, Integer>();
            Hashtable<String, Integer> hsCounts = new Hashtable<String, Integer>();
	    Hashtable<String, Integer> lsCounts = new Hashtable<String, Integer>();

            String kcModel = "KC (" + lcOptions.getPrimaryModelName() + ")";
            String opportunityName = "Opportunity (" + lcOptions.getPrimaryModelName() + ")";
            String predictedErrorRateName = "Predicted Error Rate (" + lcOptions.getPrimaryModelName() + ")";
            Integer maxOpportunityCutoff = lcOptions.getOpportunityCutOffMax();
            List<String> secondaryPredictedErrorRateNames = new ArrayList<String>();
            for (String s : lcOptions.getSecondaryModelNames()) {
                String secondaryHeaderName = "Predicted Error Rate (" + s + ")";
                secondaryPredictedErrorRateNames.add(secondaryHeaderName);
                Map<String, Double> avgSecondaryPER = new Hashtable<String, Double>();
                avgSecondaryPERMap.put(secondaryHeaderName, avgSecondaryPER);
            }

            Vector<String> skillsSelected = new Vector<String>();
            Vector<String> studentsSelected = new Vector<String>();
            HashSet<String> criteriaSet = new HashSet<String>();

            String highStakesName = lcOptions.getHighStakesCFName();
	    if (highStakesName != null) {
		if (!highStakesName.startsWith("CF (")) {
		    highStakesName = "CF (" + highStakesName + ")";
		}
		if (headingMap.get(highStakesName) == null) {
		    logger.debug("Failed to find CF column for: " + highStakesName);
		}
	    } else {
		logger.debug("highStakesErrorRate not requested.");
	    }

            Integer lineIndex = -1;
            while ((line = br.readLine())
                    != null) {
                lineIndex++;
                // Grab next line
                String fields[] = line.split(STEP_EXPORT_SEPARATOR);

                // Student-Step values
                String firstAttempt = fields[headingMap.get("First Attempt")];

                // Because someone made the wise decision to roll multiple skills/error rates/opportunitiesSplit
                // into single columns, we now have to unroll them to do something simple, like calculate averages.
                // Without a solid set of requirements and good design, there is no such thing as the right way.
                int mapIndex = 0;

                mapIndex = headingMap.get(kcModel);
                String[] skillNamesSplit = new String[0];
                String[] opportunitiesSplit = new String[0];
                String[] perSplit = new String[0];
                if (fields.length > mapIndex) {
                    skillNamesSplit = fields[mapIndex].split("~~", NO_PATTERN_LIMIT);
                    opportunitiesSplit =
                        fields[headingMap.get(opportunityName)].split("~~", NO_PATTERN_LIMIT);
                    perSplit =
                        fields[headingMap.get(predictedErrorRateName)].split("~~", NO_PATTERN_LIMIT);
                }
                String anonStudentId = fields[headingMap.get("Anon Student Id")];
                String stepName = fields[headingMap.get("Step Name")];
                String problemName = fields[headingMap.get("Problem Name")];
                String sampleName = "Sample";
                if (headingMap.get("Sample") != null)
                	sampleName = fields[headingMap.get("Sample")];
                
                Double incorrects = null;
                Double hints = null;
                Double stepDuration = null;
                Double errorStepDuration = null;
                Double correctStepDuration = null;

                String criteria = null;
                // 'All Types' curve, where 'type' is KC or Student
                String allTypesCriteria = null;

                for (int skillCounter = 0; skillCounter < skillNamesSplit.length; skillCounter++) {

                    // Allow for the case where the number of skills doesn't match the
                    // number of opportunities and/or the number of predicted error rates.
                    int oppIndex = (opportunitiesSplit.length - 1 < skillCounter)
                        ? opportunitiesSplit.length - 1 : skillCounter;
                    int perIndex = (perSplit.length - 1 < skillCounter)
                        ? perSplit.length - 1 : skillCounter;

                    String hsCriteria = null;
                    criteria = null;
                    // Which criteria to use for aggregation, i.e. the "View By": KC or Student
                    if (learningCurveType != null
                            && learningCurveType.equals(LearningCurveType.CRITERIA_STUDENT_STEPS_ALL)) {
                        // Composite: all students, all KCs... deprecate this option?
                        criteria = sampleName + "_" + opportunitiesSplit[oppIndex];
                        aggregateBy.put(criteria, lcOptions.getPrimaryModelName());
                        hsCriteria = lcOptions.getPrimaryModelName();
                    } else if (learningCurveType != null && learningCurveType.equals(LearningCurveType.CRITERIA_STEPS_OPPORTUNITIES)) {
                        // For 'By KCs'
                        criteria = sampleName + "_" + skillNamesSplit[skillCounter] + "_" + opportunitiesSplit[oppIndex];
                        aggregateBy.put(criteria, skillNamesSplit[skillCounter]);
                        hsCriteria = skillNamesSplit[skillCounter];

                        allTypesCriteria = sampleName + "_" + ALL_KCS + "_" + opportunitiesSplit[oppIndex];
                        aggregateBy.put(allTypesCriteria, ALL_KCS);
                    } else if (learningCurveType != null && learningCurveType.equals(LearningCurveType.CRITERIA_STUDENTS_OPPORTUNITIES)) {
                        // For 'By Students'
                        criteria = sampleName + "_" + anonStudentId + "_" + opportunitiesSplit[oppIndex];
                        aggregateBy.put(criteria, anonStudentId);
                        hsCriteria = anonStudentId;

                        allTypesCriteria = sampleName + "_" + ALL_STUDENTS + "_" + opportunitiesSplit[oppIndex];
                        aggregateBy.put(allTypesCriteria, ALL_STUDENTS);
                    }

                    // The predicted error rate is set later to prevent exceptions during type casting
                    Double predictedErrorRate = null;
                    Map<String, Double> secondaryPredictedErrorRate = new Hashtable<String, Double>();
                    String skillName = skillNamesSplit[skillCounter];
                    skillNames.put(criteria, skillName);
                    String opportunity = opportunitiesSplit[oppIndex];
                    opportunities.put(criteria, opportunity);
                    if (allTypesCriteria != null) {
                        opportunities.put(allTypesCriteria, opportunity);
                    }

                    // Create new values for the criteria if they do not exist
                    HashSet<String> tempCriteriaSet = new HashSet<String>();
                    tempCriteriaSet.add(criteria);
                    if (allTypesCriteria != null) { tempCriteriaSet.add(allTypesCriteria); }
                    for (String crit : tempCriteriaSet) {
                        if (!validRowCounts.containsKey(crit))
                            validRowCounts.put(crit, new Integer(0));

                        if (!lsCounts.containsKey(crit)) {
                            lsCounts.put(crit, new Integer(0));
                        }

                        if (!avgAssistanceScore.containsKey(crit))
                            avgAssistanceScore.put(crit, new Double(0));
                        
                        if (!avgErrorRate.containsKey(crit))
                            avgErrorRate.put(crit, new Double(0));
                        
                        if (!avgIncorrects.containsKey(crit))
                            avgIncorrects.put(crit, new Double(0));
                        
                        if (!avgHints.containsKey(crit))
                            avgHints.put(crit, new Double(0));
                        
                        if (!avgStepDuration.containsKey(crit))
                            avgStepDuration.put(crit, new Double(0));
                        
                        if (!avgCorrectStepDuration.containsKey(crit))
                            avgCorrectStepDuration.put(crit, new Double(0));
                        
                        if (!avgErrorStepDuration.containsKey(crit))
                            avgErrorStepDuration.put(crit, new Double(0));
                        
                        if (!avgPredictedErrorRate.containsKey(crit))
                            avgPredictedErrorRate.put(crit, new Double(0));
                        
                        if (!countObservations.containsKey(crit))
                            countObservations.put(crit, new Double(0));
                        
                        if (!stepDurationObs.containsKey(crit))
                            stepDurationObs.put(crit, new Double(0));
                        
                        if (!correctStepDurationObs.containsKey(crit))
                            correctStepDurationObs.put(crit, new Double(0));
                        
                        if (!errorStepDurationObs.containsKey(crit))
                            errorStepDurationObs.put(crit, new Double(0));
                        
                        if (!countSteps.containsKey(crit))
                            countSteps.put(crit, new HashSet<String>());
                        
                        if (!countSkills.containsKey(crit))
                            countSkills.put(crit, new HashSet<String>());
                        
                        if (!countStudents.containsKey(crit))
                            countStudents.put(crit, new HashSet<String>());
                        
                        if (!countProblems.containsKey(crit))
                            countProblems.put(crit, new HashSet<String>());

                        if (crit != null) {
                            for (String s : secondaryPredictedErrorRateNames) {
                                Map<String, Double> avgSecondaryPER = avgSecondaryPERMap.get(s);
                                if (!avgSecondaryPER.containsKey(crit))
                                    avgSecondaryPER.put(crit, new Double(0));
                            }
                        }
                    }

                    // Initialize high-stakes values.
                    if (!maxOpportunities.containsKey(hsCriteria)) {
                        maxOpportunities.put(hsCriteria, new Integer(0));
                    }

                    if (!hsCounts.containsKey(hsCriteria)) {
                        hsCounts.put(hsCriteria, new Integer(0));
                    }

                    if (!hsErrorRate.containsKey(hsCriteria)) {
                        hsErrorRate.put(hsCriteria, new Double(0));
                    }

                    // Parse double values, provided the value exists
                    if (!fields[headingMap.get(predictedErrorRateName)].isEmpty()) {
                        predictedErrorRate = Double.parseDouble(perSplit[perIndex]);
                    }

                    for (String s : secondaryPredictedErrorRateNames) {
                        if (!fields[headingMap.get(s)].isEmpty()) {
                            String[] secondaryPerSplit = 
                                fields[headingMap.get(s)].split("~~", NO_PATTERN_LIMIT);
                            secondaryPredictedErrorRate.put(s,
                                                            Double.parseDouble(secondaryPerSplit[0]));
                        }
                    }
                    if (!fields[headingMap.get("Incorrects")].isEmpty()) {
                        incorrects = Double.parseDouble(fields[headingMap.get("Incorrects")]);
                    }
                    if (!fields[headingMap.get("Hints")].isEmpty()) {
                        hints = Double.parseDouble(fields[headingMap.get("Hints")]);
                    }
                    if (!fields[headingMap.get("Step Duration (sec)")].isEmpty()
                            && !fields[headingMap.get("Step Duration (sec)")].equals(".")) {
                        stepDuration = Double.parseDouble(fields[headingMap.get("Step Duration (sec)")]);
                    }
                    if (!fields[headingMap.get("Error Step Duration (sec)")].isEmpty()
                            && !fields[headingMap.get("Error Step Duration (sec)")].equals(".")) {
                        errorStepDuration = Double.parseDouble(fields[headingMap.get("Error Step Duration (sec)")]);
                    }
                    if (!fields[headingMap.get("Correct Step Duration (sec)")].isEmpty()
                            && !fields[headingMap.get("Correct Step Duration (sec)")].equals(".")) {
                        correctStepDuration = Double.parseDouble(fields[headingMap.get("Correct Step Duration (sec)")]);
                    }

                    // The following qualifications must be met for a row to be counted
                    if (!firstAttempt.equalsIgnoreCase("correct")
                            && !firstAttempt.equalsIgnoreCase("incorrect")
                            && !firstAttempt.equalsIgnoreCase("hint")) {
                        continue;
                    } else if (opportunity.isEmpty()) {
                        continue;
                    } else if (!skillsSelected.isEmpty()
                            && !skillsSelected.contains(skillName)) {
                        continue;
                    } else if (!studentsSelected.isEmpty()
                            && !studentsSelected.contains(anonStudentId)) {
                        continue;
                    }

                    Integer opp = Integer.parseInt(opportunity);

                    // Note maxOpportunity cut-off...
                    if (opp > maxOpportunityCutoff) { continue; }

                    Double errorRate = null;
                    // Values derived from the step export
                    if (incorrects + hints > 0) {
                        errorRate = 1.0;
                    } else {
                        errorRate = 0.0;
                    }

                    Integer validRowCount = validRowCounts.get(criteria);
                    validRowCounts.put(criteria, validRowCount + 1);
                    if (allTypesCriteria != null) {
                        validRowCount = validRowCounts.get(allTypesCriteria);
                        validRowCounts.put(allTypesCriteria, validRowCount + 1);
                    }

                    // Criteria for groupings
                    criteriaSet.add(criteria);
                    criteriaSet.add(hsCriteria);
                    if (allTypesCriteria != null) { criteriaSet.add(allTypesCriteria); }

		    Boolean highStakesPresent = false;
		    Integer headingMapIndex = null;
		    if (highStakesName != null) {
			headingMapIndex = headingMap.get(highStakesName);
			if ((headingMapIndex != null) && (fields.length > headingMapIndex)) {
			    highStakesPresent = true;
			}
		    }

                    tempCriteriaSet = new HashSet<String>();
                    tempCriteriaSet.add(criteria);
                    if (allTypesCriteria != null) { tempCriteriaSet.add(allTypesCriteria); }
                    for (String crit : tempCriteriaSet) {

                        // Handle required fields
                        avgAssistanceScore.put(crit,
                                               (avgAssistanceScore.get(crit) + incorrects + hints));
                        avgIncorrects.put(crit,
                                          (avgIncorrects.get(crit) + incorrects));
                        avgHints.put(crit,
                                     (avgHints.get(crit) + hints));

                        if (predictedErrorRate != null) {
                            avgPredictedErrorRate.put(crit,
                                                      (avgPredictedErrorRate.get(crit) + predictedErrorRate));
                        }

                        for (String s : secondaryPredictedErrorRateNames) {
                            Double per = secondaryPredictedErrorRate.get(s);
                            Map<String, Double> avgSecondaryPER = avgSecondaryPERMap.get(s);
                            avgSecondaryPER.put(crit,
                                                (avgSecondaryPER.get(crit) + per));
                        }

                        // Handle missing values which are allowed for some fields
                        if (stepDuration != null) {
                            avgStepDuration.put(crit,
                                                (avgStepDuration.get(crit) + stepDuration));
                            stepDurationObs.put(crit,
                                                (stepDurationObs.get(crit) + 1.0f));
                        }
                        if (errorStepDuration != null) {
                            avgErrorStepDuration.put(crit,
                                                     (avgErrorStepDuration.get(crit) + errorStepDuration));
                            errorStepDurationObs.put(crit,
                                                     (errorStepDurationObs.get(crit) + 1.0f));
                        }
                        if (correctStepDuration != null) {
                            avgCorrectStepDuration.put(crit,
                                                       (avgCorrectStepDuration.get(crit) + correctStepDuration));
                            correctStepDurationObs.put(crit,
                                                       (correctStepDurationObs.get(crit) + 1.0f));
                        }

                        // Observations
                        // "Your most unhappy customers are your greatest source of learning." --Bill Gates
                        countObservations.put(crit,
                                              (countObservations.get(crit) + 1.0f));

                        // Counts
                        HashSet<String> stepSet = countSteps.get(crit);
                        stepSet.add(stepName);
                        countSteps.put(crit, stepSet);
                        
                        HashSet<String> skillSet = countSkills.get(crit);
                        skillSet.add(skillName);
                        countSkills.put(crit, skillSet);

                        HashSet<String> studentSet = countStudents.get(crit);
                        studentSet.add(anonStudentId);
                        countStudents.put(crit, studentSet);

                        HashSet<String> problemSet = countProblems.get(crit);
                        problemSet.add(problemName);
                        countProblems.put(crit, problemSet);
                    }

                    // Keep track of max opportunity for this curve.
                    if (opp > maxOpportunities.get(hsCriteria)) {
                        maxOpportunities.put(hsCriteria, opp);
                    }

                    // If present, note highStakes errorRate.
		    if (highStakesPresent) {
			// Ignore row (w.r.t. errorRate) if highStakes is empty
			if (!fields[headingMapIndex].isEmpty()) {
                            Boolean highStakes = Boolean.parseBoolean(fields[headingMapIndex]);
                            if (highStakes) {
                                hsErrorRate.put(hsCriteria,
                                                (hsErrorRate.get(hsCriteria) + errorRate));
                                hsCounts.put(hsCriteria, hsCounts.get(hsCriteria) + 1);
                            } else {
				avgErrorRate.put(criteria,
						 avgErrorRate.get(criteria) + errorRate);
				lsCounts.put(criteria, lsCounts.get(criteria) + 1);
				avgErrorRate.put(allTypesCriteria,
						 avgErrorRate.get(allTypesCriteria) + errorRate);
				lsCounts.put(allTypesCriteria, lsCounts.get(allTypesCriteria) + 1);
			    }
                        }
                    } else {
			// If no 'highStakes' CF, revert to normal errorRate behavior.
			avgErrorRate.put(criteria, avgErrorRate.get(criteria) + errorRate);
			lsCounts.put(criteria, lsCounts.get(criteria) + 1);
                        if (allTypesCriteria != null) {
                            avgErrorRate.put(allTypesCriteria,
                                             avgErrorRate.get(allTypesCriteria) + errorRate);
                            lsCounts.put(allTypesCriteria, lsCounts.get(allTypesCriteria) + 1);
                        }
		    }

                } // end of skillNamesSplit loop

            }   // end of while loop

            // Now, run the averages with the data we collected.
            logger.debug("Result headers: " + Arrays.toString(resultHeaders));

            for (String criteria : criteriaSet) {

                // If not null, this is the 'highStakesErrorRate' criteria.
                if (hsCounts.get(criteria) != null) {
                    Integer hsCount = hsCounts.get(criteria);

                    if ((hsErrorRate.get(criteria) != null) && (hsCount > 0)) {
                        Double hsErrorRateResult = hsErrorRate.get(criteria) / new Double(hsCount);

                        LearningCurvePoint lcp = new LearningCurvePoint();
                        lcp.setOpportunityNumber(maxOpportunities.get(criteria));

                        lcp.setHighStakesErrorRate(hsErrorRateResult);

                        Vector<LearningCurvePoint> lcDataList = lcData.get(criteria);
                        if (lcDataList == null) {
                            lcDataList = new Vector<LearningCurvePoint>();
                        }
                        lcDataList.add(lcp);
                        lcData.put(criteria, lcDataList);
                    }

                    continue;
                }

                Integer validRowCount = validRowCounts.get(criteria);

                Double avgStepDurationResult =
                    avgStepDuration.get(criteria) / stepDurationObs.get(criteria);
                avgStepDuration.put(criteria, avgStepDurationResult);

                Double avgErrorStepDurationResult =
                    avgErrorStepDuration.get(criteria) / errorStepDurationObs.get(criteria);
                avgErrorStepDuration.put(criteria, avgErrorStepDurationResult);

                Double avgCorrectStepDurationResult =
                    avgCorrectStepDuration.get(criteria) / correctStepDurationObs.get(criteria);
                avgCorrectStepDuration.put(criteria, avgCorrectStepDurationResult);

                Double avgAssistanceScoreResult =
                    avgAssistanceScore.get(criteria) / new Double(validRowCount);
                avgAssistanceScore.put(criteria, avgAssistanceScoreResult);

		if ((avgErrorRate.get(criteria) != null) && (lsCounts.get(criteria) > 0)) {
		    Double avgErrorRateResult =
			avgErrorRate.get(criteria) / new Double(lsCounts.get(criteria));
		    avgErrorRate.put(criteria, avgErrorRateResult);
		}

                Double avgIncorrectsResult =
                    avgIncorrects.get(criteria) / new Double(validRowCount);
                avgIncorrects.put(criteria, avgIncorrectsResult);

                Double avgHintsResult =
                    avgHints.get(criteria) / new Double(validRowCount);
                avgHints.put(criteria, avgHintsResult);

                Double avgPredictedErrorRateResult =
                    avgPredictedErrorRate.get(criteria) / new Double(validRowCount);
                avgPredictedErrorRate.put(criteria, avgPredictedErrorRateResult);

                Map<String, Double> avgSecondaryPERResult = new Hashtable<String, Double>();
                for (String s : secondaryPredictedErrorRateNames) {
                    Map<String, Double> avgSecondaryPER = avgSecondaryPERMap.get(s);
                    Double result = avgSecondaryPER.get(criteria) / new Double(validRowCount);
                    avgSecondaryPER.put(criteria, result);
                    avgSecondaryPERMap.put(s, avgSecondaryPER);  // ???
                    avgSecondaryPERResult.put(s, result);
                }

                LearningCurvePoint lcp = new LearningCurvePoint();

                if (opportunities.get(criteria) != null) {
                    lcp.setOpportunityNumber(Integer.parseInt(opportunities.get(criteria)));
                }

                if (avgAssistanceScoreResult != null) {
                    lcp.setAssistanceScore(avgAssistanceScoreResult.doubleValue());
                }

                if (avgHintsResult != null) {
                    lcp.setAvgHints(avgHintsResult.doubleValue());
                }

                if (avgIncorrectsResult != null) {
                    lcp.setAvgIncorrects(avgIncorrectsResult.doubleValue());
                }

                if (avgCorrectStepDuration != null && !avgCorrectStepDuration.get(criteria).isNaN()) {
                    lcp.setCorrectStepDuration(avgCorrectStepDuration.get(criteria).doubleValue());
                } else {
                    lcp.setCorrectStepDuration(0.0);
                }

                if (correctStepDurationObs.get(criteria) != null) {
                    lcp.setCorrectStepDurationObservations(correctStepDurationObs.get(criteria).intValue());
                }

                if (avgErrorRate.get(criteria) != null) {
                    lcp.setErrorRates(avgErrorRate.get(criteria).doubleValue());
                }

                if (!avgErrorStepDuration.get(criteria).isNaN()) {
                    lcp.setErrorStepDuration(avgErrorStepDuration.get(criteria).doubleValue());
                } else {
                    lcp.setErrorStepDuration(0.0);
                }

                if (errorStepDurationObs.get(criteria) != null) {
                    lcp.setErrorStepDurationObservations(errorStepDurationObs.get(criteria).intValue());
                }

                if (countObservations.get(criteria) != null) {
                    lcp.setObservations(countObservations.get(criteria).intValue());
                }

                if (opportunities.get(criteria) != null) {
                    lcp.setOpportunityNumber(Integer.parseInt(opportunities.get(criteria)));
                }

                lcp.setPreCutoffObservations(0);

                if (avgPredictedErrorRateResult != null) {
                    lcp.setPredictedErrorRate(avgPredictedErrorRateResult.doubleValue());
                }

                if (avgSecondaryPERResult.size() > 0) {
                    lcp.setSecondaryPredictedErrorRateMap(avgSecondaryPERResult);
                }

                if (countProblems.get(criteria) != null) {
                    lcp.setProblemsCount(countProblems.get(criteria).size());
                }

                if (countSkills.get(criteria) != null) {
                    lcp.setSkillsCount(countSkills.get(criteria).size());
                }

                if (avgStepDuration.get(criteria) != null && !avgStepDuration.get(criteria).isNaN()) {
                    lcp.setStepDuration(avgStepDuration.get(criteria).doubleValue());
                }

                if (stepDurationObs.get(criteria) != null) {
                    lcp.setStepDurationObservations(stepDurationObs.get(criteria).intValue());
                }

                if (countSteps.get(criteria) != null) {
                    lcp.setStepsCount(countSteps.get(criteria).size());
                }

                if (countStudents.get(criteria) != null) {
                    lcp.setStudentsCount(countStudents.get(criteria).size());
                }

                if (aggregateBy.get(criteria) != null) {
                    Vector<LearningCurvePoint> lcDataList = lcData.get(aggregateBy.get(criteria));
                    if (lcDataList == null) {
                        lcDataList = new Vector<LearningCurvePoint>();
                    }
                    lcDataList.add(lcp);
                    lcData.put(aggregateBy.get(criteria), lcDataList);
                }
                /*
                StringBuffer sb = new StringBuffer("[");
                for (String s : secondaryPredictedErrorRateNames) {
                    sb.append(avgSecondaryPERResult.get(s));
                    sb.append(", ");
                }
                sb.append("]");
                */
            }

            if (br != null) {
                br.close();
            }
        } catch (IOException e) {
            // "A consistency proof for [any sufficiently powerful] system ...
            // can be carried out only by means of modes of inference that are not formalized in the system ... itself." --Noam Chomsky
            logger.error("Failed to read Student-Step data: " + stepRollupFile.getAbsolutePath());
	}
        return lcData;
    }

    /**
     * Map of skill name to learning curve category. Computed as part of init() but
     * cached for access when generating output.
     */
    private Map<String, String> skillCategoryMap = new HashMap<String, String>();

    /**
     * Get the learning curve category for the named skill.
     * Return null if the curves have not be categorized or skill isn't present.
     * @param skillName
     * @return category
     */
    public String getSkillCategory(String skillName) {
        String category = null;

        if ((skillCategoryMap != null) && (skillName != null)) {
            category = skillCategoryMap.get(skillName);

            if ((category != null) &&
                (category.equals(LearningCurveImage.CLASSIFIED_OTHER))) {
                category = LearningCurveImage.CLASSIFIED_OTHER_LABEL;
            }
        }

        return category;
    }

    // File prefix must be at least 3 characters. Pad if necessary.
    private static final String FILE_PREFIX_PAD = "_";

    /** Call this immediately after calling checkEmpty(), and before calling anything else. */
    public Map<String, List<File>> init(Hashtable<String, Vector<LearningCurvePoint>> lcData,
                                        LearningCurveVisualizationOptions lcOptions,
                                        GraphOptions lcGraphOptions,
                                        String componentWorkingDir,
                                        File stepRollupFile,
                                        File parametersFile) {

        Map<String, List<File>> imageFiles = new HashMap<String, List<File>>();
        LearningCurveDatasetProducerStandalone producer =
            new LearningCurveDatasetProducerStandalone(lcOptions, stepRollupFile);

        Boolean showErrorBars = true;
        if (lcOptions.getErrorBarType() == null) {
            showErrorBars = false;
        }

        StringBuffer sBuffer = new StringBuffer();
        for (String key : lcData.keySet()) {

            Collections.sort(lcData.get(key), new LearningCurvePoint.SortByOpportunity());

            LearningCurveImage lcImage = null;
            // Guard against slashes and spaces in the skill name, otherwise, allow whatever 
            // characters are there... too much cleaning is an issue with Chinese characters.
            String filePrefix = key.replaceAll("\\\\", "_").replaceAll("/", "_").replaceAll(" ", "_");
	    while (filePrefix.length() < 3) {
		filePrefix += FILE_PREFIX_PAD;
	    }

            String fileSuffix = ".png";

            File imageFile;
            try {

                imageFile = File.createTempFile(filePrefix, fileSuffix, new File(componentWorkingDir));
		logger.debug("imageFile = " + imageFile);
                String fullFilePath = imageFile.getAbsolutePath().replaceAll("\\\\", "/");

                logger.debug("LC Graph Image filePath: " + fullFilePath);

                // Determine AFM slope (gamma) using parameters file, if present. Null otherwise.
                Double gamma = getGamma(key, lcOptions.isViewBySkill(), parametersFile);

                lcImage = producer.produceDataset(key, gamma, lcOptions, lcGraphOptions, lcData.get(key));

                String classification = lcImage.getClassification();
                if (classification == null) { classification = LearningCurveImage.NOT_CLASSIFIED; }
                if (classification.equals(LearningCurveImage.CLASSIFIED_OTHER)) {
                    classification = LearningCurveImage.CLASSIFIED_OTHER_LABEL;
                }

                String titleText = key;
                if (key.equals(ALL_KCS)) { titleText = "All Knowledge Components"; }
                if (key.equals(ALL_STUDENTS)) { titleText = "All Students"; }
                // If curve has been classified, update titleText for non-thumb graphs.
                if (!classification.equals(LearningCurveImage.NOT_CLASSIFIED)) {
                    titleText += " (Category: " + classification + ")";
                }
                lcGraphOptions.setTitle(titleText);

                producer.generateXYChart(lcGraphOptions, fullFilePath, showErrorBars);

                addImageToMap(imageFile, lcImage.getClassification(), imageFiles);

                // Update skill-to-category map.
                skillCategoryMap.put(key, lcImage.getClassification());

            } catch (IOException e) {
                logger.error("Could not create file for key, " + key);
            } catch (Exception e) {
		logger.error("Problem creating img file: " + e);
	    }
        }

        return imageFiles;
    }

    /**
     * Helper method to read AFM gamma (slope) value from parameters file, if present.
     *
     * @param objName the object of interest, skill or student
     * @param isSkill flag indicating object is skill or not (student)
     * @param parametersFile the file with AFM output parameters
     * @return Double the gamma value
     */
    private Double getGamma(String objName, Boolean isSkill, File parametersFile) {

        if (parametersFile == null) { return null; }

        String typeStr = isSkill ? "skill" : "student";

        // Parse XML parameters file for gamma of named object and type.
        SAXBuilder builder = new SAXBuilder();
        builder.setReuseParser(false);
        try {
            String xmlStr = FileUtils.readFileToString(parametersFile, null);
            StringReader reader = new StringReader(xmlStr.replaceAll("[\r\n]+", ""));
            Document doc = builder.build(reader);
            List<Element> cList = doc.getRootElement().getChildren();
            logger.debug("Found root: " + doc.getRootElement().getName() + " with " + cList.size() + " children.");
            Iterator<Element> iter = cList.iterator();
            while (iter.hasNext()) {
                Element e = (Element) iter.next();
                if (e.getName().equals("parameter")) {
                    List<Element> children = e.getChildren();
                    String pType = e.getChildText("type");
                    String pName = e.getChildText("name");
                    if (pType.equalsIgnoreCase(typeStr) && pName.equalsIgnoreCase(objName)) {
                        String pSlope = e.getChildText("slope");
                        return new Double(pSlope);
                    }
                }
            }
        } catch (IOException ioe) {
            String exErr = "XML file not found. Error: " + ioe.getMessage();
            logger.info(exErr);
            return null;
        } catch (JDOMException je) {
            String exErr = "XML file in wrong format. Error: " + je.getMessage();
            logger.info(exErr);
            return null;
        } catch (NumberFormatException nfe) {
            String exErr = "Slope value in XML file not a number. Error: " + nfe.getMessage();
            logger.info(exErr);
            return null;
        }

        return null;
    }

    private void addImageToMap(File theFile, String classification,
                               Map<String, List<File>> map) {

        List<File> imageList = map.get(classification);
        if (imageList == null) {
            imageList = new ArrayList<File>();
            map.put(classification, imageList);
        }
        imageList.add(theFile);
    }
}
