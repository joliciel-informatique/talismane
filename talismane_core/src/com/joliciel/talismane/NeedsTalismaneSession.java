//Copyright (C) 2014 Joliciel Informatique
package com.joliciel.talismane;

/**
 * An object which needs a Talismane session assigned to it.
 * @author Assaf Urieli
 *
 */
public interface NeedsTalismaneSession {
	public TalismaneSession getTalismaneSession();
	public void setTalismaneSession(TalismaneSession talismaneSession);
}
