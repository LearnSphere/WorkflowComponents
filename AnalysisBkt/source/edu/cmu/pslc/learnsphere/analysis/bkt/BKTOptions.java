package edu.cmu.pslc.learnsphere.analysis.bkt;

import java.util.ArrayList;
import java.util.List;

public class BKTOptions {

    List<String> errorMessages = null;

    /** The structure type. */
    // Yudelson: the one below doesn't make sense, just flip columns in the data
//     public enum StructureType {
//         BY_SKILL("1") {
//             public String toString() {
//                 return "bySkill";
//             }
// 
//         },
//         BY_USER(2) {
//             public String toString() {
//                 return "byUser";
//             }
// 
//         };
// 
//         private String value;
//         private StructureType(String value) {
//             this.value = value;
//         }
// 
//         /** Provides string IDs for backwards compatibility.
//          * @return the string ID for backwards compatibility
//          */
//         public String getId() {
//             return value;
//         }
//     };

    /** The solver type. */
    public enum SolverType {
        BAUM_WELCH("1.1") {
            public String toString() {
                return "Baum-Welch";
            }

        },
        GRADIENT_DESCENT("1.2") {
            public String toString() {
                return "Gradient Descent";
            }

        },
        CONJUGATE_GRADIENT_DESCENT_POLAK_RIBIERE("1.3.1") {
            public String toString() {
                return "Conjugate Gradient Descent, Polak-Ribiere";
            }

        },
        CONJUGATE_GRADIENT_DESCENT_FLETCHER_REEVES("1.3.2") {
            public String toString() {
                return "Conjugate Gradient Descent, Fletcher-Reeves";
            }

        },
        CONJUGATE_GRADIENT_DESCENT_HESTENES_STIEFEL("1.3.3") {
            public String toString() {
                return "Conjugate Gradient Descent, Hestenes-Stiefel";
            }

        },
        CONJUGATE_GRADIENT_DESCENT_DAI_YUAN("1.3.4") {
            public String toString() {
                return "Conjugate Gradient Descent, Dai-Yuan";
            }

        },
        GRADIENT_DESCENT_LAGRANGIAN_STEPPING("1.4") {
            public String toString() {
                return "Gradient Descent, Lagrangian stepping";
            }

        };

        private String value;
        private SolverType(String value) {
            this.value = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public String getId() {
            return value;
        }
    };

    /** The Conjugate Gradient Descent option. */
// 	Yudelson: obsolete
//     public enum ConjugateGradientDescentOption {
//         POLAK_RIBIERE(1) {
//             public String toString() {
//                 return "Polak-Ribiere";
//             }
// 
//         },
//         FLETCHER_REEVES(2) {
//             public String toString() {
//                 return "Fletcher-Reeves";
//             }
// 
//         },
//         HESTENES_STIEFEL(3) {
//             public String toString() {
//                 return "Hestenes-Stiefel";
//             }
// 
//         };
// 
//         private int value;
//         private ConjugateGradientDescentOption(int value) {
//             this.value = value;
//         }
// 
//         /** Provides string IDs for backwards compatibility.
//          * @return the string ID for backwards compatibility
//          */
//         public Integer getId() {
//             return value;
//         }
//     };

    /** The "fit as one skill" option. */
    // Yudelson: non really needed
//     public enum FitAsOneSkillOption {
//         NO(0) {
//             public String toString() {
//                 return "No";
//             }
// 
//         },
//         FIT_AS_ONE_WITH_MULTISKILL(1) {
//             public String toString() {
//                 return "FitAsOneWithMultiskill";
//             }
// 
//         },
//         YES(2) {
//             public String toString() {
//                 return "ForceOneSkill";
//             }
// 
//         };
// 
//         private int value;
//         private FitAsOneSkillOption(int value) {
//             this.value = value;
//         }
// 
//         /** Provides string IDs for backwards compatibility.
//          * @return the string ID for backwards compatibility
//          */
//         public Integer getId() {
//             return value;
//         }
//     };

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
//     private StructureType structure; // Yudelson: obsolete
    /** The solver type. */
    private SolverType solver;
    /** The conjugate gradient descent option. */
//     private ConjugateGradientDescentOption conjugateGradientDescentOption; // Yudelson: obsolete
    /** The "fit as one skill" option. */
//     FitAsOneSkillOption fitAsOneSkillOption; // Yudelson: non really needed

    /** Max iterations. */
    private Integer maxIterations;
    /** Number of hidden states. */
    private Integer hiddenStates;
    /** Initial parameters (probabilities) for priors, transitions, and emissions. */
//     private List<Double> initialParameters; // Yudelson, will be split
    private Double initialPInit;   // Yudelson starting p-init
    private Double initialPForget; // Yudelson starting p-forget
    private Double initialPLearn;  // Yudelson starting p-learn
    private Double initialPSlip;   // Yudelson starting p-slip
    private Double initialPGuess;  // Yudelson starting p-guess
    /** Lower boundaries for prior, transition, and emissions probabilities. */
//     private List<Double> lowerBoundaries;  // Yudelson, will be split
    private Double lowerPInit;   // Yudelson lower boundary for p-init
    private Double lowerPForget; // Yudelson lower boundary for p-forget
    private Double lowerPLearn;  // Yudelson lower boundary for p-learn
    private Double lowerPSlip;   // Yudelson lower boundary for p-slip
    private Double lowerPGuess;  // Yudelson lower boundary for p-guess
    /** Upper boundaries for prior, transition, and emissions probabilities. */
//     private List<Double> upperBoundaries;  // Yudelson, will be split
    private Double upperPInit;   // Yudelson upper boundary for p-init
    private Double upperPForget; // Yudelson upper boundary for p-forget
    private Double upperPLearn;  // Yudelson upper boundary for p-learn
    private Double upperPSlip;   // Yudelson upper boundary for p-slip
    private Double upperPGuess;  // Yudelson upper boundary for p-guess
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

		// Yudelson: obsolete
//         if (structure == null) {
//             errorMessages.add("The structure parameter is required." + lineBreak);
//         }
        if (solver == null) {
            errorMessages.add("The solver parameter is required." + lineBreak);
        }
        // Yudelson: obsolete
//         if (solver != null && solver.equals(SolverType.CONJUGATE_GRADIENT_DESCENT)
//                 && conjugateGradientDescentOption == null) {
//             errorMessages.add("If " + SolverType.CONJUGATE_GRADIENT_DESCENT
//                 + " is chosen as the solver, the conjugate gradient descent parameter is required."
//                     + lineBreak);
//         }

        return errorMessages;
    }
    /**
     * @return the structure
     */
     // Yudelson: obsolete
//     public StructureType getStructure() {
//         return structure;
//     }

    /**
     * @param structure the structure to set
     */
    // Yudelson: obsolete
//     public void setStructure(StructureType structure) {
//         this.structure = structure;
//     }

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
     // Yudelson: obsolete
//     public ConjugateGradientDescentOption getConjugateGradientDescentOption() {
//         return conjugateGradientDescentOption;
//     }

    /**
     * @param conjugateGradientDescentOption the conjugateGradientDescentOption to set
     */
     // Yudelson: obsolete
//     public void setConjugateGradientDescentOption(
//             ConjugateGradientDescentOption conjugateGradientDescentOption) {
//         this.conjugateGradientDescentOption = conjugateGradientDescentOption;
//     }

    /**
     * @return the fitAsOneSkillOption
     */
	// Yudelson: non really needed
//     public FitAsOneSkillOption getFitAsOneSkillOption() {
//         return fitAsOneSkillOption;
//     }

    /**
     * @param fitAsOneSkillOption the fitAsOneSkillOption to set
     */
    // Yudelson: non really needed
//     public void setFitAsOneSkillOption(FitAsOneSkillOption fitAsOneSkillOptions) {
//         this.fitAsOneSkillOption = fitAsOneSkillOption;
//     }

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
//     public List<Double> getInitialParameters() {
//         return initialParameters;
//     }
    public Double getInitialPInit() {
        return initialPInit;
    }
    public Double getInitialPForget() {
        return initialPForget;
    }
    public Double getInitialPLearn() {
        return initialPLearn;
    }
    public Double getInitialPSlip() {
        return initialPSlip;
    }
    public Double getInitialPGuess() {
        return initialPGuess;
    }

    /**
     * @param initialParamters the initialParameters to set
     */
//     public void setInitialParameters(List<Double> initialParameters) {
//         this.initialParameters = initialParameters;
//     }
    public void setInitialPInit(Double initialPInit) {
        this.initialPInit = initialPInit;
    }
    public void setInitialPForget(Double initialPForget) {
        this.initialPForget = initialPForget;
    }
    public void setInitialPLearn(Double initialPLearn) {
        this.initialPLearn = initialPLearn;
    }
    public void setInitialPSlip(Double initialPSlip) {
        this.initialPSlip = initialPSlip;
    }
    public void setInitialPGuess(Double initialPGuess) {
        this.initialPGuess = initialPGuess;
    }

    /**
     * @return the lowerBoundaries
     */
//     public List<Double> getLowerBoundaries() {
//         return lowerBoundaries;
//     }
    public Double getPInitLowerBoundary() {
    	return lowerPInit;
    }
    public Double getPForgetLowerBoundary() {
    	return lowerPForget;
    }
    public Double getPLearnLowerBoundary() {
    	return lowerPLearn;
    }
    public Double getPSlipLowerBoundary() {
    	return lowerPSlip;
    }
    public Double getPGuessLowerBoundary() {
    	return lowerPGuess;
    }

    /**
     * @param lowerBoundaries the lowerBoundaries to set
     */
//     public void setLowerBoundaries(List<Double> lowerBoundaries) {
//         this.lowerBoundaries = lowerBoundaries;
//     }
    public void setPInitLowerBoundary(Double lowerPInit) {
    	this.lowerPInit = lowerPInit;
    }
    public void setPForgetLowerBoundary(Double lowerPForget) {
    	this.lowerPForget = lowerPForget;
    }
    public void setPLearnLowerBoundary(Double lowerPLearn) {
    	this.lowerPLearn = lowerPLearn;
    }
    public void setPSlipLowerBoundary(Double lowerPSlip) {
    	this.lowerPSlip = lowerPSlip;
    }
    public void setPGuessLowerBoundary(Double lowerPGuess) {
    	this.lowerPGuess = lowerPGuess;
    }

    /**
     * @return the upperBoundaries
     */
//     public List<Double> getUpperBoundaries() {
//         return upperBoundaries;
//     }
    public Double getPInitUpperBoundary() {
    	return upperPInit;
    }
    public Double getPForgetUpperBoundary() {
    	return upperPForget;
    }
    public Double getPLearnUpperBoundary() {
    	return upperPLearn;
    }
    public Double getPSlipUpperBoundary() {
    	return upperPSlip;
    }
    public Double getPGuessUpperBoundary() {
    	return upperPGuess;
    }

    /**
     * @param upperBoundaries the upperBoundaries to set
     */
//     public void setUpperBoundaries(List<Double> upperBoundaries) {
//         this.upperBoundaries = upperBoundaries;
//     }
    public void setPInitUpperBoundary(Double upperPInit) {
    	this.upperPInit = upperPInit;
    }
    public void setPForgetUpperBoundary(Double upperPForget) {
    	this.upperPForget = upperPForget;
    }
    public void setPLearnUpperBoundary(Double upperPLearn) {
    	this.upperPLearn = upperPLearn;
    }
    public void setPSlipUpperBoundary(Double upperPSlip) {
    	this.upperPSlip = upperPSlip;
    }
    public void setPGuessUpperBoundary(Double upperPGuess) {
    	this.upperPGuess = upperPGuess;
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

        String structureSolver = getSolver().getId();
		// Yudelson: simplified the below to above
//         String structureSolver = getStructure().getId() + "." + getSolver().getId();
//         if (getSolver().equals(SolverType.CONJUGATE_GRADIENT_DESCENT)) {
//             structureSolver = structureSolver + "." + getConjugateGradientDescentOption().getId();
//         }

        if (structureSolver != null) {
            returnParamString.append(" -s " + structureSolver);
        }

//         if (fitAsOneSkillOption != null) {
//             returnParamString.append(" -f " + fitAsOneSkillOption.getId());
//         }
        if (maxIterations != null) {
            returnParamString.append(" -i " + maxIterations);
        }
        if (hiddenStates != null) {
            returnParamString.append(" -n " + hiddenStates);
        }
//         if (initialParameters != null) {
//             returnParamString.append(" -0 " + getCommaSeparatedString(initialParameters));
//         }
        // Yudelson, split parameters
        if (initialPInit!=null && initialPForget!=null && initialPLearn!=null &&
        	initialPSlip!=null && initialPGuess!=null) {
        	returnParamString.append(" -0 " + initialPInit + "," + initialPForget + "," +
        		initialPLearn + "," + initialPSlip + "," + initialPGuess);
        }
        
//         if (lowerBoundaries != null) {
//             returnParamString.append(" -l " + getCommaSeparatedString(lowerBoundaries));
//         }
//         if (upperBoundaries != null) {
//             returnParamString.append(" -u " + getCommaSeparatedString(upperBoundaries));
//         }
        if (lowerPInit!=null && lowerPForget!=null && lowerPLearn!=null && 
        	lowerPSlip!=null && lowerPGuess!=null && 
        	upperPInit!=null && upperPForget!=null && upperPLearn!=null && 
        	upperPSlip!=null && upperPGuess!=null) {
        	
            returnParamString.append(" -l " + 
            	lowerPInit + "," + (1 - upperPInit) + "," +
            	(1-upperPForget) + "," + lowerPForget + "," +
            	lowerPLearn + "," + (1 - upperPLearn) + "," +
            	(1-upperPSlip) + "," + lowerPSlip + "," +
            	lowerPGuess + "," + (1 - upperPGuess)
            	);

            returnParamString.append(" -u " + 
            	upperPInit + "," + (1 - lowerPInit) + "," +
            	(1-lowerPForget) + "," + upperPForget + "," +
            	upperPLearn + "," + (1 - lowerPLearn) + "," +
            	(1-lowerPSlip) + "," + upperPSlip + "," +
            	upperPGuess + "," + (1 - lowerPGuess) 
            	);

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
