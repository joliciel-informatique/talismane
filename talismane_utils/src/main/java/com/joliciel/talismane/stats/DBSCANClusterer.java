///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs clustering on a dataset using the DBSCAN algorithm and Euclidean distance for similarity.
 * 
 * @author Assaf Urieli
 *
 */
public class DBSCANClusterer<T> {
  private static final Logger LOG = LoggerFactory.getLogger(DBSCANClusterer.class);

  List<T> objectSet;
  List<double[]> dataSet;
  boolean[] visited;
  List<Set<T>> clusterList;
  
  /**
   * Constructor
   * @param objectSet the objects to be clustered
   * @param dataSet the data vectors describing the objects to be clustered - clustering will be performed using the Euclidean distance data vectors
   */
  public DBSCANClusterer(List<T> objectSet, List<double[]> dataSet) {
    if (objectSet.size()!=dataSet.size())
      throw new RuntimeException("Object Set size has to be the same as Data Set size");
    this.objectSet = objectSet;
    this.dataSet = dataSet;
  }
  
  /**
   * Performs a cluster
   * @param epsilon the maximum distance for including two points in the same cluster
   * @param minPoints the minimum points required to form a cluster
   * @param includeNoise whether or not noise should be included when returning the clusters
   */
  public Set<Set<T>> cluster(double epsilon, int minPoints, boolean includeNoise) {
    LOG.debug("cluster: epsilon=" + epsilon + ", minPoints=" + minPoints + ", includeNoise=" + includeNoise);
    /*
    DBSCAN(D, eps, MinPts)
       C = 0
       for each unvisited point P in dataset D
          mark P as visited
          N = getNeighbors (P, eps)
          if sizeof(N) < MinPts
             mark P as NOISE
          else
             C = next cluster
             expandCluster(P, N, C, eps, MinPts)
     */
    Set<Set<T>> clusters = new HashSet<Set<T>>();
    Set<T> noise = new HashSet<T>();
    this.visited = new boolean[dataSet.size()];
    this.clusterList = new ArrayList<Set<T>>(dataSet.size());
    for (int i = 0; i < dataSet.size(); i++)
      this.clusterList.add(null);
    
    Set<T> cluster = null;
    
    for (int index = 0; index<dataSet.size(); index++) {
      if (visited[index])
        continue;
      visited[index] = true;
      Set<Integer> neighbours = this.getNeighbours(index, epsilon);
      if (neighbours.size()<minPoints - 1)
        noise.add(objectSet.get(index));
      else {
        cluster = new HashSet<T>();
        expandCluster(index, neighbours, cluster, epsilon, minPoints);
        clusters.add(cluster);
      }
    }
    LOG.debug("Found " + clusters.size() + " clusters"); 
    LOG.debug("Found " + noise.size() + " noise"); 
    if (includeNoise) {
      for (T object : noise) {
        Set<T> oneObject = new HashSet<T>();
        oneObject.add(object);
        clusters.add(oneObject);
        
      }
    }
    return clusters;
  }

  private void expandCluster(int index, Set<Integer> neighbours,
      Set<T> cluster, double epsilon, int minPoints) {
    /*
    expandCluster(P, N, C, eps, MinPts)
       add P to cluster C
       for each point P' in N 
          if P' is not visited
             mark P' as visited
             N' = getNeighbors(P', eps)
             if sizeof(N') >= MinPts
                N = N joined with N'
          if P' is not yet member of any cluster
             add P' to cluster C
    */
    cluster.add(objectSet.get(index));
    clusterList.set(index, cluster);
    Stack<Integer> points = new Stack<Integer>();
    points.addAll(neighbours);
    
    while (!points.isEmpty()) {
      int i = points.pop();
      if (!visited[i]) {
        visited[i] = true;
        Set<Integer> nPrime = this.getNeighbours(i, epsilon);
        if (nPrime.size()>=minPoints-1) {
          points.addAll(nPrime);
        }
      }
      if (clusterList.get(i)==null) {
        cluster.add(objectSet.get(i));
        clusterList.set(i, cluster);
      }
    }
  }

  /**
   * Get neighbours based on Euclidean distance.
   */
  Set<Integer> getNeighbours(int i, double epsilon) {
    Set<Integer> neighbours = new HashSet<Integer>();
    double[] point = dataSet.get(i);
    int dimensions = point.length;
    for (int j = 0; j < dataSet.size(); j++) {
      if (i!=j) {
        double[] otherPoint = dataSet.get(j);
        double sum = 0.0;
        for (int n = 0; n < dimensions; n++) {
          double diff = point[n]-otherPoint[n];
          sum += (diff * diff);
        }
        double distance = Math.sqrt(sum);
        if (distance <= epsilon)
          neighbours.add(j);
      }
    }
    return neighbours;
  }
}
