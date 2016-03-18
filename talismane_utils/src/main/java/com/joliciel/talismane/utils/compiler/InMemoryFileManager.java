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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.JavaFileObject.Kind;

/**
 * A file manager capable of handling in-memory classes - otherwise it delegates
 * to a parent file manager.
 * @author Assaf Urieli
 *
 */
final class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
	private final Map<URI, JavaFileObject> javaFileObjectMap = new HashMap<URI, JavaFileObject>();
	private final InMemoryClassLoader classLoader;

	public InMemoryFileManager(JavaFileManager parent, InMemoryClassLoader classLoader) {
		super(parent);
		this.classLoader = classLoader;
	}

	@Override
	public FileObject getFileForInput(Location location, String packageName,
			String relativeName) throws IOException {
		JavaFileObject javaFileObject = javaFileObjectMap.get(this.getURI(location, packageName, relativeName));
		if (javaFileObject != null)
			return javaFileObject;
		return super.getFileForInput(location, packageName, relativeName);
	}
	
	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String qualifiedName,
			Kind kind, FileObject outputFile) throws IOException {
		JavaFileObject file = new InMemoryJavaFileObject(qualifiedName);
		classLoader.putJavaFileObject(qualifiedName, file);
		return file;
	}

	public void prepareJavaFileObject(StandardLocation location, String packageName,
			String relativeName, JavaFileObject javaFileObject) {
		if (javaFileObject.getKind()!=Kind.SOURCE)
			throw new DynamicCompilerException("Can only add source files to InMemoryFileManager");
		javaFileObjectMap.put(this.getURI(location, packageName, relativeName), javaFileObject);
	}
	
	private URI getURI(Location location, String packageName, String relativeName) {
		try {
			String name = location.getName() + '/' + packageName + '/' + relativeName;
			URI uri = new URI(name);
			return uri;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ClassLoader getClassLoader(JavaFileManager.Location location) {
		return classLoader;
	}

	@Override
	public String inferBinaryName(Location loc, JavaFileObject file) {
		String binaryName;

		if (file instanceof InMemoryJavaFileObject)
			binaryName = file.getName();
		else
			binaryName = super.inferBinaryName(loc, file);
		return binaryName;
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName,
			Set<Kind> kinds, boolean recurse) throws IOException {
		boolean wantClasses = kinds.contains(JavaFileObject.Kind.CLASS);
		boolean wantSources = kinds.contains(JavaFileObject.Kind.SOURCE);
		ArrayList<JavaFileObject> fileObjects = new ArrayList<JavaFileObject>();
		if (wantSources && location==StandardLocation.SOURCE_PATH) {
			for (JavaFileObject file : javaFileObjectMap.values()) {
				if (file.getName().startsWith(packageName))
					fileObjects.add(file);
			}
		} else if (wantClasses && location==StandardLocation.CLASS_PATH) {
			fileObjects.addAll(classLoader.list(packageName));
		}
		
		Iterable<JavaFileObject> parentObjects = super.list(location, packageName, kinds, recurse);
		for (JavaFileObject fileObject : parentObjects) {
			fileObjects.add(fileObject);
		}
		return fileObjects;
	}
}