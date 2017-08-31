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
        cmdParams.put( s, args[i + 1] );
        i++;
      }
    }

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
    String workingDir = cmdParams.get("-workingDir");
    outputDir = workingDir;
    String infile0 = cmdParams.get("-file0");
    String infile1 = cmdParams.get("-file1");
    String incompleteRows = cmdParams.get("-incompleteRows");
    String parametricModel = cmdParams.get("-parametricModel");
    String optimizer = cmdParams.get("-optimizer");
    String score = cmdParams.get("-score");
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
          reader.setMaxIntegralDiscrete(25);
          reader.setDelimiter(DelimiterType.WHITESPACE);

          DataSet data = reader.parseTabular(chars);

          List<String> variableNames = data.getVariableNames();



          fReader = new FileReader( inputFile1 );
          bReader = new BufferedReader( fReader );

          Graph graph = getGraphFromText( bReader, data );
          //Make graph into a dag if it isn't already
          try {
            Dag dag = new Dag(graph);
          } catch (IllegalArgumentException e) {
            addToDebugMessages("Graph is not a dag, will make one now..l" + e.toString());
            DagInPatternWrapper dagWrapper = new DagInPatternWrapper(graph);
            graph = dagWrapper.getGraph();
          }

          Parameters params = new Parameters();
          params.set("initializationMode", "manual");

          int numNodes = graph.getNumNodes();

          switch (parametricModel) {
          case "Bayes_Parametric_Model":
            if (data.isContinuous() || data.isMixed()) {
              addToErrorMessages("Data set is continuous or mixed, but it must be discrete.");
              break;
            }
            BayesPmWrapper bayesWrapper = new BayesPmWrapper( graph , params );

            BayesIm bayesInstantiatedModel = null;// estWrap.getEstimatedBayesIm();

            if (estimator.equals("ML_Bayes_Estimator")) {
              BayesEstimatorWrapper estWrap = new BayesEstimatorWrapper(
                  new DataWrapper( data ), bayesWrapper );
              bayesInstantiatedModel = estWrap.getEstimatedBayesIm();
            } else if (estimator.equals("EM_Bayes_Estimator")) {
              Parameters estParams = new Parameters();
              estParams.set("tolerance", tolerance);
              EmBayesEstimatorWrapper estWrap = new EmBayesEstimatorWrapper(
                  new DataWrapper(data), bayesWrapper, estParams);
              bayesInstantiatedModel = estWrap.getEstimateBayesIm();
            }

            if (incompleteRows.equals("Randomize_incomplete_rows")) {
              for (int i = 0 ; i < numNodes ; i++ ) {
                bayesInstantiatedModel.randomizeIncompleteRows( i );
              }
            }
            bWriterTable.append( bayesInstantiatedModel.toString() );
            break;

          case "SEM_Parametric_Model":
            SemPmWrapper semWrapper = new SemPmWrapper( graph );
            addToDebugMessages("made semWrapper");


            SemEstimatorWrapper semEstWrap = new SemEstimatorWrapper(
              new DataWrapper( data ), semWrapper, new Parameters() );
            addToDebugMessages("made estimator wrapper");


            SemIm semInstantiatedModel = semEstWrap.getEstimatedSemIm();

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
            buf += semInstantiatedModel.toString();

            bWriterTable.append(buf);
            break;
          }

          bWriterTable.close();

          bWriterGraph.append( graph.toString() );
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
          nodeList.add( new GraphNode( nodeAr[i] ));
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
        }

        String [] tokens = line.split("\\s+");

        if ( tokens.length < 4 ) {
          continue;
        }

        String n0 = tokens[1];
        String n1 = tokens[3];
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

        if ( tokens[2].equals("---") ) {
          //TODO: UNCOMMENT NEXT LINE
          //addToDebugMessages("" + g.addEdge( Edges.nondirectedEdge( node0, node1 ) ));
        } else if ( tokens[2].equals("-->") ) {
          addToDebugMessages("" +  g.addEdge( Edges.directedEdge( node0, node1 ) ));
        } else if ( tokens[2].equals("<--") ) {
          addToDebugMessages( "" + g.addEdge( Edges.directedEdge( node1, node0 ) ));
        } else if ( tokens[2].equals("<->") ) {
          addToDebugMessages("" + g.addEdge( Edges.bidirectedEdge(node0, node1)));

        } else {
          addToDebugMessages("edge is unreadable" + tokens[2]);
        }
      }
      return g;
    } catch ( Exception e ) {
      addToDebugMessages("Error getting graph: " + e.toString() );
      return new EdgeListGraph();
    }
  }

  /*private static int getNumCategories( String nName, DataSet d ) {
    TetradMatrix tm = d.getDoubleData();
    double[][] data = tm.toArray();

    int index = d.getVariableNames().indexOf(nName);

    double[] col = data[index];
    HashSet<Double> hs = new HashSet<Double>();

    for ( int i = 0; i < col.length; i++ ) {
      if ( (col[i] % 1) == 0 ) {
        hs.add( new Double( col[i] ) );
      } else {
        return -1;
      }
    }
    return hs.size();
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