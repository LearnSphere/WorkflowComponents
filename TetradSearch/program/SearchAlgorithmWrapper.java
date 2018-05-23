/*
    CMU HCII 2017
    https://github.com/cmu-phil/tetrad
    Tetrad plugged into Tigris Workflows

    -Peter
*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.io.CharArrayWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.lang.*;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.util.*;
import edu.cmu.tetradapp.model.*;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.*;
import edu.cmu.tetrad.search.*;
import edu.cmu.tetrad.algcomparison.score.ConditionalGaussianBicScore;
import edu.cmu.tetrad.algcomparison.score.DiscreteBicScore;
import edu.cmu.tetrad.algcomparison.independence.*;


/*
	Wraps the different search algorithms together
*/

public class SearchAlgorithmWrapper {
	private DataSet data;
	private GraphSearch searchAlgorithm = null;

	private IKnowledge knowledge = new Knowledge2();
	private IndependenceTest indTest = null;
	private Score score = null;
	private String algorithmType = null;
	private String dataType = null;

	private double alpha = 0.01;
	private int maxSizeConditioningSet = -1;
	private String testType = null;
	private double penaltyDiscount = 2;
	private boolean discretizeContinuousVars = true;
	private int numCatForDiscretize = 3;
	private boolean useHeuristic = true;
	private int maxPathLength = 3;
	private String scoreType = null;
	//private boolean verbose = false;
	private double structurePrior = 1;
	private boolean faithfulnessAssumed = false;
	private int maxDegree = 100;
	private boolean symmetricFirstStep = false;
	private boolean completeRuleSetUsed = false;
	private int maxLengthDiscriminatingPath = -1;
	private double samplePrior = 1;

	private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

	/**
	 * Public contstructor.
	 * inputs: data set, and command line arguments (params)
	 */
	public SearchAlgorithmWrapper (DataSet data, HashMap<String,String> params, File inFile) {
		//addToDebugMessages("in SearchAlgorithmWrapper constructor");

		if( data == null ) {
			throw new IllegalArgumentException("Data cannot be null");
		}
		this.data = data;

		//Get Knowledge if the user gave it as input
		try {

			if (inFile != null) {
		    File inputFile1 = new File( inFile );
		    if (inputFile1.exists() && inputFile1.isFile() && inputFile1.canRead()) {
		    	getKnowledgeFromFile(inputFile1);
		    } else {
		    	this.knowledge = new Knowledge2();
		    }
		  } else {
		  	this.knowledge = new Knowledge2();
		  }
		} catch (Exception e) {
			addToErrorMessages("Exception creating knowledge: " + e.toString());
		}
    addToDebugMessages("Knowledge: \n" + this.knowledge.toString());

		parseParameters(params);

		if( this.algorithmType == null ) {
			addToErrorMessages("No algorithm type selected");
			throw new IllegalArgumentException("No algorithm type");
		}

		switch (this.algorithmType) {
			case "PC":
				try {
					getIndTest();
					Pc pc = new Pc( this.indTest );
					pc.setDepth(this.maxSizeConditioningSet);
					pc.setKnowledge(this.knowledge);
					this.searchAlgorithm = pc;
				} catch (Exception e) {
					addToErrorMessages("Exception setting up PC algorithm: "+e);
				}
				break;

			case "CPC":
				try {
					getIndTest();
					Cpc cpc = new Cpc( this.indTest );
					cpc.setDepth(this.maxSizeConditioningSet);
					cpc.setKnowledge(this.knowledge);
					this.searchAlgorithm = cpc;
				} catch (Exception e) {
					addToErrorMessages("Exception setting up CPC algorithm: "+e);
				}
				break;

			case "PCStable":
				try {
					getIndTest();
					PcStable pcStable = new PcStable( this.indTest );
					pcStable.setDepth(this.maxSizeConditioningSet);
					pcStable.setKnowledge(this.knowledge);
					this.searchAlgorithm = pcStable;
				} catch (Exception e) {
					addToErrorMessages("Exception setting up PCStable algorithm: "+e);
				}
				break;

			case "CPCStable":
				try {
					getIndTest();
					CpcStable cpcStable = new CpcStable( this.indTest );
					cpcStable.setDepth(this.maxSizeConditioningSet);
					cpcStable.setKnowledge(this.knowledge);
					this.searchAlgorithm = cpcStable;
				} catch (Exception e) {
					addToErrorMessages("Exception setting up CPCStable algorithm: "+e);
				}
				break;

			case "PcMax":
				try {
					getIndTest();
					PcMax pcMax = new PcMax( this.indTest );
					pcMax.setDepth(this.maxSizeConditioningSet);
					pcMax.setUseHeuristic( this.useHeuristic );
					pcMax.setMaxPathLength( this.maxPathLength );
					pcMax.setKnowledge(this.knowledge);
					this.searchAlgorithm = pcMax;
				} catch (Exception e) {
					addToErrorMessages("Exception setting up PcMax algorithm: "+e);
				}
				break;

			case "FGES":
				try {
					getScore();
					Fges fges = new Fges( this.score );
					fges.setNumPatternsToStore(0);
					fges.setFaithfulnessAssumed( this.faithfulnessAssumed );
					fges.setMaxDegree( this.maxDegree );
					fges.setSymmetricFirstStep( this.symmetricFirstStep );
					fges.setKnowledge(this.knowledge);
					this.searchAlgorithm = fges;
				} catch (Exception e) {
					addToErrorMessages("Exception setting up FGES algorithm: "+e);
				}
				break;

			case "FCI":
				try {
					getIndTest();
					Fci fci = new Fci( this.indTest );
					fci.setCompleteRuleSetUsed( this.completeRuleSetUsed );
					fci.setMaxPathLength( this.maxLengthDiscriminatingPath );
					fci.setDepth( this.maxSizeConditioningSet );
					fci.setKnowledge(this.knowledge);
					this.searchAlgorithm = fci;
				} catch (Exception e) {
					addToErrorMessages("Exception setting up FCI algorithm: "+e);
				}
				break;

			case "MBFS":
				try {
					getScore();
					getIndTest();
					Mbfs mbfs = new Mbfs( this.indTest, -1 );
					mbfs.setDepth( this.maxSizeConditioningSet );
					mbfs.setKnowledge(this.knowledge);
					this.searchAlgorithm = mbfs;
				} catch (Exception e) {
					addToErrorMessages("Exception setting up MBFS algorithm: "+e);
				}
				break;
		}
		addToDebugMessages("Done with SearchAlgorithmWrapper constructor");
	}

	/**
	 *public methods
	 */

	public GraphSearch getGraphSearch() {
		return this.searchAlgorithm;
	}

	/**
	 *Private methods
	*/

	private boolean getIndTest() throws IllegalArgumentException {

		if( this.testType == null ) {
			throw new IllegalArgumentException("testType can't be null");
		}

		switch (this.testType) {
			case "Fisher_Z":
				try {
					this.indTest = new IndTestFisherZ (this.data, this.alpha);
					return true;
				} catch (Exception e){
					addToErrorMessages("Exception creating Fisher_Z test: "+e.toString());
					return false;
				}
			case "Correlation_T":
				try {
					this.indTest = new IndTestCorrelationT (this.data, this.alpha);
					return true;
				} catch (Exception e){
					addToErrorMessages("Exception creating Correlation_T test: "+e.toString());
					return false;
				}
			case "Conditional_Correlation":
				try {
					this.indTest = new IndTestConditionalCorrelation (this.data, this.alpha);
					return true;
				} catch (Exception e){
					addToErrorMessages("Exception creating Conditional_Correlation test: "+e.toString());
					return false;
				}
			/*case "SEM_BIC":
			NOT SURE HOW TO IMPLEMENT THIS
			*/
			case "Conditional_Gaussian_LRT":
				try {
					IndTestConditionalGaussianLRT test =
							new IndTestConditionalGaussianLRT (this.data, this.alpha);
					test.setNumCategoriesToDiscretize( this.numCatForDiscretize );
					test.setPenaltyDiscount( this.penaltyDiscount );
					this.indTest = test;
					return true;
				} catch (Exception e) {
					addToErrorMessages("Exception creating Conditional_Gaussian_LRT test"+e.toString());
					return false;
				}
			case "ChiSquare":
				try {
					ChiSquare test = new ChiSquare();
					Parameters p = new Parameters();
					p.set("alpha", this.alpha);
					this.indTest = test.getTest(this.data, p);
					return true;
				} catch (Exception e) {
					addToErrorMessages("Exception creating ChiSquare: "+e.toString());
					return false;
				}
			case "GSquare":
				try {
					GSquare test = new GSquare();
					Parameters p = new Parameters();
					p.set("alpha", this.alpha);
					this.indTest = test.getTest(this.data, p);
					return true;
				} catch (Exception e) {
					addToErrorMessages("Exception creating GSquare: "+e.toString());
					return false;
				}
			case "Discrete_BIC_Test":
				try {
					DiscreteBicTest test = new DiscreteBicTest();
					Parameters p = new Parameters();
					this.indTest = test.getTest(this.data, p);
					return true;
				} catch (Exception e) {
					addToErrorMessages("Exception creating Discrete_BIC_Test: "+e.toString());
					return false;
				}

		}
		return false;
	}
	private void parseParameters(HashMap<String,String> params) {
		addToDebugMessages("In parseParameters. params="+params.toString());

		try {
			Double temp = Double.parseDouble( params.get("-alpha") );
			this.alpha = temp.doubleValue();
		} catch (Exception e){
			addToErrorMessages("Issue parsing alpha:"+ e );
		}

		try {
			Integer temp = Integer.parseInt( params.get("-maxSizeConditioningSet") );
			this.maxSizeConditioningSet = temp.intValue();
		} catch (Exception e){
			addToErrorMessages("Issue parsing maxSizeConditioningSet:"+ e );
		}

		try {
			this.algorithmType = params.get("-algorithm");
		} catch (Exception e) {
			addToErrorMessages("Issue parsing algorithm:"+ e );
		}

		try {
			this.dataType = params.get("-dataType");
		} catch (Exception e) {
			addToErrorMessages("Issue parsing dataType:"+ e );
		}

		//this.testType = params.get("-testType");
		switch (dataType) {
			case "Completely_Continuous":
				this.testType = params.get("-testTypeContinuous");
				//convert the data to be continuous
				this.data = DataUtils.convertNumericalDiscreteToContinuous(this.data);

				if (this.data.isDiscrete() == true) {
					addToErrorMessages("Dataset is not actually continuous");
				}
				break;
			case "Completely_Discrete":
				this.testType = params.get("-testTypeDiscrete");
				if (this.data.isDiscrete() == false) {
					addToErrorMessages("Dataset is not actually discrete");
				}
				break;
			case "Mixed":
				this.testType = params.get("-testTypeMixed");
				break;
		}

		try {
			Double temp = Double.parseDouble( params.get("-penaltyDiscount") );
			this.penaltyDiscount = temp.doubleValue();
		} catch (Exception e){
			addToErrorMessages("Issue parsing penaltyDiscount:"+ e );
		}

		try {
			String temp = params.get("-discretizeContinuousVars");
			if (temp.equals("yes")) {
				this.discretizeContinuousVars = true;
			} else if (temp.equals("no")) {
				this.discretizeContinuousVars = false;
			}
		} catch (Exception e){
			addToErrorMessages("Issue parsing discretizeContinuousVars:"+ e );
		}

		try {
			Integer temp = Integer.parseInt( params.get("-numCatForDiscretize") );
			this.numCatForDiscretize = temp.intValue();
		} catch (Exception e){
			addToErrorMessages("Issue parsing numCatForDiscretize:"+ e );
		}

		try {
			Boolean temp = Boolean.parseBoolean( params.get("-useHeuristic") );
			this.useHeuristic = temp.booleanValue();
		} catch (Exception e){
			addToErrorMessages("Issue parsing useHeuristic:"+ e );
		}

		try {
			Integer temp = Integer.parseInt( params.get("-maxPathLength") );
			this.maxPathLength = temp.intValue();
		} catch (Exception e){
			addToErrorMessages("Issue parsing maxPathLength:"+ e );
		}

		//this.scoreType = params.get("-score");
		switch (dataType) {
			case "Completely_Continuous":
				this.scoreType = params.get("-scoreTypeContinuous");
				break;
			case "Completely_Discrete":
				this.scoreType = params.get("-scoreTypeDiscrete");
				break;
			case "Mixed":
				this.scoreType = params.get("-scoreTypeMixed");
				break;
		}

		/*String temp6 = params.get("-verbose");
		if( temp6.equals("yes") ){
			this.verbose = true;
		} else if (temp6.equals("no")) {
			this.verbose = false;
		}*/

		try {
			Double temp = Double.parseDouble( params.get("-structurePrior") );
			this.structurePrior = temp.doubleValue();
		} catch (Exception e){
			addToErrorMessages("Issue parsing structurePrior:"+ e );
		}

		try {
			String temp = params.get("-faithfulnessAssumed");
			if (temp.equals("yes")) {
				this.faithfulnessAssumed = true;
			} else if (temp.equals("no")) {
				this.faithfulnessAssumed = false;
			}
		} catch (Exception e){
			addToErrorMessages("Issue parsing faithfulnessAssumed:"+ e );
		}

		try {
			Integer temp = Integer.parseInt( params.get("-maxDegree") );
			this.maxDegree = temp.intValue();
		} catch (Exception e){
			addToErrorMessages("Issue parsing maxDegree:"+ e );
		}

		try {
			String temp = params.get("-symmetricFirstStep");
			if (temp.equals("yes")) {
				this.symmetricFirstStep = true;
			} else if (temp.equals("no")) {
				this.symmetricFirstStep = false;
			}
		} catch (Exception e){
			addToErrorMessages("Issue parsing symmetricFirstStep:"+ e );
		}

		try {
			String temp = params.get("-completeRuleSetUsed");
			if (temp.equals("yes")) {
				this.completeRuleSetUsed = true;
			} else if (temp.equals("no")) {
				this.completeRuleSetUsed = false;
			}
		} catch (Exception e){
			addToErrorMessages("Issue parsing completeRuleSetUsed:"+ e );
		}

		try {
			Integer temp = Integer.parseInt( params.get("-maxLengthDiscriminatingPath") );
			this.maxLengthDiscriminatingPath = temp.intValue();
		} catch (Exception e){
			addToErrorMessages("Issue parsing maxLengthDiscriminatingPath:"+ e );
		}

		try {
			Double temp = Double.parseDouble( params.get("-samplePrior") );
			this.samplePrior = temp.doubleValue();
		} catch (Exception e){
			addToErrorMessages("Issue parsing samplePrior:"+ e );
		}
	}

  private boolean getScore() {
  	addToDebugMessages("In getScore");

    if (this.scoreType == null) {
    	addToErrorMessages("scoreType can't be null");
      throw new IllegalArgumentException ("scoreType can't be null");
    }

    if (this.scoreType.equals("SEM_BIC")) {
    	try {
	      ICovarianceMatrix cov = new CovarianceMatrixOnTheFly(this.data);
	      /*this.score = new SemBicScore(cov);
				this.score.setPenaltyDiscount(this.penaltyDiscount);*/
				SemBicScore semBicSocre = new SemBicScore(cov);
				semBicSocre.setPenaltyDiscount(this.penaltyDiscount);
				this.score = semBicSocre;
			} catch (Exception e) {
				addToErrorMessages("Exception trying to create SemBicScore: "+e.toString());
				return false;
			}
			return true;
		}
		else if (this.scoreType.equals("Conditional_Gaussian_BIC")){
			try {
				Parameters p = new Parameters();
				p.set("structurePrior", this.structurePrior);
				p.set("penaltyDiscount", this.penaltyDiscount);
				p.set("numCategoriesToDiscretize", this.numCatForDiscretize);
				ConditionalGaussianBicScore s = new ConditionalGaussianBicScore();
				this.score = s.getScore(this.data, p);
			} catch (Exception e) {
				addToErrorMessages("Couldn't creat ConditionalGaussianBicScore: "+e.toString());
				return false;
			}
			return true;
		}
		else if (this.scoreType.equals("BDeu")) {
			try {
				BDeuScore bDeuScore = new BDeuScore(this.data);
				bDeuScore.setStructurePrior(this.structurePrior);
				bDeuScore.setSamplePrior(this.samplePrior);
				this.score = bDeuScore;
			} catch (Exception e) {
				addToErrorMessages("Coundn't create BDeuScore: "+e.toString());
				return false;
			}
			return true;
		}
		else if (this.scoreType.equals("Discrete_BIC")) {
			try {
				Parameters p = new Parameters();
				p.set("penaltyDiscount", this.penaltyDiscount);
				DiscreteBicScore s = new DiscreteBicScore();
				this.score = s.getScore(this.data, p);
			} catch (Exception e) {
				addToErrorMessages("Couldn't creat DiscreteBicScore: "+e.toString());
				return false;
			}
			return true;
		}
		return false;
	}

	public void getKnowledgeFromFile(File infile) {
		addToDebugMessages("Getting knowledge from file");
		try {
			BufferedReader br = new BufferedReader(new FileReader(infile));

			this.knowledge = new Knowledge2();

			/* GET TIER INFO */
			boolean onKnowledge = false;
			boolean onAddTemporal = false;
			while (br.ready()) {
				String line = br.readLine();
				//addToDebugMessages("tier line "+line);
				//addToDebugMessages("onKnowledge "+onKnowledge + "  onAddTemporal"+onAddTemporal);

				if (line.contains("forbiddirect") || line.contains("requiredirect")) {
					break;
				}

				if (onKnowledge && onAddTemporal) {
					String [] tokens = line.split("\\s+");

					if (tokens.length < 2) {
						continue;
					}

					List<String> varsInTier = new ArrayList<String>();
					for (String var : Arrays.copyOfRange(tokens, 1, tokens.length)) {
						//varsInTier.addAll(Arrays.copyOfRange(tokens, 1, tokens.length));
						varsInTier.add(var);
					}
					//addToDebugMessages("varsInTier "+varsInTier);

					int tierNum = 0;
					try {
						tierNum = Integer.parseInt(tokens[0].replaceAll("\\*",""));
					} catch (Exception e) {
						//addToDebugMessages("tokens[0] = " + tokens[0].replaceAll("*",""));
						addToErrorMessages("Exception parsing tierNum." + e.toString());
					}

					this.knowledge.setTier(tierNum, varsInTier);
					//addToDebugMessages("tokens[0] = "+tokens[0] +" contains *= "+tokens[0].contains("*"));

					if (tokens[0].contains("*")) {
						//addToDebugMessages("forbidden within tier "+tierNum);
						this.knowledge.setTierForbiddenWithin(tierNum, true);
					} else {
						this.knowledge.setTierForbiddenWithin(tierNum, false);
					}
				} else {
					if (line.contains("knowledge")) {
						onKnowledge = true;
					}
					if (line.contains("addtemporal")) {
						onAddTemporal = true;
					}
				}
			}

			/* GET FORBIDDEN EDGES */
			br = new BufferedReader(new FileReader(infile));
			boolean onForbiddenEdges = false;
			while (br.ready()) {
				String line = br.readLine();
				//addToDebugMessages("forbidden edge line "+line);

				if (line.contains("requiredirect")) {
					break;
				}

				if (onForbiddenEdges) {
					String [] tokens = line.split("\\s+");


					if (tokens.length != 3) {
						continue;
					}
					//addToDebugMessages("onForbiddenEdges "+tokens[0]+tokens[1]+tokens[2]);

					this.knowledge.setForbidden(tokens[0], tokens[2]);

				} else {
					if (line.contains("forbiddirect")) {
						onForbiddenEdges = true;
					}
				}
			}

			/* GET REQUIRED EDGES */
			br = new BufferedReader(new FileReader(infile));
			boolean onRequiredEdges = false;
			while (br.ready()) {
				String line = br.readLine();

				if (onRequiredEdges) {
					String [] tokens = line.split("\\s+");

					if (tokens.length != 3) {
						continue;
					}

					this.knowledge.setRequired(tokens[0], tokens[2]);

				} else {
					if (line.contains("requiredirect")) {
						onRequiredEdges = true;
					}
				}
			}

		} catch (IOException e) {
			addToErrorMessages("IOException reading from knowledge file: " + e.toString());
		} catch (Exception e) {
			addToErrorMessages("Exception adding info to knowledge: " + e.toString());
		}
	}

	/**
   *Save ERROR message string from component to a file.
   */
  public boolean addToErrorMessages(String message) {
    try {
      FileWriter fw = new FileWriter(outputDir + FILENAME, true);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(ERROR_PREPEND + message + "\n");
      bw.flush();
      bw.close();
    } catch (IOException e) {
      addToErrorMessages("Unable to write to file: " + e.toString());
      return false;
    }
    return true;
  }

  /**
   *Save DEBUG message string from component to a file.
   */
  public boolean addToDebugMessages(String message) {
    try {
      FileWriter fw = new FileWriter(outputDir + FILENAME, true);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(DEBUG_PREPEND + message + "\n");
      bw.flush();
      bw.close();
    } catch (IOException e) {
      addToErrorMessages("Unable to write to file: " + e.toString());
      return false;
    }
    return true;
  }

}