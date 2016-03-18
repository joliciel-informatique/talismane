package com.joliciel.talismane.stats;
 
/** 
 * <p> 
 * Gaussian Function, allows you to plot a Gaussian Probability Density Function 
 * </p> 
 * 
 * <p> 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation; either version 2 
 * of the License, or (at your option) any later version, 
 * provided that any use properly credits the author. 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU Affero General Public License for more details at http://www.gnu.org 
 * </p> 
 * 
 @author Olly Oechsle, University of Essex 
 @version 1.0 
*/ 
public class Gaussian { 
 
    protected double stdDeviation, variance, mean; 
 
    public Gaussian(double stdDeviation, double mean) { 
 
        this.stdDeviation = stdDeviation; 
        variance = stdDeviation * stdDeviation; 
        this.mean = mean; 
 
    } 
 
    public double getY(double x) { 
 
        return Math.pow(Math.exp(-(((x - mean) * (x - mean)) / ((2 * variance)))), 1 / (stdDeviation * Math.sqrt(2 * Math.PI))); 
 
    } 
} 
