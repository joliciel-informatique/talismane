///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Safety Data -CFH
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
package com.joliciel.talismane.regex.parser;

import java.util.EmptyStackException;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.function.Function;

import com.joliciel.talismane.regex.bytecode.CharRange;
import com.joliciel.talismane.regex.lexer.CharToken;
import com.joliciel.talismane.regex.lexer.IntToken;
import com.joliciel.talismane.regex.lexer.Lexer;
import com.joliciel.talismane.regex.lexer.Lexer.State;
import com.joliciel.talismane.regex.lexer.SpecialToken;
import com.joliciel.talismane.regex.lexer.StringToken;
import com.joliciel.talismane.regex.lexer.Token;
import com.joliciel.talismane.regex.lexer.TokenType;
import com.joliciel.talismane.resources.WordList;
import com.joliciel.talismane.resources.WordListFinder;
import com.joliciel.talismane.utils.Either;

import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

/**
 * A regular expression parser.
 * 
 * @author Lucas Satabin
 *
 */
public class Parser {

	private final WordListFinder wordListFinder;

	// the input lexer
	final private Lexer lexer;

	// the last parsed token stack
	private final Stack<Either<Token, Node>> stack;

	private final TIntStack openGgroup;
	private int group;

	public Parser(String input, WordListFinder wordListFinder) {
		this.wordListFinder = wordListFinder;
		this.lexer = new Lexer(input);
		this.stack = new Stack<>();
		this.openGgroup = new TIntArrayStack();
		this.group = 0;
	}

	public Parser(String input) {
		this(input, null);
	}

	private static final CharSet numbers = new CharSet(new CharRange('0', '9'));
	private static final CharSet word = new CharSet(new CharRange('a', 'z'), new CharRange('A', 'Z'),
			new CharRange('0', '9'), new CharRange('_'));
	private static final CharSet whitespace = new CharSet(new CharRange(' '), new CharRange('\t'), new CharRange('\n'),
			new CharRange('\u000b'), new CharRange('\f'), new CharRange('\r'));

	public Node parse() {
		// parse the input string
		Token token = lexer.nextToken();
		while (token.type != TokenType.EOI) {
			if (token instanceof CharToken) {
				// push the character onto the stack
				stack.push(Either.ofRight(new Char(((CharToken) token).character)));
			} else if (token instanceof IntToken || token instanceof StringToken) {
				// push the token onto the stack to be reduced later
				stack.push(Either.ofLeft(token));
			} else {
				// so this is a special token, which one?
				SpecialToken tok = (SpecialToken) token;
				switch (tok.type) {
				case DOT:
					stack.push(Either.ofRight(Dot.INSTANCE));
					break;
				case OPT:
					reduceOne(n -> new Repeated(n, 0, 1));
					break;
				case STAR:
					reduceOne(n -> new Repeated(n, 0, -1));
					break;
				case PLUS:
					reduceOne(n -> new Repeated(n, 1, -1));
					break;
				case RPAREN:
					if (lexer.getState() == State.CLASS_NAME) {
						// will be reduced with state name
						stack.push(Either.ofLeft(tok));
					} else {
						// reduce many nodes to create a capturing group
						reduceCapture();
					}
					break;
				case RBRACKET:
					// reduce the character set
					reduceCharSet();
					break;
				case RBRACE:
					// reduce repetition or named character class
					reduceRepOrClass();
					break;
				case PIPE:
					// reduce the first part of the alternative
					reduceAlternative();
					// and push the pipe
					stack.push(Either.ofLeft(tok));
					break;
				case NUMBER:
					stack.push(Either.ofRight(numbers));
					break;
				case WORD_CHARACTER:
					stack.push(Either.ofRight(word));
					break;
				case WHITESPACE:
					stack.push(Either.ofRight(whitespace));
					break;
				case WORD_BOUNDARY:
					stack.push(Either.ofRight(WordBoundary.INSTANCE));
					break;
				case NAMED_CLASS:
					stack.push(Either.ofLeft(tok));
					break;
				case LPAREN:
					if (lexer.getState() != State.CLASS_NAME) {
						openGgroup.push(group++);
					}
					// fall-through intentional because the parenthesis is
					// pushed onto the stack to delimit later reduction
				default:
					// other token will be reduced later
					stack.push(Either.ofLeft(tok));
				}
			}
			token = lexer.nextToken();
		}

		// reduce the potential last alternative
		reduceAlternative();

		if (stack.isEmpty()) {
			return Empty.INSTANCE;
		} else if (stack.size() == 1) {
			// in the end the stack must contain exactly one element, which is
			// the result
			return stack.pop().getRight();
		} else {
			throw new ParserException("Malformed regular expression");
		}
	}

	private void reduceRepOrClass() {
		// the top of the stack must be in one of this state
		// - StringToken(name) :: NamedClassToken :: ...
		// - RParenToken :: StringToken(name) :: LParenToken :: WordListToken :: NamedClassToken :: ...
		// - IntToken(n) :: LBraceToken :: ...
		// - IntToken(max) :: CommaToken :: IntToken(min) :: LBraceToken :: ...
		// - IntToken(max) :: CommaToken :: LBraceToken :: ...
		// - CommaToken :: IntToken(min) :: LBraceToken :: ...
		try {
			Token token = stack.pop().getLeft();

			switch (token.type) {
			case STRING:
				String name = ((StringToken) token).value;
				token = stack.pop().getLeft();
				switch (token.type) {
				case NAMED_CLASS:
					stack.push(Either.ofRight(new NamedClass(name)));
					break;
				default:
					throw new ParserException(
							"Malformed regular expression. Unexpected " + token.type + " at index " + token.offset);
				}
				break;
			case INT:
				int i = ((IntToken) token).value;
				token = stack.pop().getLeft();
				switch (token.type) {
				case COMMA:
					token = stack.pop().getLeft();
					switch (token.type) {
					case LBRACE:
						// n{,i}
						Node n = stack.pop().getRight();
						stack.push(Either.ofRight(new Repeated(n, 0, i)));
						break;
					case INT:
						int j = ((IntToken) token).value;
						token = stack.pop().getLeft();
						switch (token.type) {
						case LBRACE:
							// n{j,i}
							n = stack.pop().getRight();
							stack.push(Either.ofRight(new Repeated(n, j, i)));
							break;
						default:
							throw new ParserException("Malformed regular expression. Unexpected " + token.type
									+ " at index " + token.offset);
						}
						break;
					default:
						throw new ParserException(
								"Malformed regular expression. Unexpected " + token.type + " at index " + token.offset);
					}
					break;
				case LBRACE:
					// n{i}
					Node n = stack.pop().getRight();
					stack.push(Either.ofRight(new Repeated(n, i, i)));
					break;
				default:
					throw new ParserException(
							"Malformed regular expression. Unexpected " + token.type + " at index " + token.offset);
				}
				break;
			case COMMA:
				// n{i,}
				token = stack.pop().getLeft();
				switch (token.type) {
				case INT:
					i = ((IntToken) token).value;
					token = stack.pop().getLeft();
					switch (token.type) {
					case LBRACE:
						Node n = stack.pop().getRight();
						stack.push(Either.ofRight(new Repeated(n, i, -1)));
						break;
					default:
						throw new ParserException(
								"Malformed regular expression. Unexpected " + token.type + " at index " + token.offset);
					}
					break;
				default:
					throw new ParserException(
							"Malformed regular expression. Unexpected " + token.type + " at index " + token.offset);
				}
				break;
			case RPAREN:
				token = stack.pop().getLeft();
				switch (token.type) {
				case STRING:
					name = ((StringToken) token).value;
					token = stack.pop().getLeft();
					switch (token.type) {
					case LPAREN:
						token = stack.pop().getLeft();
						switch (token.type) {
						case STRING:
							String wl = ((StringToken) token).value;
							if ("WordList".equals(wl)) {
								token = stack.pop().getLeft();
								switch (token.type) {
								case NAMED_CLASS:
									WordList list = wordListFinder.getWordList(name);
									if (list == null) {
										throw new ParserException("Malformed regular expression. Unknown word list "
												+ name + " at index " + token.offset);
									}
									Node words = null;
									for (String word : list) {
										Node wordNode = null;
										for (char c : word.toCharArray()) {
											if (wordNode == null) {
												wordNode = new Char(c);
											} else {
												wordNode = new Concat(wordNode, new Char(c));
											}
										}
										if (wordNode != null) {
											if (words == null) {
												words = wordNode;
											} else {
												words = new Alternative(words, wordNode);
											}
										}
									}
									if (words != null) {
										stack.push(Either.ofRight(words));
									}
									break;
								default:
									throw new ParserException("Malformed regular expression. Unexpected " + token.type
											+ " at index " + token.offset);
								}
							} else {
								throw new ParserException("Malformed regular expression. Unexpected " + token.type
										+ " at index " + token.offset);
							}
							break;
						default:
							throw new ParserException("Malformed regular expression. Unexpected " + token.type
									+ " at index " + token.offset);
						}
						break;
					default:
						throw new ParserException(
								"Malformed regular expression. Unexpected " + token.type + " at index " + token.offset);
					}
					break;
				default:
					throw new ParserException(
							"Malformed regular expression. Unexpected " + token.type + " at index " + token.offset);
				}
				break;
			default:
				throw new ParserException(
						"Malformed regular expression. Unexpected " + token.type + " at index " + token.offset);
			}

		} catch (EmptyStackException | NoSuchElementException | ClassCastException e) {
			throw new ParserException("Malformed regular expression", e);
		}
	}

	private void reduceCharSet() {

		SortedSet<CharRange> charset = new TreeSet<>();
		boolean cont = !stack.isEmpty();
		try {
			while (cont) {
				Either<Token, Node> top = stack.pop();
				if (top.isLeft()) {
					switch (((SpecialToken) top.getLeft()).type) {
					case LBRACKET:
						cont = false;
						break;
					default:
						throw new ParserException("Malformed regular expression. Unexpected " + top.getLeft().type
								+ " at index " + top.getLeft().offset);
					}
				} else {
					Node n = top.getRight();
					switch (n.type) {
					case CHAR:
						char stop = ((Char) n).character;
						Either<Token, Node> prev = stack.peek();
						if (prev.isLeft() && (prev.getLeft().type == TokenType.DASH)) {
							Token dash = stack.pop().getLeft();
							n = stack.pop().getRight();
							switch (n.type) {
							case CHAR:
								char start = ((Char) n).character;
								charset.add(new CharRange(start, stop));
								break;
							default:
								throw new ParserException(
										"Malformed refular exception. Unexpected - at index " + dash.offset);
							}
						} else {
							charset.add(new CharRange(stop, stop));
						}
						break;
					case CHARSET:
						CharSet cs = (CharSet) n;
						charset.addAll(cs.charset);
						break;
					default:
						throw new ParserException("Malformed regular exception. Unexpected " + n);
					}

					if (stack.isEmpty()) {
						throw new ParserException("Malformed regular expression");
					}
				}
			}
		} catch (EmptyStackException | NoSuchElementException | ClassCastException e) {
			throw new ParserException("Malformed regular expression", e);
		}

		stack.push(Either.ofRight(new CharSet(charset)));
	}

	private void reduceCapture() {
		Node acc = null;
		boolean cont = !stack.isEmpty();
		try {
			while (cont) {
				Either<Token, Node> top = stack.pop();
				if (top.isLeft()) {
					switch (top.getLeft().type) {
					case LPAREN:
						// a capture group boundary
						stack.push(Either.ofRight(new Capture(openGgroup.pop(), acc == null ? Empty.INSTANCE : acc)));
						cont = false;
						break;
					default:
						throw new ParserException("Malformed regular expression");
					}
				} else {
					Either<Token, Node> prev = stack.peek();
					if (prev.isLeft() && prev.getLeft().type == TokenType.PIPE) {
						stack.pop();
						Node first = stack.pop().getRight();

						if (acc == null) {
							acc = new Alternative(first, top.getRight());
						} else {
							acc = new Alternative(first, new Concat(top.getRight(), acc));
						}
					} else {

						if (acc == null) {
							acc = top.getRight();
						} else {
							acc = new Concat(top.getRight(), acc);
						}
						if (stack.isEmpty()) {
							throw new ParserException("Malformed regular expression");
						}
					}
				}
			}
		} catch (EmptyStackException | NoSuchElementException | ClassCastException e) {
			throw new ParserException("Malformed regular expression", e);
		}
	}

	private void reduceAlternative() {
		Node acc = null;
		boolean cont = !stack.isEmpty();
		try {
			while (cont) {
				Either<Token, Node> top = stack.peek();
				if (top.isLeft()) {
					switch (top.getLeft().type) {
					case PIPE:
						// an alternative boundary, create the alternative
						stack.pop();
						if (acc != null) {
							stack.push(Either.ofRight(new Alternative(stack.pop().getRight(), acc)));
						}
						cont = false;
						break;
					case LPAREN:
						// a capture group boundary, make alternative with empty
						// case
						stack.push(Either.ofRight(acc == null ? Empty.INSTANCE : acc));
						cont = false;
						break;
					default:
						throw new ParserException("Malformed regular expression");
					}
				} else {
					stack.pop();
					if (acc == null) {
						acc = top.getRight();
					} else {
						acc = new Concat(top.getRight(), acc);
					}
					if (stack.isEmpty()) {
						// push back the accumulator and stop, we reached the
						// bottom
						stack.push(Either.ofRight(acc));
						cont = false;
					}
				}
			}
		} catch (EmptyStackException | NoSuchElementException | ClassCastException e) {
			throw new ParserException("Malformed regular expression", e);
		}
	}

	private void reduceOne(Function<Node, Node> constr) {
		try {
			stack.push(Either.ofRight(constr.apply(stack.pop().getRight())));
		} catch (Exception e) {
			throw new ParserException("Malformed regular expression", e);
		}
	}

}
