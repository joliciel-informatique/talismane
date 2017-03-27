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
package com.joliciel.talismane.machineLearning.features;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.JolicielException;

/**
 * A textual descriptor of a function, used for parsing a list of textual
 * function descriptors.
 * 
 * @author Assaf Urieli
 *
 */
public class FunctionDescriptor {
  enum FunctionDescriptorType {
    Function,
    Argument
  }

  private static final Logger LOG = LoggerFactory.getLogger(FunctionDescriptor.class);

  String functionName = null;
  List<FunctionDescriptor> arguments = new ArrayList<FunctionDescriptor>();
  Object object = null;
  String descriptorName = null;
  boolean binaryOperator = false;
  boolean empty = false;
  FunctionDescriptor parent;
  String groupName = null;

  /**
   * Get a function descriptor that wraps an actual function of the given name.
   */
  public FunctionDescriptor(String functionName) {
    this(functionName, FunctionDescriptorType.Function);
  }

  /**
   * If type=Argument, construct a function descriptor that simply wraps an
   * object, however, this argument is described by the argument provided as a
   * string. If it can be parsed as an int, and doesn't contain a ".", the
   * wrapped object will be an int. If it can be parsed as a double, the wrapped
   * object will be a double. If it can be parsed as a boolean, the wrapped
   * object will be a boolean. Otherwise, assumes it's a function name.
   */
  FunctionDescriptor(String name, FunctionDescriptorType type) {
    if (type == FunctionDescriptorType.Function)
      this.setFunctionName(name);
    else {
      this.setObjectAsString(name);
    }
  }

  /**
   * Get a function descriptor that simply wraps an object, rather than an
   * actual function.
   */
  FunctionDescriptor(Object object, boolean isObject) {
    if (!isObject)
      throw new JolicielException("the boolean argument is just for differentiating constructors!");
    this.object = object;
  }

  FunctionDescriptor(FunctionDescriptor toClone) {
    if (toClone.isFunction()) {
      this.setFunctionName(toClone.getFunctionName());
      if (toClone.isBinaryOperator())
        this.setBinaryOperator(true);
      for (FunctionDescriptor argument : toClone.getArguments()) {
        this.addArgument(argument.cloneDescriptor());
      }
    } else {
      this.setObject(toClone.getObject());
    }
    this.setGroupName(toClone.getGroupName());
  }

  /**
   * The function name when this descriptor describes a function, otherwise
   * null.
   */
  public String getFunctionName() {
    return functionName;
  }

  public void setFunctionName(String functionName) {
    if (this.object != null)
      throw new RuntimeException("Descriptor cannot be both a literal and a function");

    this.functionName = functionName;
    if (functionName.length() == 0)
      empty = true;
  }

  /**
   * The list of arguments when this descriptor describes a function.
   */
  public List<FunctionDescriptor> getArguments() {
    return arguments;
  }

  /**
   * Like addArgument(Object), but adds the argument at a particular index.
   */
  public void addArgument(int index, Object argument) {
    if (argument == null)
      throw new RuntimeException("Cannot add a null argument!");

    FunctionDescriptor argumentFunction = null;
    if (argument instanceof FunctionDescriptor)
      argumentFunction = (FunctionDescriptor) argument;
    else
      argumentFunction = new FunctionDescriptor(argument, true);

    if (index < 0)
      arguments.add(argumentFunction);
    else
      arguments.add(index, argumentFunction);

    if (LOG.isTraceEnabled())
      LOG.trace("Add argument: " + this.toString());

    argumentFunction.setParent(this);
  }

  /**
   * Add an argument to this descriptor (at the end of the argument list). If
   * the argument is a FunctionDescriptor, will get added as is. Otherwise, will
   * get added as FunctionDescriptor wrapper to the argument provided.
   */
  public void addArgument(Object argument) {
    this.addArgument(-1, argument);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (this.functionName != null) {
      sb.append(this.functionName);
      sb.append("(");
    } else if (this.object != null) {
      if (this.object instanceof String)
        sb.append("\"" + this.object.toString() + "\"");
      else
        sb.append(this.object.toString());
    }
    if (this.arguments.size() > 0) {
      boolean firstArgument = true;
      for (FunctionDescriptor argument : arguments) {
        if (!firstArgument)
          sb.append(",");
        sb.append(argument.toString());
        firstArgument = false;
      }
    }
    if (this.functionName != null) {
      sb.append(")");
    }
    return sb.toString();
  }

  /**
   * When this function descriptor describes an object, rather than a function,
   * the object it describes. Otherwise null.
   */
  public Object getObject() {
    return object;
  }

  public void setObject(Object object) {
    if (this.functionName != null)
      throw new RuntimeException("Descriptor cannot be both a function and a literal");
    this.object = object;
  }

  public void setObjectAsString(String argumentString) {
    if (this.object == null) {
      if (argumentString.contains(".")) {
        try {
          double doubleArgument = Double.parseDouble(argumentString);
          this.setObject(doubleArgument);
        } catch (NumberFormatException nfe) {
          // nothing to do here
        }
      }
    }

    if (this.object == null) {
      try {
        int intArgument = Integer.parseInt(argumentString);
        this.setObject(intArgument);
      } catch (NumberFormatException nfe) {
        // nothing to do here
      }
    }

    if (this.object == null) {
      if (argumentString.equals("true")) {
        this.setObject(true);
      } else if (argumentString.equals("false")) {
        this.setObject(false);
      }
    }

    if (this.object == null) {
      // assume it's a function not followed by parentheses
      this.functionName = argumentString;
    }
  }

  /**
   * A name given to this descriptor, or null otherwise.
   */
  public String getDescriptorName() {
    return descriptorName;
  }

  public void setDescriptorName(String descriptorName) {
    this.descriptorName = descriptorName;
  }

  /**
   * Returns true if function, false if literal.
   */
  public boolean isFunction() {
    return this.object == null;
  }

  /**
   * Is this descriptor a binary operator?
   */
  public boolean isBinaryOperator() {
    return binaryOperator;
  }

  public void setBinaryOperator(boolean binaryOperator) {
    this.binaryOperator = binaryOperator;
  }

  /**
   * Is this descriptor an empty function (without a name, typically
   * corresponding to open parenthesis).
   */
  public boolean isEmpty() {
    return empty;
  }

  /**
   * This function descriptor's parent.
   */
  public FunctionDescriptor getParent() {
    return parent;
  }

  void setParent(FunctionDescriptor parent) {
    this.parent = parent;
  }

  /**
   * Deep clone this function descriptor.
   */
  public FunctionDescriptor cloneDescriptor() {
    FunctionDescriptor descriptor = new FunctionDescriptor(this);
    return descriptor;
  }

  /**
   * Replace a given named parameter (basically a placeholder) within this
   * function descriptor by an actual argument value (a function descriptor to
   * put instead of the placeholder).
   */
  public void replaceParameter(String parameterName, FunctionDescriptor argument) {
    List<Integer> replaceIndexes = new ArrayList<Integer>();

    int i = 0;
    for (FunctionDescriptor descriptor : this.getArguments()) {
      if (descriptor.isFunction() && descriptor.getFunctionName().equals(parameterName)) {
        replaceIndexes.add(i);
      }
      i++;
    }
    for (int index : replaceIndexes) {
      this.getArguments().remove(index);
      this.getArguments().add(index, argument);
    }
    i = 0;
    for (FunctionDescriptor descriptor : this.getArguments()) {
      if (replaceIndexes.contains(i))
        continue;
      descriptor.replaceParameter(parameterName, argument);
      i++;
    }
  }

  /**
   * The name of the group to which this descriptor belongs, or null otherwise.
   * Several descriptors can be grouped together in a group, for mutual handling
   * downstream by other descriptors.
   */
  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  /**
   * Was this descriptor defined at the top level (a line of its own), or is it
   * inside another descriptor.
   */
  public boolean isTopLevelDescriptor() {
    return this.getParent() == null;
  }

}
