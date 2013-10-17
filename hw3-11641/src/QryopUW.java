import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;



public class QryopUW extends Qryop {

  private int n;
  
  public QryopUW(int n,Qryop... q) {
    this.n = n;
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  /**
   * Evaluate the query operator.
   */
  public QryResult evaluate() throws IOException {

    //InvList list1 =  args.get(0).evaluate().invertedList;
    //InvList list2 =  args.get(1).evaluate().invertedList;
    Vector<InvList> list = new Vector<InvList>();
    int iDoc[] = new int[args.size()];
    Vector<Integer> cDocId = new Vector<Integer>(); //store collection of matched doc
    
    list.add(args.get(0).evaluate().invertedList);
    iDoc[0] =0;
    for(int j=0 ; j<list.get(0).postings.size(); j++)
    {
      cDocId.add(list.get(0).postings.elementAt(j).docid);
    }
    
    for (int i = 1; i < args.size(); i++) {
      list.add(args.get(i).evaluate().invertedList);
      iDoc[i] = 0;
      
      Vector<Integer> tmp = new Vector<Integer>();
      for(int j=0 ; j<list.get(i).postings.size(); j++)
      {
        tmp.add(list.get(i).postings.elementAt(j).docid);
      }
      cDocId.retainAll(tmp);
      
    }
    
    
    QryResult result = new QryResult();
    
    result.invertedList.setFieldString(list.get(0).getFieldString());
    //int i1Doc = 0; /* Index of a document in list1. */
    //int i2Doc = 0; /* Index of a document in list2. */
    int df = 0;
    int ctf = 0;
 
 
    for(int id=0; id< cDocId.size();id++)
    {
 
      int docID = cDocId.get(id);
      //For all docID that have these terms
      /****************************************************/
      ArrayList<Vector<Integer>> arrPos = new ArrayList<Vector<Integer>>();
      int itr[] = new int[list.size()];
      //get positions vector of each terms
      for(int i=0; i<list.size();i++)
      {
        int j=iDoc[i] ;
        while(list.get(i).postings.elementAt(j).docid != docID)
        {
            j++;
        }
        iDoc[i] = j;
        arrPos.add(list.get(i).postings.elementAt(j).positions);
        itr[i] = 0;
      }
    //  System.out.println(arrPos);
        //Vector<Integer> pos1 =  list1.postings.elementAt(i1Doc).positions;
        //Vector<Integer> pos2 =  list2.postings.elementAt(i2Doc).positions;
      DocPosting doc = new DocPosting(docID);
      
      while(true) //loop until some itr end 
      {
        //Find Min, Max
        int maxItr = 0, minItr = 0;
        int maxValue =-1, minValue = 9999999;
        boolean finish = false;
        
        for(int i=0; i<list.size();i++)
        {
          
          int value = arrPos.get(i).elementAt(itr[i]);
          if(value > maxValue)
          {
             maxValue = value;
             maxItr = i;
          }
          
          if(value < minValue)
          {
             minValue = value;
             minItr = i;
          }
          
        }
        if(maxValue < minValue)
        {
          System.out.println("Bug!!");
        }
        //check window
        int window = 1+maxValue - minValue;
        if(window <= n) // match!, move all interator
        {
          
          doc.tf++;
          doc.positions.add(maxValue);
          for(int i=0;i<list.size();i++)
          {
            itr[i]++;
            if(itr[i] >= arrPos.get(i).size())
            {
                finish = true;
                break;
            }
          }
        }
        else
        {
          itr[minItr]++;
          if(itr[minItr] >= arrPos.get(minItr).size())
          {
              finish = true;
              break;
          }
        }
        
        if(finish)
        {
          break;
        }
              
      }
      
      
        
      if(doc.tf>0)
      {
         result.invertedList.postings.add(doc);
         df++;
         ctf += doc.tf;
      }
         
  
    }

    result.invertedList.df = df;
    result.invertedList.ctf = ctf;
    
    return result;
  }
}
