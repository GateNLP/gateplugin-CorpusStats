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
/**
 *
 *  TfIdf: Simple PR to calculate count DF and TF and calculate TFIDF scores,
 *  with support for parallel processing.
 */
package gate.plugin.corpusstats;

import org.jpetrak.gate8.api.plugins.AbstractDocumentProcessor;
import gate.*;
import gate.creole.metadata.*;
import gate.util.Benchmark;
import gate.util.GateRuntimeException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@CreoleResource(name = "CorpusStatsTfIdfPR",
        helpURL = "https://gatenlp.github.io/gateplugin-CorpusStats/doc-CorpusStatsTfIdfPR",
        comment = "Calculate tf, df, and additional statistics over a corpus")
public class CorpusStatsTfIdfPR extends AbstractDocumentProcessor {

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
  @Optional
  @CreoleParameter(
          comment = "The URL of the TSV file where to store the per-term counts"
  )
  public void setTfDfFileUrl(URL u) {
    tfDfFileUrl = u;
  }

  public URL getTfDfFileUrl() {
    return tfDfFileUrl;
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
  
  private Locale ccLocale = new Locale("en");
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
          comment = "The URL of where to store the data in binary compressed format"
  )
  public void setDataFileUrl(URL u) {
    dataFileUrl = u;
  }

  public URL getDataFileUrl() {
    return dataFileUrl;
  }

  private int minTf = 1;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The minimum term frequency for the term stats to get saved",
          defaultValue = "1"
  )
  public void setMinTf(Integer value) {
    if (value == null) {
      minTf = 1;
    } else {
      minTf = value;
    }
  }

  public Integer getMinTf() {
    return minTf;
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
   * being reset to zero between runs if reuseExisting is false. However,
   * if reuseExisting is false, the stats get saved when processing ends and
   * loaded next time processing starts, so each run adds to the stats
   * already there from previous runs!
   * 
   * @param val  TODO
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
          
  ////////////////////// FIELDS
  // these fields will contain references to objects which are shared
  // because all duplicated copies of the PR
  private CorpusStatsTfIdfData corpusStats;

  // fields local to each duplicated PR
  private int mostFrequentWordFreq = 0;
  private int documentWordFreq = 0;

  ////////////////////// PROCESSING
  @Override
  protected void process(Document document) {

    if(corpusStats == null) {
      corpusStats = (CorpusStatsTfIdfData)getSharedData().get("corpusStatsTfIdf");
    }
    AnnotationSet inputAS;
    if (inputASName == null
            || inputASName.isEmpty()) {
      inputAS = document.getAnnotations();
    } else {
      inputAS = document.getAnnotations(inputASName);
    }

    AnnotationSet inputAnns;
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

    //fireStatusChanged("CorpusStatsTfIdfPR: running on " + document.getName() + "...");

    // we first count the terms in this document in our own map, then 
    // add the final counts to the global map.
    HashMap<String, Integer> wordcounts = new HashMap<>();

    long startTime = Benchmark.startPoint();

    mostFrequentWordFreq = 0;
    documentWordFreq = 0;

    if (containingAnns == null) {
      // go through all input annotations 
      for (Annotation ann : inputAnns) {
        doIt(document, ann, wordcounts);
        if (isInterrupted()) {
          throw new GateRuntimeException("CorpusStatsTfIdfPR has been interrupted");
        }
      }
    } else {
      // go through the input annotations contained in the containing annotations
      for (Annotation containingAnn : containingAnns) {
        AnnotationSet containedAnns = gate.Utils.getContainedAnnotations(inputAnns, containingAnn);
        for (Annotation ann : containedAnns) {
          doIt(document, ann, wordcounts);
          if (isInterrupted()) {
            throw new GateRuntimeException("CorpusStatsTfIdfPR has been interrupted");
          }
        }
      }
    }

    // now add the locally counted term frequencies to the global map
    // also add the weighted/normalized term frequencies
    for (String key : wordcounts.keySet()) {
      corpusStats.map.computeIfAbsent(key, (k -> new TermStats())).incrementTfBy(wordcounts.get(key));
      corpusStats.map.computeIfAbsent(key, (k -> new TermStats())).incrementWTfBy(wordcounts.get(key) / ((double) documentWordFreq));
      corpusStats.map.computeIfAbsent(key, (k -> new TermStats())).incrementNTfBy(wordcounts.get(key) / ((double) mostFrequentWordFreq));
    }

    corpusStats.nDocs.add(1);
    benchmarkCheckpoint(startTime, "__TfIdfProcess");

    //fireProcessFinished();
    //fireStatusChanged("CorpusStatsTfIdfPR: processing complete!");
  }

  // NOTE: this method updates the global fields documentWordFreq
  // and 
  private void doIt(Document doc, Annotation ann, Map<String, Integer> wordmap) {
    String key;
    FeatureMap fm = ann.getFeatures();
    if (getKeyFeature() == null || getKeyFeature().isEmpty()) {
      key = Utils.cleanStringFor(document, ann);
    } else {
      key = (String) fm.get(getKeyFeature());
    }
    if(!getCaseSensitive()) {
      key = key.toLowerCase(ccLocale);
    }
    if (key != null) {
      // count total number of words found
      corpusStats.nWords.add(1);
      documentWordFreq += 1;
      // check if we have seen this word in this document already:
      // if no, increase document frequency and remember it 
      if (!wordmap.containsKey(key)) {
        wordmap.put(key, 1);
        if (mostFrequentWordFreq == 0) {
          mostFrequentWordFreq = 1;
        }
        // lets also add to the document frequency right here ....
        corpusStats.map.computeIfAbsent(key, (k -> new TermStats())).incrementDf();
      } else {
        int thisWf = wordmap.get(key) + 1;
        wordmap.put(key, thisWf);  // increase the count in our own map
        if (thisWf > mostFrequentWordFreq) {
          mostFrequentWordFreq = thisWf;
        }
      }
    }
  }

  @Override
  protected void beforeFirstDocument(Controller ctrl) {
    // if reference null, create the global map
    // synchronized (syncObject) {  // the syncing is now done in the calling execute()
      corpusStats = (CorpusStatsTfIdfData)getSharedData().get("corpusStatsTfIdf");
      if (corpusStats != null) {        
        System.err.println("INFO: corpusStats already created, we are duplicate " + duplicateId + " of PR " + this.getName());
      } else {
        System.err.println("INFO: creating corpusStats in duplicate " + duplicateId + " of PR " + this.getName());
        corpusStats = new CorpusStatsTfIdfData();
        corpusStats.map = new ConcurrentHashMap<>(1024 * 1024, 32, 32);
        corpusStats.nDocs = new LongAdder();
        corpusStats.nWords = new LongAdder();
        corpusStats.isCaseSensitive = getCaseSensitive();
        corpusStats.ccLocale = new Locale(getCaseConversionLanguage());
        getSharedData().put("corpusStatsTfIdf", corpusStats);
        System.err.println("INFO: corpusStats created and initialized in duplicate " + duplicateId + " of PR " + this.getName());
      }
      // Now at this point we have a CorpusStats instance for sure. However, 
      // if reuseStats is true, we may still have to load it. 
      // NOTE: if the PR is run several times in a row, then when we arrive
      // here, the corpusstats object should always be initialized to empty,
      // since we always remove it after processing has finished. 
      corpusStats.isInitialized = true;
      if(getReuseExisting()) {
        corpusStats.load(dataFileUrl, sumsFileUrl, tfDfFileUrl);
      }
    // } // syncing
  }

  @Override
  protected void afterLastDocument(Controller ctrl, Throwable t) {
    synchronized (SYNC_OBJECT) {
      long startTime = Benchmark.startPoint();
      // TODO: we had this here, but why do we need it?
      corpusStats = (CorpusStatsTfIdfData) sharedData.get("corpusStatsTfIdf");
      if (corpusStats != null) {
        corpusStats.save(dataFileUrl, sumsFileUrl, tfDfFileUrl, getMinTf());
        // After each run, we clean up, so that the code before each run can 
        // recreate or reload the data as if it was the first time
        corpusStats.map = null;
        corpusStats = null;
        sharedData.remove("corpusStatsTfIdf");
      } // if corpusstats is not null
      benchmarkCheckpoint(startTime, "__TfIdfSave");
    }
  }

  @Override
  protected void finishedNoDocument(Controller ctrl, Throwable t) {
    // After each run, we clean up, so that the code before each run can 
    // recreate or reload the data as if it was the first time
    synchronized (SYNC_OBJECT) {
      corpusStats.map = null;
      corpusStats = null;
      sharedData.remove("corpusStatsTfIdf");
    }
  }


} // class 
