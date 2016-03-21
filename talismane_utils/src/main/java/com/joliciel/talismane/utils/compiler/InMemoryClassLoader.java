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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.JavaFileObject;

/**
 * A class loader capable of handling in-memory classes -
 * otherwise it delegates to a parent class loader.
 * @author Assaf Urieli
 *
 */
final class InMemoryClassLoader extends ClassLoader {
	private Map<String, JavaFileObject> classMap = new HashMap<String, JavaFileObject>();

	InMemoryClassLoader(final ClassLoader parent) {
		super(parent);
	}

	@Override
	protected Class<?> findClass(final String qualifiedName)
	throws ClassNotFoundException {
		JavaFileObject javaFileObject = classMap.get(qualifiedName);
		if (javaFileObject != null) {
			byte[] byteArray = ((InMemoryJavaFileObject) javaFileObject).getBytes();
			return defineClass(qualifiedName, byteArray, 0, byteArray.length);
		}

		try {
			Class<?> clazz = Class.forName(qualifiedName);
			return clazz;
		} catch (ClassNotFoundException nf) {
			// do nothing
		}

		return super.findClass(qualifiedName);
	}

	void putJavaFileObject(final String qualifiedClassName, final JavaFileObject javaFile) {
		classMap.put(qualifiedClassName, javaFile);
	}

	@Override
	protected synchronized Class<?> loadClass(final String name, final boolean resolve)
		throws ClassNotFoundException {
		return super.loadClass(name, resolve);
	}

	@Override
	public InputStream getResourceAsStream(final String name) {
		if (name.endsWith(".class")) {
			String qualifiedClassName = name.substring(0, name.length() - ".class".length());
			qualifiedClassName = qualifiedClassName.replace('/', '.');
			InMemoryJavaFileObject javaFileObject = (InMemoryJavaFileObject) classMap.get(qualifiedClassName);
			if (javaFileObject != null) {
				return javaFileObject.openInputStream();
			}
		}
		return super.getResourceAsStream(name);
	}
	
	public Collection<JavaFileObject> list(String packageName) {
		List<JavaFileObject> fileObjects = new ArrayList<JavaFileObject>();
		for (String qualifiedName : classMap.keySet()) {
			if (qualifiedName.startsWith(packageName)) {
				fileObjects.add(classMap.get(qualifiedName));
			}
		}
		return fileObjects;
	}


}