//Copyright (C) 2014 Joliciel Informatique
package com.joliciel.talismane;

/**
 * An object which needs a Talismane session id assigned to it.
 * 
 * @author Assaf Urieli
 *
 */
public interface NeedsSessionId {
  public void setSessionId(String sessionId);
}
