package com.joliciel.talismane;

/**
 * A Talismane server, for loading all resources up front and processing sentences received on the fly.
 * @author Assaf Urieli
 *
 */
public interface TalismaneServer extends Talismane {
	/**
	 * The port to listen on - default is 7272.
	 */
	public int getPort();

	public void setPort(int port);

	/**
	 * Whether or not any new connections will be accepted.
	 */
	public boolean isListening();

	public void setListening(boolean listening);

}