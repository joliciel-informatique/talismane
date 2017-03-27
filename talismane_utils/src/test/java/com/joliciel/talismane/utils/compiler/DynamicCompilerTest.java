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
package com.joliciel.talismane.utils.compiler;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

public class DynamicCompilerTest {
  private static final Logger LOG = LoggerFactory.getLogger(DynamicCompilerTest.class);
  public static class DiagnositicsLogger implements DiagnosticListener<JavaFileObject>
  {
    private List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<Diagnostic<? extends JavaFileObject>>();
    public void report(Diagnostic<? extends JavaFileObject> diagnostic)
    {
      LOG.info("Line Number: " + diagnostic.getLineNumber());
      LOG.info("Code: " + diagnostic.getCode());
      LOG.info("Message: " + diagnostic.getMessage(Locale.ENGLISH));
      LOG.info("Source: " + diagnostic.getSource());
      diagnostics.add(diagnostic);
    }
    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
      return diagnostics;
    }   
  }

  public static interface HelloInterface {
    public String hello(String who);
  }

  @Test
  public void testCompileOneClass() throws Exception {
    ClassLoader classLoader = this.getClass().getClassLoader();
    DiagnositicsLogger diagnosticsLogger = new DiagnositicsLogger();
    
    DynamicCompiler compiler = new DynamicCompiler(classLoader, diagnosticsLogger);

    String src =  "package com.joliciel.talismane.utils.compiler.foo;\n" +
    "import com.joliciel.talismane.utils.compiler.DynamicCompilerTest.HelloInterface;\n" +
    "public class Foo implements HelloInterface {\n" +
    "        public String hello(String who) {\n" +
    "            return \"Hello \" + who;\n" +
    "        }\n" +
    "    }";

    String fullName = "com.joliciel.talismane.utils.compiler.foo.Foo";

    @SuppressWarnings("unchecked")
    Class<HelloInterface> helloClass = (Class<HelloInterface>) compiler.compile(fullName, src, null);

    HelloInterface foo = helloClass.newInstance();
    assertEquals("Hello world", foo.hello("world"));
  }
  
  @Test
  public void testCompileTwoClasses() throws Exception {
    ClassLoader classLoader = this.getClass().getClassLoader();
    DiagnositicsLogger diagnosticsLogger = new DiagnositicsLogger();
    
    DynamicCompiler compiler = new DynamicCompiler(classLoader, diagnosticsLogger);

    String src1 =  "package com.joliciel.talismane.utils.compiler.foo;\n" +
    "import com.joliciel.talismane.utils.compiler.DynamicCompilerTest.HelloInterface;\n" +
    "public class Foo implements HelloInterface {\n" +
    "        public String hello(String who) {\n" +
    "            return \"Hello \" + who;\n" +
    "        }\n" +
    "    }";

    String name1 = "com.joliciel.talismane.utils.compiler.foo.Foo";
    
    String src2 =  "package com.joliciel.talismane.utils.compiler.foo;\n" +
    "import com.joliciel.talismane.utils.compiler.DynamicCompilerTest.HelloInterface;\n" +
    "public class Bar implements HelloInterface {\n" +
    "        public String hello(String who) {\n" +
    "     Foo foo = new Foo();\n" +
    "           return foo.hello(who) + \"!\";\n" +
    "        }\n" +
    "    }";
    
    String name2 =  "com.joliciel.talismane.utils.compiler.foo.Bar";

    Map<String,CharSequence> sources = new LinkedHashMap<String,CharSequence>();
    sources.put(name1, src1);
    sources.put(name2, src2);
    
    Map<String, Class<?>> classes = compiler.compileMany(sources, null);
    @SuppressWarnings("unchecked")
    Class<HelloInterface> helloClass = (Class<HelloInterface>) classes.get(name2);

    HelloInterface bar = helloClass.newInstance();
    assertEquals("Hello world!", bar.hello("world"));
  }
  
  @Test
  public void testCompileError() throws Exception {
    ClassLoader classLoader = this.getClass().getClassLoader();
    DiagnositicsLogger diagnosticsLogger = new DiagnositicsLogger();
    
    DynamicCompiler compiler = new DynamicCompiler(classLoader, diagnosticsLogger);

    String src =  "package com.joliciel.talismane.utils.compiler.foo;\n" +
    "import com.joliciel.talismane.utils.compiler.DynamicCompilerTest.HelloInterface;\n" +
    "public class Foo implements HelloInterface {\n" +
    "        public String hello(String who) {\n" +
    "            return \"Hello \" + who\n" +
    "        }\n" +
    "    }";

    String fullName = "com.joliciel.talismane.utils.compiler.foo.Foo";

    try {
      compiler.compile(fullName, src, null);
      fail("Expected exception");
    } catch (DynamicCompilerException e) {
      LOG.debug(e.getMessage());
    }
    
    Diagnostic<? extends JavaFileObject> diagnostic = diagnosticsLogger.getDiagnostics().get(0);
    assertEquals(5, diagnostic.getLineNumber());
    assertEquals("compiler.err.expected", diagnostic.getCode());
    assertEquals("Foo.java", diagnostic.getSource().getName());
  }

}
