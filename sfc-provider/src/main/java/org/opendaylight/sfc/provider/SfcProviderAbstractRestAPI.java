/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Abstract parent Runnable for the REST API.
 * <p/>
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since 2014-07-28
 */
abstract public class SfcProviderAbstractRestAPI implements Runnable {
    protected static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static final Logger LOG = LoggerFactory.getLogger
            (SfcProviderAbstractRestAPI.class);
    private String methodName = null;
    private Object[] parameters;

    public String getMethodName()
    {
        return methodName;
    }

    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }

    public Object[] getParameters()
    {
        return parameters;
    }

    public void setParameters(Object[] parameters)
    {
        this.parameters = Arrays.copyOf(parameters, parameters.length);
    }

    public Class[] getParameterTypes()
    {
        return parameterTypes;
    }

    public void setParameterTypes(Class[] parameterTypes)
    {
        this.parameterTypes = Arrays.copyOf(parameterTypes, parameterTypes.length);
    }

    private Class[] parameterTypes;

    protected SfcProviderAbstractRestAPI(Object[] params, Class[] paramsTypes, String m) {
        setMethodName(m);
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        setParameters(params);
        setParameterTypes(paramsTypes);
    }

    @Override
    public void run() {
        if (methodName != null) {
            Class<?> c = this.getClass();
            Method method;
            try {
                method = c.getDeclaredMethod(methodName, parameterTypes);
                method.invoke(this, parameters);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                LOG.error("\nFailed to find proper REST method: {}",
                        e.getMessage());
            }
        }
    }

}