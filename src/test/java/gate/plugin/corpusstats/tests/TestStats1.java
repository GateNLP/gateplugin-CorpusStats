/*
 * Copyright (c) 2015-2016 The University Of Sheffield.
 *
 * This file is part of gateplugin-LearningFramework 
 * (see https://github.com/GateNLP/gateplugin-LearningFramework).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package gate.plugin.corpusstats.tests;

import gate.plugin.corpusstats.CorpusStatsCollocationsData;
import gate.plugin.corpusstats.PairStats;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import static org.junit.Assert.*;
import org.junit.Test;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;

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
    ConcurrentHashMap<String,LongAdder> cps = cs.countsPairs;
    cps.computeIfAbsent("companies\tnew", (var -> new LongAdder())).add(8);
    cs.countsTerms.computeIfAbsent("companies", (var -> new LongAdder())).add(8+4667);
    cs.countsTerms.computeIfAbsent("new", (var -> new LongAdder())).add(8+15820);
    cs.initStats();
    PairStats ret = cs.calcStats("companies\tnew");
    System.err.println("DEBUG: got stats="+ret);
    
    assertEquals(1.54886, ret.chi2, 0.001);
    assertEquals(1.54886, ret.chi2, 0.001);
    
    assertEquals(0.999932, ret.student_t, 0.0000005);
    
    TDistribution tdist = new TDistribution(8+15820+4667+14287173-1);
    double prob_critical = tdist.cumulativeProbability(2.576);
    assertEquals(0.995, prob_critical, 0.0001);
    
    
    
    
 } // testStats1
  
  
} // class
