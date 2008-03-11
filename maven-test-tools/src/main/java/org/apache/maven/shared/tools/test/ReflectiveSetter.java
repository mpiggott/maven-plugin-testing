/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.tools.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.StringUtils;

public class ReflectiveSetter
{

    private Map cachedPropertySetters = new HashMap();

    private final Class targetClass;

    public ReflectiveSetter( Class targetClass )
    {
        this.targetClass = targetClass;
    }

    public void setProperty( String propertyName, Object value, Object target )
        throws Throwable
    {

        String preferredMethodName = "set" + StringUtils.capitalizeFirstLetter( propertyName );

        Setter setter = null;

        Method method = ReflectionUtils.getSetter( preferredMethodName, targetClass );

        if ( method != null )
        {
            setter = new MethodSetter( propertyName, method );
        }
        else
        {
            Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses( propertyName, targetClass );

            setter = new FieldSetter( propertyName, field );
        }

        if ( setter == null )
        {
            throw new IllegalArgumentException( "No such property: " + propertyName + " in: " + targetClass
                + ". Searched for: {method:" + preferredMethodName + ", method:" + propertyName + ", field:"
                + propertyName + "}" );
        }

        cachedPropertySetters.put( setter.getProperty(), setter );

        try
        {
            setter.set( value, target );
        }
        catch ( InvocationTargetException e )
        {
            throw e.getTargetException();
        }
    }

    private interface Setter
    {
        void set( Object value, Object target )
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException;

        String getProperty();
    }

    private static class MethodSetter
        implements Setter
    {
        private Method method;

        private String name;

        MethodSetter( String name, Method method )
        {
            this.name = name;
            this.method = method;
        }

        public String getProperty()
        {
            return name;
        }

        public void set( Object value, Object target )
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
        {
            boolean wasAccessible = method.isAccessible();

            method.setAccessible( true );
            try
            {
                method.invoke( target, new Object[] { value } );
            }
            finally
            {
                method.setAccessible( wasAccessible );
            }
        }
    }

    private static class FieldSetter
        implements Setter
    {
        private Field field;

        private String name;

        FieldSetter( String name, Field field )
        {
            this.name = name;
            this.field = field;
        }

        public String getProperty()
        {
            return name;
        }

        public void set( Object value, Object target )
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
        {
            boolean wasAccessible = field.isAccessible();

            field.setAccessible( true );
            try
            {
                field.set( target, value );
            }
            finally
            {
                field.setAccessible( wasAccessible );
            }
        }
    }

}