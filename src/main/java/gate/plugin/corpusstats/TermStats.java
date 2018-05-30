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

import java.io.Serializable;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * @author Johann Petrak
 */
public class TermStats implements Serializable {

  private static final long serialVersionUID = -5075722602609027484L;
  
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
