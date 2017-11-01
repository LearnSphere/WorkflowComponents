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


public class GraphEditor {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

  private static Graph graph = new Dag();

  public GraphEditor () {}

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
          if (args[j].charAt(0) == '-' && j > i+1) {
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

    if ( cmdParams.containsKey("-workingDir") == false ) {
      addToErrorMessages("No workingDir");
      return;
    }

    if (cmdParams.containsKey("-file0")) {
      try {
        File inputFile = new File(cmdParams.get("-file0"));
        BufferedReader graphBReader = new BufferedReader(new FileReader(inputFile));
        graph = getGraphFromText(graphBReader);
      } catch (IOException e) {
        addToErrorMessages("Exception opening input file: " + e.toString());
      }
    }
    addToDebugMessages("Graph: \n" + graph.toString());

    String workingDir = cmdParams.get("-workingDir");
    outputDir = workingDir;

    String outputFile = workingDir + "EditedGraph.txt";

    try {

      BufferedReader bReader = null;
      FileReader fReader = null;

      BufferedWriter bWriter = null;
      FileWriter fWriter = null;

      try {

        fWriter = new FileWriter(outputFile);
        bWriter = new BufferedWriter(fWriter);

        int numEdgeEdits = 0;
        int numNodeEdits = 0;
        try {
          numEdgeEdits = Integer.parseInt(cmdParams.get("-numEdgeEdits"));
          numNodeEdits = Integer.parseInt(cmdParams.get("-numNodeEdits"));
        } catch (Exception e) {
          addToDebugMessages("Exception paring number of edits: " + e.toString());
        }
        for (int i = 0; i < numNodeEdits; i++) {
          editNode(i + 1, cmdParams);
        }
        for (int i = 0; i < numEdgeEdits; i++) {
          editEdge(i + 1, cmdParams);
        }

        bWriter.append(graph.toString());
        bWriter.close();

      } catch (IOException e) {
        addToErrorMessages(e.toString());
      }
    } catch (Exception e) {
      addToErrorMessages(e.toString());
    }

    System.setErr(sysErr);

  }

  private static void editNode(int index, HashMap<String, String> params) {
    //Get options from command line
    String operation = params.get("-nodeOperation" + index);
    String nodeType = params.get("-nodeType" + index);
    String nodeName = params.get("-nodeName" + index);

    List<String> existingNodeNames = graph.getNodeNames();

    switch (operation) {
      case "Add_Node":
        if (existingNodeNames.contains(nodeName)) {
          addToErrorMessages("Graph already contains a node named " + nodeName);
          return;
        }
        Node newNode = null;
        try {
          newNode = new GraphNode(nodeName);
          if (nodeType.equals("Latent_Variable")) {
            newNode.setNodeType(NodeType.LATENT);
          }
        } catch (Exception e) {
          addToErrorMessages("Unable to create node " + nodeName + ": " + e.toString());
        }
        try {
          graph.addNode(newNode);
        } catch (Exception e) {
          addToErrorMessages("Unable to add node " + nodeName + " to graph: " + e.toString());
        }
        break;
      case "Remove_Node":
        if (existingNodeNames.contains(nodeName) == false) {
          addToErrorMessages("Graph does not have a node named: " + nodeName);
        }
        Node node = null;
        try {
          node = new GraphNode(nodeName);
          if (nodeType.equals("Latent_Variable")) {
            node.setNodeType(NodeType.LATENT);
          }
        } catch (Exception e) {
          addToErrorMessages("Unable to create node (to be deleted) " + 
              nodeName + ": " + e.toString());
        }
        try {
          graph.removeNode(node);
        } catch (Exception e) {
          addToErrorMessages("Unable to remove node " + nodeName + " from graph: " + e.toString());
        }
        break;
    }
  }

  private static void editEdge(int index, HashMap<String, String> params) {
    //Get options from command line
    String operation = params.get("-operation" + index);
    String from = params.get("-fromNode" + index);
    String to = params.get("-toNode" + index);
    String edgeType = params.get("-edge" + index);

    addToDebugMessages("-fromNode" + index);
    addToDebugMessages(params.get("-fromNode"+index));

    //Check params
    List<String> variables = graph.getNodeNames();
    if (variables.contains(from) == false) {
      addToErrorMessages("Error. Variable " + from + " is trying to be edited in spot #" + 
          (index+1) + ". but it does not exist in graph.");
    }
    if (variables.contains(to) == false) {
      addToErrorMessages("Error. Variable " + to + " is trying to be edited in spot #" + 
          (index+1) + ". but it does not exist in graph.");
    }

    Node a = graph.getNode(from);
    Node b = graph.getNode(to);

    //Create edge to remove or create
    Edge edge = null;
    try {
      switch (edgeType) {
        case "Directed":
          edge = Edges.directedEdge(a, b);
          break;
        case "Bidirected":
          edge = Edges.bidirectedEdge(a, b);
          break;
        case "Undirected":
          edge = Edges.undirectedEdge(a, b);
          break;
        case "Partially_Oriented":
          edge = Edges.partiallyOrientedEdge(a, b);
          break;
        case "Nondirected":
          edge = Edges.nondirectedEdge(a, b);
          break;
      }
    } catch (Exception e) {
      addToErrorMessages("Exception making edge to add/remove: " + e.toString());
    }

    //Add/remove edge
    switch (operation) {
      case "Add_Edge":
        try {
          if (!graph.addEdge(edge)) {
            addToErrorMessages("Was not able to add edge: " + edge.toString());
          }
        } catch (Exception e) {
          addToErrorMessages("Exception adding edge: " + 
              edge.toString() + " ... " + e.toString());
        }
        break;
      case "Remove_Edge":
        try {
          if (!graph.removeEdge(edge)) {
            addToErrorMessages("Was not able to remove edge: " + edge.toString());
          }
        } catch (Exception e) {
          addToErrorMessages("Exception removing edge: " + 
              edge.toString() + " ... " + e.toString());
        }
        break;
    }
  }

  private static Graph getGraphFromText(BufferedReader b) {
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
          g.addEdge(Edges.undirectedEdge(node0, node1));
        } else if (arrow.equals("-->")) {
          g.addEdge(Edges.directedEdge( node0, node1 ) );
        } else if (arrow.equals("<--")) {
          g.addEdge(Edges.directedEdge(node1, node0));
        } else if (arrow.equals("<->")) {
          g.addEdge(Edges.bidirectedEdge(node0, node1));
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