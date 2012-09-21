///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.stats;

import java.math.BigInteger;

/**
 * From http://www.merriampark.com/comb.htm
 * 
 * Combination Generator
 * by Michael Gilleland
 * 
 * The CombinationGenerator Java class systematically generates all combinations of n elements, taken r at a time. The algorithm is described by Kenneth H. Rosen, Discrete Mathematics and Its Applications, 2nd edition (NY: McGraw-Hill, 1991), pp. 284-286.
 * 
 * The class is very easy to use. Suppose that you wish to generate all possible three-letter combinations of the letters "a", "b", "c", "d", "e", "f", "g". Put the letters into an array. Keep calling the combination generator's getNext () method until there are no more combinations left. The getNext () method returns an array of integers, which tell you the order in which to arrange your original array of letters. Here is a snippet of code which illustrates how to use the CombinationGenerator class.
 * 
 * <code>
String[] elements = {"a", "b", "c", "d", "e", "f", "g"};
int[] indices;
CombinationGenerator x = new CombinationGenerator (elements.length, 3);
StringBuffer combination;
while (x.hasMore ()) {
  combination = new StringBuffer ();
  indices = x.getNext ();
  for (int i = 0; i < indices.length; i++) {
    combination.append (elements[indices[i]]);
  }
  System.out.println (combination.toString ());
}
</code>
 * @author Assaf Urieli
 *
 */
public class CombinationGenerator {

	private int[] a;
	private int n;
	private int r;
	private BigInteger numLeft;
	private BigInteger total;

	//------------
	// Constructor
	//------------

	public CombinationGenerator (int n, int r) {
		if (r > n) {
			throw new IllegalArgumentException ();
		}
		if (n < 1) {
			throw new IllegalArgumentException ();
		}
		this.n = n;
		this.r = r;
		a = new int[r];
		BigInteger nFact = getFactorial (n);
		BigInteger rFact = getFactorial (r);
		BigInteger nminusrFact = getFactorial (n - r);
		total = nFact.divide (rFact.multiply (nminusrFact));
		reset ();
	}

	//------
	// Reset
	//------

	public void reset () {
		for (int i = 0; i < a.length; i++) {
			a[i] = i;
		}
		numLeft = new BigInteger (total.toString ());
	}

	//------------------------------------------------
	// Return number of combinations not yet generated
	//------------------------------------------------

	public BigInteger getNumLeft () {
		return numLeft;
	}

	//-----------------------------
	// Are there more combinations?
	//-----------------------------

	public boolean hasMore () {
		return numLeft.compareTo (BigInteger.ZERO) == 1;
	}

	//------------------------------------
	// Return total number of combinations
	//------------------------------------

	public BigInteger getTotal () {
		return total;
	}

	//------------------
	// Compute factorial
	//------------------

	private static BigInteger getFactorial (int n) {
		BigInteger fact = BigInteger.ONE;
		for (int i = n; i > 1; i--) {
			fact = fact.multiply (new BigInteger (Integer.toString (i)));
		}
		return fact;
	}

	//--------------------------------------------------------
	// Generate next combination (algorithm from Rosen p. 286)
	//--------------------------------------------------------

	public int[] getNext () {

		if (numLeft.equals (total)) {
			numLeft = numLeft.subtract (BigInteger.ONE);
			return a;
		}

		int i = r - 1;
		while (a[i] == n - r + i) {
			i--;
		}
		a[i] = a[i] + 1;
		for (int j = i + 1; j < r; j++) {
			a[j] = a[i] + j - i;
		}

		numLeft = numLeft.subtract (BigInteger.ONE);
		return a;

	}
}
