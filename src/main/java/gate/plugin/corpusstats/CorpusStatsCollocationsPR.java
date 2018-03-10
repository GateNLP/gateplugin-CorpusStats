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
    this.spanAnnotationType = val;
  }

  public String getSpanAnnotationType() {
    return spanAnnotationType;
  }
  protected String spanAnnotationType = "";

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

  private int minContextsT1 = 1;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The minimum contexts number for a term or term1 to get considered",
          defaultValue = "1"
  )
  public void setMinContextsT1(Integer value) {
    if (value == null) {
      minContextsT1 = 1;
    } else {
      minContextsT1 = value;
    }
  }
  

  public Integer getMinContextsT1() {
    return minContextsT1;
  }
  

  private int minContextsT2 = -1;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The minimum contexts number for a term or term1 to get considered, if -1 same as minContexts1",
          defaultValue = "-1"
  )
  public void setMinContextsT2(Integer value) {
    if (value == null) {
      minContextsT2 = -1;
    } else {
      minContextsT2 = value;
    }
  }

  public Integer getMinContextsT2() {
    return minContextsT2;
  }
  
  private int minContextsP = 1;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The minimum contexts number for a pair to occur in",
          defaultValue = "-1"
  )
  public void setMinContextsP(Integer value) {
    if (value == null) {
      minContextsP = 1;
    } else {
      minContextsP = value;
    }
  }

  public Integer getMinContextsP() {
    return minContextsP;
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

    //System.out.println("!!! DEBUG: processing document "+document.getName());
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
    inputTypes.add(inputType2);    
    inputAnns = inputAS.get(inputTypes);

    AnnotationSet splitAnns = null;
    if(getSplitAnnotationType() != null && !getSplitAnnotationType().isEmpty()) {
      splitAnns = inputAS.get(getSplitAnnotationType());
    }
    
    boolean haveTwoTypes = !inputType1.equals(inputType2);
    

    AnnotationSet containingAnns = null;
    if (spanAnnotationType == null || spanAnnotationType.isEmpty()) {
      // leave the containingAnns null to indicate we do not use containing annotations
    } else {
      containingAnns = inputAS.get(spanAnnotationType);
      //System.out.println("DEBUG: got containing annots: "+containingAnns.size()+" type is "+containingAnnotationType);
    }

    // we first do the counting locally then add everythin to the global map.
    // So we need to count terms, pairs and contexts. 
    // term2counts is only used if we have different types
    HashMap<String, Integer> term1counts = new HashMap<String, Integer>();
    HashMap<String, Integer> term2counts = new HashMap<String, Integer>();
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
    System.out.println("DEBUG: span from offsets: "+spanFromOffsets);
    System.out.println("DEBUG: span to offsets: "+spanToOffsets);

    // if a split annotation type has been specified, go through all the spans
    // and get the contained split annotations and re-create the from and to
    // offsets again
    if (splitAnns!=null) {
      List<Long> oldSpanFromOffsets = spanFromOffsets;
      List<Long> oldSpanToOffsets = spanToOffsets;
      spanFromOffsets = new ArrayList<>();
      spanToOffsets = new ArrayList<>();
      // go throw each span
      for(int i=0;i<oldSpanFromOffsets.size();i++) {
        long oldFrom = oldSpanFromOffsets.get(i);
        long oldTo = oldSpanToOffsets.get(i);
        if(oldFrom==oldTo) continue;
        AnnotationSet splits = splitAnns.get(oldFrom,oldTo);        
        spanFromOffsets.add(oldFrom);
        if(splits.size()>0) {
          List<Annotation> sanns = splits.inDocumentOrder();
          // add all the spans from the start of the old one to each of the 
          // contained splits to the end of the old one
          for(Annotation sann : sanns) {
            spanToOffsets.add(sann.getStartNode().getOffset());
            spanFromOffsets.add(sann.getEndNode().getOffset());
          }
        }
        spanToOffsets.add(oldTo);
      }
      System.out.println("DEBUG: after splits span from offsets: "+spanFromOffsets);
      System.out.println("DEBUG: sfter splits span to offsets: "+spanToOffsets);
    }

    
    // we re-use these sets for every context to figure out which unique
    // pairs and terms we find in the context.
    HashSet<String> term1sForContext = new HashSet<String>();
    HashSet<String> term2sForContext = new HashSet<String>();
    HashSet<String> pairsForContext = new HashSet<String>();

    for(int i=0;i<spanFromOffsets.size();i++) {
      long fromOffset = spanFromOffsets.get(i);
      long toOffset = spanToOffsets.get(i);
      // get the terms inside that span in document order as a list 
      List<Annotation> inAnns = inputAnns.get(fromOffset, toOffset).inDocumentOrder();
      if(inAnns.size() < 2) continue; // Spans with less than 2 elements are ignored
      
      // we have a span to process. We do this by extracting the strings 
      // and processing by string index now, instead of using offsets any more
      List<String> strings = new ArrayList<String>();           
      List<Integer> anntypes = new ArrayList<Integer>();

      for (Annotation ann : inAnns) {
        strings.add(getStringForAnn(ann));
        if(ann.getType().equals(inputType1)) {
          anntypes.add(1);
        } else {
          anntypes.add(2);
        }
      }
      System.out.println("DEBUG: strings for span: "+strings);
      System.out.println("DEBUG: anntypes for span: "+anntypes);

      int spanLength = strings.size();
      // Now that we have an array of strings and another of types, we can
      // go through each window and then for each window do the pairs counting
      int workingSlWSize = spanLength;
      if(getSlidingWindowSize()>0 && getSlidingWindowSize() < spanLength) {
        workingSlWSize = getSlidingWindowSize();
      }
      // now iterate the sliding window as often inside the span as possible
      int maxWindowIndex = spanLength-workingSlWSize;
      //System.out.println("DEBUG: nrWindows: "+nrWindows);
      // Iterate over the contexts, once we are here, each context gets 
      // counted, no matter if we actually find a pair inside (if two different types)
      for(int k=0; k<=maxWindowIndex; k++) {
        // count the context
        term1sForContext.clear();
        term1sForContext.clear();
        pairsForContext.clear();
        contexts += 1;
        if(haveTwoTypes) {
          // If we have two term types, we need to iterate over type one in one loop
          // and type 2 in another and find and count separately. Also, in that 
          // case the first part of a pair is always from type 1 and the second from
          // type 2. 
          // We iterate over the whole window and look at each term and depending
          // on type add it to the set. If it is type 1 we also iterate over the 
          // same window fully to find all strings of type 2 and build a pair.
          // We could avoid looping over all n elements for each element of type 1
          // but that would require constructing another index array first which 
          // may end up being slower
          for (int m = 0; m < workingSlWSize; m++) {
            String term = strings.get(k+m);
            int thetype = anntypes.get(k+m);
            if(thetype==1) { 
              term1sForContext.add(term);              
              for (int n = 0; n < workingSlWSize; n++) {
                if(thetype!=anntypes.get(k+n)) {
                  pairsForContext.add(term+"\t"+strings.get(k+n));
                }
              }
            } else { 
              term2sForContext.add(term);            
            }
          } // outer: m
          System.out.println("DEBUG: term1s for context: "+term1sForContext);
          System.out.println("DEBUG: term2s for context: "+term2sForContext);
          System.out.println("DEBUG: pairs for context: "+pairsForContext);
          for(String t4c : term1sForContext) {
            incrementHashMapInteger(term1counts,t4c,1);
          }
          for(String t4c : term2sForContext) {
            incrementHashMapInteger(term2counts,t4c,1);
          }
          for(String p4c : pairsForContext) {
            incrementHashMapInteger(paircounts,p4c,1);
          }
          
        } else {
          // If we just have one term type: got through each string, then
          // find pairs by going through all strings following that string
          for (int m = 0; m < workingSlWSize; m++) {
            String term1 = strings.get(k+m);
            term1sForContext.add(term1);
            for (int n = m + 1; n < workingSlWSize; n++) {
              String term2 = strings.get(k+n);
              term1sForContext.add(term1);              
              if(term1.compareTo(term2) < 0) {
                //System.out.print(term+"|"+term2+" ");
                pairsForContext.add(term1+"\t"+term2);
              } else if(term1.compareTo(term2) > 0) {
                //System.out.print(term2+"|"+term+" ");
                pairsForContext.add(term2+"\t"+term1);
              } else {
                // if they are equal we do not add a pair
                // We could but then we would need to find out how to calculate
                // the counts for chi2 properly since inclusion/exclusion only
                // works for different terms in the pair
              }                            
            } // inner for: m
          } // outer for: n
          System.out.println("DEBUG: term1s for context: "+term1sForContext);
          System.out.println("DEBUG: pairs for context: "+pairsForContext);
          for(String t4c : term1sForContext) {
            incrementHashMapInteger(term1counts,t4c,1);
          }
          for(String p4c : pairsForContext) {
            incrementHashMapInteger(paircounts,p4c,1);
          }
        } // process window if we have just one type

      }
      
            
      
    } // for spans

    System.out.println("DEBUG: term1counts for document "+document.getName()+": "+term1counts);
    System.out.println("DEBUG: term1counts for document "+document.getName()+": "+term2counts);
    System.out.println("DEBUG: paircounts for document "+document.getName()+": "+paircounts);
    //System.out.println("DEBUG: contexts for document "+document.getName()+": "+contexts);
      
    for (String term : term1counts.keySet()) {
      //System.err.println("DEBUG: add term="+term+" count="+termcounts.get(term));
      corpusStats.countsTerms1.computeIfAbsent(term, (var -> new LongAdder())).add(term1counts.get(term));
    }
    if(haveTwoTypes) {
      for (String term : term2counts.keySet()) {
        //System.err.println("DEBUG: add term="+term+" count="+termcounts.get(term));
        corpusStats.countsTerms2.computeIfAbsent(term, (var -> new LongAdder())).add(term2counts.get(term));
      }
    }
    for (String pair : paircounts.keySet()) {
      //System.err.println("DEBUG: add pair="+pair+" count="+paircounts.get(pair));
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
    // check parameters and adapt if necessary!
    if(caseConversionLanguage == null || caseConversionLanguage.isEmpty()) {
      caseConversionLanguage = "en";
    }
    if(spanAnnotationType == null || spanAnnotationType.isEmpty()) {
      spanAnnotationType = "";
    }
    if(inputASName == null || inputASName.isEmpty()) {
      inputASName = "";
    }
    if(inputType1 == null || inputType1.isEmpty()) {
      throw new GateRuntimeException("Input Annotation type 1 is required");
    }
    if(inputType2 == null || inputType1.isEmpty()) {
      inputType2 = inputType1;
    }
    
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
        corpusStats.minContexts_p = getMinContextsP();
        corpusStats.minContexts_t1 = getMinContextsT1();
        corpusStats.minContexts_t2 = getMinContextsT2();
        corpusStats.haveTwoTypes = !inputType1.equals(inputType2);
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
        corpusStats.save(dataFileUrl, sumsFileUrl, pairStatsFileUrl, getMinContextsT1());
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
