import java.io.IOException;

public class QryopRankOr extends Qryop {

  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new
   * qryopAnd (arg1, arg2, arg3, ...).
   */
  public QryopRankOr(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  /**
   * Evaluate the query operator.
   */
  public QryResult evaluate() throws IOException {

    // Seed the result list by evaluating the first query argument. The result could be docScores or
    // invList, depending on the query operator. Wrap a SCORE query operator around it to force it
    // to be a docScores list. There are more efficient ways to do this. This approach is just easy
    // to see and understand.
    Qryop impliedQryOp = new QryopScore(args.get(0),false);
    QryResult result = impliedQryOp.evaluate();

    // Each pass of the loop evaluates one query argument.
    for (int i = 1; i < args.size(); i++) {

      impliedQryOp = new QryopScore(args.get(i),false);
      QryResult iResult = impliedQryOp.evaluate();

      // Use the results of the i'th argument to incrementally compute the query operator.
      // Intersection-style query operators iterate over the incremental results, not the results of
      // the i'th query argument.
      int rDoc = 0; /* Index of a document in result. */
      int iDoc = 0; /* Index of a document in iResult. */
/*
      while (rDoc < result.docScores.scores.size() && iDoc < iResult.docScores.scores.size()) {

        // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
        // Unranked Boolean Or. Add to the incremental result any documents that are not already in the result yet.

        if(result.docScores.getDocid(rDoc) == iResult.docScores.getDocid(iDoc))
        {
          
          float rScore = result.docScores.getDocidScore(rDoc); 
          float iScore = iResult.docScores.getDocidScore(iDoc);
          result.docScores.setDocidScore(rDoc, Math.max(rScore,iScore));
          rDoc++;
          iDoc++;
        }
        else if(result.docScores.getDocid(rDoc) < iResult.docScores.getDocid(iDoc))
        {
          rDoc++;
        }
        else
        {
          result.docScores.scores.add(rDoc, iResult.docScores.scores.get(iDoc));
          iDoc++;
          rDoc++;  //check this line!
        }
        
      }
      //add leftover doc from iResult if any
      while(iDoc < iResult.docScores.scores.size())
      {
        result.docScores.scores.add(iResult.docScores.scores.get(iDoc));
        iDoc++;
      }
  */
      
      QryResult tmpResult = new QryResult();
      while (rDoc < result.docScores.scores.size() && iDoc < iResult.docScores.scores.size()) {

        // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
        // Unranked Boolean Or. Add to the incremental result any documents that are not already in the result yet.
        float rScore = result.docScores.getDocidScore(rDoc); 
        float iScore = iResult.docScores.getDocidScore(iDoc);
        if(result.docScores.getDocid(rDoc) == iResult.docScores.getDocid(iDoc))
        {

          tmpResult.docScores.add(result.docScores.getDocid(rDoc), Math.max(rScore, iScore));
          rDoc++;
          iDoc++;

        }
        else if(result.docScores.getDocid(rDoc) < iResult.docScores.getDocid(iDoc))
        {
          tmpResult.docScores.add(result.docScores.getDocid(rDoc),  rScore);
          rDoc++;
          
        }
        else
        {
          tmpResult.docScores.add(iResult.docScores.getDocid(iDoc), iScore);
          iDoc++;
        }
        
      }
      //add leftover doc from iResult if any
      while(iDoc < iResult.docScores.scores.size())
      {
        tmpResult.docScores.add(iResult.docScores.getDocid(iDoc), iResult.docScores.getDocidScore(iDoc));
        iDoc++;
      }
      //add leftover doc from result if any
      while(rDoc < result.docScores.scores.size())
      {
        tmpResult.docScores.add(result.docScores.getDocid(rDoc), result.docScores.getDocidScore(rDoc));
        rDoc++;
      }
      
      result = tmpResult;
    }

    return result;
  }
}