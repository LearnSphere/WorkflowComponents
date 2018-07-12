/*
 * Carnegie Mellon University, Human Computer Interaction Institute
 * Copyright 2005
 * All Rights Reserved
 */
package edu.cmu.pslc.learnsphere.visualization.learningcurves;

import java.util.Map;

import static edu.cmu.pslc.datashop.dto.LearningCurveOptions.ASSISTANCE_SCORE_TYPE;
import static edu.cmu.pslc.datashop.dto.LearningCurveOptions.AVG_HINTS_TYPE;
import static edu.cmu.pslc.datashop.dto.LearningCurveOptions.AVG_INCORRECTS_TYPE;
import static edu.cmu.pslc.datashop.dto.LearningCurveOptions.CORRECT_STEP_DURATION_TYPE;
import static edu.cmu.pslc.datashop.dto.LearningCurveOptions.ERROR_RATE_TYPE;
import static edu.cmu.pslc.datashop.dto.LearningCurveOptions.ERROR_STEP_DURATION_TYPE;
import static edu.cmu.pslc.datashop.dto.LearningCurveOptions.STEP_DURATION_TYPE;
import static edu.cmu.pslc.datashop.util.FormattingUtils.LC_DECIMAL_FORMAT;
import static edu.cmu.pslc.datashop.util.UtilConstants.MAGIC_1000;

/**
 * This class represents all the information required for a single
 * point on a learning curve graph.
 *
 * @author Benjamin K. Billings
 * @version $Revision: 13723 $
 * <BR>Last modified by: $Author: ctipper $
 * <BR>Last modified on: $Date: 2016-12-19 14:35:17 -0500 (Mon, 19 Dec 2016) $
 * <!-- $KeyWordsOff: $ -->
 */
/*
@DTO.Properties(root = "learningCurvePoint",
                properties = { "errorRates", "assistanceScore", "predictedErrorRate",
                               "avgIncorrects", "avgHints", "stepDuration", "correctStepDuration",
                               "errorStepDuration", "secondaryPredictedErrorRate",
                               "opportunityNumber", "observations", "preCutoffObservations",
                               "stepDurationObservations", "correctStepDurationObservations",
                               "errorStepDurationObservations", "studentsCount", "problemsCount",
                               "skillsCount", "stepsCount", "stdDevErrorRate",
                               "stdDevAssistanceScore", "stdDevIncorrects", "stdDevHints",
                               "stdDevStepDuration", "stdDevCorrectStepDuration",
                               "stdDevErrorStepDuration", "stdErrErrorRate",
                               "stdErrAssistanceScore", "stdErrIncorrects", "stdErrHints",
                               "stdErrStepDuration", "stdErrCorrectStepDuration",
                               "stdErrErrorStepDuration", "highStakesErrorRate" })
public class LearningCurvePoint extends DTO {
*/
public class LearningCurvePoint {

    /** Error Rate at this opportunity. */
    private Double errorRates = null;

    /** Assistance Score at this opportunity. */
    private Double assistanceScore = null;

    /** Predicted Error Rate at this opportunity. */
    private Double predictedErrorRate = null;

    /** Average number of Incorrects at this opportunity. */
    private Double avgIncorrects = null;

    /** Average number of Hints at this opportunity. */
    private Double avgHints = null;

    /** Average Step Duration at this opportunity. */
    private Double stepDuration = null;

    /** Average Correct Step Duration at this opportunity. */
    private Double correctStepDuration = null;

    /** Average Error Step Duration at this opportunity. */
    private Double errorStepDuration = null;

    /** Secondary Predicted Error Rate at this opportunity. */
    private Map<String, Double> secondaryPredictedErrorRateMap = null;

    /** The opportunity number for this point. */
    private Integer opportunityNumber = null;

    /** The number of observations for this point. */
    private Integer observations = null;

    /** The number of observations before a standard deviation cutoff. */
    private Integer preCutoffObservations = null;

    /** The number of step duration observations. */
    private Integer stepDurationObservations = null;

    /** The number of correct step duration observations. */
    private Integer correctStepDurationObservations = null;

    /** The number of error step duration observations. */
    private Integer errorStepDurationObservations = null;

    /** The number of students for this point. */
    private int studentsCount;
    /** The number of problems for this point. */
    private int problemsCount;
    /** The number of skills for this point. */
    private int skillsCount;
    /** The number of steps for this point. */
    private int stepsCount;

    /** The standard deviation for the Error Rate. */
    private Double stdDevErrorRate;
    /** The standard deviation for the Assistance Score. */
    private Double stdDevAssistanceScore;
    /** The standard deviation for the number of Incorrects. */
    private Double stdDevIncorrects;
    /** The standard deviation for the number of Hints. */
    private Double stdDevHints;
    /** The standard deviation for the Step Duration. */
    private Double stdDevStepDuration;
    /** The standard deviation for the Correct Step Duration. */
    private Double stdDevCorrectStepDuration;
    /** The standard deviation for the Error Step Duration. */
    private Double stdDevErrorStepDuration;

    /** The standard error for the Error Rate. */
    private Double stdErrErrorRate;
    /** The standard error for the Assistance Score. */
    private Double stdErrAssistanceScore;
    /** The standard error for the number of Incorrects. */
    private Double stdErrIncorrects;
    /** The standard error for the number of Hints. */
    private Double stdErrHints;
    /** The standard error for the Step Duration. */
    private Double stdErrStepDuration;
    /** The standard error for the Correct Step Duration. */
    private Double stdErrCorrectStepDuration;
    /** The standard error for the Error Step Duration. */
    private Double stdErrErrorStepDuration;

    /** The highStakes error rate. */
    private Double highStakesErrorRate;

    /** Default blank constructor. */
    public LearningCurvePoint() { }

    /**
     * Returns assistanceScore.
     * @return Returns the assistanceScore.
     */
    public Double getAssistanceScore() {
        return assistanceScore;
    }

    /**
     * Set assistanceScore.
     * @param assistanceScore The assistanceScore to set.
     */
    public void setAssistanceScore(Double assistanceScore) {
        this.assistanceScore = formatMe(assistanceScore);
    }

    /**
     * Returns errorRates.
     * @return Returns the errorRates.
     */
    public Double getErrorRates() {
        return errorRates;
    }

    /**
     * Set errorRates.
     * @param errorRates The errorRates to set.
     */
    public void setErrorRates(Double errorRates) {
        this.errorRates = formatMe(errorRates);
    }

    /**
     * Returns observations.
     * @return Returns the observations.
     */
    public Integer getObservations() {
        return observations;
    }

    /** Set observations. @param observations The observations to set. */
    public void setObservations(Integer observations) {
        this.observations = observations;
    }

    /** Returns preCutoffObservations. @return Returns the preCutoffObservations. */
    public Integer getPreCutoffObservations() {
        return preCutoffObservations;
    }

    /** Set preCutoffObservations. @param preCutoffObservations The preCutoffObservations to set. */
    public void setPreCutoffObservations(Integer preCutoffObservations) {
        this.preCutoffObservations = preCutoffObservations;
    }

    /**
     * Set the assistance time observations.
     * @param observations the assistance time observation to set.
     */
    public void setStepDurationObservations(Integer observations) {
        this.stepDurationObservations = observations;
    }

    /**
     * Returns the step duration observations.
     * @return Returns the step duration observations.
     */
    public Integer getStepDurationObservations() {
        return stepDurationObservations;
    }

    /**
     * Set the correct step duration observations.
     * @param observations The correct step duration observation to set.
     */
    public void setCorrectStepDurationObservations(Integer observations) {
        this.correctStepDurationObservations = observations;
    }

    /**
     * Returns the correct step duration observations.
     * @return Returns the correct step duration observations.
     */
    public Integer getCorrectStepDurationObservations() {
        return correctStepDurationObservations;
    }

    /**
     * Returns opportunityNumber.
     * @return Returns the opportunityNumber.
     */
    public Integer getOpportunityNumber() {
        return opportunityNumber;
    }

    /**
     * Set opportunityNumber.
     * @param opportunityNumber The opportunityNumber to set.
     */
    public void setOpportunityNumber(Integer opportunityNumber) {
        this.opportunityNumber = opportunityNumber;
    }

    /**
     * Returns predictedErrorRate.
     * @return Returns the predictedErrorRate.
     */
    public Double getPredictedErrorRate() {
        return predictedErrorRate;
    }

    /**
     * Set predictedErrorRate.
     * @param predictedErrorRate The predictedErrorRate to set.
     */
    public void setPredictedErrorRate(Double predictedErrorRate) {
        this.predictedErrorRate = formatMe(predictedErrorRate);
    }

    /**
     * Returns secondaryPredictedErrorRateMap.
     * @return Returns the secondaryPredictedErrorRateMap.
     */
    public Map<String, Double> getSecondaryPredictedErrorRateMap() {
        return secondaryPredictedErrorRateMap;
    }

    /**
     * Set secondaryPredictedErrorRateMap.
     * @param secondaryPredictedErrorRateMap The secondaryPredictedErrorRateMap to set.
     */
    public void setSecondaryPredictedErrorRateMap(Map<String, Double> secondaryPredictedErrorRateMap) {
        this.secondaryPredictedErrorRateMap = secondaryPredictedErrorRateMap;
    }

    /**
     * Returns the stepDuration.
     * @return the stepDuration
     */
    public Double getStepDuration() {
        return stepDuration;
    }

    /**
     * Sets the stepDuration.  Value passed in is in milliseconds.
     * @param stepDuration the stepDuration to set
     */
    public void setStepDuration(Double stepDuration) {
        if (stepDuration != null) {
            this.stepDuration = formatMe(stepDuration / MAGIC_1000);
        } else {
            this.stepDuration = stepDuration;
        }
    }

    /**
     * Returns the avgHints.
     * @return the avgHints
     */
    public Double getAvgHints() {
        return avgHints;
    }

    /**
     * Sets the avgHints.
     * @param avgHints the avgHints to set
     */
    public void setAvgHints(Double avgHints) {
        this.avgHints = formatMe(avgHints);
    }

    /**
     * Returns the avgIncorrects.
     * @return the avgIncorrects
     */
    public Double getAvgIncorrects() {
        return avgIncorrects;
    }

    /**
     * Sets the avgIncorrects.
     * @param avgIncorrects the avgIncorrects to set
     */
    public void setAvgIncorrects(Double avgIncorrects) {
        this.avgIncorrects = formatMe(avgIncorrects);
    }

    /**
     * Returns the correctStepDuration.
     * @return the correctStepDuration
     */
    public Double getCorrectStepDuration() {
        return correctStepDuration;
    }

    /**
     * Sets the correctStepDuration.  Value passed in is in milliseconds.
     * @param correctStepDuration the correctStepDuration to set
     */
    public void setCorrectStepDuration(Double correctStepDuration) {
        if (correctStepDuration != null) {
            this.correctStepDuration = formatMe(correctStepDuration / MAGIC_1000);
        } else {
            this.correctStepDuration = correctStepDuration;
        }
    }

    /**
     * Get the errorStepDuration.
     * @return the errorStepDuration
     */
    public Double getErrorStepDuration() {
        return errorStepDuration;
    }

    /**
     * Set the errorStepDuration.  Value passed in is in milliseconds.
     * @param errorStepDuration the errorStepDuration to set
     */
    public void setErrorStepDuration(Double errorStepDuration) {
        if (errorStepDuration != null) {
            this.errorStepDuration = formatMe(errorStepDuration / MAGIC_1000);
        } else {
            this.errorStepDuration = errorStepDuration;
        }
    }

    /** The number of students for this point.
     * @return the number of students for this point */
    public int getStudentsCount() {
        return studentsCount;
    }
    /** Set the number of students for this point.
     * @param studentsCount the number of students for this point */
    public void setStudentsCount(int studentsCount) {
        this.studentsCount = studentsCount;
    }

    /** The number of problems for this point.
     * @return the number of problems for this point */
    public int getProblemsCount() {
        return problemsCount;
    }
    /** Set the number of problems for this point.
     * @param problemsCount the number of problems for this point */
    public void setProblemsCount(int problemsCount) {
        this.problemsCount = problemsCount;
    }

    /** The number of skills for this point.
     * @return the number of skills for this point */
    public int getSkillsCount() {
        return skillsCount;
    }
    /** Set the number of skills for this point.
     * @param skillsCount the number of skills for this point */
    public void setSkillsCount(int skillsCount) {
        this.skillsCount = skillsCount;
    }

    /** The number of steps for this point.
     * @return the number of steps for this point */
    public int getStepsCount() {
        return stepsCount;
    }

    /** Set the number of steps for this point.
     * @param stepsCount the number of steps for this point */
    public void setStepsCount(int stepsCount) {
        this.stepsCount = stepsCount;
    }

    /**
     * Get the errorStepDurationObservations.
     * @return the errorStepDurationObservations
     */
    public Integer getErrorStepDurationObservations() {
        return errorStepDurationObservations;
    }

    /**
     * Set the errorStepDurationObservations.
     * @param errorStepDurationObservations the errorStepDurationObservations to set
     */
    public void setErrorStepDurationObservations(
            Integer errorStepDurationObservations) {
        this.errorStepDurationObservations = errorStepDurationObservations;
    }

    /**
     * Get the stdDevErrorRate.
     * @return the stdDevErrorRate
     */
    public Double getStdDevErrorRate() {
        return stdDevErrorRate;
    }

    /**
     * Set the stdDevErrorRate.
     * @param in the stdDevErrorRate to set
     */
    public void setStdDevErrorRate(Double in) {
        this.stdDevErrorRate = in;
    }

    /**
     * Get the stdDevAssistanceScore.
     * @return the stdDevAssistanceScore
     */
    public Double getStdDevAssistanceScore() {
        return stdDevAssistanceScore;
    }

    /**
     * Set the stdDevAssistanceScore.
     * @param in the stdDevAssistanceScore to set
     */
    public void setStdDevAssistanceScore(Double in) {
        this.stdDevAssistanceScore = in;
    }

    /**
     * Get the stdDevIncorrects.
     * @return the stdDevIncorrects
     */
    public Double getStdDevIncorrects() {
        return stdDevIncorrects;
    }

    /**
     * Set the stdDevIncorrects.
     * @param in the stdDevIncorrects to set
     */
    public void setStdDevIncorrects(Double in) {
        this.stdDevIncorrects = in;
    }

    /**
     * Get the stdDevHints.
     * @return the stdDevHints
     */
    public Double getStdDevHints() {
        return stdDevHints;
    }

    /**
     * Set the stdDevHints.
     * @param in the stdDevHints to set
     */
    public void setStdDevHints(Double in) {
         this.stdDevHints = in;
    }

    /**
     * Get the stdDevStepDuration.
     * @return the stdDevStepDuration
     */
    public Double getStdDevStepDuration() {
        return stdDevStepDuration;
    }

    /**
     * Set the stdDevStepDuration.
     * @param in the stdDevStepDuration to set
     */
    public void setStdDevStepDuration(Double in) {
        if (in != null) {
            this.stdDevStepDuration = formatMe(in / MAGIC_1000);
        } else {
            this.stdDevStepDuration = in;
        }
    }

    /**
     * Get the stdDevCorrectStepDuration.
     * @return the stdDevStepDuration
     */
    public Double getStdDevCorrectStepDuration() {
        return stdDevCorrectStepDuration;
    }

    /**
     * Set the stdDevCorrectStepDuration.
     * @param in the stdDevCorrectStepDuration to set
     */
    public void setStdDevCorrectStepDuration(Double in) {
        if (in != null) {
            this.stdDevCorrectStepDuration = formatMe(in / MAGIC_1000);
        } else {
            this.stdDevCorrectStepDuration = in;
        }
    }

    /**
     * Get the stdDevErrorStepDuration.
     * @return the stdDevErrorStepDuration
     */
    public Double getStdDevErrorStepDuration() {
        return stdDevErrorStepDuration;
    }

    /**
     * Set the stdDevErrorStepDuration.
     * @param in the stdDevErrorStepDuration to set
     */
    public void setStdDevErrorStepDuration(Double in) {
        if (in != null) {
            this.stdDevErrorStepDuration = formatMe(in / MAGIC_1000);
        } else {
            this.stdDevErrorStepDuration = in;
        }
    }

    /**
     * Get the stdErrErrorRate.
     * @return the stdErrErrorRate
     */
    public Double getStdErrErrorRate() {
        return stdErrErrorRate;
    }

    /**
     * Set the stdErrErrorRate.
     * @param in the stdErrErrorRate to set
     */
    public void setStdErrErrorRate(Double in) {
        this.stdErrErrorRate = in;
    }

    /**
     * Get the stdErrAssistanceScore.
     * @return the stdErrAssistanceScore
     */
    public Double getStdErrAssistanceScore() {
        return stdErrAssistanceScore;
    }

    /**
     * Set the stdErrAssistanceScore.
     * @param in the stdErrAssistanceScore to set
     */
    public void setStdErrAssistanceScore(Double in) {
        this.stdErrAssistanceScore = in;
    }

    /**
     * Get the stdErrIncorrects.
     * @return the stdErrIncorrects
     */
    public Double getStdErrIncorrects() {
        return stdErrIncorrects;
    }

    /**
     * Set the stdErrIncorrects.
     * @param in the stdErrIncorrects to set
     */
    public void setStdErrIncorrects(Double in) {
        this.stdErrIncorrects = in;
    }

    /**
     * Get the stdErrHints.
     * @return the stdErrHints
     */
    public Double getStdErrHints() {
        return stdErrHints;
    }

    /**
     * Set the stdErrHints.
     * @param in the stdErrHints to set
     */
    public void setStdErrHints(Double in) {
         this.stdErrHints = in;
    }

    /**
     * Get the stdErrStepDuration.
     * @return the stdErrStepDuration
     */
    public Double getStdErrStepDuration() {
        return stdErrStepDuration;
    }

    /**
     * Set the stdErrStepDuration.
     * @param in the stdErrStepDuration to set
     */
    public void setStdErrStepDuration(Double in) {
        if (in != null) {
            this.stdErrStepDuration = formatMe(in / MAGIC_1000);
        } else {
            this.stdErrStepDuration = in;
        }
    }

    /**
     * Get the stdErrCorrectStepDuration.
     * @return the stdErrStepDuration
     */
    public Double getStdErrCorrectStepDuration() {
        return stdErrCorrectStepDuration;
    }

    /**
     * Set the stdErrCorrectStepDuration.
     * @param in the stdErrCorrectStepDuration to set
     */
    public void setStdErrCorrectStepDuration(Double in) {
        if (in != null) {
            this.stdErrCorrectStepDuration = formatMe(in / MAGIC_1000);
        } else {
            this.stdErrCorrectStepDuration = in;
        }
    }

    /**
     * Get the stdErrErrorStepDuration.
     * @return the stdErrErrorStepDuration
     */
    public Double getStdErrErrorStepDuration() {
        return stdErrErrorStepDuration;
    }

    /**
     * Set the stdErrErrorStepDuration.
     * @param in the stdErrErrorStepDuration to set
     */
    public void setStdErrErrorStepDuration(Double in) {
        if (in != null) {
            this.stdErrErrorStepDuration = formatMe(in / MAGIC_1000);
        } else {
            this.stdErrErrorStepDuration = in;
        }
    }

    /**
     * Get HighStakes error rate
     * @return Returns the highStakesErrorRate
     */
    public Double getHighStakesErrorRate() {
        return highStakesErrorRate;
    }

    /**
     * Set HighStakes error rate
     * @param errorRate the highStakesErrorRate
     */
    public void setHighStakesErrorRate(Double errorRate) {
        this.highStakesErrorRate = formatMe(errorRate);
    }

    /**
     * Get the observations for the specified curve.
     * @param curveType the specified curve
     * @return the observations for the specified curve.
     */
    public Integer getObservationsForCurveType(String curveType) {
        if (curveType.equals(STEP_DURATION_TYPE)) {
            return getStepDurationObservations();
        }
        if (curveType.equals(CORRECT_STEP_DURATION_TYPE)) {
            return getCorrectStepDurationObservations();
        }
        if (curveType.equals(ERROR_STEP_DURATION_TYPE)) {
            return getErrorStepDurationObservations();
        }
        return getObservations();
    }

    /**
     * Get the standard deviation for the specified curve.
     * @param curveType the specified curve
     * @return the observations for the specified curve.
     */
    public Double getStdDeviationForCurveType(String curveType) {
        if (curveType.equals(ERROR_RATE_TYPE)) {
            return getStdDevErrorRate();
        }
        if (curveType.equals(ASSISTANCE_SCORE_TYPE)) {
            return getStdDevAssistanceScore();
        }
        if (curveType.equals(AVG_INCORRECTS_TYPE)) {
            return getStdDevIncorrects();
        }
        if (curveType.equals(AVG_HINTS_TYPE)) {
            return getStdDevHints();
        }
        if (curveType.equals(STEP_DURATION_TYPE)) {
            return getStdDevStepDuration();
        }
        if (curveType.equals(CORRECT_STEP_DURATION_TYPE)) {
            return getStdDevCorrectStepDuration();
        }
        if (curveType.equals(ERROR_STEP_DURATION_TYPE)) {
            return getStdDevErrorStepDuration();
        }
        return 0.0;
    }

    /**
     * Get the standard error for the specified curve.
     * @param curveType the specified curve
     * @return the observations for the specified curve.
     */
    public Double getStdErrorForCurveType(String curveType) {
        if (curveType.equals(ERROR_RATE_TYPE)) {
            return getStdErrErrorRate();
        }
        if (curveType.equals(ASSISTANCE_SCORE_TYPE)) {
            return getStdErrAssistanceScore();
        }
        if (curveType.equals(AVG_INCORRECTS_TYPE)) {
            return getStdErrIncorrects();
        }
        if (curveType.equals(AVG_HINTS_TYPE)) {
            return getStdErrHints();
        }
        if (curveType.equals(STEP_DURATION_TYPE)) {
            return getStdErrStepDuration();
        }
        if (curveType.equals(CORRECT_STEP_DURATION_TYPE)) {
            return getStdErrCorrectStepDuration();
        }
        if (curveType.equals(ERROR_STEP_DURATION_TYPE)) {
            return getStdErrErrorStepDuration();
        }
        return 0.0;
    }

    public String toString() {
	StringBuffer sb = new StringBuffer();

	sb.append("LearningCurvePoint [");
	sb.append("errorRate = ").append(getErrorRates());
	sb.append(", opportunityNumber = ").append(getOpportunityNumber());
	sb.append(", observations = ").append(getObservations());
	sb.append(", highStakesErrorRate = ").append(getHighStakesErrorRate());
	sb.append("]");
	return sb.toString();
    }

    /**
     * Format the provided value with the LC_FORMATTER.
     * @param value the value to be formatted.
     * @return a nicely formatted value, or null if the value is null.
     */
    protected Double formatMe(Double value) {
        if (value == null) {
            return value;
        } else {
            return Double.parseDouble(LC_DECIMAL_FORMAT.format(value));
        }
    }

} // end LearningCurvePoint.java
