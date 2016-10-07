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
 *  
 *  Currently this is a simple implementation which uses two in-memory 
 *  Guava AtomicLongMaps. This can probably made more memory efficient
 *  by using a single map that maps to an atomic tuple for both DF and TF.
 */
package gate.plugin.corpusstats;


import gate.*;
import gate.api.AbstractDocumentProcessor;
import gate.creole.metadata.*;
import gate.util.GateRuntimeException;
import java.net.URL;
import com.google.common.util.concurrent.AtomicLongMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@CreoleResource(name = "TfIdf",
        comment = "Calculate tf, df, and idf statistics over a corpus")
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
  @Optional
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
  
  private AtomicLongMap<String> mapTf = null;  
  private AtomicLongMap<String> mapDf = null;
  private AtomicInteger nDocs = null;
  private AtomicLong nWords = null;
  
  private static final Object syncObject = new Object();
  
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

    HashMap<String,Long> dmap = new HashMap<String,Long>();
    
    if (containingAnns == null) {
      // go through all input annotations 
      for (Annotation ann : inputAnns) {
        doIt(document, ann, dmap);
        if(isInterrupted()) {
          throw new GateRuntimeException("TfIdf has been interrupted");
        }
      }
    } else {
      // go through the input annotations contained in the containing annotations
      for (Annotation containingAnn : containingAnns) {
        AnnotationSet containedAnns = gate.Utils.getContainedAnnotations(inputAnns, containingAnn);
        for (Annotation ann : containedAnns) {
          doIt(document, ann, dmap);
          if(isInterrupted()) { 
            throw new GateRuntimeException("TfIdf has been interrupted");
          }
        }
      }
    }

    // TODO:
    // process per-document hash: add counts to TF map increment by 1 DF map
    for(String key : dmap.keySet()) {
      mapDf.addAndGet(key, 1);
      mapTf.addAndGet(key, dmap.get(key));
    }

    nDocs.addAndGet(1);
    
    fireProcessFinished();
    fireStatusChanged("TfIdf: processing complete!");
    return document;
  }
  
  private void doIt(Document doc, Annotation ann, HashMap<String,Long> dmap) {
    String key;
    FeatureMap fm = ann.getFeatures();
    if (getKeyFeature() == null || getKeyFeature().isEmpty()) {
      key = Utils.cleanStringFor(document, ann);
    } else {
      key = (String) fm.get(getKeyFeature());
    }
    if (key != null) {
      nWords.addAndGet(1);
      Long curCount = dmap.get(key);
      if(curCount == null) {
        dmap.put(key,1L);
      } else {
        dmap.put(key,curCount+1L);
      }
    }
  }
  

  @Override
  protected void beforeFirstDocument(Controller ctrl) {
    // if reference null, create the global map
    synchronized (syncObject) {
      mapTf = (AtomicLongMap<String>)sharedData.get("mapTf");
      if (mapTf != null) {
        System.err.println("INFO: shared maps already created");
        mapDf = (AtomicLongMap<String>)sharedData.get("mapDf");
        nDocs = (AtomicInteger)sharedData.get("nDocs");
        nWords = (AtomicLong)sharedData.get("nWords");
        //System.err.println("INFO: copied existing maptf/mapdf/ndocs/nwords: "+mapTf+"/"+mapDf+"/"+nDocs+"/"+nWords);
      } else {
        System.err.println("INFO: creating shared maps");
        mapTf = AtomicLongMap.create(new HashMap<String, Long>());
        sharedData.put("mapTf", mapTf);
        mapDf = AtomicLongMap.create(new HashMap<String, Long>());
        sharedData.put("mapDf", mapDf);
        nDocs = new AtomicInteger();
        sharedData.put("nDocs", nDocs);
        nWords = new AtomicLong();
        sharedData.put("nWords", nWords);
        System.err.println("INFO: shared maps created");
      }
    }
  }
    

  @Override
  protected void afterLastDocument(Controller ctrl, Throwable t) {
    synchronized (syncObject) {
      mapTf = (AtomicLongMap<String>)sharedData.get("mapTf");
      if (mapTf != null) {
        
        int ndocs = nDocs.get();
        int nterms = mapTf.size();
        long nwords = nWords.get();
        
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
          pw.println("term\ttf\tdf");
          for(String key : mapTf.asMap().keySet()) {
            long tf = mapTf.get(key);
            long df = mapDf.get(key);
            pw.print(key);
            pw.print("\t");
            pw.print(tf);
            pw.print("\t");
            pw.println(df);
          }
        } catch (Exception ex) {
          throw new GateRuntimeException("Could not save tfidf file", ex);
        }
        
        mapTf = null;
        sharedData.remove("mapTf");
      } // if getMapTf() != null
    }
  }

  @Override
  protected void finishedNoDocument(Controller ctrl, Throwable t) {
  }
  

  
  
} // class JdbcLookup
