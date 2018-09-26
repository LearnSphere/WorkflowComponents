package edu.cmu.pslc.learnsphere.visualization.learningcurves;

import java.util.ArrayList;
import java.util.List;

public class LearningCurveVisualizationOptions {

    /** The type (View By) of learning curve. */
    public enum LearningCurveType {
        CRITERIA_STUDENTS_OPPORTUNITIES(0) {
            public String toString() {
                return "Students";
            }
            public String getId() {
                return "students";
            }
        },
        CRITERIA_STUDENT_STEPS_ALL(1) {
            public String toString() {
                return "All Student KCs";
            }
            public String getId() {
                return "all_student_kcs";
            }
        },
        CRITERIA_STEPS_OPPORTUNITIES(2) {
            public String toString() {
                return "Knowledge Components";
            }
            public String getId() {
                return "knowledge_components";
            }
        };

        private int value;
        private LearningCurveType(int value) {
            this.value = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public String getId() {
            return this.toString();
        }
    };

    /** The learning curve metric as an enumerated type
     * which contains overridden toString methods
     * so that titles can easily be obtained from the values.
     * I also added a getId method for backwards compatibility
     * with the existing code. */
    public enum LearningCurveMetric {
        ASSISTANCE_SCORE(0) {
            public String toString() {
                return "Assistance Score";
            }
            public String getId() {
                return "assistance_score";
            }
        },
        ERROR_RATE(1) {
            public String toString() {
                return "Error Rate";
            }
            public String getId() {
                return "error_rate";
            }
        },
        NUMBER_OF_INCORRECTS(2) {
            public String toString() {
                return "Number of Incorrects";
            }
            public String getId() {
                return "average_incorrects";
            }
        },
        NUMBER_OF_HINTS(3) {
            public String toString() {
                return "Number of Hints";
            }
            public String getId() {
                return "average_hints";
            }
        },
        STEP_DURATION(4) {
            public String toString() {
                return "Step Duration";
            }
            public String getId() {
                return "step_duration";
            }
        },
        CORRECT_STEP_DURATION(5) {
            public String toString() {
                return "Correct Step Duration";
            }
            public String getId() {
                return "correct_step_duration";
            }
        },
        ERROR_STEP_DURATION(6) {
            public String toString() {
                return "Error Step Duration";
            }
            public String getId() {
                return "error_step_duration";
            }
        };
        /** Integer value of the enum options. */
        private int value;
        /** Private constructor to allow enum id assignments.
         * @param value the integer value of the enum option
         */
        private LearningCurveMetric(int value) {
            this.value = value;
        }
        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public String getId() {
            return this.toString();
        }

    };

    /** The error bar type with overridden toString methods. */
    public enum ErrorBarType {
        ERROR_BAR_TYPE_STANDARD_ERROR  {
            public String toString() {
                return "Standard Error";
            }
        },
        ERROR_BAR_TYPE_STANDARD_DEVIATION  {
            public String toString() {
                return "Standard Deviation";
            }
        },
        ERROR_BAR_TYPE_NONE  {
            public String toString() {
                return "No Error Bars";
            }
        },
    };

    /** Magic 2.5 for the stdDeviationCutoff default value. */
    private static final Double MAGIC_TWO_POINT_FIVE = new Double(2.5);

    /** The learning curve type. */
    private LearningCurveType learningCurveType;

    /** The selected learning curve metric. */
    private LearningCurveMetric selectedMetric;

    /** List of skill items to draw the curve for. */
    private List skillList;

    /** List of student items to draw the curve for. */
    private List studentList;

    /** Indicates if this graph is a view by skill.  If false it is a view by student. */
    private boolean isViewBySkill = false;

    /**
     * The min opportunity to allow.
     * Will remove student/skill pairs w/o this number of opportunities.
     */
    private Integer opportunityCutOffMin;

    /**
     * The maximum opportunity to allow.
     * Will remove any opportunities above this number.
     */
    private Integer opportunityCutOffMax;

    /**
     * The standard deviation cutoff for latency curves.
     * Will remove any opportunities above this number.
     */
    private Double stdDeviationCutOff = MAGIC_TWO_POINT_FIVE;

    /** Skill model being using to draw the primary curve. */
    private String primaryModel;
    /** Skill models to draw secondary predicted curves. */
    private List<String> secondaryModelNames = new ArrayList<String>();

    /** Type of error bar being displayed. */
    private ErrorBarType errorBarType = ErrorBarType.ERROR_BAR_TYPE_STANDARD_ERROR;

    /** Name of the CustomField that hold highStakes info. */
    private String highStakesCFName;

    /** Flag indicating if curves are to be categorized. */
    private Boolean classifyCurves = true;

    /** High error threshold value. */
    private Double highErrorThreshold = 40.0;

    /** Low error threshold value. */
    private Double lowErrorThreshold = 20.0;

    /** AFM slope threshold value. */
    private Double afmSlopeThreshold = 0.001;

    /** Opportunity threshold value. */
    private Integer opportunityThreshold = 3;

    /** Student threshold value. */
    private Integer studentThreshold = 10;

    public LearningCurveVisualizationOptions() {

    }

    /**
     * Returns opportunityCutOffMax.
     * @return Returns the opportunityCutOffMax.
     */
    public Integer getOpportunityCutOffMax() {
        return opportunityCutOffMax;
    }

    /**
     * Set opportunityCutOffMax.
     * @param opportunityCutOffMax The opportunityCutOffMax to set.
     */
    public void setOpportunityCutOffMax(Integer opportunityCutOffMax) {
        this.opportunityCutOffMax = opportunityCutOffMax;
    }

    /**
     * Returns opportunityCutOffMin.
     * @return Returns the opportunityCutOffMin.
     */
    public Integer getOpportunityCutOffMin() {
        return opportunityCutOffMin;
    }

    /**
     * Set opportunityCutOffMin.
     * @param opportunityCutOffMin The opportunityCutOffMin to set.
     */
    public void setOpportunityCutOffMin(Integer opportunityCutOffMin) {
        this.opportunityCutOffMin = opportunityCutOffMin;
    }

    /** Returns primaryModel. @return Returns the primaryModel. */
    public String getPrimaryModelName() {
        return primaryModel;
    }

    /** Set primaryModel. @param primaryModel The primaryModel to set. */
    public void setPrimaryModelName(String primaryModel) {
        this.primaryModel = primaryModel;
    }

    public List<String> getSecondaryModelNames() { return secondaryModelNames; }
    public void setSecondaryModelNames(List<String> in) { secondaryModelNames = in; }

    /**
     * Returns skillList.
     * @return Returns the skillList.
     */
    public List getSkillList() {
        return skillList;
    }

    /**
     * Set skillList.
     * @param skillList The skillList to set.
     */
    public void setSkillList(List skillList) {
        this.skillList = skillList;
    }

    /**
     * Returns studentList.
     * @return Returns the studentList.
     */
    public List getStudentList() {
        return studentList;
    }

    /**
     * Set studentList.
     * @param studentList The studentList to set.
     */
    public void setStudentList(List studentList) {
        this.studentList = studentList;
    }

    /**
     * Returns if this graph is a view by skill or not.
     * @return true if viewing by skill, false otherwise.
     */
    public boolean isViewBySkill() {
        return isViewBySkill;
    }

    /**
     * Set isViewBySkill.
     * @param viewBySkillFlag the value to set.
     */
    public void setIsViewBySkill(boolean viewBySkillFlag) {
        this.isViewBySkill = viewBySkillFlag;
    }

    /**
     * Returns the selectedType (Assistance Score, Error Rate, etc).
     * @return the selectedType
     */
    public LearningCurveMetric getSelectedMetric() {
        return selectedMetric;
    }

    /**
     * Sets the selectedMetric.
     * @param selectedMetric the selectedMetric to set
     */
    public void setSelectedMetric(LearningCurveMetric selectedMetric) {
        this.selectedMetric = selectedMetric;
    }

    /**
     * Returns the stdDeviationCutOff.
     * @return the stdDeviationCutOff
     */
    public Double getStdDeviationCutOff() {
        return stdDeviationCutOff;
    }

    /**
     * Sets the stdDeviationCutOff.  Defaults to 2.5.
     * @param stdDeviationCutOff the stdDeviationCutOff to set
     */
    public void setStdDeviationCutOff(Double stdDeviationCutOff) {
        if (stdDeviationCutOff != null) {
            this.stdDeviationCutOff = stdDeviationCutOff;
        }
    }

    /**
     * Helper method to determine if the selectedType is a latency curve.
     * @return true if selectedType is Assistance Time or Correct Step Time, false otherwise.
     */
    public Boolean isLatencyCurve() {
        return selectedMetric.equals(LearningCurveMetric.STEP_DURATION)
            || selectedMetric.equals(LearningCurveMetric.CORRECT_STEP_DURATION)
            || selectedMetric.equals(LearningCurveMetric.ERROR_STEP_DURATION);
    }

    /**
     * Returns the ErrorBarType value.
     * @return the ErrorBarType
     */
    public ErrorBarType getErrorBarType() {
        return errorBarType;
    }

    /**
     * Set error bar type.
     * @param errorBarType the ErrorBarType
     */
    public void setErrorBarType(ErrorBarType errorBarType) {
        this.errorBarType = errorBarType;
    }

    /**
     * Returns the point info upper bound label, in a user-friendly format.
     * @return Returns the upperBoundLabel
     */
    public String getUpperBoundLabelStr() {
        if (errorBarType == null) {
            return "";
        }
        if (errorBarType.equals(ErrorBarType.ERROR_BAR_TYPE_STANDARD_ERROR)) {
            return "Upper Bound (M+1SE): ";
        } else {
            return "Upper Bound (M+1SD): ";
        }
    }

    /**
     * Returns the point info lower bound label, in a user-friendly format.
     * @return Returns the lowerBoundLabel
     */
    public String getLowerBoundLabelStr() {
        if (errorBarType == null) {
            return "";
        }
        if (errorBarType.equals(ErrorBarType.ERROR_BAR_TYPE_STANDARD_ERROR)) {
            return "Lower Bound (M+1SE): ";
        } else {
            return "Lower Bound (M+1SD): ";
        }
    }

    public LearningCurveType getLearningCurveType() {
        return learningCurveType;
    }

    public void setLearningCurveType(LearningCurveType learningCurveType) {
        this.learningCurveType = learningCurveType;
    }

    /**
     * Get the name of the CustomField that defines highStakes items.
     * @return CF name
     */
    public String getHighStakesCFName() { return highStakesCFName; }

    /**
     * Set the name of the CustomField that defines highStakes items.
     * @param cfName the name of the CF
     */
    public void setHighStakesCFName(String cfName) { this.highStakesCFName = cfName; }

    /**
     * Get the 'classifyCurves' flag.
     * @return Boolean classifyCurves
     */
    public Boolean getClassifyCurves() { return this.classifyCurves; }

    /**
     * Set the 'classifyCurves' flag.
     * @param flag
     */
    public void setClassifyCurves(Boolean flag) { this.classifyCurves = flag; }

    /**
     * Get the high error threshold.
     * @return Double highErrorThreshold
     */
    public Double getHighErrorThreshold() { return this.highErrorThreshold; }

    /**
     * Set the high error threshold.
     * @param threshold
     */
    public void setHighErrorThreshold(Double threshold) {
        this.highErrorThreshold = threshold;
    }

    /**
     * Get the low error threshold.
     * @return Double lowErrorThreshold
     */
    public Double getLowErrorThreshold() { return this.lowErrorThreshold; }

    /**
     * Set the low error threshold.
     * @param threshold
     */
    public void setLowErrorThreshold(Double threshold) {
        this.lowErrorThreshold = threshold;
    }

    /**
     * Get the AFM slope threshold.
     * @return Double afmSlopeThreshold
     */
    public Double getAfmSlopeThreshold() { return this.afmSlopeThreshold; }

    /**
     * Set the AFM slope threshold.
     * @param threshold
     */
    public void setAfmSlopeThreshold(Double threshold) {
        this.afmSlopeThreshold = threshold;
    }

    /**
     * Get the opportunity threshold.
     * @return Integer opportunityThreshold
     */
    public Integer getOpportunityThreshold() { return this.opportunityThreshold; }

    /**
     * Set the opportunity threshold.
     * @param threshold
     */
    public void setOpportunityThreshold(Integer threshold) {
        this.opportunityThreshold = threshold;
    }

    /**
     * Get the student threshold.
     * @return Integer studentThreshold
     */
    public Integer getStudentThreshold() { return this.studentThreshold; }

    /**
     * Set the student threshold.
     * @param threshold
     */
    public void setStudentThreshold(Integer threshold) {
        this.studentThreshold = threshold;
    }
}
