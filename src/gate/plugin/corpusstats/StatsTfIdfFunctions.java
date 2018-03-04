/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author Johann Petrak
 */
public class StatsTfIdfFunctions {
  private static final Map<String,StatsTfIdfFunction> name2function = new HashMap<String,StatsTfIdfFunction>();
  static {
    // Our default idf prevents division by zero by adding one to the DF and one to the nDocs
    // (if we would not add 1 to nDocs, the quotient could become smaller than 1 and the logarithm
    // would get negative which we do not want)
    // We also add 1 to the logarithm, so the lower bound is 1.0. Because of how we smooth the quotient,
    // this is a strict lower bound/
    StatsTfIdfFunction idf = 
          (TermStats termstats, long nDocs, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return 1.0+Math.log(((double)nDocs+1.0)/((double)termstats.getDf()+1.0));
          }; 
    name2function.put("idf",idf);
    
    StatsTfIdfFunction df = 
          (TermStats termstats, long nDocs, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return (double)termstats.getDf();
          }; 
    name2function.put("df",df);

    StatsTfIdfFunction tf = 
          (TermStats termstats, long nDocs, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return ((Number)localtf).doubleValue();
          }; 
    name2function.put("tf",tf);

    StatsTfIdfFunction ntf = 
          (TermStats termstats, long nDocs, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return ((Number)localtf).doubleValue()/(double)maxTf;
          }; 
    name2function.put("ntf",ntf);

    StatsTfIdfFunction wtf = 
          (TermStats termstats, long nDocs, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return ((Number)localtf).doubleValue()/(double)sumTf;
          }; 
    name2function.put("wtf",wtf);

    StatsTfIdfFunction ltf = 
          (TermStats termstats, long nDocs, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return 1.0 + Math.log(localtf);
          }; 
    name2function.put("ltf",ltf);

    StatsTfIdfFunction tfidf = 
          (TermStats termstats, long nDocs, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return localtf * idf.apply(termstats, nDocs, nTerms, localtf, maxTf, sumTf, options);
          }; 
    name2function.put("tfidf",tfidf);
    
    StatsTfIdfFunction ntfidf = (TermStats termstats, long nDocs, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return ntf.apply(termstats, nDocs, nTerms, localtf, maxTf, sumTf, options) * 
                   idf.apply(termstats, nDocs, nTerms, localtf, maxTf, sumTf, options);
          }; 
    name2function.put("ntfidf",ntfidf);
    
    StatsTfIdfFunction wtfidf = (TermStats termstats, long nDocs, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return wtf.apply(termstats, nDocs, nTerms, localtf, maxTf, sumTf, options) * 
                   idf.apply(termstats, nDocs, nTerms, localtf, maxTf, sumTf, options);
          }; 
    name2function.put("wtfidf",wtfidf);
    
    StatsTfIdfFunction ltfidf = (TermStats termstats, long nDocs, long nTerms, long localtf, long maxTf, long sumTf, Map<String,Object> options) -> {
            return ltf.apply(termstats, nDocs, nTerms, localtf, maxTf, sumTf, options) * 
                   idf.apply(termstats, nDocs, nTerms, localtf, maxTf, sumTf, options);
          }; 
    name2function.put("ltfidf",ltfidf);
  }
  // Convert a list of comma/semicolon/whitespace separated names of functions to a list of actual function lambdas
  public static Map<String,StatsTfIdfFunction> names2functions(String names) {
    Map<String,StatsTfIdfFunction> ret = new HashMap<>();
    String[] namesArray = names.split("[,;\\s]",-1);
    for(String name : namesArray) {
      if(name2function.containsKey(name)) {
        ret.put(name,name2function.get(name));
      } else {
        throw new GateRuntimeException("Statistic not know: "+name);
      }
    }
    return ret;
  }
}
