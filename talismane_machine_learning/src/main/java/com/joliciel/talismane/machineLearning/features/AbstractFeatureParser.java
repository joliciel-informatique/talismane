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
package com.joliciel.talismane.machineLearning.features;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * An abstract base class for feature parsers,
 * which simplifies the parsing by performing reflection on the feature classes corresponding to function names.
 * @author Assaf Urieli
 *
 */
public abstract class AbstractFeatureParser<T> implements FeatureParserInternal<T>, FeatureClassContainer {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractFeatureParser.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(AbstractFeatureParser.class);

	private FeatureService featureService;
	private ExternalResourceFinder externalResourceFinder;
	private Map<String,List<Feature<T, ?>>> namedFeatures = new HashMap<String, List<Feature<T,?>>>();
	private Map<String,List<Feature<T, ?>>> featureGroups = new HashMap<String, List<Feature<T,?>>>();
	private Map<String,NamedFeatureWithParameters> namedFeaturesWithParameters = new HashMap<String, NamedFeatureWithParameters>();
	private Map<String,List<Feature<T, ?>>> parsedFeatures = new HashMap<String, List<Feature<T,?>>>();
	
	@SuppressWarnings("rawtypes")
	private Map<String,List<Class<? extends Feature>>> featureClasses = null;
	@SuppressWarnings("rawtypes")
	private Map<Class<? extends Feature>,List<String>> featureClassDescriptors = null;
	@SuppressWarnings("rawtypes")
	private Map<Class<? extends Feature>, Constructor<? extends Feature>[]> featureConstructors = null;
	
	private Dynamiser<T> dynamiser = null;
	
	public AbstractFeatureParser(FeatureService featureService) {
		super();
		this.featureService = featureService;
	}

	@SuppressWarnings("rawtypes")
	final void addFeatureClassesInternal() {
		if (featureClasses==null) {
			// note: for a given classname with both IntegerFeature and DoubleFeature arguments,
			// the version with the IntegerFeature arguments should always be added first.
			featureClasses = new HashMap<String, List<Class<? extends Feature>>>();
			featureClassDescriptors = new HashMap<Class<? extends Feature>, List<String>>();
			featureConstructors = new HashMap<Class<? extends Feature>, Constructor<? extends Feature>[]>();
			
			this.addFeatureClass("RootWrapper", RootWrapper.class);
			this.addFeatureClass("-", MinusIntegerOperator.class);
			this.addFeatureClass("-", MinusOperator.class);
			this.addFeatureClass("+", PlusIntegerOperator.class);
			this.addFeatureClass("+", PlusOperator.class);
			this.addFeatureClass("*", MultiplyIntegerOperator.class);
			this.addFeatureClass("*", MultiplyOperator.class);
			this.addFeatureClass("/", DivideOperator.class);
			this.addFeatureClass("%", ModuloOperator.class);
			this.addFeatureClass("==", EqualsOperatorForString.class);
			this.addFeatureClass("==", EqualsOperatorForInteger.class);
			this.addFeatureClass("==", EqualsOperatorForDouble.class);
			this.addFeatureClass("==", EqualsOperatorForBoolean.class);
			this.addFeatureClass("!=", NotEqualsOperator.class);
			this.addFeatureClass(">", GreaterThanIntegerOperator.class);
			this.addFeatureClass(">", GreaterThanOperator.class);
			this.addFeatureClass(">=", GreaterThanOrEqualsIntegerOperator.class);
			this.addFeatureClass(">=", GreaterThanOrEqualsOperator.class);
			this.addFeatureClass("<", LessThanIntegerOperator.class);
			this.addFeatureClass("<", LessThanOperator.class);
			this.addFeatureClass("<=", LessThanOrEqualsIntegerOperator.class);
			this.addFeatureClass("<=", LessThanOrEqualsOperator.class);
			this.addFeatureClass("&", AndFeatureAllowNulls.class);
			this.addFeatureClass("&&", AndFeature.class);
			this.addFeatureClass("|", OrFeatureAllowNulls.class);
			this.addFeatureClass("||", ConcatenateFeature.class);
			this.addFeatureClass("||", OrFeature.class);
			this.addFeatureClass("And", AndFeature.class);
			this.addFeatureClass("AndAllowNulls", AndFeatureAllowNulls.class);
			this.addFeatureClass("Concat", ConcatenateFeature.class);
			this.addFeatureClass("ConcatWithNulls", ConcatenateWithNullsFeature.class);
			this.addFeatureClass("EndsWith", EndsWithFeature.class);
			this.addFeatureClass("ExternalResource", ExternalResourceFeature.class);
			this.addFeatureClass("ExternalResourceDouble", ExternalResourceDoubleFeature.class);
			this.addFeatureClass("Graduate", GraduateFeature.class);
			this.addFeatureClass("IfThenElse", IfThenElseStringFeature.class);
			this.addFeatureClass("IfThenElse", IfThenElseIntegerFeature.class);
			this.addFeatureClass("IfThenElse", IfThenElseDoubleFeature.class);
			this.addFeatureClass("IfThenElse", IfThenElseBooleanFeature.class);
			this.addFeatureClass("InSet", StringInSetFeature.class);
			this.addFeatureClass("Integer", IntegerLiteralFeatureWrapper.class);
			this.addFeatureClass("Inverse", InverseFeature.class);
			this.addFeatureClass("IsNull", IsNullFeature.class);
			this.addFeatureClass("MultivaluedExternalResource", MultivaluedExternalResourceFeature.class);
			this.addFeatureClass("Normalise", NormaliseFeature.class);
			this.addFeatureClass("Not", NotFeature.class);
			this.addFeatureClass("NullIf", NullIfStringFeature.class);
			this.addFeatureClass("NullIf", NullIfIntegerFeature.class);
			this.addFeatureClass("NullIf", NullIfDoubleFeature.class);
			this.addFeatureClass("NullIf", NullIfBooleanFeature.class);
			this.addFeatureClass("NullToFalse", NullToFalseFeature.class);
			this.addFeatureClass("OnlyTrue", OnlyTrueFeature.class);
			this.addFeatureClass("Or", OrFeature.class);
			this.addFeatureClass("OrAllowNulls", OrFeatureAllowNulls.class);
			this.addFeatureClass("Round", RoundFeature.class);
			this.addFeatureClass("StartsWith", StartsWithFeature.class);
			this.addFeatureClass("ToString", ToStringFeature.class);
			this.addFeatureClass("ToStringAllowNulls", ToStringAllowNullsFeature.class);
			this.addFeatureClass("Truncate", TruncateFeature.class);
			
			this.addFeatureClasses(this);
			
			this.addFeatureClass("IfThenElse", IfThenElseGenericFeature.class);
			this.addFeatureClass("NullIf", NullIfGenericFeature.class);
			
		}
	}

	/**
	 * Get the features corresponding to a particular descriptor by performing
	 * reflection on the corresponding feature class to be instantiated.
	 */
	final List<Feature<T, ?>> getFeatures(FunctionDescriptor descriptor, @SuppressWarnings("rawtypes") Class<? extends Feature> featureClass, FunctionDescriptor topLevelDescriptor) {
		MONITOR.startTask("getFeatures");
		try {
			if (featureClass==null)
				throw new FeatureSyntaxException("No class provided for", descriptor, topLevelDescriptor);
			
			List<Feature<T,?>> features = new ArrayList<Feature<T,?>>();
			int i = 0;
			List<List<Object>> argumentLists = new ArrayList<List<Object>>();
			List<Object> initialArguments = new ArrayList<Object>();
			argumentLists.add(initialArguments);
			
			for (FunctionDescriptor argumentDescriptor : descriptor.getArguments()) {
				List<List<Object>> newArgumentLists = new ArrayList<List<Object>>();
				for (List<Object> arguments : argumentLists) {
					if (!argumentDescriptor.isFunction()) {
						Object literal = argumentDescriptor.getObject();
						Object convertedObject = literal;
						if (literal instanceof String) {
							StringLiteralFeature<T> stringLiteralFeature = new StringLiteralFeature<T>((String) literal);
							convertedObject = stringLiteralFeature;
						} else if (literal instanceof Boolean) {
							BooleanLiteralFeature<T> booleanLiteralFeature = new BooleanLiteralFeature<T>((Boolean) literal);
							convertedObject = booleanLiteralFeature;
						} else if (literal instanceof Double) {
							DoubleLiteralFeature<T> doubleLiteralFeature = new DoubleLiteralFeature<T>((Double) literal);
							convertedObject = doubleLiteralFeature;
						} else if (literal instanceof Integer) {
							IntegerLiteralFeature<T> integerLiteralFeature = new IntegerLiteralFeature<T>((Integer) literal);
							convertedObject = integerLiteralFeature;
						} else {
							// do nothing - this was some sort of other object added by getModifiedDescriptors that should
							// be handled as is.
						}
						arguments.add(convertedObject);
						newArgumentLists.add(arguments);
						
					} else {
						List<Feature<T,?>> featureArguments = this.parseInternal(argumentDescriptor, topLevelDescriptor);
						// a single argument descriptor could produce multiple arguments
						// e.g. when a function with an array argument is mapped onto multiple function calls
						for (Feature<T,?> featureArgument : featureArguments) {
							List<Object> newArguments = new ArrayList<Object>(arguments);
							newArguments.add(featureArgument);
							newArgumentLists.add(newArguments);
						}
					} // function or object?
	
				} // next argument list (under construction from original arguments)
				argumentLists = newArgumentLists;
			} // next argument
			
			for (List<Object> originalArgumentList : argumentLists) {
				// add the argument types (i.e. classes)
				// and convert arrays to multiple constructor calls
				List<Object[]> argumentsList = new ArrayList<Object[]>();
				argumentsList.add(new Object[originalArgumentList.size()]);
				
				Class<?>[] argumentTypes = new Class<?>[originalArgumentList.size()];
				List<Object[]> newArgumentsList = new ArrayList<Object[]>();
				for (i=0;i<originalArgumentList.size();i++) {
					Object arg = originalArgumentList.get(i);
					
					if (arg.getClass().isArray()) {
						// arrays represent multiple constructor calls
						Object[] argArray = (Object[]) arg;
						for (Object oneArg : argArray) {
							for (Object[] arguments : argumentsList) {
								Object[] newArguments = arguments.clone();
								newArguments[i] = oneArg;
								newArgumentsList.add(newArguments);
							}
						}
						argumentTypes[i] = arg.getClass().getComponentType();
					} else {
						for (Object[] myArguments : argumentsList) {
							newArgumentsList.add(myArguments);
							myArguments[i] = arg;
						}
						argumentTypes[i] = arg.getClass();
					}
					argumentsList = newArgumentsList;
					newArgumentsList = new ArrayList<Object[]>();
				} // next argument
				
				@SuppressWarnings("rawtypes")
				Constructor<? extends Feature> constructor = null;
				MONITOR.startTask("findContructor");
				try {
					constructor = this.getMatchingAccessibleConstructor(featureClass, argumentTypes);
									
					if (constructor==null) {
						@SuppressWarnings("rawtypes")
						Constructor<? extends Feature>[] constructors = this.featureConstructors.get(featureClass);
						
						// check if there's a variable argument constructor
						for (Constructor<?> oneConstructor : constructors) {
							Class<?>[] parameterTypes = oneConstructor.getParameterTypes();
							
							if (parameterTypes.length>=1 && argumentsList.size()==1 && argumentsList.get(0).length>=parameterTypes.length) {
								Object[] arguments = argumentsList.get(0);
								Class<?> parameterType = parameterTypes[parameterTypes.length-1];
								if (parameterType.isArray()) {
									// assume it's a variable-argument constructor
									// build the argument for this constructor
									// find a common type for all of the arguments.
									Object argument = arguments[parameterTypes.length-1];
									Class<?> clazz = null;
									if (argument instanceof StringFeature)
										clazz = StringFeature.class;
									else if (argument instanceof BooleanFeature)
										clazz = BooleanFeature.class;
									else if (argument instanceof DoubleFeature)
										clazz = DoubleFeature.class;
									else if (argument instanceof IntegerFeature)
										clazz = IntegerFeature.class;
									else if (argument instanceof StringCollectionFeature)
										clazz = StringFeature.class;
									else {
										// no good, can't make arrays of this type
										continue;
									}
										
									Object[] argumentArray = (Object[]) Array.newInstance(clazz, (arguments.length-parameterTypes.length)+1);
									int j = 0;
									for (int k=parameterTypes.length-1; k<arguments.length; k++) {
										Object oneArgument = arguments[k];
										if (oneArgument instanceof StringCollectionFeature) {
											@SuppressWarnings("unchecked")
											StringCollectionFeature<T> stringCollectionFeature = (StringCollectionFeature<T>) oneArgument;
											StringCollectionFeatureProxy<T> proxy = new StringCollectionFeatureProxy<T>(stringCollectionFeature);
											oneArgument = proxy;
										}
										if (!clazz.isAssignableFrom(oneArgument.getClass())) {
											throw new FeatureSyntaxException("Mismatched array types: " + clazz.getSimpleName() + ", " + oneArgument.getClass().getSimpleName(), descriptor, topLevelDescriptor);
										}
										argumentArray[j++] = oneArgument;
									} // next argument
									
									Class<?>[] argumentTypesWithArray = new Class<?>[parameterTypes.length];
									for (int k=0;k<parameterTypes.length-1;k++) {
										Object oneArgument = arguments[k];
										argumentTypesWithArray[k] = oneArgument.getClass();
									}
									argumentTypesWithArray[argumentTypesWithArray.length-1] = argumentArray.getClass();
									constructor = this.getMatchingAccessibleConstructor(featureClass, argumentTypesWithArray);
									
									if (constructor!=null) {
										argumentsList = new ArrayList<Object[]>();
										Object[] argumentsWithArray = new Object[parameterTypes.length];
	 									for (int k=0;k<parameterTypes.length-1;k++) {
											Object oneArgument = arguments[k];
											argumentsWithArray[k] = oneArgument;
										}
	 									argumentsWithArray[parameterTypes.length-1] = argumentArray;
										argumentsList.add(argumentsWithArray);
										break;
									}
								} // constructor takes an array
							} // exactly one parameter for constructor
						} // next constructor
						
						if (constructor==null) {
							// See if various conversions allow us to find a constructor
							// Integer to Double
							// StringCollectionFeature to StringFeature
							for (Constructor<?> oneConstructor : constructors) {
								Class<?>[] parameterTypes = oneConstructor.getParameterTypes();
								boolean isMatchingConstructor = false;
								List<Integer> intParametersToConvert = new ArrayList<Integer>();
								List<Integer> stringCollectionParametersToConvert = new ArrayList<Integer>();
								List<Integer> customParametersToConvert = new ArrayList<Integer>();
								if (parameterTypes.length==argumentTypes.length) {
									int j=0;
									isMatchingConstructor = true;
									
									for (Class<?> parameterType : parameterTypes) {
										if (parameterType.isAssignableFrom(argumentTypes[j])&& !StringCollectionFeature.class.isAssignableFrom(argumentTypes[j])) {
											// nothing to do here
										} else if (parameterType.equals(DoubleFeature.class) && IntegerFeature.class.isAssignableFrom(argumentTypes[j])) {
											intParametersToConvert.add(j);
										} else if ((parameterType.equals(StringFeature.class)||parameterType.equals(Feature.class)) && StringCollectionFeature.class.isAssignableFrom(argumentTypes[j])) {
											stringCollectionParametersToConvert.add(j);
										} else if (this.canConvert(parameterType, argumentTypes[j])) {
											customParametersToConvert.add(j);
										} else {
											isMatchingConstructor = false;
											break;
										}
										j++;
									}
								}
								if (isMatchingConstructor) {
									@SuppressWarnings({ "rawtypes", "unchecked" })
									Constructor<? extends Feature> matchingConstructor = (Constructor<? extends Feature>) oneConstructor;
									constructor = matchingConstructor;
									
									for (Object[] myArguments : argumentsList) {
										for (int indexToConvert : intParametersToConvert) {
											@SuppressWarnings("unchecked")
											IntegerFeature<T> integerFeature = (IntegerFeature<T>) myArguments[indexToConvert];
											IntegerToDoubleFeature<T> intToDoubleFeature = new IntegerToDoubleFeature<T>(integerFeature);
											myArguments[indexToConvert] = intToDoubleFeature;
										}
										for (int indexToConvert : stringCollectionParametersToConvert) {
											@SuppressWarnings("unchecked")
											StringCollectionFeature<T> stringCollectionFeature = (StringCollectionFeature<T>) myArguments[indexToConvert];
											StringCollectionFeatureProxy<T> proxy = new StringCollectionFeatureProxy<T>(stringCollectionFeature);
											myArguments[indexToConvert] = proxy;
										}
										for (int indexToConvert : customParametersToConvert) {
											@SuppressWarnings("unchecked")
											Feature<T,?> argumentToConvert = (Feature<T, ?>) myArguments[indexToConvert];
											Feature<T,?> customArgument = this.convertArgument(parameterTypes[indexToConvert], argumentToConvert);
											myArguments[indexToConvert] = customArgument;
											customArgument.addArgument(argumentToConvert);
										}
									}
									break;
								} // found a matching constructor
							} // next possible constructor
						} // still haven't found a constructor, what next?
					} // didn't find a constructor yet
				} finally {
					MONITOR.endTask();
				}
				
				if (constructor==null)
					throw new NoConstructorFoundException("No constructor found for " + descriptor.getFunctionName() + " (" + featureClass.getName() + ") matching the arguments provided", descriptor, topLevelDescriptor);			
				
				for (Object[] myArguments : argumentsList) {
					@SuppressWarnings("rawtypes")
					Feature feature;
					try {
						feature = constructor.newInstance(myArguments);
					} catch (IllegalArgumentException e) {
						throw new RuntimeException(e);
					} catch (InstantiationException e) {
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e);
					}
					
					@SuppressWarnings("unchecked")
					Feature<T,?> genericFeature = (Feature<T,?>) feature;
					this.injectDependencies(feature);
					if (genericFeature instanceof ExternalResourceFeature) {
						if (this.getExternalResourceFinder()==null) {
							throw new JolicielException("No external resource finder set.");
						}
						@SuppressWarnings("unchecked")
						ExternalResourceFeature<T> externalResourceFeature = (ExternalResourceFeature<T>) genericFeature;
						externalResourceFeature.setExternalResourceFinder(this.getExternalResourceFinder());
					} else if (genericFeature instanceof ExternalResourceDoubleFeature) {
						if (this.getExternalResourceFinder()==null) {
							throw new JolicielException("No external resource finder set.");
						}
						@SuppressWarnings("unchecked")
						ExternalResourceDoubleFeature<T> externalResourceFeature = (ExternalResourceDoubleFeature<T>) genericFeature;
						externalResourceFeature.setExternalResourceFinder(this.getExternalResourceFinder());
					} else if (genericFeature instanceof MultivaluedExternalResourceFeature) {
						if (this.getExternalResourceFinder()==null) {
							throw new JolicielException("No external resource finder set.");
						}
						@SuppressWarnings("unchecked")
						MultivaluedExternalResourceFeature<T> externalResourceFeature = (MultivaluedExternalResourceFeature<T>) genericFeature;
						externalResourceFeature.setExternalResourceFinder(this.getExternalResourceFinder());
					} 
	
					// add this feature's arguments
					for (Object argument : myArguments) {
						if (argument instanceof Feature[]) {
							@SuppressWarnings("unchecked")
							Feature<T,?>[] featureArray = (Feature<T,?>[]) argument;
							for (Feature<T,?> oneFeature : featureArray) {
								genericFeature.addArgument(oneFeature);
							}
						} else {
							@SuppressWarnings("unchecked")
							Feature<T,?> featureArgument = (Feature<T,?>) argument;
							genericFeature.addArgument(featureArgument);
						}
					}
					
					Feature<T,?> convertedFeature = this.convertFeature(genericFeature);
					features.add(convertedFeature);
				} // next internal argument list
			} // next argument list
			return features;
		} finally {
			MONITOR.endTask();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private Constructor<? extends Feature> getMatchingAccessibleConstructor(
			Class<? extends Feature> featureClass, Class<?>[] argumentTypes) {
		MONITOR.startTask("getMatchingAccessibleConstructor");
		try {
			Constructor<? extends Feature> constructor = null;
			for (Constructor<? extends Feature> oneConstructor : this.featureConstructors.get(featureClass)) {
				Class<?>[] parameters = oneConstructor.getParameterTypes();
				if (parameters.length != argumentTypes.length)
					continue;
				boolean foundConstructor = true;
				for (int j=0; j<parameters.length; j++) {
					Class<?> parameter = parameters[j];
					Class<?> argumentType = argumentTypes[j];
					if (parameter.isArray() && !argumentType.isArray()) {
						foundConstructor = false;
						break;
					} else if (!parameter.isAssignableFrom(argumentType)) {
						foundConstructor = false;
						break;
					}
				}
				if (foundConstructor) {
					constructor = oneConstructor;
					break;
				}
			}
			return constructor;
		} finally {
			MONITOR.endTask();
		}
	}

	/**
	 * Is it possible to convert a given argument type to the requested parameter type?
	 */
	protected abstract boolean canConvert(Class<?> parameterType, Class<?> originalArgumentType);

	/**
	 * If canConvert returned true, convert the original argument to the requested parameter type.
	 */
	protected abstract Feature<T,?> convertArgument(Class<?> parameterType, Feature<T,?> originalArgument);
	
	/**
	 * Add the feature return-type interface if required,
	 * so that the feature can be used as an argument for features requiring this return type.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	Feature<T, ?> convertFeature(Feature<T,?> feature) {
		Feature<T, ?> convertedFeature = feature;
		if (feature.getFeatureType().equals(StringFeature.class) && !(feature instanceof StringFeature)) {
			convertedFeature = new StringFeatureWrapper(feature);
		} else if (feature.getFeatureType().equals(BooleanFeature.class) && !(feature instanceof BooleanFeature)) {
			convertedFeature = new BooleanFeatureWrapper(feature);
		} else if (feature.getFeatureType().equals(DoubleFeature.class) && !(feature instanceof DoubleFeature)) {
			convertedFeature = new DoubleFeatureWrapper(feature);
		} else if (feature.getFeatureType().equals(IntegerFeature.class) && !(feature instanceof IntegerFeature)) {
			convertedFeature = new IntegerFeatureWrapper(feature);
		} else {
			convertedFeature = this.convertFeatureCustomType(feature);
			if (convertedFeature==null)
				convertedFeature = feature;
		}
		return convertedFeature;
	}
	
	/**
	 * Add the feature return-type interface if required,
	 * so that the feature can be used as an argument for features requiring this return type.
	 */
	public abstract Feature<T, ?> convertFeatureCustomType(Feature<T,?> feature);
	
	/**
	 * Inject any dependencies required by this feature to function correctly.
	 */
	public abstract void injectDependencies(@SuppressWarnings("rawtypes") Feature feature);
	
	@SuppressWarnings("unchecked")
	@Override
	public final List<Feature<T, ?>> parse(FunctionDescriptor descriptor) {
		MONITOR.startTask("parse");
		try {
			LOG.debug("Parsing: " + descriptor.toString());
			LOG.debug("Name: " + descriptor.getDescriptorName());
			this.addFeatureClassesInternal();
			List<Feature<T, ?>> features = new ArrayList<Feature<T,?>>();
			
			boolean hasDescriptorName = descriptor.getDescriptorName()!=null && descriptor.getDescriptorName().length()>0;
			boolean namedFeatureWithZeroParameters = false;
			String featureName = descriptor.getDescriptorName();
			
			NamedFeatureWithParameters namedFeatureWithParameters = null;
			if (hasDescriptorName) {
				if (descriptor.getDescriptorName().indexOf("(")>=0) {
					namedFeatureWithParameters = new NamedFeatureWithParameters(descriptor.getDescriptorName(), descriptor);
					featureName = namedFeatureWithParameters.getFeatureName();
					if (namedFeatureWithParameters.getParameterNames().size()==0) {
						// if zero parameters, we want to parse it and store it against the name
						// but not return it from this function
						namedFeatureWithParameters = null;
						namedFeatureWithZeroParameters = true;
					}
				}
				if (featureClasses.containsKey(featureName)
						||namedFeatures.containsKey(featureName)
						||namedFeaturesWithParameters.containsKey(featureName)
						||featureGroups.containsKey(featureName)) {
					throw new FeatureSyntaxException("Feature name already used: " + descriptor.getDescriptorName(), descriptor, descriptor);
				}
			}
			
			if (namedFeatureWithParameters!=null) {
				// named feature with parameters
				// can't parse for now, as we don't know what arguments will be provided
				namedFeaturesWithParameters.put(namedFeatureWithParameters.getFeatureName(), namedFeatureWithParameters);
			} else {
				// named feature without parameters, or anonymous feature
				// need to parse it immediately
							
				// normal parsing
				FunctionDescriptor rootDescriptor = this.featureService.getFunctionDescriptor("RootWrapper");
				rootDescriptor.addArgument(descriptor);
				List<Feature<T, ?>> rootFeatures = this.parseInternal(rootDescriptor, descriptor);
		
				for (Feature<T, ?> rootFeature : rootFeatures) {
					Feature<T, ?> oneRootFeature = rootFeature;
					while (oneRootFeature instanceof FeatureWrapper) {
						oneRootFeature = ((FeatureWrapper<T,?>) oneRootFeature).getWrappedFeature();
					}
					RootWrapper<T, ?> rootWrapper = (RootWrapper<T, ?>) oneRootFeature;
					features.add(rootWrapper.feature);
				}
				
				// If this feature contains any named StringCollectionFeatures
				// wrap all top level features in CollectionFeatureWrapper
				List<Feature<T, ?>> wrappedFeatures = new ArrayList<Feature<T,?>>();
				for (Feature<T,?> feature : features) {
					// recursively check if this feature contains any StringCollectionFeatures
					// but this is only possible if we can always recurse through all the arguments!
					Set<StringCollectionFeature<T>> collectionFeaturesToExtract = new HashSet<StringCollectionFeature<T>>();
					this.findStringCollectionFeatures(feature, collectionFeaturesToExtract);
					if (collectionFeaturesToExtract.size()>0) {
						StringCollectionFeatureWrapper<T> collectionFeatureWrapper = new StringCollectionFeatureWrapper<T>(feature, collectionFeaturesToExtract);
						wrappedFeatures.add(collectionFeatureWrapper);
					} else {
						wrappedFeatures.add(feature);
					}
				}
				features = wrappedFeatures;
				
				// dynamise the features if manager available
				if (dynamiser!=null) {
					List<Feature<T, ?>> dynamisedFeatures = new ArrayList<Feature<T,?>>();
					for (Feature<T,?> feature : features) {
						DynamicSourceCodeBuilder<T> builder = dynamiser.getBuilder(feature);
						Feature<T,?> dynamisedFeature = builder.getFeature();
						dynamisedFeatures.add(dynamisedFeature);
					}
					features = dynamisedFeatures;
				}
	
				if (hasDescriptorName) {
					this.namedFeatures.put(featureName, features);
					
					for (Feature<T,?> feature : features) {
						feature.setCollectionName(featureName);
						while (feature instanceof FeatureWrapper) {
							Feature<T,?> wrappedFeature = ((FeatureWrapper<T,?>) feature).getWrappedFeature();
							wrappedFeature.setCollectionName(featureName);
							feature = wrappedFeature;
						}
					}
					if (features.size()==1) {
						Feature<T,?> feature = features.get(0);
						feature.setName(featureName);
						while (feature instanceof FeatureWrapper) {
							Feature<T,?> wrappedFeature = ((FeatureWrapper<T,?>) feature).getWrappedFeature();
							wrappedFeature.setName(featureName);
							feature = wrappedFeature;
						}
						// now, if this is a top-level wrapper for a string-collection feature, set the wrapped feature's name
						// this allows us to use the same string-collection feature twice with two different names,
						// thus making them not equal to each other, which means we can override the default
						// behaviour and induce a cross-product between the two
						if (feature instanceof StringCollectionFeatureProxy) {
							StringCollectionFeature<?> stringCollectionFeature = ((StringCollectionFeatureProxy<?>)feature).getStringCollectionFeature();
							stringCollectionFeature.setName(featureName);
						}
					} // exactly one feature returned
				} // has a descriptor name
				
				if (descriptor.getGroupName()!=null) {
					List<Feature<T, ?>> featureGroup = featureGroups.get(descriptor.getGroupName());
					if (featureGroup==null) {
						featureGroup = new ArrayList<Feature<T,?>>();
						featureGroups.put(descriptor.getGroupName(), featureGroup);
					}
					featureGroup.addAll(features);
				}
				
				if (namedFeatureWithZeroParameters) {
					// if it's a named feature with zero parameters,
					// we parse it, but we don't return it, since we don't want it
					// to act alone
					features = new ArrayList<Feature<T,?>>();
				}
			} // named feature with parameters?
			
			return features;
		} finally {
			MONITOR.endTask();
		}
	}
	
	final void findStringCollectionFeatures(Feature<T,?> feature, Set<StringCollectionFeature<T>> collectionFeaturesToExtract) {
		if (feature instanceof StringCollectionFeatureProxy) {
			@SuppressWarnings("unchecked")
			StringCollectionFeatureProxy<T> stringCollectionFeatureProxy = (StringCollectionFeatureProxy<T>) feature;
			StringCollectionFeature<T> stringCollectionFeature = stringCollectionFeatureProxy.getStringCollectionFeature();
			collectionFeaturesToExtract.add(stringCollectionFeature);
		}
		for (Feature<T,?> argument : feature.getArguments()) {
			this.findStringCollectionFeatures(argument, collectionFeaturesToExtract);
		}
	}
	
	final List<Feature<T, ?>> parseInternal(FunctionDescriptor descriptor, FunctionDescriptor topLevelDescriptor) {
		MONITOR.startTask("parseInternal");
		try {
			if (LOG.isTraceEnabled())
				LOG.trace(descriptor.toString());
			List<Feature<T, ?>> features = new ArrayList<Feature<T,?>>();
			
			boolean topLevelFeature = descriptor.isTopLevelDescriptor();
			
			List<FunctionDescriptor> modifiedDescriptors = new ArrayList<FunctionDescriptor>();
			if (descriptor.getFunctionName().equals("IndexRange")) {
				if (descriptor.getArguments().size()<2 || descriptor.getArguments().size()> 3)
					throw new FeatureSyntaxException(descriptor.getFunctionName() + " needs 2 or 3 arguments", descriptor, topLevelDescriptor);
				if (!(descriptor.getArguments().get(0).getObject() instanceof Integer))
					throw new FeatureSyntaxException(descriptor.getFunctionName() + " argument 1 must be a whole number", descriptor, topLevelDescriptor);
				if (!(descriptor.getArguments().get(1).getObject() instanceof Integer))
					throw new FeatureSyntaxException(descriptor.getFunctionName() + " argument 2 must be a whole number", descriptor, topLevelDescriptor);
				if (descriptor.getArguments().size()==3 && !(descriptor.getArguments().get(2).getObject() instanceof Integer))
					throw new FeatureSyntaxException(descriptor.getFunctionName() + " argument 3 must be a whole number", descriptor, topLevelDescriptor);
					
				int start = ((Integer)descriptor.getArguments().get(0).getObject()).intValue();
				int end = ((Integer)descriptor.getArguments().get(1).getObject()).intValue();
				int step = 1;
				if (descriptor.getArguments().size()==3)
					step = ((Integer)descriptor.getArguments().get(2).getObject()).intValue();
				
				for (int i=start;i<=end;i+=step) {
					FunctionDescriptor indexDescriptor = this.getFeatureService().getFunctionDescriptor("Integer");
					indexDescriptor.addArgument(i);
					modifiedDescriptors.add(indexDescriptor);
				}
			}
			
			if (modifiedDescriptors.size()==0)
				modifiedDescriptors = this.getModifiedDescriptors(descriptor);
			if (modifiedDescriptors==null)
				modifiedDescriptors = new ArrayList<FunctionDescriptor>();
			if (modifiedDescriptors.size()==0)
				modifiedDescriptors.add(descriptor);
			
			for (FunctionDescriptor modifiedDescriptor : modifiedDescriptors) {
				String functionName = modifiedDescriptor.getFunctionName();
				// check if this is a named feature with parameters
				while (namedFeaturesWithParameters.containsKey(functionName)) {
					// replace the parameters by the arguments provided
					NamedFeatureWithParameters namedFeature = namedFeaturesWithParameters.get(functionName);
					if (namedFeature.getParameterNames().size()!=modifiedDescriptor.getArguments().size()) {
						throw new FeatureSyntaxException("Wrong number of arguments (" + modifiedDescriptor.getArguments().size() + ") for named feature '" + namedFeature.getFeatureName() + "' in: " + modifiedDescriptor, modifiedDescriptor, topLevelDescriptor);
					}
					FunctionDescriptor clonedDescriptor = namedFeature.getDescriptor().cloneDescriptor();
					int i = 0;
					for (String parameterName : namedFeature.getParameterNames()) {
						FunctionDescriptor argument =  modifiedDescriptor.getArguments().get(i);
						clonedDescriptor.replaceParameter(parameterName, argument);
						i++;
					}
					modifiedDescriptor = clonedDescriptor;
					functionName = clonedDescriptor.getFunctionName();
					// we consider top-level features anything the user placed at the top-level
					// whether or not it's parametrised
					topLevelFeature = true;
				} // is a named feature with parameters
				
				if (namedFeatures.containsKey(functionName)) {
					features.addAll(namedFeatures.get(functionName));
					topLevelFeature = true;
				} else if (featureGroups.containsKey(functionName)) {
					features.addAll(featureGroups.get(functionName));
				} else if (parsedFeatures.containsKey(modifiedDescriptor.toString())) {
					features.addAll(parsedFeatures.get(modifiedDescriptor.toString()));
				} else {
					@SuppressWarnings("rawtypes")
					List<Class<? extends Feature>> featureClasses = this.featureClasses.get(functionName);
					if (featureClasses!=null) {
						// add the features corresponding to the first class for which a constructor is found
						int i = 0;
						for (@SuppressWarnings("rawtypes") Class<? extends Feature> featureClass : featureClasses) {
							boolean lastClass = (i==featureClasses.size()-1);
							boolean foundConstructor = false;
							try {
								features.addAll(this.getFeatures(modifiedDescriptor, featureClass, topLevelDescriptor));
								foundConstructor = true;
							} catch (NoConstructorFoundException ncfe) {
								if (lastClass)
									throw ncfe;
							}
							if (foundConstructor)
								break;
							i++;
						} // next feature class
					} else {
						throw new FeatureSyntaxException("Unknown function: " + functionName, descriptor, topLevelDescriptor);
					} // have feature classes for this function name
					parsedFeatures.put(modifiedDescriptor.toString(), features);
				} // is a named feature
			} // next modified descriptor
			
			if (topLevelFeature) {
				for (Feature<T, ?> feature : features)
					feature.setTopLevelFeature(true);
			}
			return features;
		} finally {
			MONITOR.endTask();
		}
	}
	
	/**
	 * Add all feature classes supported by this parser via calls to addFeatureClass.
	 * Note: for a given classname which is mapped to two different classes,
	 * one with IntegerFeature and one with DoubleFeature arguments,
	 * the version with the IntegerFeature arguments should always be added first.
	 * This is only required if the class returns a different type of feature result
	 * (e.g. int or double) depending on the arguments provided.
	 */
	public abstract void addFeatureClasses(FeatureClassContainer container);
	
	@SuppressWarnings("rawtypes")
	final public void addFeatureClass(String name, Class<? extends Feature> featureClass) {
		List<Class<? extends Feature>> featureClasses = this.featureClasses.get(name);
		if (featureClasses==null) {
			featureClasses = new ArrayList<Class<? extends Feature>>();
			this.featureClasses.put(name, featureClasses);
		}
		featureClasses.add(featureClass);
		
		List<String> descriptors = this.featureClassDescriptors.get(featureClass);
		if (descriptors==null) {
			descriptors = new ArrayList<String>();
			this.featureClassDescriptors.put(featureClass, descriptors);
		}
		descriptors.add(name);
		
		@SuppressWarnings("unchecked")
		Constructor<? extends Feature>[] constructors = (Constructor<? extends Feature>[]) featureClass.getConstructors();
		this.featureConstructors.put(featureClass, constructors);
	}
	
	/**
	 * Return the feature classes currently mapped to the name provided.
	 */
	@SuppressWarnings("rawtypes")
	final public List<Class<? extends Feature>> getFeatureClasses(String name) {
		return this.featureClasses.get(name);
	}
	
	

	@Override
	public List<String> getFeatureClassDescriptors(
			@SuppressWarnings("rawtypes") Class<? extends Feature> featureClass) {
		return this.featureClassDescriptors.get(featureClass);
	}

	/**
	 * Given a feature descriptor, converts it into multiple feature descriptors if required,
	 * for example when generating a separate feature for each pos-tag, or for an whole range of indexes.
	 * Should return a List containing the initial function descriptor if no modification is required.
	 */
	public abstract List<FunctionDescriptor> getModifiedDescriptors(FunctionDescriptor functionDescriptor);
	
	public static class RootWrapper<T,Y> extends AbstractFeature<T,Y> implements Feature<T,Y> {
		private Feature<T,Y> feature;
		public RootWrapper(Feature<T,Y> feature) {
			this.feature = feature;
			this.setName(super.getName() + "|" + this.feature.getName());
			this.addArgument(feature);
		}

		@Override
		public FeatureResult<Y> check(T context, RuntimeEnvironment env) {
			return null;
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public Class<? extends Feature> getFeatureType() {
			return feature.getFeatureType();
		}
	}

	private static class StringFeatureWrapper<T> extends AbstractFeature<T,String> implements StringFeature<T>, FeatureWrapper<T,String> {
		private Feature<T,String> feature;

		public StringFeatureWrapper(Feature<T,String> feature) {
			this.feature = feature;
			this.setName(this.feature.getName());
			this.addArgument(feature);
		}

		@Override
		public FeatureResult<String> check(T context, RuntimeEnvironment env) {
			return this.feature.check(context, env);
		}

		@Override
		public Feature<T, String> getWrappedFeature() {
			return feature;
		}

	}
	
	private static class BooleanFeatureWrapper<T> extends AbstractFeature<T,Boolean> implements BooleanFeature<T>, FeatureWrapper<T,Boolean> {
		private Feature<T,Boolean> feature;
		public BooleanFeatureWrapper(Feature<T,Boolean> feature) {
			this.feature = feature;
			this.setName(this.feature.getName());
			this.addArgument(feature);
		}
		
		@Override
		public FeatureResult<Boolean> check(T context, RuntimeEnvironment env) {
			return this.feature.check(context, env);
		}

		@Override
		public Feature<T, Boolean> getWrappedFeature() {
			return feature;
		}
	}
	
	private static class DoubleFeatureWrapper<T> extends AbstractFeature<T,Double> implements DoubleFeature<T>, FeatureWrapper<T,Double> {
		private Feature<T,Double> feature;
		public DoubleFeatureWrapper(Feature<T,Double> feature) {
			this.feature = feature;
			this.setName(this.feature.getName());
			this.addArgument(feature);
		}
		
		@Override
		public FeatureResult<Double> check(T context, RuntimeEnvironment env) {
			return this.feature.check(context, env);
		}

		@Override
		public Feature<T, Double> getWrappedFeature() {
			return feature;
		}
	}
	
	private static class IntegerFeatureWrapper<T> extends AbstractFeature<T,Integer> implements IntegerFeature<T>, FeatureWrapper<T,Integer> {
		private Feature<T,Integer> feature;
		public IntegerFeatureWrapper(Feature<T,Integer> feature) {
			this.feature = feature;
			this.setName(this.feature.getName());
			this.addArgument(feature);
		}
		
		@Override
		public FeatureResult<Integer> check(T context, RuntimeEnvironment env) {
			return this.feature.check(context, env);
		}
		
		@Override
		public Feature<T, Integer> getWrappedFeature() {
			return feature;
		}
	}
	
	private static class NamedFeatureWithParameters {
		private String featureName;
		private List<String> parameterNames = new ArrayList<String>();
		private FunctionDescriptor descriptor;
		
		public NamedFeatureWithParameters(String fullName, FunctionDescriptor descriptor) {
			this.descriptor = descriptor;
			
			int openParenthesisIndex = fullName.indexOf('(');
			if (openParenthesisIndex>=0) {
				int closeParenthesisIndex = fullName.indexOf(')');
				if (closeParenthesisIndex<0)
					throw new FeatureSyntaxException("Open parenthesis without close parenthesis in: " + fullName, descriptor, descriptor);
				this.featureName = fullName.substring(0, openParenthesisIndex);
				String[] parameters = fullName.substring(openParenthesisIndex+1, closeParenthesisIndex).split(",");
				for (String parameter : parameters) {
					String paramTrim = parameter.trim();
					if (paramTrim.length()>0)
						this.parameterNames.add(parameter.trim());
				}
			} else {
				this.featureName = fullName;
			}
		}

		public String getFeatureName() {
			return featureName;
		}

		public List<String> getParameterNames() {
			return parameterNames;
		}

		public FunctionDescriptor getDescriptor() {
			return descriptor;
		}
	}

	public final FeatureService getFeatureService() {
		return featureService;
	}

	public final void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	public ExternalResourceFinder getExternalResourceFinder() {
		return externalResourceFinder;
	}

	public void setExternalResourceFinder(
			ExternalResourceFinder externalResourceFinder) {
		this.externalResourceFinder = externalResourceFinder;
	}

	public Dynamiser<T> getDynamiser() {
		return dynamiser;
	}

	public void setDynamiser(Dynamiser<T> dynamiser) {
		this.dynamiser = dynamiser;
	}

}
