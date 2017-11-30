/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.corpusstats;

import java.io.Serializable;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * @author Johann Petrak
 */
public class TermStats implements Serializable {
  
  private final LongAdder tf = new LongAdder();
  private final DoubleAdder wtf = new DoubleAdder(); // weighted tf: by document length
  private final DoubleAdder ntf = new DoubleAdder(); // normalized tf: by maximum tf in document
  private final LongAdder df = new LongAdder();

  public void incrementTf() {
    tf.add(1);
  }

  public void incrementDf() {
    df.add(1);
  }

  public void incrementTfBy(int by) {
    tf.add(by);
  }

  public void incrementWTfBy(double by) {
    wtf.add(by);
  }

  public void incrementNTfBy(double by) {
    ntf.add(by);
  }

  public long getTf() {
    return tf.sum();
  }

  public double getWTf() {
    return wtf.sum();
  }

  public double getNTf() {
    return ntf.sum();
  }

  public long getDf() {
    return df.sum();
  }
  
}
