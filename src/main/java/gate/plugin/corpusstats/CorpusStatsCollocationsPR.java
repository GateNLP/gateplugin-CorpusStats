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
 *  CorpusStatsCollocationsPR: obtain statistics for pairs of words occuring
 *  in the same context or sliding window.
 * 
 */
package gate.plugin.corpusstats;

import gate.*;
import gate.api.AbstractDocumentProcessor;
import gate.creole.metadata.*;
import gate.util.Benchmark;
import gate.util.GateRuntimeException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@CreoleResource(name = "CorpusStatsCollocationsPR",
        helpURL = "https://github.com/johann-petrak/gateplugin-CorpusStats/wiki/CorpusStatsCollocationsPR",
        comment = "Calculate pairwise statistics like PMI ")
public class CorpusStatsCollocationsPR extends AbstractDocumentProcessor {

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

  protected String inputType1 = "";

  @RunTime
  @CreoleParameter(
          comment = "The input annotation type of the first or both terms in a pair",
          defaultValue = "Token")
  public void setInputAnnotationType1(String val) {
    this.inputType1 = val;
  }

  public String getInputAnnotationType1() {
    return inputType1;
  }
  
  protected String inputType2 = "";
  
  @RunTime
  @CreoleParameter(
          comment = "The input annotation type of the first or both terms in a pair",
          defaultValue = "Token")
  public void setInputAnnotationType2(String val) {
    this.inputType2 = val;
  }

  public String getInputAnnotationType2() {
    return inputType2;
  }

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "Annotation which indicates span within which the collocation is counted. If missing, whole document.",
          defaultValue = "")
  public void setSpanAnnotationType(String val) {
    this.spanType = val;
  }

  public String getSpanAnnotationType() {
    return spanType;
  }
  protected String spanType = "";

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The feature from the input annotation to use as term string, if left blank the document text",
          defaultValue = "")
  public void setStringFeature1(String val) {
    this.stringFeature1 = val;
  }

  public String getStringFeature1() {
    return stringFeature1;
  }
  protected String stringFeature1 = "";

  
    @RunTime
  @Optional
  @CreoleParameter(
          comment = "The feature from the input annotation to use as term string, if left blank the document text",
          defaultValue = "")
  public void setStringFeature2(String val) {
    this.stringFeature2 = val;
  }

  public String getStringFeature2() {
    return stringFeature2;
  }
  protected String stringFeature2 = "";

  
  private URL pairStatsFileUrl;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The URL of the TSV file where to store the collocation statistics"
  )
  public void setPairStatsFileUrl(URL u) {
    pairStatsFileUrl = u;
  }

  public URL getPairStatsFileUrl() {
    return pairStatsFileUrl;
  }

  private Integer slidingWindowSize = 0;
  
  @RunTime
  @Optional
  @CreoleParameter(
          comment = "Size of sliding window within a span, if 0 or missing, no sliding window is used."
  )  
  public void setSlidingWindowSize(Integer value) {
    slidingWindowSize = value;
  }
  
  public Integer getSlidingWindowSize() {
    return slidingWindowSize;
  }
  
  private String splitAnnotationType = "";
          
  @RunTime
  @Optional
  @CreoleParameter(
          comment = "Type of annotations which separate pairs and prevent them to get counted. Default: none"
  )
  public void setSplitAnnotationType(String value) {
    splitAnnotationType = value;
  }
  
  public String getSplitAnnotationType() {
    return splitAnnotationType;
  }
  
  
  private boolean caseSensitive = true;
  @RunTime
  @CreoleParameter(comment = "If false, convert to lower case before calculating the stats", 
          defaultValue = "true")
  public void setCaseSensitive(Boolean val) {
    caseSensitive = val;
  }
  public Boolean getCaseSensitive() {
    return caseSensitive;
  }
  
  Locale ccLocale = new Locale("en");
  private String caseConversionLanguage = "en";
  @RunTime
  @CreoleParameter(comment = "Language for mapping to lower case, only relevant if caseSensitive=false",
          defaultValue = "en")
  public void setCaseConversionLanguage(String val) {
    ccLocale = new Locale(val);
    caseConversionLanguage = val;
  }
  public String getCaseConversionLanguage() {
    return caseConversionLanguage;
  }
  
  
  private URL sumsFileUrl;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The URL of the TSV file where to store the global sums and counts"
  )
  public void setSumsFileUrl(URL u) {
    sumsFileUrl = u;
  }

  public URL getSumsFileUrl() {
    return sumsFileUrl;
  }

  private URL dataFileUrl;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The URL of where to store the data in binary compressed format, not used if left empty"
  )
  public void setDataFileUrl(URL u) {
    dataFileUrl = u;
  }

  public URL getDataFileUrl() {
    return dataFileUrl;
  }

  private int minContexts1 = 1;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The minimum contexts number for a term or term1 to get considered",
          defaultValue = "1"
  )
  public void setMinContexts1(Integer value) {
    if (value == null) {
      minContexts1 = 1;
    } else {
      minContexts1 = value;
    }
  }
  

  public Integer getMinContexts1() {
    return minContexts1;
  }
  

  private int minContexts2 = -1;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The minimum contexts number for a term or term1 to get considered, if -1 same as minContexts1",
          defaultValue = "-1"
  )
  public void setMinContexts2(Integer value) {
    if (value == null) {
      minContexts2 = -1;
    } else {
      minContexts2 = value;
    }
  }

  public Integer getMinContexts2() {
    return minContexts2;
  }
  
  
  private boolean reuseExisting = false;
  
  /**
   * Whether or not to load any existing stats data before processing.
   * 
   * If set to true and the binary data file already exists, it is loaded
   * before processing starts and the counts for the run are added to that
   * data. If the binary data file does not exist, the TSV files are loaded,
   * if they exist. 
   * <p>
   * NOTE: If a pipeline is run several times in a row, the stats are 
   * being reset to zero between runs if reuseExisting is false. 
   * 
   * @param val 
   */
  @RunTime
  @Optional
  @CreoleParameter(comment = "Should any existing stats data be loaded and reused?", defaultValue = "false")
  public void setReuseExisting(Boolean val) {
    reuseExisting = val;
  }
  public Boolean getReuseExisting() {
    return reuseExisting;
  }
          
  private URL tfFileUrl;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The URL of where to store the data in binary compressed format, not used if left empty"
  )
  public void setTfFileUrl(URL u) {
    tfFileUrl = u;
  }

  public URL getTfFileUrl() {
    return tfFileUrl;
  }

          
  private double minTf = 1;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The minimum 'tf' from the tf-file necessary for the term1 or both terms to get considered",
          defaultValue = "0.0"
  )
  public void setMinTf(Double value) {
    if (value == null) {
      minTf = 0.0;
    } else {
      minTf = value;
    }
  }

  public Double getMinTf() {
    return minTf;
  }
  
  
  private double laplaceCoefficient = 0.0;
  @RunTime
  @Optional
  @CreoleParameter(
          comment = "Coefficient used for laplacian smoothing",
          defaultValue = "0.0"
  )
  public void setLaplaceCoefficient(Double value) {
    if (value == null) {
      laplaceCoefficient = 0.0;
    } else {
      laplaceCoefficient = value;
    }
  }

  public Double getLaplaceCoefficient() {
    return laplaceCoefficient;
  }
  
  private double dampeningCoefficient = 1.0;
  @RunTime
  @Optional
  @CreoleParameter(
          comment = "Coefficient used for dampening the term2 probability estimates. If 1.0, not used",
          defaultValue = "1.0"
  )
  public void setDampeningCoefficient(Double value) {
    if (value == null) {
      dampeningCoefficient = 0.0;
    } else {
      dampeningCoefficient = value;
    }
  }

  public Double getDampeningCoefficient() {
    return laplaceCoefficient;
  }
  

  ////////////////////// FIELDS
  // these fields will contain references to objects which are shared
  // because all duplicated copies of the PR
  CorpusStatsCollocationsData corpusStats;
  private static final Object syncObject = new Object();

  // fields local to each duplicated PR
  private int mostFrequentWordFreq = 0;
  private int documentWordFreq = 0;

  
  // Helper method: get the string for an annotation, from the right source
  // and correctly case-folded
  private String getStringForAnn(Annotation ann) {
    String str = null;
    if(getStringFeature1()==null || getStringFeature1().isEmpty()) {
      str = gate.Utils.cleanStringFor(document, ann);
    } else {
      str = (String)ann.getFeatures().get(getStringFeature1());
    }
    if(str==null) str="";
    if(!getCaseSensitive()) {
      str = str.toLowerCase(ccLocale);
    }
    return str;
  }
  
  private void incrementHashMapInteger(Map<String,Integer> map, String k, int by) {
    Integer curval = map.get(k);
    if(curval == null) {
      map.put(k, by);
    } else {
      map.put(k, by+curval);
    }
  }
  
  ////////////////////// PROCESSING
  @Override
  protected Document process(Document document) {

    System.out.println("!!! DEBUG: processing document "+document.getName());
    fireStatusChanged("CorpusStatsCollocationsPR: running on " + document.getName() + "...");
    
    AnnotationSet inputAS = null;
    if (inputASName == null
            || inputASName.isEmpty()) {
      inputAS = document.getAnnotations();
    } else {
      inputAS = document.getAnnotations(inputASName);
    }

    AnnotationSet inputAnns = null;
    // TODO: BEGIN move to run init code
    if (inputType1 == null || inputType1.isEmpty()) {
      throw new GateRuntimeException("Input annotation type must not be empty!");
    }
    // if we have a split annotation type defined, we also need to find those    
    HashSet<String> inputTypes = new HashSet<String>();
    inputTypes.add(inputType1);    
    if(getSplitAnnotationType() != null && !getSplitAnnotationType().isEmpty()) {
      inputTypes.add(getSplitAnnotationType());
    }
    // TODO: END
    
    inputAnns = inputAS.get(inputTypes);

    AnnotationSet containingAnns = null;
    if (spanType == null || spanType.isEmpty()) {
      // leave the containingAnns null to indicate we do not use containing annotations
    } else {
      containingAnns = inputAS.get(spanType);
      //System.out.println("DEBUG: got containing annots: "+containingAnns.size()+" type is "+containingAnnotationType);
    }

    // we first do the counting locally then add everythin to the global map.
    // So we need to count terms, pairs and contexts. 
    HashMap<String, Integer> termcounts = new HashMap<String, Integer>();
    HashMap<String, Integer> paircounts = new HashMap<String, Integer>();
    int contexts = 0;

    long startTime = Benchmark.startPoint();

    // method to do this: we create contexts first by finding the offsets 
    // of the context span. This could be the whole document, containing annotation
    // span 
    // To simplify the whole process we always create a list of spans to
    // process first. If we also do sliding windows, then that logic is done 
    // separately for the spans
    
    List<Long> spanFromOffsets = new ArrayList<Long>();
    List<Long> spanToOffsets = new ArrayList<Long>();
    
    if(containingAnns == null || containingAnns.isEmpty()) {
      spanFromOffsets.add(0L);
      spanToOffsets.add(document.getContent().size());
    } else {
      for (Annotation containingAnn : containingAnns) {
        spanFromOffsets.add(containingAnn.getStartNode().getOffset());
        spanToOffsets.add(containingAnn.getEndNode().getOffset());        
      }
    }
    
    // we re-use these sets
    HashSet<String> termsForContext = new HashSet<String>();
    HashSet<String> pairsForContext = new HashSet<String>();
    
    //System.out.println("DEBUG: span from offsets: "+spanFromOffsets);
    //System.out.println("DEBUG: span to offsets: "+spanToOffsets);
    for(int i=0;i<spanFromOffsets.size();i++) {
      long fromOffset = spanFromOffsets.get(i);
      long toOffset = spanToOffsets.get(i);
      // get the terms and maybe split annotations inside that span in document order as a list 
      List<Annotation> inAnns = inputAnns.get(fromOffset, toOffset).inDocumentOrder();
      if(inAnns.size() < 2) continue; // Spans with less than 2 elements are ignored
      // now do the whole processing for each section split up by the split annotations
      // however we only need to check if we actually have a span annotation type
      // The following lists contain the indices of where a span starts to where
      // a span ends in inAnns
      List<Integer> spanStarts = new ArrayList<Integer>();
      List<Integer> spanEnds = new ArrayList<Integer>();
      if(!getSplitAnnotationType().isEmpty()) {
        // get all the span positions in the inAnns list
        boolean inSpan = false;
        int j = 0;
        for(Annotation inAnn : inAnns) {
          if(!inSpan && inAnn.getType().equals(inputType1)) {
            spanStarts.add(j);
            inSpan = true;
          } else if(inSpan && inAnn.getType().equals(splitAnnotationType)) {
            spanEnds.add(j-1);
            inSpan = false;
          }
          j++;
        } // for
        // if after this loop we are still "inSpan" we need to add the latest
        // ann index as the end
        if(inSpan) spanEnds.add(j-1);
        assert(spanEnds.size() == spanStarts.size());
      } else {
        // we just go from the first to the last
        spanStarts.add(0);
        spanEnds.add(inAnns.size()-1);
      }
      //System.out.println("DEBUG: splitspans from offsets: "+spanFromOffsets);
      //System.out.println("DEBUG: splitspans to offsets: "+spanFromOffsets);
      
      
      // Now we have the actual spans described as the index ranges in spanStarts and spanEnds,
      // so iterate over those
      for(int j=0; j<spanStarts.size(); j++) {
        int fromIndex = spanStarts.get(j);
        int toIndex = spanEnds.get(j);
        // again, if the span is < 2, skip it
        int spanLength = toIndex-fromIndex+1;
        if(spanLength<2) continue;
        // OK, we have a span to process, if we have a sliding window size
        // defined then slide the window if possible, otherwise just 
        // process the span as one window. To use the same code, we just define
        // the "working sliding window" size to be the length of the span length
        int workingSlWSize = spanLength;
        // TODO: need to check that the sliding window size is 0 or some value
        // >= 2 in the runtime init code, so here we rely it is either 0 or valid
        if(getSlidingWindowSize()>0) {
          workingSlWSize = getSlidingWindowSize();
        }
        // now iterate the sliding window as often inside the span as possible
        int nrWindows = spanLength-workingSlWSize;
        //System.out.println("DEBUG: nrWindows: "+nrWindows);
        for(int k=0; k<=nrWindows; k++) {
          // count the context
          contexts += 1;
          // the annotations from 
          // fromIndex+k to toIndex+k are getting processed to 
          // count any occuring terms or pairs. Note that if a term
          // or pair occurs more than once, we only count once! So we 
          // create a set of all terms and a set of all pairs
          // TODO: not sure what the fastest way to do this could be!
          termsForContext.clear();
          pairsForContext.clear();
          for(int m=0; m<workingSlWSize; m++) {
            Annotation termAnn = inAnns.get(fromIndex+k+m);
            String term = getStringForAnn(termAnn);
            System.out.println("DEBUG: context from="+fromOffset+" to="+toOffset+" fromindex="+fromIndex+" k="+k+" m="+m+" toindex="+toIndex+" workingSize="+workingSlWSize+" term="+term);
            termsForContext.add(term);
            System.out.print("DEBUG: Pairs=");
            for(int n=m+1; n<workingSlWSize; n++) {
              Annotation termAnn2 = inAnns.get(fromIndex+k+n);
              String term2 = getStringForAnn(termAnn2);
              if(term.compareTo(term2) < 0) {
                System.out.print(term+"|"+term2+" ");
                pairsForContext.add(term+"\t"+term2);
              } else if(term.compareTo(term2) > 0) {
                System.out.print(term2+"|"+term+" ");
                pairsForContext.add(term2+"\t"+term);
              } else {
                // if they are equal we do not add a pair
                // We could but then we would need to find out how to calculate
                // the counts for chi2 properly since inclusion/exclusion only
                // works for different terms in the pair
              }              
            } // inner for for term2
            System.err.println();
          } // outer for loop for term1
          // Now we have got the unique terms and pairs occuring in the context
          // count them
          System.out.println("DEBUG: terms for context: "+termsForContext);
          System.out.println("DEBUG: pairs for context: "+pairsForContext);
          
          for(String t4c : termsForContext) {
            incrementHashMapInteger(termcounts,t4c,1);
          }
          for(String p4c : pairsForContext) {
            incrementHashMapInteger(paircounts,p4c,1);
          }
          
        } // iterate over the actual contexts inside a span
        
      } // iterate over the spans

      // Now add all the collected values to the global counter variables
      
      
    }

      System.out.println("DEBUG: termcounts for document "+document.getName()+": "+termcounts);
      System.out.println("DEBUG: paircounts for document "+document.getName()+": "+paircounts);
      System.out.println("DEBUG: contexts for document "+document.getName()+": "+contexts);
      
          for(String term : termcounts.keySet()) {
            System.err.println("DEBUG: add term="+term+" count="+termcounts.get(term));
            corpusStats.countsTerms.computeIfAbsent(term, (var -> new LongAdder())).add(termcounts.get(term));
          }
          for(String pair : paircounts.keySet()) {
            System.err.println("DEBUG: add pair="+pair+" count="+paircounts.get(pair));
            corpusStats.countsPairs.computeIfAbsent(pair, (var -> new LongAdder())).add(paircounts.get(pair));
          }
          corpusStats.totalContexts.add(contexts);

    corpusStats.nDocs.add(1);
    benchmarkCheckpoint(startTime, "__CollocationsProcess");

    fireProcessFinished();
    fireStatusChanged("CorpusStatsCollocations: processing complete!");
    return document;
  }


  @Override
  protected void beforeFirstDocument(Controller ctrl) {
    // !!! TODO: more checking of parameters and make sure they return a proper
    // canonical default value!
    
    
    // if reference null, create the global map
    synchronized (syncObject) {
      corpusStats = (CorpusStatsCollocationsData)sharedData.get("corpusStats");
      if (corpusStats != null) {        
        System.err.println("INFO: corpusStats already created, we are duplicate " + duplicateId + " of PR " + this.getName());
      } else {
        System.err.println("INFO: creating corpusStats in duplicate " + duplicateId + " of PR " + this.getName());
        corpusStats = new CorpusStatsCollocationsData();
        corpusStats.nDocs = new LongAdder();
        corpusStats.isCaseSensitive = getCaseSensitive();
        corpusStats.ccLocale = new Locale(getCaseConversionLanguage());
        sharedData.put("corpusStats", corpusStats);
        System.err.println("INFO: corpusStats created and initialized in duplicate " + duplicateId + " of PR " + this.getName());
      }
      // Now at this point we have a CorpusStats instance for sure. However, 
      // if reuseStats is true, we may still have to load it. 
      // NOTE: if the PR is run several times in a row, then when we arrive
      // here, the corpusstats object should always be initialized to empty,
      // since we always remove it after processing has finished. 
      if(getReuseExisting()) {
        corpusStats.load(dataFileUrl, sumsFileUrl, pairStatsFileUrl);
      }
    }
  }

  @Override
  protected void afterLastDocument(Controller ctrl, Throwable t) {
    synchronized (syncObject) {
      long startTime = Benchmark.startPoint();
      // TODO: we had this here, but why do we need it?
      corpusStats = (CorpusStatsCollocationsData) sharedData.get("corpusStats");
      if (corpusStats != null) {
        corpusStats.save(dataFileUrl, sumsFileUrl, pairStatsFileUrl, getMinContexts1());
        // After each run, we clean up, so that the code before each run can 
        // recreate or reload the data as if it was the first time
        //!!!corpusStats.map = null;
        corpusStats = null;
        sharedData.remove("corpusStats");
      } // if corpusstats is not null
      benchmarkCheckpoint(startTime, "__TfIdfSave");
    }
  }

  @Override
  protected void finishedNoDocument(Controller ctrl, Throwable t) {
    // After each run, we clean up, so that the code before each run can 
    // recreate or reload the data as if it was the first time
    synchronized (syncObject) {
    //!!!corpusStats.map = null;
    corpusStats = null;
    sharedData.remove("corpusStats");
    }
  }


} // class JdbcLookup
