///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DaoUtils {
    private static final Log LOG = LogFactory.getLog(DaoUtils.class);

    public static void LogParameters(Map<String,Object> paramMap) {
        if (LOG.isDebugEnabled()) {
            for (Object obj : paramMap.entrySet()) {
                @SuppressWarnings("rawtypes")
				Entry entry = (Entry) obj;
                LOG.debug(entry.getKey() + ": " + (entry.getValue()==null? "null" : entry.getValue().toString()));
            }
        }
    }
    
    public static List<String> getSelectArray(String selectString, String alias) {
        List<String> selectArray = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(selectString, ",", false);
        boolean haveAlias = alias!=null && alias.length()>0;
        while (st.hasMoreTokens()) {
            String column = st.nextToken().trim();
            if (haveAlias)
                selectArray.add(alias + "." + column + " as " + alias + "_" + column );
            else
                selectArray.add(column);
            
        }
        return selectArray;
    }

}
