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
package com.joliciel.frenchTreebank;

/** *********************************
Exception thrown by the internal business logic of the French Treebank application.
@author Assaf Urieli
********************************* */
public class TreebankException extends RuntimeException
{
	private static final long serialVersionUID = -372594443442653223L;
	public TreebankException() { super(); }
	public TreebankException(String s) { super(s); }
	public TreebankException(Exception e) { super(e); }
}
