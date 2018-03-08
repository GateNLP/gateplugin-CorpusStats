/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.corpusstats;

import gate.api.UrlUtils;
import gate.util.Files;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;

/**
 * Class representing pairwise collocation statistics.
 * Also has methods for saving and loading data using either a binary 
 * file or two TSV files.
 * @author Johann Petrak
 */
public class CorpusStatsCollocationsData implements Serializable {
  // The way we count is this: 
  // * for each context, totalContexts is incremented
  // * each time a term is encountered (one or more times) in a context, the countsTerms(t) is incremented
  //   This is used to calculate p(t) as countTerms(t) / totalContexts
  // * each time a pair a,b is encountered (one or more times) in a context, the countsPairs(a,b) is incremented
  //   This is used to calculate p(a,b) as countsPairs(a,b) / totalContexts
  //   Also to calculate p(¬a,b) as (countsTerms(b)-countsPairs(a,b))/totalContexts
  //   Also to calculate p(¬a,¬b) as totalContexts - countsTerms(a) - countsTerms(b) + countsTerms(a,b)
  // So in general the p means probability to encounter a context with that pattern in it. 
  
  // TODO: need to document the default value for pairs which have not been found and what 
  // the value of each metric should be for this. Maybe add this to the sum file if it depends on 
  // the number of pairs, contexts etc.!!!!
  
  public ConcurrentHashMap<String, LongAdder> countsTerms =
          new ConcurrentHashMap<String, LongAdder>(); // used to estimate p(t)
  public ConcurrentHashMap<String, LongAdder> countsPairs =
          new ConcurrentHashMap<String, LongAdder>(); // used to estimate p(t1,t2)
  // total number of single term occurrences, used to estimate p(t) 
  public LongAdder totalContexts = new LongAdder();
  
  // We do not need this but still good to know the number of documents
  public LongAdder nDocs = new LongAdder();
  
  public boolean isCaseSensitive = true;
  public Locale ccLocale = new Locale("en");
  
  public void load(URL dataUrl, URL sumsTsvUrl, URL statsTsvUrl) {
    boolean haveLoaded = false;
    if(dataUrl != null && !dataUrl.toExternalForm().isEmpty()) {
      // if it is a file URL, convert and check if it exists, if it is not
      // a file URL, check if we can open it. 
      boolean tryOpen  = false;
      if(UrlUtils.isFile(dataUrl)) {
        tryOpen = Files.fileFromURL(dataUrl).exists();
      } else {
        tryOpen = UrlUtils.exists(dataUrl);
      }
      if(tryOpen) {
        try (   InputStream is = dataUrl.openStream();
                GZIPInputStream gis = new GZIPInputStream(is);
                ObjectInputStream ois = new ObjectInputStream(gis)
            ) {
          Object obj = ois.readObject();
          if(obj instanceof CorpusStatsCollocationsData) {
            CorpusStatsCollocationsData other = (CorpusStatsCollocationsData)obj;
            countsTerms = other.countsTerms;
            countsPairs = other.countsPairs;
            // NOTE: if the loaded stats file has a different case sensitivity setting, 
            // we throw an error, this does not make sense to have!
            if(isCaseSensitive != other.isCaseSensitive) {
              throw new GateRuntimeException("Data file loaded has a different caseSensitivy setting");
            }
            if(!ccLocale.equals(other.ccLocale)) {
              throw new GateRuntimeException("Data file loaded has a different case conversion language");
            }
            other = null;
            return;
          }
        } catch(Exception ex) {
          throw new GateRuntimeException("Error when trying to restore data from "+dataUrl,ex);
        }
        
      }
    }
    // If we arrive here we did not load a data file, so try loading from
    // the TSV files.
    // We do not know the case sensitivity setting of the files we load,
    // so we just add to the map whatever we find. It is the responsibility
    // of the user to do something that makes sense here!
    
    // !!!!TODO !!!! TODO !!!! TODO
  }
  
  double LOG2 = Math.log(2.0);
  private double _log2(long value) {
    if(value == 0L) return 0.0;
    return Math.log((double)value)/LOG2;
  }
  private double _log2(double value) {
    if(value == 0.0) return 0.0;
    return Math.log(value)/LOG2;
  }
  
  
  public void save(URL dataUrl, URL sumTsvUrl, URL statsTsvUrl, int minTf) {

        if (sumTsvUrl != null && !sumTsvUrl.toExternalForm().isEmpty()) {
          File file = gate.util.Files.fileFromURL(sumTsvUrl);
          System.err.println("Storing total counts to file " + file);
          try (
                  FileOutputStream fos = new FileOutputStream(file);
                  PrintWriter pw = new PrintWriter(fos)) {
            // output the header
            // n_contexts = number of contexts (documents, spans, sliding windows)
            // n_terms = number of different terms
            // n_pairs = number of different pairs encountered
            // TODO: add scores for PMI, npmi, chi2_p etc. for pairs not found in the corpus!
            pw.println("ncontexts\tnterms\tnpairs");
            pw.println(totalContexts + "\t" + countsTerms.size() + "\t" + countsPairs.size());
            System.err.println("Number of contexts: " +totalContexts);
            System.err.println("Number of different terms: " + countsTerms.size());
            System.err.println("Number of different pairs: " + countsPairs.size());
            System.err.println("Docs:  " + nDocs);
          } catch (Exception ex) {
            throw new GateRuntimeException("Could not save tfidf file", ex);
          }
        }

        // NOTE: currently we only export the pair statistics but we may
        // want to add a code snipped for also exporting the term counts.
        if (statsTsvUrl != null && !statsTsvUrl.toExternalForm().isEmpty()) {
          File file = gate.util.Files.fileFromURL(statsTsvUrl);
          System.err.println("Storing counts to file " + file);
          try (
                  FileOutputStream fos = new FileOutputStream(file);
                  PrintWriter pw = new PrintWriter(fos)) {
            // term1 - first term of pair (lexically smaller) 
            // term2 - second term of pair (lexically larger or equal) 
            // freq_pair = number of contexts the pair occurs in
            // freq_term1 = 
            // frequ_term2
            // prob = estimated probability to find the pair in context
            // pmi = log2(p(x,y) / p(x)p(y))
            // npmi 
            // chi2_p = p-value of the chi-squared statistic
            // student_t_p - p-value of the student t value
            pw.println("term1\tterm2\tfreqp\tfreqt0\tfreqt1\tprob\tpmi\tnpmi\tchi2\tchi2_p\tstudent_t\tstudent_t_p");
            long N = totalContexts.sum();
            double Nfloat = (double)N;
            
            TDistribution tdist = null;
            if(N>1) 
              tdist = new TDistribution(Nfloat-1.0);
            else {
              System.err.println("WARNING: only one context, cannot calculate student-t p-value, setting to 0");
            }
            ChiSquaredDistribution chdist = new ChiSquaredDistribution(1);
            
            int lines = 0;
            for (String key : countsPairs.keySet()) {
              String[] terms = key.split("\\t");
              // System.out.println("DEBUG: retrieve counts for pair "+key+"t0="+terms[0]+" t1="+terms[1]);              
              long pairCount = countsPairs.get(key).sum();
              long term0Count = countsTerms.get(terms[0]).sum();
              long term1Count = countsTerms.get(terms[1]).sum();
              // probability of the pair a,b is the number of contexts it appears in
              // divided by the total number of contexts
              double p_a_b = (float)pairCount / Nfloat;
              
              // TODO: skip this if we do not have a pair where the minimum
              // frequency of both terms is satisfied!
              
              // 1) calculate ordinary PMI
              double pmi = _log2(pairCount) + _log2(N) - _log2(term0Count) - _log2(term1Count);
              
              // 2) calculate normalized PMI
              // if pairCount is 1 then log of paircount is 0 so we would get -Inf or +inf
              // here. Instead, we set this to 0.0 
              double npmi = 0.0;
              if(pairCount == 1L) {
                npmi = -1.0;
              } else {
                npmi = pmi / -_log2(p_a_b);
              }
              
              // 3) person's chi-squared 
              
              // prob of b occuring in a context that does not have a is number of 
              // times b occurs minus the times b occurs with a, then divided by ...
              double p_na_b = (term1Count - pairCount) / Nfloat;
              // mirror image for a where b does not occur
              double p_a_nb = (term0Count - pairCount) / Nfloat;
              // neither a nor b: total contexts minus where a occurs, minus where
              // be occurs plus the ones where a and b occur together 
              double p_na_nb = (Nfloat - term0Count - term1Count + pairCount) / Nfloat;
              System.err.println("DEBUG: pair="+key+", N="+Nfloat+", t0c="+term0Count+", t1c="+term1Count+", pairc="+pairCount);
              System.err.println("DEBUG: p_a_b,p_na_b,p_a_nb,p_na_nb="+p_a_b+","+p_na_b+","+p_a_nb+","+p_na_nb);
              // Expected values for all 4 combinations, calculated from the margins
              //double e_a_b = (p_a_b + p_a_nb) * (p_a_b + p_na_b);
              //double e_na_b = (p_na_b + p_na_nb) * (p_na_b + p_a_b);
              //double e_a_nb = (p_a_nb + p_a_b) * (p_a_nb + p_na_nb);
              //double e_na_nb = (p_na_nb + p_na_b) * (p_na_nb + p_a_nb);
              
              double tmp = (p_a_b * p_na_nb - p_a_nb * p_na_b);
              tmp = Nfloat * tmp * tmp;
              double chi2 = tmp / 
                      ((p_a_b + p_a_nb) * (p_a_b + p_na_b) * (p_a_nb + p_na_nb) * (p_na_b + p_na_nb));
              
              double chi2_p =  chdist.cumulativeProbability(chi2);
              
              // 4) calculate student t p-value
              
              double pab = (term0Count/Nfloat) * (term1Count/Nfloat);  // expected p if indep
              double samplevariance = p_a_b * (1.0 - p_a_b);
              double student_t = (p_a_b - pab)/ Math.sqrt(samplevariance/Nfloat);
              
              double student_t_p = tdist.cumulativeProbability(student_t);
              
              lines++;
            // term1 - first term of pair (lexically smaller) 
            // term2 - second term of pair (lexically larger or equal) 
            // freq_pair = number of contexts the pair occurs in
            // freq_term1 = 
            // frequ_term2
            // prob = estimated probability to find the pair in context
            // pmi = log2(p(x,y) / p(x)p(y))
            // npmi 
            // chi2_p = p-value of the chi-squared statistic
            // student_t_p - p-value of the student t value
              pw.print(key); // contains the tab to separate the two terms
              pw.print("\t");
              pw.print(pairCount);
              pw.print("\t");
              pw.print(term0Count);
              pw.print("\t");
              pw.print(term1Count);
              pw.print("\t");
              pw.print(p_a_b);
              pw.print("\t");
              pw.print(pmi);
              pw.print("\t");
              pw.print(npmi);
              pw.print("\t");
              pw.print(chi2);
              pw.print("\t");
              pw.print(chi2_p);
              pw.print("\t");
              pw.print(student_t);
              pw.print("\t");
              pw.print(student_t_p);
              pw.println();
            }
            System.err.println("Term stats rows written to file, lines: " + lines);
          } catch (Exception ex) {
            throw new GateRuntimeException("Could not save collocation file", ex);
          }
        }

        if (dataUrl != null && !dataUrl.toExternalForm().isEmpty()) {
          File file = gate.util.Files.fileFromURL(dataUrl);
          System.err.println("Storing data to file " + file);
          try (
                  FileOutputStream fos = new FileOutputStream(file);
                  GZIPOutputStream gos = new GZIPOutputStream(fos);
                  ObjectOutputStream oos = new ObjectOutputStream(gos)) {
            oos.writeObject(this);
          } catch (IOException ex) {
            throw new GateRuntimeException("Could not save data to "+file,ex);
          }
        }
    
  }
}
