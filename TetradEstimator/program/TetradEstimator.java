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


public class TetradEstimator {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

  public TetradEstimator () {}

  public static void main(String [] args) {
    PrintStream sysErr = System.err;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setErr(new PrintStream(baos));

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
    } else if ( cmdParams.containsKey("-file0") == false ) {
      addToErrorMessages("No infile0 ");
      //return;
    } else if ( cmdParams.containsKey("-file1") == false ) {
      addToErrorMessages("No infile1 ");
      //return;
    } else if ( cmdParams.containsKey("-incompleteRows") == false ) {
      addToErrorMessages("No method of filling incomplete rows. ");
      return;
    } else if ( cmdParams.containsKey("-parametricModel") == false ) {
      addToErrorMessages("No parametricModel. ");
      return;
    }

    String estimator = cmdParams.get("-estimator");
    String infile0 = cmdParams.get("-file0");
    String infile1 = cmdParams.get("-file1");
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

      String regressionTableFile = workingDir + "BayesIm.txt";
      String regressionGraphFile = workingDir + "Graph.txt";

      try {

        BufferedReader bReader = null;
        FileReader fReader = null;

        BufferedWriter bWriterTable = null;
        FileWriter fWriterTable = null;

        BufferedWriter bWriterGraph = null;
        FileWriter fWriterGraph = null;

        try {

          fWriterTable = new FileWriter(regressionTableFile);
          bWriterTable = new BufferedWriter(fWriterTable);

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
          //Make graph into a dag if it isn't already
          try {
            Dag dag = new Dag(graph);
          } catch (IllegalArgumentException e) {
            addToDebugMessages("Graph is not a dag, will make one now..." + e.toString());
            DagInPatternWrapper dagWrapper = new DagInPatternWrapper(graph);
            graph = dagWrapper.getGraph();
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
            bWriterTable.append( bayesInstantiatedModel.toString() );
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

            SemPm semPm = new SemPm(graph);
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
                     semInstantiatedModel.getEdgeCoef(edge) + "\n";
            }
            bWriterGraph.append(buf);
            buf += semInstantiatedModel.toString();

            bWriterTable.append(buf);
            break;
          }

          bWriterTable.close();
          bWriterGraph.close();

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
  }

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

}