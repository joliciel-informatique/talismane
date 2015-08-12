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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;


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
    
    @SuppressWarnings("unchecked")
    public static void LogParameters(MapSqlParameterSource paramSource) {
       DaoUtils.LogParameters(paramSource.getValues());
    }

}
