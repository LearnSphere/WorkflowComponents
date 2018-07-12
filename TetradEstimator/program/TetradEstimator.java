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
import java.util.Map;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.*;
import java.util.regex.Pattern;
import cern.colt.Arrays;

import java.util.HashMap;
import java.util.ArrayList;
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
import java.lang.Math;
import java.text.NumberFormat;
import java.awt.Color;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.regression.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.*;
import edu.cmu.tetradapp.model.*;
import edu.cmu.tetradapp.model.datamanip.*;
import edu.cmu.tetrad.regression.LogisticRegression.Result;
import edu.cmu.tetrad.bayes.*;
import edu.cmu.tetrad.sem.*;
import edu.cmu.tetrad.util.NumberFormatUtil;


public class TetradEstimator {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";
  public static SemPm semPm = null;

  public TetradEstimator () {}

  public static void main(String [] args) {
    PrintStream sysErr = System.err;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setErr(new PrintStream(baos));

    /* The new parameter syntax for files is -node m -fileIndex n <infile>. */
    Map<Integer, File> inFiles = new HashMap<Integer, File>();

    for ( int i = 0; i < args.length; i++) {	// Cursory parse to get the input files
        String arg = args[i];
        String nodeIndex = null;
        String fileIndex = null;
        String filePath = null;
        if (i < args.length - 4) {
        	if (arg.equalsIgnoreCase("-node")) {
        		File inFile = null;
        		String[] fileParamsArray = { args[i] /* -node */, args[i+1] /* node (index) */,
    				args[i+2] /* -fileIndex */, args[i+3] /* fileIndex */, args[i+4] /* infile */ };
        		String fileParamsString = Arrays.toString(fileParamsArray);
        		// Use regExp to get the file path
        		String regExp = "^\\[-node, ([0-9]+), -fileIndex, ([0-9]+), ([^\\]]+)\\]$";
        		Pattern pattern = Pattern.compile(regExp);
        		if (fileParamsString.matches(regExp)) {
        			// Get the third argument in parens from regExp
        			inFile = new File(fileParamsString.replaceAll(regExp, "$3"));
        		}
        		nodeIndex = args[i+1];
        		Integer nodeIndexInt = Integer.parseInt(nodeIndex);
        		fileIndex = args[i+3];
        		inFiles.put(nodeIndexInt, inFile);
        		// 5 arguments, but for loop still calls i++ after
        		i += 4;
        	}
        }

    }

    HashMap<String, String> cmdParams = new HashMap<String, String>();
    for ( int i = 0; i < args.length; i++ ) {
      String s = args[i];
      if ( s.charAt(0) == '-' && i != args.length - 1) {
        String value = "";
        for (int j = i + 1; j < args.length; j++) {
          if (args[j].charAt(0) == '-' && j > i + 1) {
            break;
          } else if (j != i + 1) {
            value += " ";
          }
          value += args[j];
        }
        cmdParams.put(s, value);
        i++;
      }
    }

    String workingDir = cmdParams.get("-workingDir");
    outputDir = workingDir;

    if ( cmdParams.containsKey("-estimator") == false ) {
      addToErrorMessages("No Regression Specified.");
      return;
    } else if ( cmdParams.containsKey("-workingDir") == false ) {
      addToErrorMessages("No workingDir");
      return;
    } else if (!inFiles.containsKey(0)) {
      addToErrorMessages("No infile0 ");
      return;
    } else if (!inFiles.containsKey(1)) {
      addToErrorMessages("No infile1 ");
      return;
    } else if ( cmdParams.containsKey("-incompleteRows") == false ) {
      addToErrorMessages("No method of filling incomplete rows. ");
      return;
    } else if ( cmdParams.containsKey("-parametricModel") == false ) {
      addToErrorMessages("No parametricModel. ");
      return;
    }

    String programDir = cmdParams.get("-programDir");

    String estimator = cmdParams.get("-estimator");
    String infile0 = inFiles.get(0).getAbsolutePath();
    String infile1 = inFiles.get(1).getAbsolutePath();
    String incompleteRows = cmdParams.get("-incompleteRows");
    String parametricModel = cmdParams.get("-parametricModel");
    String optimizer = cmdParams.get("-optimizer");
    String score = cmdParams.get("-score");
    String correlation = cmdParams.get("-correlation");
    double pseudocounts = 1.00;
    try {
      Double temp = Double.parseDouble(cmdParams.get("-pseudocounts"));
      pseudocounts = temp.doubleValue();
    } catch (Exception e) {
      addToErrorMessages("Could not parse pseudocounts" + e.toString());
    }
    double tolerance = 0.0001;
    try {
      Double temp = Double.parseDouble(cmdParams.get("-tolerance"));
      tolerance = temp.doubleValue();
    } catch (Exception e) {
      addToErrorMessages("Could not parse tolerance" + e.toString());
    }
    int randomRestarts = 1;
    try {
      Integer temp = Integer.parseInt(cmdParams.get("-randomRestarts"));
      randomRestarts = temp.intValue();
    } catch (Exception e) {
      addToErrorMessages("Could not parse randomRestarts" + e.toString());
    }

    File inputFile0 = new File( infile0 );
    File inputFile1 = new File( infile1 );

    if (inputFile0.exists() && inputFile0.isFile() && inputFile0.canRead() ) {

      //String regressionTableFile = workingDir + "BayesIm.txt";
      String regressionGraphFile = workingDir + "Graph.html";

      try {

        BufferedReader bReader = null;
        FileReader fReader = null;

        //BufferedWriter bWriterTable = null;
        //FileWriter fWriterTable = null;

        BufferedWriter bWriterGraph = null;
        FileWriter fWriterGraph = null;

        try {

          //fWriterTable = new FileWriter(regressionTableFile);
          //bWriterTable = new BufferedWriter(fWriterTable);

          fWriterGraph = new FileWriter(regressionGraphFile);
          bWriterGraph = new BufferedWriter(fWriterGraph);

          char[] chars = fileToCharArray(inputFile0);

          DataReader reader = new DataReader();
          reader.setMaxIntegralDiscrete(4);
          reader.setDelimiter(DelimiterType.TAB);

          DataSet data = reader.parseTabular(chars);

          List<String> variableNames = data.getVariableNames();



          fReader = new FileReader( inputFile1 );
          bReader = new BufferedReader( fReader );

          Graph graph = getGraphFromText( bReader, data );

          addToDebugMessages("Graph from getGraphFromText(): \n" + graph.toString());

          //Make graph into a dag if it isn't already
          ArrayList<Edge> newEdges = new ArrayList<Edge>();
          try {
            Dag dag = new Dag(graph);
          } catch (IllegalArgumentException e) {
            addToDebugMessages("Graph is not a dag, will make one now..." + e.toString());
            DagInPatternWrapper dagWrapper = new DagInPatternWrapper(graph);
            Graph oldGraph = graph;
            graph = dagWrapper.getGraph();

            //Determine which were the new edges
            for (Edge edge : graph.getEdges()) {
              if (!oldGraph.containsEdge(edge)) {
                newEdges.add(edge);
                addToDebugMessages("newEdge: " + edge.toString());
              }
            }
          }

          Parameters params = new Parameters();
          params.set("initializationMode", "automatic");
          params.set("minCategories", 2);
          params.set("maxCategories", 10);

          int numNodes = graph.getNumNodes();

          switch (parametricModel) {
          case "Bayes_Parametric_Model":
            addToDebugMessages("graph: " + graph.toString());
            addToDebugMessages("data: " + data.toString().substring(0, 200));
            if (data.isContinuous() || data.isMixed()) {
              addToErrorMessages("Data set is continuous or mixed, but it must be discrete.");
              break;
            }
            BayesPmWrapper bayesWrapper = new BayesPmWrapper( graph , params );

            BayesPm bayesPm = new BayesPm(graph, 2, 10);

            BayesIm bayesInstantiatedModel = null;// estWrap.getEstimatedBayesIm();
            addToDebugMessages("made pm");

            //Set the number of categories for each var
            List<Node> nodes1 = data.getVariables();
            for (Node node : nodes1) {
              DiscreteVariable n = (DiscreteVariable)node;
              String name = node.getName();
              int numCategories = n.getNumCategories();
              Node setThis = bayesPm.getNode(name);
              bayesPm.setNumCategories(setThis, Math.max(2, numCategories));
            }

            if (estimator.equals("ML_Bayes_Estimator")) {
              MlBayesEstimator mlbe = new MlBayesEstimator();
              bayesInstantiatedModel = mlbe.estimate(bayesPm, data);
            } else if (estimator.equals("EM_Bayes_Estimator")) {
              EmBayesEstimator embe = new EmBayesEstimator(bayesPm, data);
              bayesInstantiatedModel = embe.maximization(tolerance);
            } else if (estimator.equals("Dirichlet_Estimator")) {
              DirichletBayesIm prior = DirichletBayesIm.symmetricDirichletIm(bayesPm, pseudocounts);
              bayesInstantiatedModel = DirichletEstimator.estimate(prior, data);
            }
            addToDebugMessages("made estWrap");

            if (incompleteRows.equals("Randomize_incomplete_rows")) {
              for (int i = 0 ; i < numNodes ; i++ ) {
                bayesInstantiatedModel.randomizeIncompleteRows( i );
              }
            }
            //bWriterTable.append( bayesInstantiatedModel.toString() );
            bWriterGraph.append( graph.toString() );
            break;

          case "SEM_Parametric_Model":
            SemOptimizer opt = new SemOptimizerRegression();

            if ("Regression".equals(optimizer)) {
              opt = new SemOptimizerRegression();
            } else if ("EM".equals(optimizer)) {
              opt = new SemOptimizerEm();
            } else if ("Powell".equals(optimizer)) {
              opt = new SemOptimizerPowell();
            } else if ("Random Search".equals(optimizer)) {
              opt = new SemOptimizerScattershot();
            } else if ("RICF".equals(optimizer)) {
              opt = new SemOptimizerRicf();
            }

            semPm = new SemPm(graph);
            addToDebugMessages("made semWrapper");

            DataSet continuousData = DataUtils.convertNumericalDiscreteToContinuous( data );

            SemEstimator semEst = null;

            addToDebugMessages("correlation: " + correlation);
            if (correlation.equals("Yes")) {
              ICovarianceMatrix corr = new CorrelationMatrix(continuousData);
              semEst = new SemEstimator(corr, semPm, opt);
            } else if (correlation.equals("No")) {
              semEst = new SemEstimator(continuousData, semPm, opt);
            }
            semEst.setNumRestarts(randomRestarts);
            if (score.equals("Fgls")) {
              semEst.setScoreType(SemIm.ScoreType.Fgls);
            } else {
              semEst.setScoreType(SemIm.ScoreType.Fml);
            }

            addToDebugMessages("made estimator wrapper");

            SemIm semInstantiatedModel = semEst.estimate();

            Graph g = semInstantiatedModel.getSemPm().getGraph();

            String buf = "";
            buf += "Graph Nodes SEM:\n";
            List<Node> nodes = graph.getNodes();
            int c = 1;
            for (Node n : nodes) {
              buf += n.getName() + " " +
                     semInstantiatedModel.getMean(n);
              if (c++ < nodes.size()) {
                buf += ",";
              }
            }
            buf += "\n\nGraph Edges SEM:\n";
            int i = 1;
            for (Edge edge : graph.getEdges()) {
              buf += (i++) + ". " + edge.toString() + " " +
                     semInstantiatedModel.getEdgeCoef(edge);
              if (newEdges.contains(edge)) {
                buf += " y";
              }
              buf += "\n";
            }
            //bWriterGraph.append(buf);
            writeGraphToHtml(buf, programDir, bWriterGraph);

            outputModelInformation(semInstantiatedModel);
            //buf += semInstantiatedModel.toString();

            //bWriterTable.append(buf);
            break;
          }

          //bWriterTable.close();
          bWriterGraph.close();

          // Output correlation matrix
          String correlationMatrixFileName = outputDir + "CorrelationMatrix.txt";
          try {
            FileWriter fw = new FileWriter(correlationMatrixFileName);
            BufferedWriter bw = new BufferedWriter( fw );

            TetradMatrix corrMat = data.getCorrelationMatrix();
            bw.write(corrMat.toString());
            bw.close();
          } catch (IOException e) {
            addToErrorMessages("Could not write correlation matrix to output: " + e.toString());
          }

        } catch (IOException e) {
          addToErrorMessages("IOException main case: " + e.toString());
        } catch (Exception e) {
          addToErrorMessages("Exception main case: " + e.toString());
        }


      } catch (Exception e) {
        addToErrorMessages(e.toString());
      }


    } else if (inputFile0 == null || !inputFile0.exists()
               || !inputFile0.isFile()) {
      addToErrorMessages("Tab-delimited file does not exist.");

    } else if (!inputFile0.canRead()) {
      addToErrorMessages("Tab-delimited file cannot be read.");
    }
    System.setErr(sysErr);
  }

  private static Graph getGraphFromText( BufferedReader b, DataSet d ) {
    try {
      //retrieve graph from html
      StringBuilder htmlStr = new StringBuilder();
      while(b.ready()) {
        htmlStr.append(b.readLine());
        if (b.ready()) {
          htmlStr.append("\n");
        }
      }

      String s = htmlStr.toString();

      String [] graphStrSplit = s.split("<div id=\"graphData\" style=\"visibility:hidden\">\n");
      if (graphStrSplit.length < 2) {
        addToErrorMessages("Couldn't get graph.  When splitting input graph, not enough tokens.");
      }

      String graphStr = graphStrSplit[1].split("</div>")[0];
      addToDebugMessages("graphStr: \n" + graphStr);

      String [] graphLines = graphStr.split("\n");

      //Get nodes
      List<Node> nodeList = new ArrayList<Node>();
      boolean onNodes = false;
      //while ( b.ready() ) {
      int c = 0;
      for ( ; c < graphLines.length; c++) {
        String line = graphLines[c];
        //String line = b.readLine();
        if ( !onNodes ) {
          if ( line.contains("Graph Nodes:") ) {
            onNodes = true;
            continue;
          }
        }

        String [] nodeAr = line.split(",");
        for ( int i = 0; i < nodeAr.length; i++ ) {
          nodeList.add( new GraphNode( nodeAr[i].replaceAll(" ", "_") ));
        }
        break;
      }

      Graph g = new EdgeListGraph( nodeList );

      //Get edges
      boolean onEdges = false;
      //while ( b.ready() ) {
        //String line = b.readLine();
      for ( ; c < graphLines.length; c++) {
        String line = graphLines[c];
        if ( !onEdges ) {
          if ( line.contains("Graph Edges:") ) {
            onEdges = true;
            continue;
          }
          continue;
        }

        String [] tokens = line.split("\\s+");

        if ( tokens.length < 4 ) {
          continue;
        }

        String n0 = ""; //tokens[1];
        String n1 = ""; //tokens[3];
        boolean onFirstNodeName = true;
        //get node names (even if they have spaces) UPDATE NO SUPPORT FOR SPACES (SINCE FCI GRAPHS)
        for (int i = 1; i < tokens.length; i++) {
          String t = tokens[i];
          if (t.equals("---") || t.equals("-->") || t.equals("<->") || t.equals("o->") || t.equals("o-o")) {
            onFirstNodeName = false;
            continue;
          }
          if (onFirstNodeName) {
            if (i != 1) {
              n0 += "_";
            }
            n0 += t;
          } else {
            /*if (n1.length() > 0) {
              n1 += "_";
            }
            n1 += t;*/
            n1 = t;
            break;
          }

        }
        Node node0 = new GraphNode("");
        Node node1 = new GraphNode("");

        for ( int i = 0; i < nodeList.size(); i++ ) {
          Node curr = nodeList.get(i);
          if ( curr.getName().equals(n0) ) {
            node0 = curr;
          }
          if ( curr.getName().equals(n1) ) {
            node1 = curr;
          }
        }

        //get the arrow from the line
        String arrow = "";
        for (int i = 0; i < tokens.length; i++) {
          String t = tokens[i];
          if (t.equals("---") || t.equals("-->") || t.equals("<->") || t.equals("o->") || t.equals("o-o")) {
            arrow = t;
            break;
          }
        }

        Edge newEdge = null;
        if ( arrow.equals("---") ) {
          newEdge = Edges.undirectedEdge(node0, node1);
        } else if ( arrow.equals("-->") ) {
          newEdge = Edges.directedEdge(node0, node1);
        } else if ( arrow.equals("<--") ) {
          newEdge = Edges.directedEdge(node1, node0);
        } else if ( arrow.equals("<->") ) {
          newEdge = Edges.bidirectedEdge(node0, node1);
        } else if ( arrow.equals("o->") ) {
          newEdge = Edges.partiallyOrientedEdge(node0, node1);
        } else if ( arrow.equals("o-o") ) {
          newEdge = Edges.nondirectedEdge(node0, node1);
        } else {
          addToDebugMessages("edge is unreadable" + arrow);
          continue;
        }

        // Set if the edge is dashed, colored
        for (String t : tokens) {
          switch (t) {
            case "nl":
              newEdge.setDashed(true);
              newEdge.addProperty(Edge.Property.nl);
            case "y":
              newEdge.setLineColor(Color.YELLOW);
            case "dd":
              newEdge.setLineColor(Color.GREEN);
              newEdge.addProperty(Edge.Property.dd);
          }
        }

        addToDebugMessages("" + newEdge);
        g.addEdge(newEdge);
      }
      return g;
    } catch ( Exception e ) {
      addToDebugMessages("Error getting graph: " + e.toString() );
      return new EdgeListGraph();
    }
  }

  private static void outputModelInformation(SemIm semIm) {
    String modelStatisticsFileName = outputDir + "ModelStatistics.txt";
    String estimatorTableFileName = outputDir + "EstimatorTable.txt";
    String correlationMatrixFileName = outputDir + "CorrelationMatrix.txt";

    FileWriter fw = null;
    BufferedWriter bw = null;

    // Output Model Statistics
    try {
      fw = new FileWriter(modelStatisticsFileName);
      bw = new BufferedWriter( fw );

      double modelChiSquare = semIm.getChiSquare();
      double modelDof = semPm.getDof();
      double modelPValue = semIm.getPValue();
      double modelBicScore = semIm.getBicScore();
      double modelCfi = semIm.getCfi();
      double modelRmsea = semIm.getRmsea();

      String modelText = "The above chi square test assumes that the maximum likelihood function over" +
          " the measured variables has been minimized. Under that assumption, the null hypothesis for" +
          " the test is that the population covariance matrix over all of the measured variables is" +
          " equal to the estimated covariance matrix over all of the measured variables written as a" +
          " function of the free model parameters--that is, the unfixed parameters for each directed" +
          " edge (the linear coefficient for that edge), each exogenous variable (the variance for" +
          " the error term for that variable), and each bidirected edge (the covariance for the" +
          " exogenous variables it connects).  The model is explained in Bollen, Structural" +
          " Equations with Latent Variable, 110. Degrees of freedom are calculated as m (m + 1)" +
          " / 2 - d, where d is the number of linear coefficients, variance terms, and error" +
          " covariance terms that are not fixed in the model. For latent models, the degrees of" +
          " freedom are termed 'estimated' since extra contraints (e.g. pentad constraints) are not" +
          " taken into account.";
      NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(2);
      bw.write("Degrees of Freedom = " + formatDouble(modelDof, nf) + "\n");
      bw.write("Chi Square = " + formatDouble(modelChiSquare, nf) + "\n");
      bw.write("P Value = " + formatDouble(modelPValue, nf) + "\n");
      bw.write("BIC Score = " + formatDouble(modelBicScore, nf) + "\n");
      bw.write("CFI = " + formatDouble(modelCfi, nf) + "\n");
      bw.write("RMSEA = " + formatDouble(modelRmsea, nf) + "\n");
      addToDebugMessages("dof"+modelDof);
      addToDebugMessages("Chi"+modelChiSquare);
      addToDebugMessages("P"+modelPValue);
      addToDebugMessages("BIC"+modelBicScore);
      addToDebugMessages("CFI"+modelCfi);
      addToDebugMessages("RMSEA"+modelRmsea);

      bw.write(modelText);
      bw.close();
    } catch (IOException e) {
      addToErrorMessages("Could not write model statistics to output: " + e.toString());
    }

    // Output Tabular editor table
    try {
      fw = new FileWriter(estimatorTableFileName);
      bw = new BufferedWriter( fw );

      ParamTableModel table = new ParamTableModel(semIm);

      int numRows = table.getRowCount();
      int numCols = table.getColumnCount();

      //write column headers
      for (int i = 0; i < numCols; i++) {
        bw.write(table.getColumnName(i));
        if (i != numCols - 1) {
          bw.write("\t");
        }
      }
      bw.write("\n");

      for (int i = 0; i < numRows; i++) {
        for (int j = 0; j < numCols; j++) {
          bw.write(table.getValueAt(i, j).toString());
          if (j != numCols - 1) {
            bw.write("\t");
          }
        }
        if (i != numRows - 1) {
          bw.write("\n");
        }
      }
      bw.close();
    } catch (IOException e) {
      addToErrorMessages("Could not write tabular model data to output: " + e.toString());
    }

    // Output correlation matrix

  }

  /**
   * Format the double into a string
   */
  private static String formatDouble(double d, NumberFormat nf) {
    if (d == Double.POSITIVE_INFINITY) {
      return "Infinity";
    }

    if (-0.001 < d && d < 0.001) {
      return "0.0";
    }

    if (Double.isNaN(d)) {
      return "NaN";
    }

    return nf.format(d);
  }

  /*private static Graph getGraphFromText( BufferedReader b, DataSet d ) {
    try {
      //Get nodes
      List<Node> nodeList = new ArrayList<Node>();
      boolean onNodes = false;
      while ( b.ready() ) {
        String line = b.readLine();
        if ( !onNodes ) {
          if ( line.contains("Graph Nodes:") ) {
            onNodes = true;
            continue;
          }
        }

        String [] nodeAr = line.split(",");
        for ( int i = 0; i < nodeAr.length; i++ ) {
          nodeList.add( new GraphNode( nodeAr[i].replaceAll(" ", "_") ));
        }
        break;
      }

      Graph g = new EdgeListGraph( nodeList );

      //Get edges
      boolean onEdges = false;
      while ( b.ready() ) {
        String line = b.readLine();
        if ( !onEdges ) {
          if ( line.contains("Graph Edges:") ) {
            onEdges = true;
            continue;
          }
          continue;
        }

        String [] tokens = line.split("\\s+");

        if ( tokens.length < 4 ) {
          continue;
        }

        String n0 = ""; //tokens[1];
        String n1 = ""; //tokens[3];
        boolean onFirstNodeName = true;
        //get node names (even if they have spaces)
        for (int i = 1; i < tokens.length; i++) {
          String t = tokens[i];
          if (t.equals("---") || t.equals("-->") || t.equals("<->") || t.equals("o->") || t.equals("o-o")) {
            onFirstNodeName = false;
            continue;
          }
          if (onFirstNodeName) {
            if (i != 1) {
              n0 += "_";
            }
            n0 += t;
          } else {
            if (n1.length() > 0) {
              n1 += "_";
            }
            n1 += t;
          }

        }
        Node node0 = new GraphNode("");
        Node node1 = new GraphNode("");

        for ( int i = 0; i < nodeList.size(); i++ ) {
          Node curr = nodeList.get(i);
          if ( curr.getName().equals(n0) ) {
            node0 = curr;
          }
          if ( curr.getName().equals(n1) ) {
            node1 = curr;
          }
        }

        //get the arrow from the line
        String arrow = "";
        for (int i = 0; i < tokens.length; i++) {
          String t = tokens[i];
          if (t.equals("---") || t.equals("-->") || t.equals("<->") || t.equals("o->") || t.equals("o-o")) {
            arrow = t;
            break;
          }
        }

        if ( arrow.equals("---") ) {
          //TODO: UNCOMMENT NEXT LINE
          addToDebugMessages("" + g.addEdge( Edges.undirectedEdge( node0, node1 ) ));
        } else if ( arrow.equals("-->") ) {
          addToDebugMessages("" + g.addEdge( Edges.directedEdge( node0, node1 ) ));
        } else if ( arrow.equals("<--") ) {
          addToDebugMessages("" + g.addEdge( Edges.directedEdge( node1, node0 ) ));
        } else if ( arrow.equals("<->") ) {
          addToDebugMessages("" + g.addEdge( Edges.bidirectedEdge(node0, node1)));
        } else {
          addToDebugMessages("edge is unreadable" + arrow);
        }
      }
      return g;
    } catch ( Exception e ) {
      addToDebugMessages("Error getting graph: " + e.toString() );
      return new EdgeListGraph();
    }
  }*/

  private static char[] fileToCharArray(File file) {
    try {
      FileReader reader = new FileReader(file);
      CharArrayWriter writer = new CharArrayWriter();
      int c;

      while ((c = reader.read()) != -1) {
        writer.write(c);
      }

      return writer.toCharArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public static boolean addToErrorMessages(String message) {
    try {
      System.out.println(message);

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
  public static boolean addToDebugMessages(String message) {
    try {
      System.out.println(message);

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

  private static void writeGraphToHtml(String graphStr, String programDir, BufferedWriter bWriter) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(programDir + "/program/tetradGraph.html"));

      StringBuilder htmlStr = new StringBuilder();
      while(br.ready()) {
        htmlStr.append(br.readLine());
        if (br.ready()) {
          htmlStr.append("\n");
        }
      }

      String s = htmlStr.toString();

      s = s.replaceAll("PutGraphDataHere", graphStr);

      bWriter.write(s);
      bWriter.close();
    } catch (IOException e) {
      addToErrorMessages("Could not write graph to file. " + e.toString());
    }
  }

  /**
   * Helper class for outputting the tabular statistics.  This code comes from the Tetrad
   * class SemImEditor.java
   */
  private static class ParamTableModel {
    private final NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();
    //private final SemImWrapper wrapper;
    //private SemImEditor.OneEditor editor = null;
    private int maxFreeParamsForStatistics = 500;
    private boolean editable = true;

    private SemIm semIm = null;

    public ParamTableModel (SemIm s) {
      semIm = s;
    }

    public int getRowCount() {
        int numNodes = semIm().getVariableNodes().size();
        return semIm().getNumFreeParams() + semIm().getFixedParameters().size() + numNodes;
    }

    public int getColumnCount() {
        return 7;
    }

    public String getColumnName(int column) {
      switch (column) {
        case 0:
            return "From";
        case 1:
            return "To";
        case 2:
            return "Type";
        case 3:
            return "Value";
        case 4:
            return "SE";
        case 5:
            return "T";
        case 6:
            return "P";
      }

      return null;
    }

    public Object getValueAt(int row, int column) {
          List nodes = semIm().getVariableNodes();
          List parameters = new ArrayList<>(semIm().getFreeParameters());
          parameters.addAll(semIm().getFixedParameters());

          int numParams = semIm().getNumFreeParams() + semIm().getFixedParameters().size();

          if (row < numParams) {
              Parameter parameter = ((Parameter) parameters.get(row));

              switch (column) {
                  case 0:
                      return parameter.getNodeA();
                  case 1:
                      return parameter.getNodeB();
                  case 2:
                      return typeString(parameter);
                  case 3:
                      return asString(paramValue(parameter));
                  case 4:
                      if (parameter.isFixed()) {
                          return "*";
                      } else {
                          return asString(semIm().getStandardError(parameter,
                                  maxFreeParamsForStatistics));
                      }
                  case 5:
                      if (parameter.isFixed()) {
                          return "*";
                      } else {
                          return asString(semIm().getTValue(parameter,
                                  maxFreeParamsForStatistics));
                      }
                  case 6:
                      if (parameter.isFixed()) {
                          return "*";
                      } else {
                          return asString(semIm().getPValue(parameter,
                                  maxFreeParamsForStatistics));
                      }
              }
          } else if (row < numParams + nodes.size()) {
              int index = row - numParams;
              Node node = semIm().getVariableNodes().get(index);
              int n = semIm().getSampleSize();
              int df = n - 1;
              double mean = semIm().getMean(node);
              double stdDev = semIm().getMeanStdDev(node);
              double stdErr = stdDev / Math.sqrt(n);

              double tValue = mean / stdErr;
              double p = 2.0 * (1.0 - ProbUtils.tCdf(Math.abs(tValue), df));

              switch (column) {
                  case 0:
                      return nodes.get(index);
                  case 1:
                      return nodes.get(index);
                  case 2:
                      if (false) {
                          return "Intercept";
                      } else {
                          return "Mean";
                      }
                  case 3:
                      if (false) {
                          double intercept = semIm().getIntercept(node);
                          return asString(intercept);
                      } else {
                          return asString(mean);
                      }
                  case 4:
                      return asString(stdErr);
                  case 5:
                      return asString(tValue);
                  case 6:
                      return asString(p);
              }
          }

          return null;
      }

      private double paramValue(Parameter parameter) {
          double paramValue = semIm().getParamValue(parameter);

          if (false) {
              if (parameter.getType() == ParamType.VAR) {
                  paramValue = 1.0;
              }
              if (parameter.getType() == ParamType.COVAR) {
                  Node nodeA = parameter.getNodeA();
                  Node nodeB = parameter.getNodeB();

                  double varA = semIm().getParamValue(nodeA, nodeA);
                  double varB = semIm().getParamValue(nodeB, nodeB);

                  paramValue *= Math.sqrt(varA * varB);
              }
          } else {
              if (parameter.getType() == ParamType.VAR) {
                  paramValue = Math.sqrt(paramValue);
              }
          }

          return paramValue;
      }

      private String asString(double value) {
          if (Double.isNaN(value)) {
              return " * ";
          } else {
              return nf.format(value);
          }
      }

      private String typeString(Parameter parameter) {
          ParamType type = parameter.getType();

          if (type == ParamType.COEF) {
              return "Edge Coef.";
          }

          if (false) {
              if (type == ParamType.VAR) {
                  return "Correlation";
              }

              if (type == ParamType.COVAR) {
                  return "Correlation";
              }
          }

          if (type == ParamType.VAR) {
              //return "Variance";
              return "Std. Dev.";
          }

          if (type == ParamType.COVAR) {
              return "Covariance";
          }

          throw new IllegalStateException("Unknown param type.");
      }

      private SemIm semIm() {
          return semIm;
      }

  }
}