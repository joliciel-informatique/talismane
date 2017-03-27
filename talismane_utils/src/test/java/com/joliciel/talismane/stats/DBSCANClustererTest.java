///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class DBSCANClustererTest {

  @Test
  public void testCluster() {
    List<Integer> objectSet = new ArrayList<Integer>();
    List<double[]> dataSet = new ArrayList<double[]>();
    objectSet.add(1);
    objectSet.add(2);
    objectSet.add(3);
    objectSet.add(4);
    objectSet.add(5);
    objectSet.add(6);

    dataSet.add(new double[] { 3.8 });
    dataSet.add(new double[] { 4.0 });
    dataSet.add(new double[] { 4.2 });
    dataSet.add(new double[] { 8.0 });
    dataSet.add(new double[] { 8.1 });
    dataSet.add(new double[] { 12.0 });

    DBSCANClusterer<Integer> clusterer = new DBSCANClusterer<Integer>(objectSet, dataSet);
    Set<Set<Integer>> clusters = clusterer.cluster(0.3, 2, false);
    assertEquals(2, clusters.size());

    for (Set<Integer> cluster : clusters) {
      if (cluster.contains(1)) {
        assertTrue(cluster.contains(2));
        assertTrue(cluster.contains(3));
      } else if (cluster.contains(4)) {
        assertTrue(cluster.contains(5));
      } else {
        fail("Unknown cluster");
      }
    }

  }

}
