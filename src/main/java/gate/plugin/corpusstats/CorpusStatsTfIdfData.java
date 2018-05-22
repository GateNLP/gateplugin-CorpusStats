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

/**
 * Class representing corpus statistics.
 * Also has methods for saving and loading data using either a binary 
 * file or two TSV files.
 * @author Johann Petrak
 */
public class CorpusStatsTfIdfData implements Serializable {

  private static final long serialVersionUID = 8999291900287145505L;
  public ConcurrentHashMap<String, TermStats> map;
  public LongAdder nDocs = null;
  public LongAdder nWords = null;  
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
          if(obj instanceof CorpusStatsTfIdfData) {
            CorpusStatsTfIdfData other = (CorpusStatsTfIdfData)obj;
            map = other.map;
            nDocs = other.nDocs;
            nWords = other.nWords;
            // NOTE: if the loaded stats file has a different case sensitivity setting, 
            // we throw an error, this does not make sense to have!
            if(isCaseSensitive != other.isCaseSensitive) {
              throw new GateRuntimeException("Data file loaded has a different caseSensitivy setting");
            }
            if(!ccLocale.equals(other.ccLocale)) {
              throw new GateRuntimeException("Data file loaded has a different case conversion language");
            }
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
  public void save(URL dataUrl, URL sumTsvUrl, URL statsTsvUrl, int minTf) {

        long ndocs = this.nDocs.sum();
        long nterms = this.map.size();
        long nwords = this.nWords.sum();

        if (sumTsvUrl != null && !sumTsvUrl.toExternalForm().isEmpty()) {
          File file = gate.util.Files.fileFromURL(sumTsvUrl);
          System.err.println("Storing total counts to file " + file);
          try (
                  FileOutputStream fos = new FileOutputStream(file);
                  PrintWriter pw = new PrintWriter(fos)) {
            // output the header
            // nwords=total number of words counted
            // nterms=total number of terms / different words
            // ndocs=total number of documents
            pw.println("nwords\tnterms\tndocs");
            pw.println(nwords + "\t" + nterms + "\t" + ndocs);
            System.err.println("Words: " + nwords);
            System.err.println("Terms: " + nterms);
            System.err.println("Docs:  " + ndocs);
          } catch (Exception ex) {
            throw new GateRuntimeException("Could not save tfidf file", ex);
          }
        }

        if (statsTsvUrl != null && !statsTsvUrl.toExternalForm().isEmpty()) {
          File file = gate.util.Files.fileFromURL(statsTsvUrl);
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
            int lines = 0;
            for (String key : this.map.keySet()) {
              long tf = this.map.get(key).getTf();
              if (tf < minTf) {
                continue;
              }
              long df = this.map.get(key).getDf();
              double ntf = this.map.get(key).getNTf();
              double wtf = this.map.get(key).getWTf();
              double idf = 1.0 + Math.log((((double) ndocs + 1.0)) / (df + 1.0));
              double tfidf = tf * idf;
              double ntfidf = ntf * idf;
              double wtfidf = wtf * idf;
              lines++;
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
            System.err.println("Term stats rows written to file: " + lines);
          } catch (Exception ex) {
            throw new GateRuntimeException("Could not save tfidf file", ex);
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
