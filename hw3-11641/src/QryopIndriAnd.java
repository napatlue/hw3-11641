import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class QryopIndriAnd extends Qryop {

  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new
   * qryopAnd (arg1, arg2, arg3, ...).
   */
  public QryopIndriAnd(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  
  /**
   * Compute P(q/d).
   */
  public double computePQD(double tf,double ctf,float lenD,long lenC)
  {
    double result = 0;
    float mu = QryEval.indriParam.mu;
    float lambda = QryEval.indriParam.lambda;
    
    
    double tmp1 = tf+ (mu*ctf)/lenC;
    double tmp2 = lenD + mu;
    double tmp3 = lambda * tmp1/tmp2;
    double tmp4 = (1-lambda) * ctf/lenC;
    
    result = Math.log(tmp3+tmp4);
    return result;
           
  }
  
  public double getDefaultScore(double ctf,long lenC,int Q,String field)
  {
    float lenD =  (float)QryEval.avgDocLen.get(field);
    return computePQD(0,ctf,lenD,lenC)/(float)Q;
  }
  
  /**
   * Evaluate the query operator.
   */
  public QryResult evaluate() throws IOException {


    HashMap<Integer,Double> map = new HashMap<Integer,Double>(); //map between doc id and new score
    
    double operDefaultScore = 0;
    
    //Vector<InvList> termList = new Vector<InvList>();
    //int iDoc[] = new int[args.size()];
   // Vector<Integer> cDocId = new Vector<Integer>();
    HashSet<Integer> cDocId = new HashSet<Integer>();
    Vector<QryResult> argsVector = new Vector<QryResult>();
    
    InvList list;
    ScoreList sclist;
    for (int i = 0; i < args.size(); i++) {
      //termList.add(args.get(i).evaluate().invertedList);
      //iDoc[i] = 0;
      QryResult qry = args.get(i).evaluate();
      argsVector.add(qry);
      list = qry.invertedList;
      if(list.df > 0) //case we get invertList
      {
        for(int j=0 ; j<list.postings.size(); j++)
        {
          cDocId.add(list.postings.elementAt(j).docid);

        }
      }
      else
      {
        sclist = qry.docScores;
        for(int j=0 ; j<sclist.scores.size(); j++)
        {
          cDocId.add(sclist.scores.get(j).getDocid());

        }
        
      }
    
    }
    
    
    Vector<Integer> arrDocId = new Vector<Integer>(cDocId);
    Collections.sort(arrDocId);
    //TreeSet<Integer> sDocId = new TreeSet<Integer>(cDocId);
    //int maxDocId = sDocId.last();
    //Integer[] arrDocId =  (Integer[]) sDocId.toArray();
    
    //int N = QryEval.READER.getDocCount ("body");
    QryResult result = new QryResult();
    // Each pass of the loop evaluates one query argument(plus this query term score to every docs in its posting).
    
    double tf;
    double ctf;
    long lenC;
    long lenD;
    double score;
    //double avgDocLen = QryEval.avgDocLen;
    
    for (int i = 0; i < args.size(); i++) 
    {

     
      QryResult qry = argsVector.get(i);
      list = qry.invertedList;
      
      if(list.df>0) // in case we have inverted list
      {
        int start = 0;
        String field = list.getFieldString();
        lenC = QryEval.READER.getSumTotalTermFreq(field);
        
        double defaultScore = getDefaultScore(list.ctf, lenC, args.size(),field);
        
        operDefaultScore += defaultScore; //Important! this is to set default score for this entire operation (outer operation may need it)
        
        for(int j=0 ; j<list.df; j++)
        {
          DocPosting doc = list.postings.get(j);
          int id = doc.docid;
          
          
          //Assign default score start from previous match docId to this id
          int  l = start;
          while(arrDocId.get(l) < id) 
          {
            int dId = arrDocId.get(l) ;
  //          tf = 0;
  //          ctf = list.ctf;
  //          
  //          //lenD = QryEval.dls.getDocLength(field, dId);
  //          lenD = (long)avgDocLen;
  //          score = computePQD(tf, ctf, lenD, lenC);
            
            //For and operation need to divide by query len
            //score = score/args.size();
            score = defaultScore;
                   
            
            if(!map.containsKey(dId))
            {
              map.put(dId, score);
            }
            else
            {
              double newScore = score + map.get(dId);
              map.put(dId, newScore);
            }
            l++;
          }
          start = l+1;
          
          
          //compute score p(q/d)
          tf = doc.tf; //tf
          ctf = list.ctf;
          //lenC = QryEval.READER.getSumTotalTermFreq(field);
          lenD = QryEval.dls.getDocLength(field, id);
          score = computePQD(tf, ctf, lenD, lenC);
          
          //For and operation need to divide by query len
          score = score/args.size();
          
          
          
          //check if no score for this doc in map, create a new one
          if(!map.containsKey(id))
          {
            map.put(id, score);
          }
          else
          {
            double newScore = score + map.get(id);
            map.put(id, newScore);
          }
    
          //System.gc();
          
        }
        
        //now we have to set default score for left over doc in array
        int e =start;
        while(e < arrDocId.size()) 
        {
          int dId = arrDocId.get(e) ;
  //        tf = 0;
  //        ctf = list.ctf;
  //        
  //        //lenD = QryEval.dls.getDocLength(field, dId);
  //        //lenD = (long)avgDocLen;
  //        score = computePQD(tf, ctf, lenD, lenC);
  //        
  //        //For and operation need to divide by query len
  //        score = score/args.size();
          score = defaultScore;
          
          if(!map.containsKey(dId))
          {
            map.put(dId, score);
          }
          else
          {
            double newScore = score + map.get(dId);
            map.put(dId, newScore);
          }
          e++;
        }
      }  
      else // case we get score list
      {
         sclist = qry.docScores;
       
         //Collections.sort(sclist.scores,ScoreListEntry.docIDComparator);
         operDefaultScore += sclist.defaultScore/args.size(); //Important! this is to set default score for this entire operation (outer operation may need it)
         
         //Vector<Integer> docSet = new Vector<Integer>();
         HashSet<Integer> docSet = new HashSet<Integer>();
         for(int j=0;j<sclist.scores.size();j++)
         {
           
           int id = sclist.scores.get(j).getDocid();
           docSet.add(id);
           
           score = sclist.getScoreByDocId(id)/args.size();
           
           //check if no score for this doc in map, create a new one
           if(!map.containsKey(id))
           {
             map.put(id, score);
           }
           else
           {
             double newScore = score + map.get(id);
             map.put(id, newScore);
           }
           
         }
         
         HashSet<Integer> tmp = new HashSet<Integer>(cDocId);
         tmp.removeAll(docSet);
         Iterator<Integer> it = tmp.iterator();
         
         while(it.hasNext())
         {
           
           int did = it.next();
           score = sclist.defaultScore/args.size();
           
           //check if no score for this doc in map, create a new one
           if(!map.containsKey(did))
           {
             map.put(did, score);
           }
           else
           {
             double newScore = score + map.get(did);
             map.put(did, newScore);
           }
         }
         

         
      }
      
    }
      
    //Now we have complete map from internal docid to score, we need only top 100 docs
//    Set<Integer> kDocIdSet = new TreeSet<Integer>(); 
    
    
//    for(int i=0;i<100 && i<map.size();i++)
//    {
//      int maxIndex = -1;
//      double maxValue = -9999999.99;
//      Iterator<Entry<Integer, Double>> it = map.entrySet().iterator();
//      
//      while (it.hasNext()) {
//          @SuppressWarnings("rawtypes")
//          Map.Entry pairs = (Map.Entry)it.next();
//          int key = (Integer)pairs.getKey();
//          double value = (Double)pairs.getValue();
//          
//          if(!kDocIdSet.contains(key) && ( value >maxValue  || (value==maxValue && key<maxIndex))) {
//              
//              maxIndex = key;
//              maxValue = value;
//          }
//          
//          //it.remove(); // avoids a ConcurrentModificationException
//      }
//      
//      result.docScores.add(maxIndex,(float)maxValue);
//      kDocIdSet.add(maxIndex);
//    }
    
    // Not prune anymore add everything to list
    Iterator<Entry<Integer, Double>> it = map.entrySet().iterator();
    while (it.hasNext()) {
      @SuppressWarnings("rawtypes")
      Map.Entry pairs = (Map.Entry)it.next();
      int key = (Integer)pairs.getKey();
      double value = (Double)pairs.getValue();    
      result.docScores.add(key,(float)value);
    }
    
    result.docScores.setDefaultScore(operDefaultScore); //This should set default score for this operation correctly
 
    return result;
  }
}