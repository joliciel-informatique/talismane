package com.joliciel.talismane.stats;

import org.apache.commons.math3.distribution.BinomialDistribution;

/**
 * Implements Fisher's exact test (1-tailed version)
 * 
 * @author Assaf Urieli
 *
 */
public class BinomialTest {
  private double pValue = 0;

  public BinomialTest(int a, int b, int c, int d) {
    this.pValue = this.test(a, b, c, d);
  }

  /** Calculate a p-value for Fisher's Exact Test. */
  private double test(int a, int b, int c, int d) {
    BinomialDistribution dist = new BinomialDistribution(b + c, 0.5);
    double pValue = dist.cumulativeProbability(c);
    return pValue;
  }

  public double getPValue() {
    return pValue;
  }

  public static void main(String[] args) throws Exception {
    int a = Integer.parseInt(args[0]);
    int b = Integer.parseInt(args[1]);
    int c = Integer.parseInt(args[2]);
    int d = Integer.parseInt(args[3]);
    BinomialTest fisherTest = new BinomialTest(a, b, c, d);

    System.out.println("a=" + a + ", b=" + b + ", c=" + c + ", d=" + d + ": p-value=" + fisherTest.getPValue());
  }
}
