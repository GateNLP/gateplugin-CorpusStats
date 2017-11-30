/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.corpusstats;

import java.util.Map;

/**
 * Interface describing a stats function
 * @author Johann Petrak
 */
public interface StatsFunction {
  /**
   * All statistics function accept the following parameters and return a Double.
   * The Double may be null for special functions which are also configured to filter.
   * @param t - the corpus stats for the term
   * @param nDocs - the total number of documents in the corpus the corpus stats are from
   * @param nTerms - the total number of words/terms in the corpus the corpus stats are from
   * @param tf - the local term frequency of the term in the current document
   * @param maxTf - the local maximum term frequency of any term in the current document
   * @param sumTf - the number of all terms in the current document
   * @oaram options - a Map String to Object for additional parameters and settings that
   * influence how the statistic should get calculated, this can also include any filtering 
   * thresholds/
   * @return 
   */
  public Double apply(TermStats t, long nDocs, long nTerms, long tf, long maxTf, long sumTf, Map<String,Object> options);
}
