/* 
 * Copyright (C) 2015-2018 The University of Sheffield.
 *
 * This file is part of gateplugin-CorpusStats
 * (see https://github.com/GateNLP/gateplugin-CorpusStats)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package gate.plugin.corpusstats;

import java.util.Map;

/**
 * Interface describing a stats function
 * @author Johann Petrak
 */
public interface StatsTfIdfFunction {
  /**
   * All statistics function accept the following parameters and return a Double.
   * The Double may be null for special functions which are also configured to filter.
   * @param t - the corpus stats for the term
   * @param nDocs - the total number of documents in the corpus the corpus stats are from
   * @param nWords - total number of words (not distinct terms) in the corpus
   * @param nTerms - the total number of distinct words/terms in the corpus the corpus stats are from
   * @param tf - the local term frequency of the term in the current document
   * @param maxTf - the local maximum term frequency of any term in the current document
   * @param sumTf - the number of all terms in the current document
   * @param options - a Map String to Object for additional parameters and settings that
   * influence how the statistic should get calculated, this can also include any filtering 
   * thresholds/
   * @return TODO
   */
  public Double apply(TermStats t, long nDocs, long nWords, long nTerms, long tf, long maxTf, long sumTf, Map<String,Object> options);
}
