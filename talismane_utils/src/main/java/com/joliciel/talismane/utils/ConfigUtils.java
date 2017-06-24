///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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
package com.joliciel.talismane.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

/**
 * Utilities for using Config objects in conjunction with a virtual file system.
 * 
 * @author Assaf Urieli
 *
 */
public class ConfigUtils {
  private static final Logger LOG = LoggerFactory.getLogger(ConfigUtils.class);

  public static InputStream getFileFromConfig(Config config, String configPath) throws IOException {
    LOG.debug("Getting " + configPath);
    String path = config.getString(configPath);
    return getFile(config, configPath, path);
  }

  public static InputStream getFile(Config config, String configPath, String path) throws IOException {
    FileObject fileObject = VFSWrapper.getInstance().getFileObject(path);

    if (!fileObject.exists()) {
      LOG.error(configPath + " file not found: " + path);
      throw new FileNotFoundException(configPath + " file not found: " + path);
    }

    return fileObject.getContent().getInputStream();
  }

  public static List<FileObject> getFileObjects(String path) throws FileSystemException {
    List<FileObject> fileObjects = new ArrayList<>();
    FileObject fileObject = VFSWrapper.getInstance().getFileObject(path);
    getFileObjects(fileObject, fileObjects);
    return fileObjects;
  }

  private static void getFileObjects(FileObject fileObject, List<FileObject> fileObjects) throws FileSystemException {
    if (fileObject.isFolder()) {
      for (FileObject child : fileObject.getChildren()) {
        getFileObjects(child, fileObjects);
      }
    } else {
      fileObjects.add(fileObject);
    }
  }

  private static final class VFSWrapper {
    private final String baseDir = System.getProperty("user.dir");
    private final Set<String> prefixes;
    private final Set<String> localPrefixes = new HashSet<>(Arrays.asList("file", "zip", "jar", "tar", "tgz", "tbz2", "gz", "bz2", "ear", "war"));
    private final FileSystemManager fsManager;

    private static VFSWrapper instance;

    public static VFSWrapper getInstance() throws FileSystemException {
      if (instance == null)
        instance = new VFSWrapper();
      return instance;
    }

    private VFSWrapper() throws FileSystemException {
      fsManager = VFS.getManager();
      prefixes = new HashSet<>(Arrays.asList(fsManager.getSchemes()));
    }

    public FileObject getFileObject(String path) throws FileSystemException {
      LOG.debug("Getting " + path);
      FileSystemManager fsManager = VFS.getManager();

      // make the path absolute if required, based on the working
      // directory
      String prefix = "";
      if (path.contains("://")) {
        prefix = path.substring(0, path.indexOf("://"));
        if (!prefixes.contains(prefix))
          prefix = "";
      }

      boolean makeAbsolute = prefix.length() == 0 || localPrefixes.contains(prefix);

      if (makeAbsolute) {
        if (prefix.length() > 0)
          prefix += "://";
        String fileSystemPath = path.substring(prefix.length());
        String pathNoSuffix = fileSystemPath;
        int exclamationPos = fileSystemPath.indexOf('!');

        if (exclamationPos >= 0)
          pathNoSuffix = pathNoSuffix.substring(0, exclamationPos);

        File file = new File(pathNoSuffix);
        if (!file.isAbsolute()) {
          path = prefix + baseDir + "/" + fileSystemPath;
          LOG.debug("Changed path to " + path);
        }
      }

      FileObject fileObject = fsManager.resolveFile(path);
      return fileObject;
    }
  }
}
