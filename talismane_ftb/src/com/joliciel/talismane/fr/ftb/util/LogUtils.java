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
package com.joliciel.talismane.fr.ftb.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;

public class LogUtils {
    /**
    Return the current exception & stack trace as a String.
    */
    public static String getErrorString(Throwable e)
    {
        String s = null;
        try {
            StringWriter sw = new StringWriter();
            PrintWriter ps = new PrintWriter((Writer) sw);
            e.printStackTrace(ps);
            sw.flush();
            s = sw.toString();
            sw.close();
        } catch (IOException ioe) {
            // do nothing!
        }
        return s;
    }

    /**
    Log the exception passed.
    */
    public static void logError(Log logger, Throwable e) {
        logger.error(e);
        logger.error(LogUtils.getErrorString(e));
    }
    
}
