/*
 * Created on 21 Jan 2010
 */
package com.joliciel.frenchTreebank;

public class EntityNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -3790802787786313799L;
	public EntityNotFoundException() { super(); }
    public EntityNotFoundException(String s) { super(s); }
    public EntityNotFoundException(Exception e) { super(e); }

}
