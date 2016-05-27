package edu.cmu.pslc.learnsphere.analysis.bkt;

import java.util.ArrayList;
import java.util.List;

public class BKTOptions {

    List<String> errorMessages = null;

    /** The structure type. */
    public enum StructureType {
        BY_SKILL(1) {
            public String toString() {
                return "bySkill";
            }

        },
        BY_USER(2) {
            public String toString() {
                return "byUser";
            }

        };

        private int value;
        private StructureType(int value) {
            this.value = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public Integer getId() {
            return value;
        }
    };

    /** The solver type. */
    public enum SolverType {
        BAUM_WELCH(1) {
            public String toString() {
                return "Baum-Welch";
            }

        },
        GRADIENT_DESCENT(2) {
            public String toString() {
                return "GradientDescent";
            }

        },
        CONJUGATE_GRADIENT_DESCENT(3) {
            public String toString() {
                return "ConjugateGradientDescent";
            }

        };

        private int value;
        private SolverType(int value) {
            this.value = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public Integer getId() {
            return value;
        }
    };

    /** The Conjugate Gradient Descent option. */
    public enum ConjugateGradientDescentOption {
        POLAK_RIBIERE(1) {
            public String toString() {
                return "Polak-Ribiere";
            }

        },
        FLETCHER_REEVES(2) {
            public String toString() {
                return "Fletcher-Reeves";
            }

        },
        HESTENES_STIEFEL(3) {
            public String toString() {
                return "Hestenes-Stiefel";
            }

        };

        private int value;
        private ConjugateGradientDescentOption(int value) {
            this.value = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public Integer getId() {
            return value;
        }
    };

    /** The "fit as one skill" option. */
    public enum FitAsOneSkillOption {
        NO(0) {
            public String toString() {
                return "No";
            }

        },
        FIT_AS_ONE_WITH_MULTISKILL(1) {
            public String toString() {
                return "FitAsOneWithMultiskill";
            }

        },
        YES(2) {
            public String toString() {
                return "ForceOneSkill";
            }

        };

        private int value;
        private FitAsOneSkillOption(int value) {
            this.value = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public Integer getId() {
            return value;
        }
    };

    /** The "report model predictions on the training set" option. */
    public enum ReportModelPredictionsOption {
        NO(0) {
            public String toString() {
                return "No";
            }

        },
        YES(1) {
            public String toString() {
                return "Yes";
            }

        },
        YES_WITH_STATE_PROBABILITY(2) {
            public String toString() {
                return "YesWithStateProbability";
            }

        };

        private int value;
        private ReportModelPredictionsOption(int value) {
            this.value = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public Integer getId() {
            return value;
        }
    };

    /** The line break characters. */
    private String lineBreak = "\r\n";
    /** The structure type. */
    private StructureType structure;
    /** The solver type. */
    private SolverType solver;
    /** The conjugate gradient descent option. */
    private ConjugateGradientDescentOption conjugateGradientDescentOption;
    /** The "fit as one skill" option. */
    FitAsOneSkillOption fitAsOneSkillOption;
    /** Tolerance. */
    private Double tolerance;
    /** Max iterations. */
    private Integer maxIterations;
    /** Number of hidden states. */
    private Integer hiddenStates;
    /** Initial parameters (probabilities) for priors, transitions, and emissions. */
    private List<Double> initialParameters;
    /** Lower boundaries for prior, transition, and emissions probabilities. */
    private List<Double> lowerBoundaries;
    /** Upper boundaries for prior, transition, and emissions probabilities. */
    private List<Double> upperBoundaries;
    /** L2 penalty weight. */
    private Double l2PenaltyWeight;
    /** Report model fitting metrics. */
    private Boolean reportModelFittingMetrics;
    /** Cross-validation folds. */
    private Integer xValidationFolds;
    /** Cross-validation predict state N. */
    private Integer xValidationPredictState;
    /** Report model predictions on training set. */
    private ReportModelPredictionsOption reportModelPredictionsOnTrainingSet;
    /** Delimiter for multiple skills per observation (null means single skill per observation). */
    private String delimiterForMultipleSkillsPerObservation;
    /** Block prior parameters from being fit. */
    private Boolean blockPrior;
    /** Block transition parameters from being fit. */
    private Boolean blockTransition;
    /** Block observation parameters from being fit. */
    private Boolean blockObservation;
    /** Quiet mode. */
    private Boolean quietMode;

    /** Default constructor. */
    public BKTOptions() {
    }

    /**
     * Checks the options and returns an error message if any are missing
     * or an empty string if the check passed.
     * @return an error message if any are missing or an empty string if the check passed
     */
    public List<String> checkOptions() {
        errorMessages = new ArrayList<String>();

        if (structure == null) {
            errorMessages.add("The structure parameter is required." + lineBreak);
        }
        if (solver == null) {
            errorMessages.add("The solver parameter is required." + lineBreak);
        }
        if (solver != null && solver.equals(SolverType.CONJUGATE_GRADIENT_DESCENT)
                && conjugateGradientDescentOption == null) {
            errorMessages.add("If " + SolverType.CONJUGATE_GRADIENT_DESCENT
                + " is chosen as the solver, the conjugate gradient descent parameter is required."
                    + lineBreak);
        }

        return errorMessages;
    }
    /**
     * @return the structure
     */
    public StructureType getStructure() {
        return structure;
    }

    /**
     * @param structure the structure to set
     */
    public void setStructure(StructureType structure) {
        this.structure = structure;
    }

    /**
     * @return the solver
     */
    public SolverType getSolver() {
        return solver;
    }

    /**
     * @param solver the solver to set
     */
    public void setSolver(SolverType solver) {
        this.solver = solver;
    }

    /**
     * @return the conjugateGradientDescentOption
     */
    public ConjugateGradientDescentOption getConjugateGradientDescentOption() {
        return conjugateGradientDescentOption;
    }

    /**
     * @param conjugateGradientDescentOption the conjugateGradientDescentOption to set
     */
    public void setConjugateGradientDescentOption(
            ConjugateGradientDescentOption conjugateGradientDescentOption) {
        this.conjugateGradientDescentOption = conjugateGradientDescentOption;
    }

    /**
     * @return the fitAsOneSkillOption
     */
    public FitAsOneSkillOption getFitAsOneSkillOption() {
        return fitAsOneSkillOption;
    }

    /**
     * @param fitAsOneSkillOption the fitAsOneSkillOption to set
     */
    public void setFitAsOneSkillOption(FitAsOneSkillOption fitAsOneSkillOptions) {
        this.fitAsOneSkillOption = fitAsOneSkillOption;
    }

    /**
     * @return the tolerance
     */
    public Double getTolerance() {
        return tolerance;
    }

    /**
     * @param tolerance the tolerance to set
     */
    public void setTolerance(Double tolerance) {
        this.tolerance = tolerance;
    }

    /**
     * @return the maxIterations
     */
    public Integer getMaxIterations() {
        return maxIterations;
    }

    /**
     * @param maxIterations the maxIterations to set
     */
    public void setMaxIterations(Integer maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * @return the hiddenStates
     */
    public Integer getHiddenStates() {
        return hiddenStates;
    }

    /**
     * @param hiddenStates the hiddenStates to set
     */
    public void setHiddenStates(Integer hiddenStates) {
        this.hiddenStates = hiddenStates;
    }

    /**
     * @return the initialParameters
     */
    public List<Double> getInitialParameters() {
        return initialParameters;
    }

    /**
     * @param initialParamters the initialParameters to set
     */
    public void setInitialParameters(List<Double> initialParamters) {
        this.initialParameters = initialParamters;
    }

    /**
     * @return the lowerBoundaries
     */
    public List<Double> getLowerBoundaries() {
        return lowerBoundaries;
    }

    /**
     * @param lowerBoundaries the lowerBoundaries to set
     */
    public void setLowerBoundaries(List<Double> lowerBoundaries) {
        this.lowerBoundaries = lowerBoundaries;
    }

    /**
     * @return the upperBoundaries
     */
    public List<Double> getUpperBoundaries() {
        return upperBoundaries;
    }

    /**
     * @param upperBoundaries the upperBoundaries to set
     */
    public void setUpperBoundaries(List<Double> upperBoundaries) {
        this.upperBoundaries = upperBoundaries;
    }

    /**
     * @return the l2PenaltyWeight
     */
    public Double getL2PenaltyWeight() {
        return l2PenaltyWeight;
    }

    /**
     * @param l2PenaltyWeight the l2PenaltyWeight to set
     */
    public void setL2PenaltyWeight(Double l2PenaltyWeight) {
        this.l2PenaltyWeight = l2PenaltyWeight;
    }

    /**
     * @return the reportModelFittingMetrics
     */
    public Boolean getReportModelFittingMetrics() {
        return reportModelFittingMetrics;
    }

    /**
     * @param reportModelFittingMetrics the reportModelFittingMetrics to set
     */
    public void setReportModelFittingMetrics(Boolean reportModelFittingMetrics) {
        this.reportModelFittingMetrics = reportModelFittingMetrics;
    }

    /**
     * @return the xValidationFolds
     */
    public Integer getxValidationFolds() {
        return xValidationFolds;
    }

    /**
     * @param xValidationFolds the xValidationFolds to set
     */
    public void setxValidationFolds(Integer xValidationFolds) {
        this.xValidationFolds = xValidationFolds;
    }

    /**
     * @return the xValidationPredictState
     */
    public Integer getxValidationPredictState() {
        return xValidationPredictState;
    }

    /**
     * @param xValidationPredictState the xValidationPredictState to set
     */
    public void setxValidationPredictState(Integer xValidationPredictState) {
        this.xValidationPredictState = xValidationPredictState;
    }

    /**
     * @return the reportModelPredictionsOnTrainingSet
     */
    public ReportModelPredictionsOption getReportModelPredictionsOnTrainingSet() {
        return reportModelPredictionsOnTrainingSet;
    }

    /**
     * @param reportModelPredictionsOnTrainingSet the reportModelPredictionsOnTrainingSet to set
     */
    public void setReportModelPredictionsOnTrainingSet(
            ReportModelPredictionsOption reportModelPredictionsOnTrainingSet) {
        this.reportModelPredictionsOnTrainingSet = reportModelPredictionsOnTrainingSet;
    }

    /**
     * @return the delimiterForMultipleSkillsPerObservation
     */
    public String getDelimiterForMultipleSkillsPerObservation() {
        return delimiterForMultipleSkillsPerObservation;
    }

    /**
     * @param delimiterForMultipleSkillsPerObservation the delimiterForMultipleSkillsPerObservation to set
     */
    public void setDelimiterForMultipleSkillsPerObservation(
            String delimiterForMultipleSkillsPerObservation) {
        this.delimiterForMultipleSkillsPerObservation = delimiterForMultipleSkillsPerObservation;
    }

    /**
     * @return the blockPrior
     */
    public Boolean getBlockPrior() {
        return blockPrior;
    }

    /**
     * @param blockPrior the blockPrior to set
     */
    public void setBlockPrior(Boolean blockPrior) {
        this.blockPrior = blockPrior;
    }

    /**
     * @return the blockTransition
     */
    public Boolean getBlockTransition() {
        return blockTransition;
    }

    /**
     * @param blockTransition the blockTransition to set
     */
    public void setBlockTransition(Boolean blockTransition) {
        this.blockTransition = blockTransition;
    }

    /**
     * @return the blockObservation
     */
    public Boolean getBlockObservation() {
        return blockObservation;
    }

    /**
     * @param blockObservation the blockObservation to set
     */
    public void setBlockObservation(Boolean blockObservation) {
        this.blockObservation = blockObservation;
    }

    /**
     * @return the quietMode
     */
    public Boolean getQuietMode() {
        return quietMode;
    }

    /**
     * @param quietMode the quietMode to set
     */
    public void setQuietMode(Boolean quietMode) {
        this.quietMode = quietMode;
    }

    /**
     * @return the lineBreak
     */
    public String getLineBreak() {
        return lineBreak;
    }

    /**
     * @param lineBreak the lineBreak to set
     */
    public void setLineBreak(String lineBreak) {
        this.lineBreak = lineBreak;
    }

    public String[] toArray() {
        String optionsString = this.toString();
        if (optionsString != null) {
            String[] splitOptions = optionsString.split(" ", -1);
            return splitOptions;
        }
        return null;
    }


    /** The toString method. */
    public String toString() {

        if (!checkOptions().isEmpty()) {
            return null;
        }

        StringBuffer returnParamString = new StringBuffer();

        String structureSolver = getStructure().getId() + "." + getSolver().getId();
        if (getSolver().equals(SolverType.CONJUGATE_GRADIENT_DESCENT)) {
            structureSolver = structureSolver + "." + getConjugateGradientDescentOption().getId();
        }

        if (structureSolver != null) {
            returnParamString.append(" -s " + structureSolver);
        }

        if (fitAsOneSkillOption != null) {
            returnParamString.append(" -f " + fitAsOneSkillOption.getId());
        }
        if (tolerance != null) {
            returnParamString.append(" -t " + tolerance);
        }
        if (maxIterations != null) {
            returnParamString.append(" -i " + maxIterations);
        }
        if (hiddenStates != null) {
            returnParamString.append(" -n " + hiddenStates);
        }
        if (initialParameters != null) {
            returnParamString.append(" -0 " + getCommaSeparatedString(initialParameters));
        }
        if (lowerBoundaries != null) {
            returnParamString.append(" -l " + getCommaSeparatedString(lowerBoundaries));
        }
        if (upperBoundaries != null) {
            returnParamString.append(" -u " + getCommaSeparatedString(upperBoundaries));
        }
        if (l2PenaltyWeight != null) {
            returnParamString.append(" -c " + l2PenaltyWeight);
        }
        if (reportModelFittingMetrics != null) {
            returnParamString.append(" -m " + (reportModelFittingMetrics == true ? 1 : 0));
        }
        if (xValidationFolds != null) {
            returnParamString.append(" -v " + xValidationFolds);
            if (xValidationPredictState != null) {
                returnParamString.append(",XVSUBTYPE," + xValidationPredictState);
            }
        }

        if (reportModelPredictionsOnTrainingSet != null) {
            returnParamString.append(" -p " + reportModelPredictionsOnTrainingSet.getId());
        }
        if (delimiterForMultipleSkillsPerObservation != null) {
            returnParamString.append(" -d" + delimiterForMultipleSkillsPerObservation);
        }

        if (blockPrior != null || blockTransition != null || blockObservation != null) {
            returnParamString.append(" -B ");
            if (blockPrior != null && blockPrior) {
                returnParamString.append(1 + ",");
            } else {
                returnParamString.append(0 + ",");
            }
            if (blockTransition != null && blockTransition) {
                returnParamString.append(1 + ",");
            } else {
                returnParamString.append(0 + ",");
            }
            if (blockObservation != null && blockObservation) {
                returnParamString.append(1);
            } else {
                returnParamString.append(0);
            }
        }

        if (quietMode != null) {
            returnParamString.append(" -q " + (quietMode == true ? 1 : 0));
        }



        return returnParamString.toString().replaceAll("[\\s]+", " ").replaceAll("^ ", "");
    }

    public String getCommaSeparatedString(List arrayList) {
        StringBuffer result = new StringBuffer();
        if (arrayList != null && !arrayList.isEmpty()) {
            result.append(arrayList.get(0));
            for (int i = 1; i < arrayList.size(); i++) {
               result.append("," + arrayList.get(i));
            }
        }
        return result.toString();
     }

}
