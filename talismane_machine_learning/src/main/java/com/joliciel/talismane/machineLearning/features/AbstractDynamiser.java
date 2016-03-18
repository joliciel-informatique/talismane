package com.joliciel.talismane.machineLearning.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.compiler.DynamicCompiler;

public abstract class AbstractDynamiser<T> implements Dynamiser<T> {
	private static final Log LOG = LogFactory.getLog(AbstractDynamiser.class);

	private int classIndex = 0;
	private DynamicCompiler compiler;
	private Class<T> clazz;

	public AbstractDynamiser(Class<T> clazz) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		DiagnositicsLogger diagnosticsLogger = new DiagnositicsLogger();
		
		this.compiler = new DynamicCompiler(classLoader, diagnosticsLogger);
		this.clazz = clazz;
	}
	
	@Override
	public int nextClassIndex() {
		return classIndex++;
	}

	@Override
	public DynamicCompiler getCompiler() {
		return compiler;
	}
	
	public DynamicSourceCodeBuilder<T> getBuilder(Feature<T,?> rootFeature) {
		DynamicSourceCodeBuilderImpl<T> builder = new DynamicSourceCodeBuilderImpl<T>(rootFeature, this);
		builder.setDynamiser(this);
		return builder;
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
	
	public Class<?> getOutcomeType(Feature<T,?> feature) {
		Class<?> outcomeType = null;
		if (feature.getFeatureType().equals(BooleanFeature.class)) {
			outcomeType = Boolean.class;
		} else if (feature.getFeatureType().equals(StringFeature.class)) {
			outcomeType = String.class;
		} else if (feature.getFeatureType().equals(IntegerFeature.class)) {
			outcomeType = Integer.class;
		} else if (feature.getFeatureType().equals(DoubleFeature.class)) {
			outcomeType = Double.class;
		} else {
			outcomeType = this.getOutcomeTypeExtended(feature);
			if (outcomeType==null)
				throw new JolicielException("Unknown feature type: " + feature.getFeatureType());
		}
		return outcomeType;
	}

	/**
	 * Any additional feature return types to be supported by this manager.
	 */
	protected abstract Class<?> getOutcomeTypeExtended(Feature<T, ?> feature);
	
	public Class<T> getContextClass() {
		return clazz;
	}
}
