import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class QryopBMSum extends Qryop {

  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new
   * qryopAnd (arg1, arg2, arg3, ...).
   */
  public QryopBMSum(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  /**
   * Evaluate the query operator.
   */
  public QryResult evaluate() throws IOException {


    //QryResult result = impliedQryOp.evaluate();
    HashMap<Integer,Double> map = new HashMap<Integer,Double>(); //map between doc id and new score
    
    
    QryResult result = new QryResult();
    // Each pass of the loop evaluates one query argument(plus this query term score to every docs in its posting).
    for (int i = 0; i < args.size(); i++) 
    {

      //Qryop impliedQryOp = new QryopScore(args.get(i),false);
      //QryResult iResult = impliedQryOp.evaluate();

      InvList list = args.get(i).evaluate().invertedList;
      
      int N = QryEval.READER.getDocCount (list.getFieldString());
      
      for(int j=0 ; j<list.df; j++)
      {
        DocPosting doc = list.postings.get(j);
        int id = doc.docid;
        double tf = doc.tf; //tf
        double df = list.df;
        
        //check if no score for this doc in map, create a new one
        if(!map.containsKey(id))
        {
          map.put(id, 0.0);
        }
        
        
        if(tf==0) //just to save time
        {
          continue;
        }
        
        double IDF = Math.log((N-df+0.5)/(df+0.5)); //IDF
        
        double tfWeight;
        float k1 = QryEval.bm25Param.k1;
        float b = QryEval.bm25Param.b;
        float k3 = QryEval.bm25Param.k3;
        double doclen = QryEval.dls.getDocLength(list.getFieldString(),id);
        double avgDocLen = QryEval.avgDocLen.get(list.getFieldString());
        
        double tmp = k1*( (1-b) + b*doclen/avgDocLen );
        tfWeight = tf/(tf+tmp); // tf weight
        
        int qtf =1;
        double userWeight = ( (k3+1)*qtf)/(k3+qtf);
        
        double score = IDF*tfWeight*userWeight;
        double newScore = score + map.get(id);
        map.put(id, newScore);
        
        
      }
    }
      
    //Now we have complete map from internal docid to score, we need only top 100 docs
    Set<Integer> kDocIdSet = new TreeSet<Integer>(); 
    
    
    for(int i=0;i<100 && i<map.size();i++)
    {
      int maxIndex = -1;
      double maxValue = -9999999.99;
      Iterator<Entry<Integer, Double>> it = map.entrySet().iterator();
      
      while (it.hasNext()) {
          @SuppressWarnings("rawtypes")
          Map.Entry pairs = (Map.Entry)it.next();
          int key = (Integer)pairs.getKey();
          double value = (Double)pairs.getValue();
          
          if(!kDocIdSet.contains(key) && ( value >maxValue  || (value==maxValue && key<maxIndex))) {
              
              maxIndex = key;
              maxValue = value;
          }
          
          //it.remove(); // avoids a ConcurrentModificationException
      }
      
      result.docScores.add(maxIndex,(float)maxValue);
      kDocIdSet.add(maxIndex);
    }
   

    return result;
  }
}