/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
  
  DecimalFormat df = new DecimalFormat("#.######");
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
