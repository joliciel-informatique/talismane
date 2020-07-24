package com.joliciel.talismane;

import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;

public abstract class TalismaneTest {
  @Before
  public void beforeTest() throws Exception {
    TalismaneSession.clearSessions();
  }

  @After
  public void afterTest() throws Exception {
    System.clearProperty("config.file");
    ConfigFactory.invalidateCaches();
  }
}
