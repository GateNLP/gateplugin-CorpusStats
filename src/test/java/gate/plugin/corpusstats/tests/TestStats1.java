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

package gate.plugin.corpusstats.tests;

import gate.plugin.corpusstats.CorpusStatsCollocationsData;
import gate.plugin.corpusstats.PairStats;
import java.util.concurrent.atomic.LongAdder;
import static org.junit.Assert.*;
import org.junit.Test;
import org.apache.commons.math3.distribution.TDistribution;

/**
 *
 * @author Johann Petrak
 */
public class TestStats1 {
  @Test
  public void testStats1() {
    CorpusStatsCollocationsData cs = new CorpusStatsCollocationsData();
    // example 1: table 5.8 from Manning1999
    cs.nDocs.add(1);
    cs.totalContexts.add(8+15820+4667+14287173);
    cs.countsPairs.computeIfAbsent("companies\tnew", (var -> new LongAdder())).add(8);
    cs.countsTerms1.computeIfAbsent("companies", (var -> new LongAdder())).add(8+4667);
    cs.countsTerms1.computeIfAbsent("new", (var -> new LongAdder())).add(8+15820);    
    cs.initStats();
    PairStats ret = cs.calcStats("companies\tnew");
    System.err.println("DEBUG: ex1, got stats="+ret);
    
    assertEquals(1.54886, ret.chi2, 0.001);
    assertEquals(1.54886, ret.chi2, 0.001);
    
    assertEquals(0.999932, ret.student_t, 0.0000005);
    
    TDistribution tdist = new TDistribution(8+15820+4667+14287173-1);
    double prob_critical = tdist.cumulativeProbability(2.576);
    assertEquals(0.995, prob_critical, 0.0001);
    
    // example 2: table 5.15 from Manning1999
    cs = new CorpusStatsCollocationsData();
    cs.nDocs.add(1);
    cs.totalContexts.add(31950+12004+4793+848330);
    cs.countsPairs.computeIfAbsent("chambre\thouse", (var -> new LongAdder())).add(31950);
    cs.countsTerms1.computeIfAbsent("chambre", (var -> new LongAdder())).add(31950+4793);
    cs.countsTerms1.computeIfAbsent("house", (var -> new LongAdder())).add(31950+12004);
    cs.initStats();
    ret = cs.calcStats("chambre\thouse");
    System.err.println("DEBUG: ex2a, got stats="+ret);
    
    assertEquals(4.149, ret.pmi, 0.01);
    assertEquals(553609.574, ret.chi2, 1.0);
    
    // example 3: Jurafsky2016
    cs = new CorpusStatsCollocationsData();
    cs.totalContexts.add(19);
    cs.countsPairs.computeIfAbsent("computer\tdigital", (var -> new LongAdder())).add(2);
    cs.countsPairs.computeIfAbsent("computer\tinformation", (var -> new LongAdder())).add(1);
    cs.countsPairs.computeIfAbsent("data\tdigital", (var -> new LongAdder())).add(1);
    cs.countsPairs.computeIfAbsent("data\tinformation", (var -> new LongAdder())).add(6);
    cs.countsPairs.computeIfAbsent("apricot\tpinch", (var -> new LongAdder())).add(1);
    cs.countsPairs.computeIfAbsent("pinch\tpineapple", (var -> new LongAdder())).add(1);
    cs.countsPairs.computeIfAbsent("digital\tresult", (var -> new LongAdder())).add(1);
    cs.countsPairs.computeIfAbsent("information\tresult", (var -> new LongAdder())).add(4);
    cs.countsPairs.computeIfAbsent("apricot\tsugar", (var -> new LongAdder())).add(1);
    cs.countsPairs.computeIfAbsent("pineapple\tsugar", (var -> new LongAdder())).add(1);
    
    cs.countsTerms1.computeIfAbsent("computer", (var -> new LongAdder())).add(3);
    cs.countsTerms1.computeIfAbsent("data", (var -> new LongAdder())).add(7);
    cs.countsTerms1.computeIfAbsent("pinch", (var -> new LongAdder())).add(2);
    cs.countsTerms1.computeIfAbsent("result", (var -> new LongAdder())).add(5);
    cs.countsTerms1.computeIfAbsent("sugar", (var -> new LongAdder())).add(2);
    
    cs.countsTerms1.computeIfAbsent("apricot", (var -> new LongAdder())).add(2);
    cs.countsTerms1.computeIfAbsent("pineapple", (var -> new LongAdder())).add(2);
    cs.countsTerms1.computeIfAbsent("digital", (var -> new LongAdder())).add(4);
    cs.countsTerms1.computeIfAbsent("information", (var -> new LongAdder())).add(11);
    
    ret = cs.calcStats("apricot\tpinch");
    assertEquals(0.05, ret.p_a_b, 0.01);
    
    ret = cs.calcStats("data\tinformation");
    System.err.println("DEBUG: ex3a, got stats="+ret);    
    assertEquals(0.316, ret.p_a_b, 0.001);
    assertEquals(0.579, ret.p_b, 0.001);
    assertEquals(0.368, ret.p_a, 0.001);
    // The value in the book is the result of calculating from rounded values,
    // this is the more accurate one:
    assertEquals(0.5661, ret.pmi, 0.001);
    
 } // testStats1
  
  
} // class
