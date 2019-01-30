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

import gate.util.GateRuntimeException;
import java.util.HashMap;
import java.util.Map;

/**
 * All the supported stats functions.
 * This class contains the definitions of the stats functions and a static
 * map that maps the name of the function to its definition. It also 
 * has a method to convert a list of function names to a list of functions.
 * 
 * @author Johann Petrak
 */
public class StatsTfIdfFunctions {
  private static final Map<String,StatsTfIdfFunction> NAME2FUNCTION = new HashMap<String,StatsTfIdfFunction>();
  static {
    // Our default idf prevents division by zero by adding one to the DF and one to the nDocs
    // (if we would not add 1 to nDocs, the quotient could become smaller than 1 and the logarithm
    // would get negative which we do not want)
    // We also add 1 to the logarithm, so the lower bound is 1.0. Because of how we smooth the quotient,
    // this is a strict lower bound/

    StatsTfIdfFunction f_nDocs = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return Long.valueOf(nDocs).doubleValue();
          }; 
    NAME2FUNCTION.put("nDocs", f_nDocs);
    
    StatsTfIdfFunction f_nWords = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return Long.valueOf(nWords).doubleValue();
          }; 
    NAME2FUNCTION.put("nWords", f_nWords);
    
    StatsTfIdfFunction f_nTerms = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return Long.valueOf(nTerms).doubleValue();
          }; 
    NAME2FUNCTION.put("nTerms", f_nTerms);
    
    StatsTfIdfFunction idf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return 1.0+Math.log((nDocs+1.0)/(termstats.getDf()+1.0));
          }; 
    NAME2FUNCTION.put("idf",idf);
    
    StatsTfIdfFunction df = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return (double)termstats.getDf();
          }; 
    NAME2FUNCTION.put("df",df);

    StatsTfIdfFunction tf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return ((Number)localtf).doubleValue();
          }; 
    NAME2FUNCTION.put("tf",tf);

    StatsTfIdfFunction ntf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return ((Number)localtf).doubleValue()/maxTf;
          }; 
    NAME2FUNCTION.put("ntf",ntf);

    StatsTfIdfFunction wtf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return ((Number)localtf).doubleValue()/sumTf;
          }; 
    NAME2FUNCTION.put("wtf",wtf);

    StatsTfIdfFunction ltf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return 1.0 + Math.log(localtf);
          }; 
    NAME2FUNCTION.put("ltf",ltf);

    
    
    StatsTfIdfFunction tfidf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return localtf * idf.apply(termstats, nDocs, nWords, nTerms, localtf, maxTf, sumTf, options);
          }; 
    NAME2FUNCTION.put("tfidf",tfidf);
    
    StatsTfIdfFunction ntfidf = (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return ntf.apply(termstats, nDocs, nWords, nTerms, localtf, maxTf, sumTf, options) * 
                   idf.apply(termstats, nDocs, nWords, nTerms, localtf, maxTf, sumTf, options);
          }; 
    NAME2FUNCTION.put("ntfidf",ntfidf);
    
    StatsTfIdfFunction wtfidf = (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return wtf.apply(termstats, nDocs, nWords, nTerms, localtf, maxTf, sumTf, options) * 
                   idf.apply(termstats, nDocs, nWords, nTerms, localtf, maxTf, sumTf, options);
          }; 
    NAME2FUNCTION.put("wtfidf",wtfidf);
    
    StatsTfIdfFunction ltfidf = (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return ltf.apply(termstats, nDocs, nWords, nTerms, localtf, maxTf, sumTf, options) * 
                   idf.apply(termstats, nDocs, nWords, nTerms, localtf, maxTf, sumTf, options);
          }; 
    NAME2FUNCTION.put("ltfidf",ltfidf);
    
    StatsTfIdfFunction f_ctf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return Long.valueOf(termstats.getTf()).doubleValue();
          }; 
    NAME2FUNCTION.put("ctf", f_ctf);
    
    StatsTfIdfFunction f_cntf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return termstats.getNTf();
          }; 
    NAME2FUNCTION.put("cntf", f_cntf);
    
    StatsTfIdfFunction f_cwtf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return termstats.getWTf();
          }; 
    NAME2FUNCTION.put("cwtf", f_cwtf);
    
    StatsTfIdfFunction f_actf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return Long.valueOf(termstats.getTf()).doubleValue()/nDocs;
          }; 
    NAME2FUNCTION.put("actf", f_actf);
    
    StatsTfIdfFunction f_acntf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return termstats.getNTf()/nDocs;
          }; 
    NAME2FUNCTION.put("acntf", f_acntf);
    
    StatsTfIdfFunction f_acwtf = 
          (TermStats termstats, long nDocs, long nWords, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return termstats.getNTf()/nDocs;
          }; 
    NAME2FUNCTION.put("acwtf", f_acwtf);
    
  }
  // Convert a list of comma/semicolon/whitespace separated names of functions to a list of actual function lambdas
  public static Map<String,StatsTfIdfFunction> names2functions(String names) {
    Map<String,StatsTfIdfFunction> ret = new HashMap<>();
    String[] namesArray = names.split("[,;\\s]",-1);
    for(String name : namesArray) {
      if(NAME2FUNCTION.containsKey(name)) {
        ret.put(name,NAME2FUNCTION.get(name));
      } else {
        throw new GateRuntimeException("Statistic not know: "+name);
      }
    }
    return ret;
  }
}
