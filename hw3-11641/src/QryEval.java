/*
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;





public class QryEval {

  static String usage = "Usage:  java " + System.getProperty("sun.java.command")
      + " paramFile\n\n";

  /**
   * The index file reader is accessible via a global variable. This isn't great programming style,
   * but the alternative is for every query operator to store or pass this value, which creates its
   * own headaches.
   */
  public static IndexReader READER;
  
  
  //Define retrieval mode;
  public enum Mode {UNRANKED_BOOLEAN, RANKED_BOOLEAN, BM25, INDRI};

  public static EnglishAnalyzerConfigurable analyzer =  new EnglishAnalyzerConfigurable (Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }

  public static DocLengthStore dls;
  public static BM25Param bm25Param;
  public static IndriParam indriParam;
  public static Map<String,Float> avgDocLen = new HashMap<String, Float>();
  
  /**
   * 
   * @param args The only argument should be one file name, which indicates the parameter file.
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    
    // must supply parameter file
    if (args.length < 1) {
      System.err.println(usage);
      System.exit(1);
    }
    //initialise mode to Unranked boolean
    Mode mode = Mode.UNRANKED_BOOLEAN;
    // read in the parameter file; one parameter per line in format of key=value
    Map<String, String> params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(args[0]));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());
    
    // parameters required for this example to run
    if (!params.containsKey("indexPath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }

    //Start timer to get running time
    long startTime = System.nanoTime();
    
    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));

    if (READER == null) {
      System.err.println(usage);
      System.exit(1);
    }
    
    //Get mode (ranked,unranked boolean or other algo in the future)
    if(!params.containsKey("retrievalAlgorithm"))
    {
      System.err.println("No specify retrievalAlgorithm in parameter.txt, use Unranked Boolean Mode");
      
    }
    else
    {
      String modeStr = (params.get("retrievalAlgorithm"));
      if(modeStr.equalsIgnoreCase("UnrankedBoolean"))
      {
        mode = Mode.UNRANKED_BOOLEAN;
      }
      else if(modeStr.equalsIgnoreCase("RankedBoolean"))
      {
        mode = Mode.RANKED_BOOLEAN;
      }
      else if(modeStr.equalsIgnoreCase("BM25"))
      {
        mode = Mode.BM25;
        if(!params.containsKey("BM25:k_1"))
        {
          System.err.println("Error: Parameters were missing You must specify BM25:k_1.");
          System.exit(1);
        }
        if(!params.containsKey("BM25:b"))
        {
          System.err.println("Error: Parameters were missing You must specify BM25:b.");
          System.exit(1);
        }
        if(!params.containsKey("BM25:k_3"))
        {
          System.err.println("Error: Parameters were missing You must specify BM25:k_3.");
          System.exit(1);
        }
        
        
        bm25Param = new BM25Param(Float.parseFloat(params.get("BM25:k_1")), 
                Float.parseFloat(params.get("BM25:b")), 
                Float.parseFloat(params.get("BM25:k_3")));
        
        if(!bm25Param.validateParam())
        {
          System.err.println("Error: BM25 parameter is invalid.");
          System.exit(1);
        }
      }
      else if(modeStr.equalsIgnoreCase("Indri"))
      {
        mode = Mode.INDRI;
        if(!params.containsKey("Indri:mu"))
        {
          System.err.println("Error: Parameters were missing You must specify BM25:k_1.");
          System.exit(1);
        }
        if(!params.containsKey("Indri:lambda"))
        {
          System.err.println("Error: Parameters were missing You must specify BM25:b.");
          System.exit(1);
        }
        if(!params.containsKey("Indri:smoothing"))
        {
          System.err.println("Error: Parameters were missing You must specify BM25:k_3.");
          System.exit(1);
        }
        
        
        indriParam = new IndriParam(Float.parseFloat(params.get("Indri:mu")), 
                Float.parseFloat(params.get("Indri:lambda")), 
                params.get("Indri:smoothing"));
        
        if(!indriParam.validateParam())
        {
          System.err.println("Error: Indri parameter is invalid.");
          System.exit(1);
        }
      }
    }

    //Read query from file 
    if(!params.containsKey("queryFilePath"))
    {
      System.err.println("Pls specify queryFilePath in parameter.txt");
      System.exit(1);
    }
    
    dls = new DocLengthStore(READER);
    
    avgDocLen.put("title",READER.getSumTotalTermFreq("title") /(float) QryEval.READER.getDocCount ("title"));
    avgDocLen.put("body",READER.getSumTotalTermFreq("body") /(float) QryEval.READER.getDocCount ("body"));
    avgDocLen.put("inlink",READER.getSumTotalTermFreq("inlink") /(float) QryEval.READER.getDocCount ("inlink"));
    avgDocLen.put("url",READER.getSumTotalTermFreq("url") /(float) QryEval.READER.getDocCount ("url"));
    avgDocLen.put("keywords",READER.getSumTotalTermFreq("keywords") /(float) QryEval.READER.getDocCount ("keywords"));
    
 // String[] test = tokenizeQuery("#AND (aparagus.title broccoli cauliflower #SYN(peapods peas))");
    
    Scanner scanQuery = new Scanner(new File(params.get("queryFilePath")));
    do {
      line = scanQuery .nextLine();
      int j = line.indexOf(":");
      String queryID = line.substring(0,j).trim();
      //Parse and Evaluate Result
      line = line.substring(j+1);
      doit(line,mode,queryID);
      
      
      
    } while (scanQuery.hasNext());
    
   
    long endTime = System.nanoTime();
    long duration = endTime - startTime;
    
    //System.out.println(duration); //just use it to get a report, I comment this line cos it will
    // trample with trec eval
    
  
  }

  private static void doit(String line, Mode mode, String queryId) {
    // TODO Auto-generated method stub
    TreeNode root = new TreeNode();
   /* 
    if(line.indexOf("#")<0)
    {
      root.setType(TreeNode.Type.OR);
    }
    */
    if(mode == Mode.BM25)
    {
      if(!line.startsWith("#"))
      {
        root.setType(TreeNode.Type.BMSUM); // if bag of word,use default BMSum
      }
      else
      {
        root.setType(TreeNode.Type.NULL);
      }
    }
    else if(mode == Mode.INDRI)
    {
      if(!line.startsWith("#"))
      {
        root.setType(TreeNode.Type.AND); // if bag of word,use default Indri And
      }
      else
      {
        root.setType(TreeNode.Type.NULL);
      }
    }
    else
    {
      root.setType(TreeNode.Type.OR);
    }
    root.setChildren(root.MakeChildren(line));
    QryResult result = root.eval(mode);
    try {
      //Collections.sort(result.docScores.scores,new DocScoreComparator());
      if(mode == Mode.RANKED_BOOLEAN || mode == Mode.INDRI)
      {
        selectTopK(result,100);
      }
      printResults(queryId,result);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  static private void selectTopK(QryResult list, int k) {

    for(int i=0;i<k && i<list.docScores.scores.size();i++)
    {
      int maxIndex = i;
      ScoreListEntry maxValue = list.docScores.scores.get(i);
      for(int j = i+1; j < list.docScores.scores.size(); j++)
      {
        
        if(list.docScores.scores.get(j).compareTo(maxValue) > 0)
        {
          maxIndex = j;
          maxValue = list.docScores.scores.get(j);
        }
      }
      ScoreListEntry tmp = maxValue;
      list.docScores.setScoreEntry(maxIndex, list.docScores.scores.get(i));
      list.docScores.setScoreEntry(i, tmp);
    }
  }

  /**
   *  Get the external document id for a document specified by an
   *  internal document id.  Ordinarily this would be a simple call to
   *  the Lucene index reader, but when the index was built, the
   *  indexer added "_0" to the end of each external document id.  The
   *  correct solution would be to fix the index, but it's too late
   *  for that now, so it is fixed here before the id is returned.
   * 
   * @param iid The internal document id of the document.
   * @throws IOException 
   */
  static String getExternalDocid (int iid) throws IOException {
    Document d = QryEval.READER.document (iid);
    String eid = d.get ("externalId");

    if ((eid != null) && eid.endsWith ("_0"))
      eid = eid.substring (0, eid.length()-2);

    return (eid);
  }

  /**
   * Prints the query results. 
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT.
   * YOU MUST CHANGE THIS METHOD SO THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, 
   * WHICH IS: 
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName Original query.
   * @param result Result object generated by {@link Qryop#evaluate()}.
   * @throws IOException 
   */
  static void printResults(String queryName, QryResult result) throws IOException {
    /*
    System.out.println(queryName + ":  ");
    if (result.docScores.scores.size() < 1) {
      System.out.println("\tNo results.");
    } else {
      for (int i = 0; i < result.docScores.scores.size(); i++) {
        System.out.println("\t" + i + ":  "
			   + getExternalDocid (result.docScores.getDocid(i))
			   + ", "
			   + result.docScores.getDocidScore(i));
      }
    }
    */
    
    for (int i = 0; i < result.docScores.scores.size() && i<100; i++) {
      System.out.println(queryName+"\tQ0\t" + 
      getExternalDocid (result.docScores.getDocid(i)) +"\t"+
       (i+1) + "\t"  
       + result.docScores.getDocidScore(i) + "\trun_01");
      
     // System.out.println(result.docScores.getDocid(i));
    }
    
    if(result.docScores.scores.isEmpty())
    {
      System.out.println(queryName+"\tQ0\tdummy\t1\t0\trun_01");
    }
  }

  /**
   * Given a query string, returns the terms one at a time with stopwords
   * removed and the terms stemmed using the Krovetz stemmer. 
   * 
   * Use this method to process raw query terms. 
   * 
   * @param query String containing query
   * @return Array of query tokens
   * @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {
    
    TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();
    
    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }

}
