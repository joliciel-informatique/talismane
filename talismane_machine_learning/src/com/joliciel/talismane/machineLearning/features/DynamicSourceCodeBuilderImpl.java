///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.machineLearning.features;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.compiler.DynamicCompiler;

class DynamicSourceCodeBuilderImpl<T> implements DynamicSourceCodeBuilder<T> {
	private static final Log LOG = LogFactory.getLog(DynamicSourceCodeBuilderImpl.class);
	
	Map<String,Integer> varIndexes = new HashMap<String, Integer>();
	
	StringBuilder methodBuilder = new StringBuilder();
	StringBuilder classBuilder = new StringBuilder();
	
	Map<String,Feature<T,?>> classLevelFeatures = new HashMap<String, Feature<T,?>>();
	int indentation;
	String indentString;
	boolean isDynamic = true;
	Set<Class<?>> imports = new HashSet<Class<?>>();
	boolean separateTopLevelFeatures = false;
	
	Feature<T,?> rootFeature;
	
	Stack<Map<String,String>> variablesInScope = new Stack<Map<String,String>>();

	Dynamiser<T> dynamiser;

	public DynamicSourceCodeBuilderImpl(Feature<T, ?> rootFeature, Dynamiser<T> dynamiser) {
		super();
		if (LOG.isDebugEnabled()) {
			LOG.debug("# Building dynamic code for " + rootFeature.getName());
		}
		
		this.dynamiser = dynamiser;
		this.rootFeature = rootFeature;
		this.indentation = 2;
		this.indentString = "\t\t";
		this.variablesInScope.push(new HashMap<String, String>());
		
		String rootName = this.addFeatureVariable(rootFeature, "root", true);
		
		this.append("if (" + rootName + "!=null)");
		this.indent();
		this.append(	"return this.generateResult(" + rootName + ");");
		this.outdent();
		this.append("else");
		this.indent();
		this.append(	"return null;");
		this.outdent();
	}


	@Override
	public String addFeatureVariable(Feature<T, ?> feature, String nameBase) {
		return this.addFeatureVariable(feature, nameBase, false);
	}

	String addFeatureVariable(Feature<T, ?> feature, String nameBase, boolean isRoot) {
		if (LOG.isTraceEnabled())
			LOG.trace("addArgument " + feature.getName());
		String variableName = this.findVariableInScope(feature);
		if (variableName!=null) {
			if (LOG.isTraceEnabled())
				LOG.trace("found variable: " + variableName);
			return variableName;
		}
		variableName = this.getVarName(nameBase);
		
		this.append("// Start " + feature.getClass().getSimpleName() + ": " + feature.getName());
		
		Map<String,String> varsInCurrentScope = variablesInScope.peek();
		varsInCurrentScope.put(feature.getName(), variableName);
		
		Class<?> outcomeType = this.dynamiser.getOutcomeType(feature);
		this.addImport(outcomeType);
		String returnType = outcomeType.getSimpleName();
		this.append(returnType + " " + variableName + "=null;");

		boolean codeAdded = false;
		if (separateTopLevelFeatures && feature.isTopLevelFeature()) {
			// do nothing
		} else {
			// if separateTopLevelFeatures=true,
			// only add source code for features the user has defined at the top-level
			// presumably because they are the most reusable, therefore worth caching
			codeAdded = feature.addDynamicSourceCode(this, variableName);
		}

		if (!codeAdded) {
			if (isRoot) {
				isDynamic = false;
			} else {
				this.append("FeatureResult<" + returnType + "> " + variableName + "Result=" + variableName+"Feature.check(context,env);");
				this.append("if (" + variableName + "Result!=null)" + variableName + "=" + variableName + "Result.getOutcome();");
				classLevelFeatures.put(variableName + "Feature", feature);
				this.dynamise(feature);
			}
		}
		
		this.append("// End " + feature.getClass().getSimpleName() + ": " + feature.getName());

		return variableName;
	}

	String findVariableInScope(Feature<T, ?> feature) {
		String key = feature.getName();
		String variableName = null;
		for (Map<String,String> featureVarMap : variablesInScope) {
			for (String featureName : featureVarMap.keySet()) {
				if (featureName.equals(key)) {
					variableName = featureVarMap.get(featureName);
					break;
				}
			}
			if (variableName!=null)
				break;
		}
		
		return variableName;
	}
	
	@Override
	public Feature<T, ?> getFeature() {
		if (isDynamic) {
			// this feature implemented addDynamicSourceCode
			// construct the class code surrounding the checkInternal method
			
			if (LOG.isDebugEnabled())
				LOG.debug("Building class for " + rootFeature.getName());
			
			this.indentation = 0;
			indentString = "";

			String packageName = this.rootFeature.getClass().getPackage().getName() + ".runtime";
			String className = this.rootFeature.getClass().getSimpleName() + "_" + dynamiser.nextClassIndex();		
			String qualifiedName = packageName + "." + className;
			Class<?> outcomeType = this.dynamiser.getOutcomeType(rootFeature);
			String returnType = outcomeType.getSimpleName();
			String contextType = this.dynamiser.getContextClass().getSimpleName();

			this.appendToClass("package " + packageName + ";");
			
			imports.add(Feature.class);
			imports.add(FeatureResult.class);
			imports.add(RuntimeEnvironment.class);
			
			imports.add(this.dynamiser.getContextClass());

			imports.add(this.rootFeature.getFeatureType());
			
			Class<?> parentClass = null;
			String checkMethodSuffix = "";
			if (AbstractCachableFeature.class.isAssignableFrom(rootFeature.getClass())) {
				parentClass = AbstractCachableFeature.class;
				checkMethodSuffix = "Internal";
			} else if (AbstractMonitorableFeature.class.isAssignableFrom(rootFeature.getClass())) {
				parentClass = AbstractMonitorableFeature.class;
				checkMethodSuffix = "Internal";
			} else {
				parentClass = AbstractFeature.class;
			}
			imports.add(parentClass);
			
			for (Feature<T,?> classLevelFeature : classLevelFeatures.values()) {
				imports.add(classLevelFeature.getFeatureType());
			}

			Set<String> importNames = new TreeSet<String>();
			for (Class<?> oneImport : imports) {
				if (!oneImport.getName().startsWith("java.lang."))
					importNames.add(oneImport.getName());
			}
			
			for (String importName : importNames) {
				this.appendToClass("import " + importName + ";");
			}

			String implementedInterface = this.rootFeature.getFeatureType().getSimpleName();
			if (this.rootFeature.getFeatureType().getTypeParameters().length>0)
				implementedInterface += "<" + contextType + ">";
			
			this.appendToClass("public final class " + className + " extends " + parentClass.getSimpleName() + "<" + contextType + ", " + returnType + "> implements " + implementedInterface + " {");
			this.indent();

			for (Entry<String,Feature<T,?>> classLevelFeatureEntry : classLevelFeatures.entrySet()) {
				String argumentName = classLevelFeatureEntry.getKey();
				Feature<T,?> argument = classLevelFeatureEntry.getValue();
				String argumentType = argument.getFeatureType().getSimpleName();
				
				String argumentNameInitialCaps = Character.toUpperCase(argumentName.charAt(0)) + argumentName.substring(1);
				
				if (argument.getFeatureType().getTypeParameters().length==0) {
					this.appendToClass("private " + argumentType + " " + argumentName + ";");
					this.appendToClass("public " + argumentType + " get" + argumentNameInitialCaps + "() { return " + argumentName + "; }");
					this.appendToClass("public void set" + argumentNameInitialCaps + "(" + argumentType + " value) { " + argumentName + "=value; }");
				} else {
					this.appendToClass("private " + argumentType + "<" + contextType + "> " + argumentName + ";");
					this.appendToClass("public " + argumentType + "<" + contextType + "> get" + argumentNameInitialCaps + "() { return " + argumentName + "; }");
					this.appendToClass("public void set" + argumentNameInitialCaps + "(" + argumentType + "<" + contextType + "> value) { " + argumentName + "=value; }");
				}
			}

			this.appendToClass("public FeatureResult<" + returnType + "> check" + checkMethodSuffix + "(" + contextType + " context, RuntimeEnvironment env) {");
			this.indent();

			this.classBuilder.append(methodBuilder);

			this.outdent();
			this.appendToClass("}");
			
			this.appendToClass("@SuppressWarnings({ \"rawtypes\" })");
			this.appendToClass("@Override");
			this.appendToClass("public Class<? extends Feature> getFeatureType() {");
			this.indent();
			this.appendToClass("return " + this.rootFeature.getFeatureType().getSimpleName() + ".class;");
			this.outdent();
			this.appendToClass("}");
			
			this.outdent();
			this.appendToClass("}");

			if (LOG.isTraceEnabled()) {
				// write the class to the temp directory if trace is enabled
				try {
		    	    File tempFile = File.createTempFile(className + "_", ".java"); 
		 
		    	    Writer tempFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile, false),"UTF8"));
		    	    tempFileWriter.write(this.classBuilder.toString());
		    	    tempFileWriter.flush();
		    	    tempFileWriter.close();
				} catch (IOException ioe) {
					LogUtils.logError(LOG, ioe);
				}
			}
    	    
			DynamicCompiler compiler = this.dynamiser.getCompiler();

			List<String> optionList = new ArrayList<String>();
//			optionList.add("-Xlint:unchecked");
//			optionList.add("-verbose");

			@SuppressWarnings("unchecked")
			Class<Feature<T, ?>> newFeatureClass = (Class<Feature<T, ?>>) compiler.compile(qualifiedName, this.classBuilder, optionList);

			// instantiate the feature
			Feature<T, ?> newFeature = null;
			try {
				newFeature = newFeatureClass.newInstance();
			} catch (InstantiationException e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}

			// assign the dependencies
			for (Entry<String,Feature<T,?>> classLevelFeatureEntry : classLevelFeatures.entrySet()) {
				String argumentName = classLevelFeatureEntry.getKey();
				Feature<T,?> argument = classLevelFeatureEntry.getValue();
				String argumentNameInitialCaps = Character.toUpperCase(argumentName.charAt(0)) + argumentName.substring(1);

				Method method;

				try {
					method = newFeature.getClass().getMethod("set" + argumentNameInitialCaps, argument.getFeatureType());
				} catch (SecurityException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				} catch (NoSuchMethodException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				}

				try {
					method.invoke(newFeature, argument);
				} catch (IllegalArgumentException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				}
			}		
			newFeature.setName(rootFeature.getName());
			newFeature.setCollectionName(rootFeature.getCollectionName());

			return newFeature;
		} else {
			// this feature didn't implement addDynamicSourceCode - dynamise its arguments
			if (LOG.isDebugEnabled())
				LOG.debug("No dynamic code for " + rootFeature.getName());

			this.dynamise(rootFeature);
			return rootFeature;
		}
	}

	/**
	 * Replace static arguments by their dynamic equivalents.
	 * @param feature
	 */
	void dynamise(Feature<T,?> feature) {
		try {
			if (LOG.isDebugEnabled())
				LOG.debug("Dynamising feature " + feature.getName());
			
			Method[] methods = feature.getClass().getDeclaredMethods();
			Map<String, Method> getMethods = new HashMap<String, Method>();
			Map<String, Method> setMethods = new HashMap<String, Method>();
			Map<String, Method> getArrayMethods = new HashMap<String, Method>();
			for (Method method : methods) {
				if (method.getName().startsWith("get")) {
					if (Feature.class.isAssignableFrom(method.getReturnType())) {
						getMethods.put(method.getName().substring(3), method);
					} else if (method.getReturnType().isArray() && Feature.class.isAssignableFrom(method.getReturnType().getComponentType())) {
						getArrayMethods.put(method.getName().substring(3), method);
					}
				} else if (method.getName().startsWith("set")) {
					if (Feature.class.isAssignableFrom(method.getParameterTypes()[0])) {
						setMethods.put(method.getName().substring(3), method);
					}
				}
			}

			for (String getMethodKey : getMethods.keySet()) {
				Method getMethod = getMethods.get(getMethodKey);
				Method setMethod = setMethods.get(getMethodKey);
				if (setMethod!=null) {
					if (LOG.isTraceEnabled())
						LOG.trace("Dynamising argument: " + getMethod.getName());
					
					@SuppressWarnings("unchecked")
					Feature<T,?> argument = (Feature<T, ?>) getMethod.invoke(feature);
					DynamicSourceCodeBuilder<T> argumentBuilder = this.dynamiser.getBuilder(argument);
					Feature<T,?> dynamicArgument = argumentBuilder.getFeature();
					setMethod.invoke(feature, dynamicArgument);
				}
			}
			
			for (String getArrayMethodKey : getArrayMethods.keySet()) {
				Method getArrayMethod = getArrayMethods.get(getArrayMethodKey);

				@SuppressWarnings("unchecked")
				Feature<T,?>[] argumentArray = (Feature<T, ?>[]) getArrayMethod.invoke(feature);
				@SuppressWarnings("unchecked")
				Feature<T,?>[] dynamisedArgumentArray = new Feature[argumentArray.length];
				int i=0;
				for (Feature<T,?> argument : argumentArray) {
					DynamicSourceCodeBuilder<T> argumentBuilder = this.dynamiser.getBuilder(argument);
					Feature<T,?> dynamicArgument = argumentBuilder.getFeature();
					dynamisedArgumentArray[i++] = dynamicArgument;
				}

				for (i=0;i<argumentArray.length;i++) {
					argumentArray[i] = dynamisedArgumentArray[i];
				}
			}
		} catch (IllegalArgumentException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

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

	public void appendToClass(String string) {
		this.classBuilder.append(indentString + string + "\n");
	}
	
	@Override
	public void append(String string) {
		this.methodBuilder.append(indentString + string + "\n");
		int openBrackets = string.indexOf('{');
		int closeBrackets = string.indexOf('}');
		if (openBrackets>=0 && closeBrackets>=0) {
			if (closeBrackets < openBrackets) {
				variablesInScope.pop();
				variablesInScope.push(new HashMap<String, String>());
			}
		} else if (openBrackets>=0) {
			variablesInScope.push(new HashMap<String, String>());
		} else if (closeBrackets>=0) {
			variablesInScope.pop();
		}
	}

	@Override
	public void indent() {
		indentation++;
		indentString += "\t";
	}

	@Override
	public void outdent() {
		if (indentation>0) {
			indentation--;
			indentString = indentString.substring(0, indentString.length()-1);
		}
	}


	public Dynamiser<T> getDynamiser() {
		return dynamiser;
	}


	public void setDynamiser(Dynamiser<T> dynamiser) {
		this.dynamiser = dynamiser;
	}

	public String getVarName(String base) {
		Integer varIndexObj = varIndexes.get(base);
		int varIndex = 1;
		if (varIndexObj!=null) {
			varIndex = varIndexObj.intValue()+1;
		}
		varIndexes.put(base, varIndex);
		return base + varIndex;
	}


	@Override
	public void addImport(Class<?> importClass) {
		imports.add(importClass);
	}
}
