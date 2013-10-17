/*
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoreList {

  public double cScore; // the same as ctf in inverted list
  public double defaultScore; // may use for some operator
  
  
  public double getDefaultScore() {
    return defaultScore;
  }

  public void setDefaultScore(double defaultScore) {
    this.defaultScore = defaultScore;
  }

  public ScoreList(){
    this.defaultScore = 0;
  }
  /**
   * A little utilty class to create a <docid, score> object.
   */
  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();
  Map<Integer,Integer> docIDMap = new HashMap <Integer,Integer>(); 

  /**
   * Append a document score to a score list.
   */
  public void add(int docid, float score) {
    scores.add(new ScoreListEntry(docid, score));
    docIDMap.put(docid, scores.size()-1);
  }

  public int getDocid(int n) {
    return this.scores.get(n).getDocid();
  }

  public float getDocidScore(int n) {
    return this.scores.get(n).getScore();
  }

  public int getDocidIndex(int id) {
    return docIDMap.get(id);
  }
  
  public void setDocidScore(int n,float score) {
    this.scores.get(n).setScore(score);
  }
  public void setScoreEntry(int n,ScoreListEntry entry) {
    this.scores.set(n, entry);
  }
  
  public float getScoreByDocId(int id) {
    return this.scores.get(docIDMap.get(id)).getScore();
  }
}
