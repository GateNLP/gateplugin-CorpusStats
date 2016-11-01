/* 
 * Copyright (C) 2015-2016 The University of Sheffield.
 *
 * This file is part of gateplugin-CorpusStats
 * (see https://github.com/johann-petrak/gateplugin-CorpusStats)
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


/**
 * 
 *  TfIdf: Simple PR to calculate count DF and TF and calculate TFIDF scores,
 *  with support for parallel processing.
 */
package gate.plugin.corpusstats;


import gate.*;
import gate.api.AbstractDocumentProcessor;
import gate.creole.metadata.*;
import gate.util.Benchmark;
import gate.util.GateRuntimeException;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

@CreoleResource(name = "TfDff",
        helpURL = "https://github.com/johann-petrak/gateplugin-CorpusStats/wiki/TfDf",
        comment = "Calculate tf, df, and additional statistics over a corpus")
public class TfDf  extends AbstractDocumentProcessor {

  private static final long serialVersionUID = 1L;
  
  
  
  protected String inputASName = "";
  @RunTime
  @Optional
  @CreoleParameter(
          comment = "Input annotation set",
          defaultValue = "")
  public void setInputAnnotationSet(String ias) {
    inputASName = ias;
  }

  public String getInputAnnotationSet() {
    return inputASName;
  }
  
  
  protected String inputType = "";
  @RunTime
  @CreoleParameter(
          comment = "The input annotation type",
          defaultValue = "Token")
  public void setInputAnnotationType(String val) {
    this.inputType = val;
  }

  public String getInputAnnotationType() {
    return inputType;
  }

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The optional containing annotation set type",
          defaultValue = "")
  public void setContainingAnnotationType(String val) {
    this.containingType = val;
  }

  public String getContainingAnnotationType() {
    return containingType;
  }
  protected String containingType = "";

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The feature from the input annotation to use as term string, if left blank the document text",
          defaultValue = "")
  public void setKeyFeature(String val) {
    this.keyFeature = val;
  }

  public String getKeyFeature() {
    return keyFeature;
  }
  protected String keyFeature = "";





  
  private URL tfDfFileUrl;
  @RunTime
  @CreoleParameter( 
          comment = "The URL of the TSV file where to store the per-term counts"
  )
  public void setTfDfFileUrl(URL u) {
    tfDfFileUrl = u;
  }
  public URL getTfDfFileUrl() { return tfDfFileUrl; }
  
  private URL sumsFileUrl;
  @RunTime
  @CreoleParameter( 
          comment = "The URL of the TSV file where to store the global sums and counts"
  )
  public void setSumsFileUrl(URL u) {
    sumsFileUrl = u;
  }
  public URL getSumsFileUrl() { return sumsFileUrl; }
  

  ////////////////////// FIELDS
  
  // these fields will contain references to objects which are shared
  // because all duplicated copies of the PR
  
  private ConcurrentHashMap<String,TermStats> map;
  private LongAdder nDocs = null;
  private LongAdder nWords = null;
  
  private static final Object syncObject = new Object();
  
  // fields local to each duplicated PR
  private int mostFrequentWordFreq = 0;
  private int documentWordFreq = 0;
  
  ////////////////////// PROCESSING
  
  @Override
  protected Document process(Document document) {
    
    AnnotationSet inputAS = null;
    if (inputASName == null
            || inputASName.isEmpty()) {
      inputAS = document.getAnnotations();
    } else {
      inputAS = document.getAnnotations(inputASName);
    }

    AnnotationSet inputAnns = null;
    if (inputType == null || inputType.isEmpty()) {
      throw new GateRuntimeException("Input annotation type must not be empty!");
    }
    inputAnns = inputAS.get(inputType);

    AnnotationSet containingAnns = null;
    if (containingType == null || containingType.isEmpty()) {
      // leave the containingAnns null to indicate we do not use containing annotations
    } else {
      containingAnns = inputAS.get(containingType);
      //System.out.println("DEBUG: got containing annots: "+containingAnns.size()+" type is "+containingAnnotationType);
    }

    fireStatusChanged("TfIdf: running on " + document.getName() + "...");

    // we first count the terms in this document in our own map, then 
    // add the final counts to the global map.
    HashMap<String,Integer> wordcounts = new HashMap<String,Integer>();
    
    long startTime = Benchmark.startPoint();
    
    mostFrequentWordFreq = 0;
    documentWordFreq = 0;
    
    if (containingAnns == null) {
      // go through all input annotations 
      for (Annotation ann : inputAnns) {
        doIt(document, ann, wordcounts);
        if(isInterrupted()) {
          throw new GateRuntimeException("TfIdf has been interrupted");
        }
      }
    } else {
      // go through the input annotations contained in the containing annotations
      for (Annotation containingAnn : containingAnns) {
        AnnotationSet containedAnns = gate.Utils.getContainedAnnotations(inputAnns, containingAnn);
        for (Annotation ann : containedAnns) {
          doIt(document, ann, wordcounts);
          if(isInterrupted()) { 
            throw new GateRuntimeException("TfIdf has been interrupted");
          }
        }
      }
    }
    
    // now add the locally counted term frequencies to the global map
    // also add the weighted/normalized term frequencies
    
    for(String key : wordcounts.keySet()) {
      map.computeIfAbsent(key,(k -> new TermStats())).incrementTfBy(wordcounts.get(key));
      map.computeIfAbsent(key,(k -> new TermStats())).incrementWTfBy(((double)wordcounts.get(key))/((double)documentWordFreq));
      map.computeIfAbsent(key,(k -> new TermStats())).incrementNTfBy(((double)wordcounts.get(key))/((double)mostFrequentWordFreq));
    }
    

    nDocs.add(1);
    benchmarkCheckpoint(startTime, "__TfIdfProcess");
    
    
    fireProcessFinished();
    fireStatusChanged("TfIdf: processing complete!");
    return document;
  }
  
  // NOTE: this method updates the global fields documentWordFreq
  // and 
  private void doIt(Document doc, Annotation ann, Map<String,Integer> wordmap) {
    String key;
    FeatureMap fm = ann.getFeatures();
    if (getKeyFeature() == null || getKeyFeature().isEmpty()) {
      key = Utils.cleanStringFor(document, ann);
    } else {
      key = (String) fm.get(getKeyFeature());
    }
    // we actually have a word to count
    if (key != null) {
      // count total number of words found
      nWords.add(1);
      documentWordFreq += 1;
      // check if we have seen this word in this document already:
      // if no, increase document frequency and remember it 
      if(!wordmap.containsKey(key)) {
        wordmap.put(key,1);
        if(mostFrequentWordFreq == 0) mostFrequentWordFreq = 1;
        // lets also add to the document frequency right here ....
        map.computeIfAbsent(key,(k -> new TermStats())).incrementDf();
      } else {
        int thisWf = wordmap.get(key)+1;
        wordmap.put(key, thisWf);  // increase the count in our own map
        if(thisWf > mostFrequentWordFreq) {
          mostFrequentWordFreq = thisWf;
        }
      }
    }
  }
  

  @Override
  protected void beforeFirstDocument(Controller ctrl) {
    // if reference null, create the global map
    synchronized (syncObject) {
      map = (ConcurrentHashMap<String,TermStats>)sharedData.get("map");
      if (map != null) {
        System.err.println("INFO: shared maps already created in duplicate "+duplicateId+" of PR "+this.getName());
        nDocs = (LongAdder)sharedData.get("nDocs");
        nWords = (LongAdder)sharedData.get("nWords");
        //System.err.println("INFO: copied existing maptf/mapdf/ndocs/nwords: "+mapTf+"/"+mapDf+"/"+nDocs+"/"+nWords);
      } else {
        System.err.println("INFO: creating shared maps in duplicate "+duplicateId+" of PR "+this.getName());
        map = new ConcurrentHashMap<String,TermStats>(1024*1024,32,32);
        sharedData.put("map", map);
        nDocs = new LongAdder();
        sharedData.put("nDocs", nDocs);
        nWords = new LongAdder();
        sharedData.put("nWords", nWords);
        System.err.println("INFO: shared maps created in duplicate "+duplicateId+" of PR "+this.getName());
      }
    }
  }
    

  @Override
  protected void afterLastDocument(Controller ctrl, Throwable t) {
    synchronized (syncObject) {
      long startTime = Benchmark.startPoint();
      map = (ConcurrentHashMap<String,TermStats>)sharedData.get("map");
      if (map != null) {
        
        long ndocs = nDocs.sum();
        long nterms = map.size();
        long nwords = nWords.sum();        
        
        File file = gate.util.Files.fileFromURL(sumsFileUrl);
        System.err.println("Storing total counts to file " + file);
        try (
                FileOutputStream fos = new FileOutputStream(file);
                PrintWriter pw = new PrintWriter(fos)) {
          // output the header
          // nwords=total number of words counted
          // nterms=total number of terms / different words
          // ndocs=total number of documents
          pw.println("nwords\tnterms\tndocs");
          pw.println(nwords+"\t"+nterms+"\t"+ndocs);
        } catch (Exception ex) {
          throw new GateRuntimeException("Could not save tfidf file", ex);
        }

        
        file = gate.util.Files.fileFromURL(tfDfFileUrl);
        System.err.println("Storing counts to file " + file);
        try (
                FileOutputStream fos = new FileOutputStream(file);
                PrintWriter pw = new PrintWriter(fos)) {
          // output the header
          // tf=term frequency
          // df=document frequency
          // ntf=tf normalized by each maximum tf per document
          // wtf=tf weighted by number of words per document
          pw.println("term\ttf\tdf\tntf\twtf\tidf\ttfidf\tntfidf\twtfidf");
          for(String key : map.keySet()) {
            long tf = map.get(key).getTf();
            long df = map.get(key).getDf();
            double ntf = map.get(key).getNTf();
            double wtf = map.get(key).getWTf();
            double idf = Math.log(((double)ndocs)/df);
            double tfidf = tf * idf;
            double ntfidf = ntf * idf;
            double wtfidf = wtf * idf;
            pw.print(key);
            pw.print("\t");
            pw.print(tf);
            pw.print("\t");
            pw.print(df);
            pw.print("\t");
            pw.print(ntf);
            pw.print("\t");
            pw.print(wtf);
            pw.print("\t");
            pw.print(idf);
            pw.print("\t");
            pw.print(tfidf);
            pw.print("\t");
            pw.print(ntfidf);
            pw.print("\t");
            pw.println(wtfidf);
          }
        } catch (Exception ex) {
          throw new GateRuntimeException("Could not save tfidf file", ex);
        }
        
        map = null;
        sharedData.remove("map");
      } // if getMapTf() != null
      benchmarkCheckpoint(startTime, "__TfIdfSave");
    }
  }

  @Override
  protected void finishedNoDocument(Controller ctrl, Throwable t) {
  }
  

  private static class TermStats {
    private final LongAdder tf = new LongAdder();
    private final DoubleAdder wtf = new DoubleAdder();  // weighted tf: by document length
    private final DoubleAdder ntf = new DoubleAdder();  // normalized tf: by maximum tf in document
    private final LongAdder df = new LongAdder();
    public void incrementTf() { tf.add(1); }
    public void incrementDf() { df.add(1); }
    public void incrementTfBy(int by) { tf.add(by); }
    public void incrementWTfBy(double by) { wtf.add(by); }
    public void incrementNTfBy(double by) { ntf.add(by); }
    public long getTf() { return tf.sum(); }
    public double getWTf() { return wtf.sum(); }
    public double getNTf() { return ntf.sum(); }
    public long getDf() { return df.sum(); }
  }
  
  
} // class JdbcLookup
