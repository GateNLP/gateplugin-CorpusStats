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

import gate.*;
import gate.creole.metadata.*;
import gate.util.Benchmark;
import gate.util.GateRuntimeException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@CreoleResource(name = "AssignStatsPR",
        helpURL = "https://gatenlp.github.io/gateplugin-CorpusStats/doc-AssignStatsTfIdfPR",
        comment = "Lookup and assign statistics to annotations")
public class AssignStatsTfIdfPR extends AbstractDocumentProcessor {

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
          comment = "The optional containing annotation set type: annotations will only be done within those",
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


  private URL dataFileUrl;

  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The URL that contains corpus statistics to use for the calculations"
  )
  public void setDataFileUrl(URL u) {
    dataFileUrl = u;
  }

  public URL getDataFileUrl() {
    return dataFileUrl;
  }
  
  private String statsList = "tfidf,wtfidf,ltfidf";
  private Map<String,StatsTfIdfFunction> statsFunctions;
  @RunTime
  @Optional
  @CreoleParameter(
          comment = "A comma/semicolon/whitespace separated list of stats names to add to the annotations",
          defaultValue = "tfidf,wtfidf,ltfidf"
  )
  public void setStatsList(String val) {
    statsList = val;
    statsFunctions = StatsTfIdfFunctions.names2functions(val);
  }
  public String getStatsList() {
    return statsList;
  }
  
  private String featurePrefix = "cs_";
  @RunTime
  @Optional
  @CreoleParameter(
          comment = "The prefix of all feature names to be added.",
          defaultValue = "cs_"
  )
  public void setFeaturePrefix(String val) {
    featurePrefix = val;
  }
  public String getFeaturePrefix() {
    return featurePrefix;
  }
  
  

  // The actual important parameters:
  // What values should get added? 
  // Possible as far as I can see:
  // ctf = corpus term frequency
  // mctf = mean corpus term frequency, ctf/ndocs
  // df = corpus document frequency
  // rdf = relative document frequency, df/ndocs
  // cntf = corpus normalized term frequency (normalized by per-document highest tf)
  // mcntf = mean corpus normalized term frequency
  // cwtf = corpus weighted term frequency (normalized by number of terms per decoument)
  // mcwtf = mean corpus weighted term frequency
  // idf = log(ndocs/df)
  
  
  ////////////////////// FIELDS
  // these fields will contain references to objects which are shared
  // between all duplicated copies of the PR
  private CorpusStatsTfIdfData corpusStats;
  // The following fields cache the values from corpusStats:
  private long nDocs;
  private long nWords;

  // fields local to each duplicated PR
  private int mostFrequentWordFreq = 0;
  private int documentWordFreq = 0;
  
  // The following map is a placeholder for the options map to pass to each
  // of the stats functions. This is not used yet, so we just use this empty
  // map for now
  static final Map<String,Object> DUMMY_OPTIONS = new HashMap<String,Object>();

  ////////////////////// PROCESSING
  @Override
  protected Document process(Document document) {

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

    fireStatusChanged("AssignStatsPR: running on " + document.getName() + "...");

    // we first count the terms in this document in our own map, then 
    // add the final counts to the global map.
    HashMap<String, Integer> wordcounts = new HashMap<>();

    long startTime = Benchmark.startPoint();

    mostFrequentWordFreq = 0;
    documentWordFreq = 0;

    if (containingAnns == null) {
      // go through all input annotations 
      for (Annotation ann : inputAnns) {
        calcLocalStats(document, ann, wordcounts);
        if (isInterrupted()) {
          throw new GateRuntimeException("TfIdf has been interrupted");
        }
      }
    } else {
      // go through the input annotations contained in the containing annotations
      for (Annotation containingAnn : containingAnns) {
        AnnotationSet containedAnns = gate.Utils.getContainedAnnotations(inputAnns, containingAnn);
        for (Annotation ann : containedAnns) {
          calcLocalStats(document, ann, wordcounts);
          if (isInterrupted()) {
            throw new GateRuntimeException("TfIdf has been interrupted");
          }
        }
      }
    }
    
    // TODO: now use the local stats and the global stats to calculate and assign
    // the values to the annotations
    
    if (containingAnns == null) {
      // go through all input annotations 
      for (Annotation ann : inputAnns) {
        assignStats(document, ann, wordcounts);
        if (isInterrupted()) {
          throw new GateRuntimeException("TfIdf has been interrupted");
        }
      }
    } else {
      // go through the input annotations contained in the containing annotations
      for (Annotation containingAnn : containingAnns) {
        AnnotationSet containedAnns = gate.Utils.getContainedAnnotations(inputAnns, containingAnn);
        for (Annotation ann : containedAnns) {
          assignStats(document, ann, wordcounts);
          if (isInterrupted()) {
            throw new GateRuntimeException("TfIdf has been interrupted");
          }
        }
      }
    }
    

    benchmarkCheckpoint(startTime, "__TfIdfProcess");

    fireProcessFinished();
    fireStatusChanged("TfIdf: processing complete!");
    return document;
  }

  private void assignStats(Document doc, Annotation ann, Map<String, Integer> wordmap) {
    // to calculate the various measures we have available the following building blocks
    // * wordmap[word]: the tf of the word in the document
    // * mostFrequentWordFreq: the most frequent term count (for ntf)
    // * documentWordFreq: the total number of terms (for wtf)
    // * all from TermStats for a word from the corpus.
    String key;
    FeatureMap fm = ann.getFeatures();
    if (getKeyFeature() == null || getKeyFeature().isEmpty()) {
      key = Utils.cleanStringFor(document, ann);
    } else {
      key = (String) fm.get(getKeyFeature());
    }
    if(!corpusStats.isCaseSensitive) {
      key = key.toLowerCase(corpusStats.ccLocale);
    }    
    if (key != null) {
      TermStats termStats = corpusStats.map.get(key);
      Integer tf = wordmap.get(key);
      if(tf==null) {
        tf = 0;
      }
      if(termStats==null) {
        termStats = new TermStats();
      }
      for(String fname : statsFunctions.keySet()) {
        Double stat = statsFunctions.get(fname).apply(termStats, 
                nDocs, 
                nWords, tf, 
                mostFrequentWordFreq, 
                documentWordFreq, 
                DUMMY_OPTIONS);
        String fn = fname;
        if(featurePrefix!=null && !featurePrefix.isEmpty()) {
          fn = featurePrefix+fn;
        }
        fm.put(fn, stat);
      }
    }    
  }
  
  
  private void calcLocalStats(Document doc, Annotation ann, Map<String, Integer> wordmap) {
    String key;
    FeatureMap fm = ann.getFeatures();
    if (getKeyFeature() == null || getKeyFeature().isEmpty()) {
      key = Utils.cleanStringFor(document, ann);
    } else {
      key = (String) fm.get(getKeyFeature());
    }
    if(!corpusStats.isCaseSensitive) {
      key = key.toLowerCase(corpusStats.ccLocale);
    }    
    // we actually have a word to count
    if (key != null) {
      documentWordFreq += 1;
      // check if we have seen this word in this document already:
      // if no, increase document frequency and remember it 
      if (!wordmap.containsKey(key)) {
        wordmap.put(key, 1);
        if (mostFrequentWordFreq == 0) {
          mostFrequentWordFreq = 1;
        }
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
    synchronized (SYNC_OBJECT) {
      corpusStats = (CorpusStatsTfIdfData)sharedData.get("corpusStats");
      if (corpusStats != null) {        
        System.err.println("INFO: corpusStats already created, we are duplicate " + duplicateId + " of PR " + this.getName());
      } else {
        System.err.println("INFO: creating corpusStats in duplicate " + duplicateId + " of PR " + this.getName());
        corpusStats = new CorpusStatsTfIdfData();
        corpusStats.map = new ConcurrentHashMap<>(1024 * 1024, 32, 32);
        corpusStats.nDocs = new LongAdder();
        corpusStats.nWords = new LongAdder();
        sharedData.put("corpusStats", corpusStats);
        System.err.println("INFO: corpusStats created and initialized in duplicate " + duplicateId + " of PR " + this.getName());
      }
      corpusStats.load(dataFileUrl, null, null);
      nDocs = corpusStats.nDocs.longValue();
      nWords = corpusStats.nWords.longValue();
    }
  }

  @Override
  protected void afterLastDocument(Controller ctrl, Throwable t) {
  }

  @Override
  protected void finishedNoDocument(Controller ctrl, Throwable t) {
  }


} // class JdbcLookup
