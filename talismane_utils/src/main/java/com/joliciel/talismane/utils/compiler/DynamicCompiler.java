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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A compiler for compiling classes whose source code has been constructed dynamically in memory.
 * @author Assaf Urieli
 *
 */
public class DynamicCompiler {
  private static final Logger LOG = LoggerFactory.getLogger(DynamicCompiler.class);
  private JavaCompiler javaCompiler;
  private InMemoryClassLoader inMemoryClassLoader;
  private InMemoryFileManager inMemoryFileManager;
  private DiagnosticListener<JavaFileObject> diagonosticListener;

  public DynamicCompiler(ClassLoader classLoader, DiagnosticListener<JavaFileObject> diagnosticListener) {
    this.javaCompiler = ToolProvider.getSystemJavaCompiler();
    this.diagonosticListener = diagnosticListener;
    JavaFileManager standardFileManager = javaCompiler.getStandardFileManager(diagnosticListener, null, null);
    this.inMemoryClassLoader = new InMemoryClassLoader(classLoader);
    this.inMemoryFileManager = new InMemoryFileManager(standardFileManager, inMemoryClassLoader);
  }

  public Class<?> compile(String name, CharSequence source, List<String> optionList) {
    Map<String,CharSequence> sources = new HashMap<String, CharSequence>();
    sources.put(name, source);
    Map<String, Class<?>> compiledClasses = this.compileMany(sources, optionList);
    Class<?> clazz = compiledClasses.get(name);
    return clazz;
  }

  public Map<String, Class<?>> compileMany(Map<String, CharSequence> sources, List<String> optionList) {
    List<JavaFileObject> sourceObjects = new ArrayList<JavaFileObject>();

    // prepare the JavaFileObjects to be compiled
    for (String qualifiedName : sources.keySet()) {
      CharSequence sourceCode = sources.get(qualifiedName);
      
      String className = qualifiedName;
      String packageName = "";
      int lastDot = qualifiedName.lastIndexOf('.');
      if (lastDot>=0) {
        packageName = qualifiedName.substring(0, lastDot);
        className = qualifiedName.substring(lastDot + 1);
      }
      
      InMemoryJavaFileObject sourceObject = new InMemoryJavaFileObject(className, sourceCode);

      this.inMemoryFileManager.prepareJavaFileObject(StandardLocation.SOURCE_PATH,
          packageName,
          className + ".java",
          sourceObject);
      
      sourceObjects.add(sourceObject);
    }

    // Perform the compilation
    CompilationTask task = this.javaCompiler.getTask(null,
        this.inMemoryFileManager,
        diagonosticListener,
        optionList, null, sourceObjects);

    Boolean result = task.call();
    if (result==null || !result) {
      LOG.info("Compilation failed.");
      for (JavaFileObject sourceObject : sourceObjects)
        System.out.println(((InMemoryJavaFileObject)sourceObject).getCharContent(true));
      throw new DynamicCompilerException("Compilation failed.");
    }

    // Return the compiled classes
    try {
      Map<String, Class<?>> compiledClasses = new HashMap<String, Class<?>>();
      for (String qualifiedName : sources.keySet()) {
        final Class<?> compiledClass = this.inMemoryClassLoader.loadClass(qualifiedName);
        compiledClasses.put(qualifiedName, compiledClass);
      }
      return compiledClasses;
    } catch (ClassNotFoundException e) {
      throw new DynamicCompilerException(e);
    }
  }
}
