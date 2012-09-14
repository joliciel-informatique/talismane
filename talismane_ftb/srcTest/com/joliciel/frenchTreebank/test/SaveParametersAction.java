/*
 * Created on 11 Feb 2009
 */
package com.joliciel.frenchTreebank.test;

import java.util.Collection;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

public class SaveParametersAction implements Action {
    private Collection<Object> elements;
    
    public SaveParametersAction(Collection<Object> elements) {
        this.elements = elements;
    }

    public Object invoke(Invocation invocation) throws Throwable {
        Object[] parameters = invocation.getParametersAsArray();
        for (Object parameter : parameters)
            elements.add(parameter);
        return null;
    }

    public void describeTo(Description description) {
        description.appendText("adds parameters from previous invocation to the collection passed in");
    }

}
