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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.features.FunctionDescriptor.FunctionDescriptorType;

/**
 * A parser for textual function descriptors.<br/>
 * If the descriptor contains a double-quote delimited string, this string can
 * contain \" and \\, which will escape " and \. Any other occurrences of \ will
 * not be escaped.
 * 
 * @author Assaf Urieli
 *
 */
public class FunctionDescriptorParser {
  private static final Logger LOG = LoggerFactory.getLogger(FunctionDescriptorParser.class);

  private enum CharacterClass {
    OPEN_PARENTHESIS, CLOSE_PARENTHESIS, COMMA, OPERATOR, OTHER, OTHER_SPACE
  }

  private static char[][] PRECEDENCE_RULES = new char[][] { { '*', '/', '%' }, { '+', '-' }, { '=', '!', '<', '>' }, { '&', '|' } };

  private static String ROOT_NAME = "[[ROOT]]";

  public FunctionDescriptor parseDescriptor(String text) {
    String descriptorName = null;
    String groupName = null;
    String[] parts = text.split("\t");
    if (parts.length == 1) {
      // nothing to do
    } else if (parts.length == 2) {
      descriptorName = parts[0];
      text = parts[1];
    } else if (parts.length == 3) {
      descriptorName = parts[0];
      groupName = parts[1];
      text = parts[2];
    } else {
      throw new DescriptorSyntaxException("Too many tabs in descriptor: " + parts.length, text, -1);
    }

    FunctionDescriptorParseContext context = new FunctionDescriptorParseContext(text);

    FunctionDescriptor rootDescriptor = new FunctionDescriptor(ROOT_NAME);
    push(context, rootDescriptor);

    for (int i = 0; i < text.length(); i++) {
      context.i = i;
      context.c = text.charAt(i);
      if (context.inQuote && context.c != '"') {
        if (context.c == '\\') {
          // escape for a double-quote or back-slash
          if (text.charAt(i + 1) == '"' || text.charAt(i + 1) == '\\') {
            i++;
            context.c = text.charAt(i);
          }
        }
        context.currentString += "" + context.c;
      } else if (context.c == '(' || context.c == '[') {
        this.doOpenParentheses(context);
      } else if (context.c == ')' || context.c == ']') {
        this.doCloseParentheses(context);
      } else if (context.c == ',') {
        this.doComma(context);
      } else if (context.c == ' ') {
        this.doSpace(context);
      } else if (context.c == '"') {
        this.doQuote(context);
      } else if (context.c == '+' || context.c == '-' || context.c == '<' || context.c == '>' || context.c == '=' || context.c == '!' || context.c == '*'
          || context.c == '/' || context.c == '%' || context.c == '&' || context.c == '|') {
        this.doOperator(context);
      } else {
        this.doOther(context);
      }
    }
    this.addArgument(context);

    if (!context.parenthesesStack.isEmpty())
      throw new DescriptorSyntaxException("Parentheses not closed", text, -1);

    rootDescriptor = pop(context);
    this.handleEmpty(rootDescriptor);
    if (rootDescriptor.getArguments().size() != 1) {
      throw new DescriptorSyntaxException(
          "Need exactly one top-level function per descriptor in " + descriptorName + ", have " + rootDescriptor.getArguments().size()
              + ", 1st argument: " + (rootDescriptor.getArguments().size() > 0 ? rootDescriptor.getArguments().get(0).toString() : "none"),
          context.text, -1);
    }

    FunctionDescriptor descriptor = rootDescriptor.getArguments().get(0);
    descriptor.setDescriptorName(descriptorName);
    descriptor.setGroupName(groupName);
    return descriptor;
  }

  void handleEmpty(FunctionDescriptor descriptor) {
    List<FunctionDescriptor> arguments = new ArrayList<FunctionDescriptor>(descriptor.getArguments());
    for (FunctionDescriptor argument : arguments) {
      this.handleEmpty(argument);
    }
    if (descriptor.isEmpty()) {
      // replace the empty function with its argument
      FunctionDescriptor argument = descriptor.getArguments().get(0);
      FunctionDescriptor parent = descriptor.getParent();
      int argIndex = parent.getArguments().indexOf(descriptor);
      parent.getArguments().remove(argIndex);
      parent.getArguments().add(argIndex, argument);
    }
  }

  private void doOther(FunctionDescriptorParseContext context) {
    if (context.lastCharacterClass.equals(CharacterClass.CLOSE_PARENTHESIS))
      throw new DescriptorSyntaxException("Unexpected text after close parenthesis", context.text, context.i);
    if (context.lastCharacterClass.equals(CharacterClass.OTHER_SPACE))
      throw new DescriptorSyntaxException("Unexpected space between two blocks of text", context.text, context.i - 1);
    context.currentString += "" + context.c;

    context.lastCharacterClass = CharacterClass.OTHER;
  }

  void doQuote(FunctionDescriptorParseContext context) {
    if (context.inQuote) {
      // add the quoted string as a string
      this.addArgument(context, true);

      context.inQuote = false;
    } else {
      if (context.lastCharacterClass.equals(CharacterClass.CLOSE_PARENTHESIS))
        throw new DescriptorSyntaxException("Unexpected quote after close parenthesis", context.text, context.i);
      if (context.lastCharacterClass.equals(CharacterClass.OTHER))
        throw new DescriptorSyntaxException("Unexpected quote after text", context.text, context.i);
      if (context.lastCharacterClass.equals(CharacterClass.OTHER_SPACE))
        throw new DescriptorSyntaxException("Unexpected quote after space", context.text, context.i);

      context.inQuote = true;
    }
    context.lastCharacterClass = CharacterClass.OTHER;
  }

  /**
   * Transform current string to a new descriptor, push it to the stack.
   */
  void doOpenParentheses(FunctionDescriptorParseContext context) {
    if (context.lastCharacterClass.equals(CharacterClass.CLOSE_PARENTHESIS))
      throw new DescriptorSyntaxException("Unexpected open parenthesis after close parenthesis", context.text, context.i);

    FunctionDescriptor openDescriptor = new FunctionDescriptor(context.currentString, FunctionDescriptorType.Function);

    push(context, openDescriptor);
    context.parenthesesStack.push(context.c);

    context.currentString = "";
    context.lastCharacterClass = CharacterClass.OPEN_PARENTHESIS;
  }

  /**
   * If current string length > 0, add it as argument to top descriptor. Pop
   * top-of-stack, and add it as argument to new top-of-stack.
   */
  void doCloseParentheses(FunctionDescriptorParseContext context) {
    if (context.lastCharacterClass.equals(CharacterClass.COMMA))
      throw new DescriptorSyntaxException("Close parenthesis cannot follow comma", context.text, context.i);
    if (context.lastCharacterClass.equals(CharacterClass.OPERATOR))
      throw new DescriptorSyntaxException("Close parenthesis cannot follow operator", context.text, context.i);

    if (context.parenthesesStack.isEmpty())
      throw new DescriptorSyntaxException("Too many closed parentheses", context.text, context.i);

    char openParenthesis = context.parenthesesStack.pop();
    if (openParenthesis == '(' && context.c == ']' || openParenthesis == '[' && context.c == ')') {
      throw new DescriptorSyntaxException("Parenthesis mismatch", context.text, context.i);
    }

    this.addArgument(context);
    FunctionDescriptor closedFunction = this.pop(context);
    FunctionDescriptor topOfStack = context.stack.peekFirst();
    topOfStack.addArgument(closedFunction);

    if (closedFunction.isEmpty()) {
      if (closedFunction.getArguments().size() != 1) {
        throw new DescriptorSyntaxException("Comma inside grouping parenthesis", context.text, context.i);
      }
    }

    while (topOfStack.isBinaryOperator()) {
      FunctionDescriptor operator = pop(context);
      topOfStack = context.stack.peekFirst();
      topOfStack.addArgument(operator);
    }

    context.lastCharacterClass = CharacterClass.CLOSE_PARENTHESIS;
  }

  /**
   * If current string length > 0, add it as argument to top descriptor.
   */
  void doComma(FunctionDescriptorParseContext context) {
    if (context.lastCharacterClass.equals(CharacterClass.OPERATOR))
      throw new DescriptorSyntaxException("Comma cannot follow operator", context.text, context.i);
    if (context.lastCharacterClass.equals(CharacterClass.OPEN_PARENTHESIS))
      throw new DescriptorSyntaxException("Comma cannot follow open parenthesis", context.text, context.i);
    if (context.lastCharacterClass.equals(CharacterClass.COMMA))
      throw new DescriptorSyntaxException("Comma cannot follow another comma", context.text, context.i);

    this.addArgument(context);
    context.lastCharacterClass = CharacterClass.COMMA;
  }

  /**
   * If operator is unary, add it to current string and do nothing else. If
   * operator is binary: If current string length > 0, add it as argument to
   * top descriptor. Take top(A) and transform it to top(operator(A)).
   * 
   */
  void doOperator(FunctionDescriptorParseContext context) {
    boolean unaryOperator = false;
    if (context.c == '-') {
      if (context.lastCharacterClass.equals(CharacterClass.COMMA) || context.lastCharacterClass.equals(CharacterClass.OPEN_PARENTHESIS)) {
        unaryOperator = true;
        context.currentString += "" + context.c;
        context.lastCharacterClass = CharacterClass.OTHER;
      }
    }
    if (!unaryOperator) {
      if (context.lastCharacterClass.equals(CharacterClass.COMMA))
        throw new DescriptorSyntaxException("Unexpected operator after comma", context.text, context.i);
      if (context.lastCharacterClass.equals(CharacterClass.OPEN_PARENTHESIS))
        throw new DescriptorSyntaxException("Unexpected operator after open parenthesis", context.text, context.i);

      if (context.lastCharacterClass.equals(CharacterClass.OPERATOR)) {
        FunctionDescriptor operator = context.stack.peekFirst();
        operator.setFunctionName(operator.getFunctionName() + context.c);
      } else {
        this.addArgument(context);

        FunctionDescriptor operator = new FunctionDescriptor("" + context.c);
        operator.setBinaryOperator(true);

        FunctionDescriptor topOfStack = context.stack.peekFirst();
        FunctionDescriptor lastArg = null;
        if (topOfStack.getArguments().size() > 0)
          lastArg = topOfStack.getArguments().get(topOfStack.getArguments().size() - 1);

        boolean normalOrder = true;
        if (lastArg != null && lastArg.isBinaryOperator()) {
          char firstOperator = lastArg.getFunctionName().charAt(0);
          char secondOperator = context.c;
          int firstOperatorPrecedenceIndex = -1;
          int secondOperatorPrecedenceIndex = -1;

          int j = 0;
          for (char[] operatorSet : PRECEDENCE_RULES) {
            for (char operatorChar : operatorSet) {
              if (operatorChar == firstOperator) {
                firstOperatorPrecedenceIndex = j;
              }
              if (operatorChar == secondOperator) {
                secondOperatorPrecedenceIndex = j;
              }
            }
            j++;
          }
          boolean secondOperatorPrecedence = secondOperatorPrecedenceIndex < firstOperatorPrecedenceIndex;

          if (secondOperatorPrecedence) {
            FunctionDescriptor operand2 = lastArg.getArguments().remove(lastArg.getArguments().size() - 1);
            lastArg.getArguments().remove(operand2);
            operator.addArgument(operand2);
            topOfStack.getArguments().remove(lastArg);
            push(context, lastArg);
            normalOrder = false;
          }
        }

        if (normalOrder) {
          FunctionDescriptor operand1 = topOfStack.getArguments().remove(topOfStack.getArguments().size() - 1);
          operator.addArgument(operand1);
        }

        push(context, operator);

        context.currentString = "";
      }
      context.lastCharacterClass = CharacterClass.OPERATOR;
    }
  }

  void doSpace(FunctionDescriptorParseContext context) {
    if (context.lastCharacterClass.equals(CharacterClass.OTHER)) {
      context.lastCharacterClass = CharacterClass.OTHER_SPACE;
    }
  }

  /**
   * If current string length > 0, add it as argument to top descriptor. If
   * top descriptor is binary, pop it off the stack.
   */
  void addArgument(FunctionDescriptorParseContext context) {
    this.addArgument(context, false);
  }

  /**
   * If current string length > 0, add it as argument to top descriptor. If
   * top descriptor is binary, pop it off the stack.
   */
  void addArgument(FunctionDescriptorParseContext context, boolean asString) {
    FunctionDescriptor topOfStack = context.stack.peekFirst();
    if (asString || context.currentString.length() > 0) {
      FunctionDescriptor argument = null;
      if (asString) {
        argument = new FunctionDescriptor(context.currentString, true);
      } else {
        argument = new FunctionDescriptor(context.currentString, FunctionDescriptorType.Argument);
      }
      topOfStack.addArgument(argument);
      while (topOfStack.isBinaryOperator()) {
        FunctionDescriptor operator = pop(context);
        topOfStack = context.stack.peekFirst();
        topOfStack.addArgument(operator);
      }

      context.currentString = "";
    }
  }

  void push(FunctionDescriptorParseContext context, FunctionDescriptor descriptor) {
    context.stack.push(descriptor);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Push " + descriptor);
    }
  }

  FunctionDescriptor pop(FunctionDescriptorParseContext context) {
    FunctionDescriptor descriptor = context.stack.pop();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Pop " + descriptor);
    }
    return descriptor;
  }

  private static class FunctionDescriptorParseContext {
    public String text = "";
    public String currentString = "";
    public Deque<FunctionDescriptor> stack = new ArrayDeque<FunctionDescriptor>();
    public Deque<Character> parenthesesStack = new ArrayDeque<Character>();
    public CharacterClass lastCharacterClass = CharacterClass.OPEN_PARENTHESIS;
    public boolean inQuote = false;
    public char c;
    public int i;

    private FunctionDescriptorParseContext(String text) {
      super();
      this.text = text;
    }

    @Override
    public String toString() {
      return "FunctionDescriptorParseContext [text=" + text + ", currentString=" + currentString + ", stack=" + stack + ", parenthesesStack="
          + parenthesesStack + ", lastCharacterClass=" + lastCharacterClass + ", inQuote=" + inQuote + ", c=" + c + ", i=" + i + "]";
    }

  }
}
