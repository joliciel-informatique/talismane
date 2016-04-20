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
package com.joliciel.talismane.regex.lexer;

/**
 * A lexer for regular expression string.
 * 
 * @author Lucas Satabin
 *
 */
public class Lexer {

	public static enum State {
		NORMAL,
		CHARSET,
		REP,
		CLASS_NAME;
	}

	private State state;

	private final String input;

	private int index;

	private boolean eoi;

	public Lexer(String input) {
		this.input = input;
		this.index = 0;
		this.eoi = false;
		this.state = State.NORMAL;
	}

	/**
	 * Returns the next token. Never returns <code>null</code>, but continually
	 * returns a Token of type {@link TokenType#EOI} once the end of input was
	 * reached.
	 */
	public Token nextToken() {

		if (eoi) {
			return new SpecialToken(index, TokenType.EOI);
		} else if (index >= input.length()) {
			eoi = true;
			return new SpecialToken(index, TokenType.EOI);
		}

		char current = input.charAt(index);
		if (current == '\\') {
			return escapedCharacter();
		} else {
			switch (state) {
			case NORMAL:
				if (current == '.') {
					return new SpecialToken(index++, TokenType.DOT);
				} else if (current == '|') {
					return new SpecialToken(index++, TokenType.PIPE);
				} else if (current == '?') {
					return new SpecialToken(index++, TokenType.OPT);
				} else if (current == '+') {
					return new SpecialToken(index++, TokenType.PLUS);
				} else if (current == '*') {
					return new SpecialToken(index++, TokenType.STAR);
				} else if (current == '(') {
					return new SpecialToken(index++, TokenType.LPAREN);
				} else if (current == ')') {
					return new SpecialToken(index++, TokenType.RPAREN);
				} else if (current == '[') {
					state = State.CHARSET;
					return new SpecialToken(index++, TokenType.LBRACKET);
				} else if (current == '{') {
					state = State.REP;
					return new SpecialToken(index++, TokenType.LBRACE);
				} else if (current == '}') {
					return new SpecialToken(index++, TokenType.RBRACE);
				} else {
					return new CharToken(index++, current);
				}
			case CHARSET:
				if (current == '-') {
					return new SpecialToken(index++, TokenType.DASH);
				} else if (current == ']') {
					state = State.NORMAL;
					return new SpecialToken(index++, TokenType.RBRACKET);
				} else {
					return new CharToken(index++, current);
				}
			case REP:
				if (current == ',') {
					return new SpecialToken(index++, TokenType.COMMA);
				} else if (Character.isDigit(current)) {
					int offset = index;
					int v = 0;
					for (; index < input.length() && Character.isDigit(input.charAt(index)); index++) {
						v = v * 10 + Character.getNumericValue(input.charAt(index));
					}
					return new IntToken(offset, v);
				} else if (current == '}') {
					state = State.NORMAL;
					return new SpecialToken(index++, TokenType.RBRACE);
				} else {
					throw new LexerException("Invalid regular expression at index " + index);
				}
			case CLASS_NAME:
				if (Character.isJavaIdentifierStart(current)) {
					int offset = index;
					StringBuilder builder = new StringBuilder();
					for (; index < input.length() && Character.isJavaIdentifierPart(input.charAt(index)); index++) {
						builder.append(input.charAt(index));
					}
					return new StringToken(offset, builder.toString());
				} else if (current == '}') {
					state = State.NORMAL;
					return new SpecialToken(index++, TokenType.RBRACE);
				} else if (current == '(') {
					return new SpecialToken(index++, TokenType.LPAREN);
				} else if (current == ')') {
					return new SpecialToken(index++, TokenType.RPAREN);
				} else {
					throw new LexerException("Invalid regular expression at index " + index);
				}
			default:
				throw new LexerException("Invalid regular expression at index " + index);
			}
		}

	}

	public State getState() {
		return state;
	}

	private final static String normalToEscape = "|.()[\\?*+{}";
	private final static String charsetToEscape = "-]";

	private Token escapedCharacter() {
		char escaped = input.charAt(index + 1);
		if (escaped == 'd') {
			index++;
			return new SpecialToken(index++, TokenType.NUMBER);
		} else if (escaped == 'w') {
			index++;
			return new SpecialToken(index++, TokenType.WORD_CHARACTER);
		} else if (escaped == 's') {
			index++;
			return new SpecialToken(index++, TokenType.WHITESPACE);
		} else if (escaped == 'b') {
			index++;
			return new SpecialToken(index++, TokenType.WORD_BOUNDARY);
		} else if (escaped == 'p') {
			if (input.charAt(index + 2) == '{') {
				index += 2;
				state = State.CLASS_NAME;
				return new SpecialToken(index++, TokenType.NAMED_CLASS);
			} else {
				throw new LexerException("Invalid escaped character '" + escaped + "' at index " + index);
			}
		} else {
			switch (state) {
			case NORMAL:
				if (normalToEscape.indexOf(escaped) != -1) {
					index++;
					return new CharToken(index++, escaped);
				} else {
					throw new LexerException("Invalid escaped character '" + escaped + "' at index " + index);
				}
			case CHARSET:
				if (charsetToEscape.indexOf(escaped) != -1) {
					index++;
					return new CharToken(index++, escaped);
				} else {
					throw new LexerException("Invalid escaped character '" + escaped + "' at index " + index);
				}
			default:
				// actually this will never happen
				throw new LexerException("Invalid regular expression at index " + index);
			}
		}
	}

}
