package com.joliciel.talismane.examples;

/**
 * Optional entry point for examples. Usage:<br/>
 * <pre>java -Xmx2G -jar talismane-examples-X.X.Xb.jar &lt;class name&gt; &lt;arguments ...&gt;</pre>2
 * 
 * Example:<br/>
 * <pre>java -Xmx2G -jar talismane-examples-X.X.Xb.jar TalismaneAPITest frenchLanguagePack-X.X.Xb.zip</pre>
 * 
 * @author Assaf Urieli
 *
 */
public class ExamplesMain {

  public static void main(String[] args) throws Exception {
    String className = args[0];
    
    String[] newArgs = new String[args.length-1];
    for (int i=1; i<args.length; i++) {
      newArgs[i-1] = args[i];
    }
    
    if (className.equals(TalismaneAPIExamples.class.getSimpleName())) {
      TalismaneAPIExamples.main(newArgs);
    } else if (className.equals(TalismaneClient.class.getSimpleName())) {
      TalismaneClient.main(newArgs);
    }
  }

}
