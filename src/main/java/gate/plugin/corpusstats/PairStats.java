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

import java.text.DecimalFormat;

/**
 * A container for all the pair-related statistics we calculate
 */
public class PairStats {

  public long pairCount;
  public long term1Count;
  public long term2Count;
  public double p_a_b;
  public double p_a;
  public double p_b;
  public double p_na_b;
  public double p_a_nb;
  public double p_na_nb;
  public double p_a_b_expected;
  public double pmi;
  public double npmi;
  public double wpmi;
  public double chi2;
  public double chi2_p;
  public double student_t;
  public double student_t_p;
  
  private DecimalFormat df = new DecimalFormat("#.######");
  protected String d(double val) {
    return df.format(val);
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("PairStats{");
    sb.append("freqp=");
    sb.append(pairCount);
    sb.append(",freqt1=");
    sb.append(term1Count);
    sb.append(",freqt2=");
    sb.append(term2Count);
    sb.append(",probp=");
    sb.append(d(p_a_b));
    sb.append(",probexp=");
    sb.append(d(p_a_b_expected));
    sb.append(",probt1=");
    sb.append(d(p_a));
    sb.append(",probt2=");
    sb.append(d(p_b));
    sb.append(",pmi=");
    sb.append(d(pmi));
    sb.append(",npmi=");
    sb.append(d(npmi));
    sb.append(",wpmi=");
    sb.append(d(wpmi));
    sb.append(",chi2=");
    sb.append(d(chi2));
    sb.append(",chi2_p=");
    sb.append(d(chi2_p));
    sb.append(",student_t=");
    sb.append(d(student_t));
    sb.append(",student_t_p=");
    sb.append(d(student_t_p));
    sb.append("}");
    return sb.toString();
  }
}
