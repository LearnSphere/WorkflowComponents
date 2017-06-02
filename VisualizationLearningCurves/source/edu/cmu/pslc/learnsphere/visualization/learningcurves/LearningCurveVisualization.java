package edu.cmu.pslc.learnsphere.visualization.learningcurves;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

import edu.cmu.pslc.datashop.dto.LearningCurvePoint;
import edu.cmu.pslc.datashop.servlet.learningcurve.LearningCurveImage;
import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.LearningCurveType;

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
                if (fields.length > mapIndex) {
                    skillNamesSplit = fields[mapIndex].split("~~", NO_PATTERN_LIMIT);
                    opportunitiesSplit =
                        fields[headingMap.get(opportunityName)].split("~~", NO_PATTERN_LIMIT);
                }

                String anonStudentId = fields[headingMap.get("Anon Student Id")];
                String stepName = fields[headingMap.get("Step Name")];
                String problemName = fields[headingMap.get("Problem Name")];
                String sampleName = fields[headingMap.get("Sample")];
                Double incorrects = null;
                Double hints = null;
                Double stepDuration = null;
                Double errorStepDuration = null;
                Double correctStepDuration = null;

                String criteria = null;

                for (int skillCounter = 0; skillCounter < skillNamesSplit.length; skillCounter++) {

                    String hsCriteria = null;
                    criteria = null;
                    // Which criteria to use for aggregation, i.e. the "View By":
                    // 2) by Opportunity, 2) by Step, or 3) by Student
                    if (learningCurveType != null
                            && learningCurveType.equals(LearningCurveType.CRITERIA_STUDENT_STEPS_ALL)) {
                        // For 'By Student', across all students
                        criteria = sampleName + "_" + opportunitiesSplit[skillCounter];
                        aggregateBy.put(criteria, lcOptions.getPrimaryModelName());
                        hsCriteria = lcOptions.getPrimaryModelName();
                    } else if (learningCurveType != null && learningCurveType.equals(LearningCurveType.CRITERIA_STEPS_OPPORTUNITIES)) {
                        // For 'By KC'
                        criteria = sampleName + "_" + skillNamesSplit[skillCounter] + "_" + opportunitiesSplit[skillCounter];
                        aggregateBy.put(criteria, skillNamesSplit[skillCounter]);
                        hsCriteria = skillNamesSplit[skillCounter];
                    } else if (learningCurveType != null && learningCurveType.equals(LearningCurveType.CRITERIA_STUDENTS_OPPORTUNITIES)) {
                        // For individual student...
                        criteria = sampleName + "_" + anonStudentId + "_" + opportunitiesSplit[skillCounter];
                        aggregateBy.put(criteria, anonStudentId);
                        hsCriteria = anonStudentId;
                    }

                    // The predicted error rate is set later to prevent exceptions during type casting
                    Double predictedErrorRate = null;
                    String skillName = skillNamesSplit[skillCounter];
                    skillNames.put(criteria, skillName);
                    String opportunity = opportunitiesSplit[skillCounter];
                    opportunities.put(criteria, opportunity);

                    // Create new values for the criteria if they do not exist
                    if (!validRowCounts.containsKey(criteria))
                        validRowCounts.put(criteria, new Integer(0));

                    if (!maxOpportunities.containsKey(hsCriteria)) {
                        maxOpportunities.put(hsCriteria, new Integer(0));
                    }

                    if (!lsCounts.containsKey(criteria)) {
                        lsCounts.put(criteria, new Integer(0));
                    }

                    if (!hsCounts.containsKey(hsCriteria)) {
                        hsCounts.put(hsCriteria, new Integer(0));
                    }

                    if (!hsErrorRate.containsKey(hsCriteria)) {
                        hsErrorRate.put(hsCriteria, new Double(0));
                    }

                    if (!avgAssistanceScore.containsKey(criteria))
                        avgAssistanceScore.put(criteria, new Double(0));

                    if (!avgErrorRate.containsKey(criteria))
                        avgErrorRate.put(criteria, new Double(0));

                    if (!avgIncorrects.containsKey(criteria))
                        avgIncorrects.put(criteria, new Double(0));

                    if (!avgHints.containsKey(criteria))
                        avgHints.put(criteria, new Double(0));

                    if (!avgStepDuration.containsKey(criteria))
                        avgStepDuration.put(criteria, new Double(0));

                    if (!avgCorrectStepDuration.containsKey(criteria))
                        avgCorrectStepDuration.put(criteria, new Double(0));

                    if (!avgErrorStepDuration.containsKey(criteria))
                        avgErrorStepDuration.put(criteria, new Double(0));

                    if (!avgPredictedErrorRate.containsKey(criteria))
                        avgPredictedErrorRate.put(criteria, new Double(0));

                    if (!countObservations.containsKey(criteria))
                        countObservations.put(criteria, new Double(0));

                    if (!stepDurationObs.containsKey(criteria))
                        stepDurationObs.put(criteria, new Double(0));

                    if (!correctStepDurationObs.containsKey(criteria))
                        correctStepDurationObs.put(criteria, new Double(0));

                    if (!errorStepDurationObs.containsKey(criteria))
                        errorStepDurationObs.put(criteria, new Double(0));

                    if (!countSteps.containsKey(criteria))
                        countSteps.put(criteria, new HashSet<String>());

                    if (!countSkills.containsKey(criteria))
                        countSkills.put(criteria, new HashSet<String>());

                    if (!countStudents.containsKey(criteria))
                        countStudents.put(criteria, new HashSet<String>());

                    if (!countProblems.containsKey(criteria))
                        countProblems.put(criteria, new HashSet<String>());

                    // Parse double values, provided the value exists
                    if (!fields[headingMap.get(predictedErrorRateName)].isEmpty()) {
                        predictedErrorRate = Double.parseDouble(fields[headingMap.get(predictedErrorRateName)]);
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

                    // Criteria for groupings
                    criteriaSet.add(criteria);
                    criteriaSet.add(hsCriteria);

		    Boolean highStakesPresent = false;
		    Integer headingMapIndex = null;
		    if (highStakesName != null) {
			headingMapIndex = headingMap.get(highStakesName);
			if ((headingMapIndex != null) && (fields.length > headingMapIndex)) {
			    highStakesPresent = true;
			}
		    }

                    // Handle required fields
                    avgAssistanceScore.put(criteria,
                        (avgAssistanceScore.get(criteria) + incorrects + hints));
                    avgIncorrects.put(criteria,
                        (avgIncorrects.get(criteria) + incorrects));
                    avgHints.put(criteria,
                        (avgHints.get(criteria) + hints));
		    if (predictedErrorRate != null) {
			avgPredictedErrorRate.put(criteria,
						  (avgPredictedErrorRate.get(criteria) + predictedErrorRate));
		    }

                    // Handle missing values which are allowed for some fields
                    if (stepDuration != null) {
                        avgStepDuration.put(criteria,
                            (avgStepDuration.get(criteria) + stepDuration));
                        stepDurationObs.put(criteria,
                                (stepDurationObs.get(criteria) + 1.0f));
                    }
                    if (errorStepDuration != null) {
                        avgErrorStepDuration.put(criteria,
                            (avgErrorStepDuration.get(criteria) + errorStepDuration));
                        errorStepDurationObs.put(criteria,
                            (errorStepDurationObs.get(criteria) + 1.0f));
                    }
                    if (correctStepDuration != null) {
                        avgCorrectStepDuration.put(criteria,
                            (avgCorrectStepDuration.get(criteria) + correctStepDuration));
                        correctStepDurationObs.put(criteria,
                            (correctStepDurationObs.get(criteria) + 1.0f));
                    }


                    // Observations
                    // "Your most unhappy customers are your greatest source of learning." --Bill Gates
                    countObservations.put(criteria,
                        (countObservations.get(criteria) + 1.0f));
                    // Counts
                    HashSet<String> stepSet = countSteps.get(criteria);
                    stepSet.add(stepName);
                    countSteps.put(criteria, stepSet);

                    HashSet<String> skillSet = countSkills.get(criteria);
                    skillSet.add(skillName);
                    countSkills.put(criteria, skillSet);

                    HashSet<String> studentSet = countStudents.get(criteria);
                    studentSet.add(anonStudentId);
                    countStudents.put(criteria, studentSet);

                    HashSet<String> problemSet = countProblems.get(criteria);
                    problemSet.add(problemName);
                    countProblems.put(criteria, problemSet);

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
			    }
                        }
                    } else {
			// If no 'highStakes' CF, revert to normal errorRate behavior.
			avgErrorRate.put(criteria, avgErrorRate.get(criteria) + errorRate);
			lsCounts.put(criteria, lsCounts.get(criteria) + 1);
		    }

                } // end of skillNamesSplit loop

            }   // end of while loop

            // Now, run the averages with the data we collected.
            logger.debug("Result headers: " + Arrays.toString(resultHeaders));

            for (String criteria : criteriaSet) {

                // If not null, this is the 'highStakesErrorRate' criteria.
                if (hsCounts.get(criteria) != null) {
                    logger.debug("*** getting hsErrorRate for criteria = " + criteria);
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

                Object[] result = {
                    skillNames.get(criteria),
                    opportunities.get(criteria),
                    avgAssistanceScore.get(criteria),
                    avgErrorRate.get(criteria),
                    avgIncorrects.get(criteria),
                    avgHints.get(criteria),
                    avgPredictedErrorRate.get(criteria),
                    avgStepDuration.get(criteria),
                    avgErrorStepDuration.get(criteria),
                    avgCorrectStepDuration.get(criteria),
                    countObservations.get(criteria),
                    countSteps.get(criteria).size(),
                    countSkills.get(criteria).size(),
                    countStudents.get(criteria).size(),
                    countProblems.get(criteria).size()
                };

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

                logger.fatal("Result: " + Arrays.toString(result));
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


    /** the LearningCurveImage, includes filename/URL, created by the producer */
    private LearningCurveImage lcImage;

    /** Call this immediately after calling checkEmpty(), and before calling anything else. */
    public List<File> init(Hashtable<String, Vector<LearningCurvePoint>> lcData,
            LearningCurveVisualizationOptions lcOptions,
                GraphOptions lcGraphOptions,
                    String componentWorkingDir) {

        List<File> imageFiles = new ArrayList<File>();
        LearningCurveDatasetProducerStandalone producer = new LearningCurveDatasetProducerStandalone(lcOptions);

        Boolean showErrorBars = true;
        if (lcOptions.getErrorBarType() == null) {
            showErrorBars = false;
        }

        StringBuffer sBuffer = new StringBuffer();
        for (String key : lcData.keySet()) {
            lcImage = null;
            String filePrefix = key.replaceAll(WorkflowHelper.BAD_FILEPATH_CHARS, "_");
            String fileSuffix = ".png";

            File imageFile;
            try {

                imageFile = File.createTempFile(filePrefix, fileSuffix, new File(componentWorkingDir));
                String fullFilePath = imageFile.getAbsolutePath().replaceAll("\\\\", "/");

                logger.debug("LC Graph Image filePath: " + fullFilePath);

                lcGraphOptions.setTitle(key);
                producer.produceDataset(lcOptions, lcGraphOptions, lcData.get(key));
                producer.generateXYChart(lcGraphOptions, fullFilePath, showErrorBars);

                imageFiles.add(imageFile);

            } catch (IOException e) {
                logger.error("Could not create file for key, " + key);
            }
        }

        return imageFiles;
    }
}
