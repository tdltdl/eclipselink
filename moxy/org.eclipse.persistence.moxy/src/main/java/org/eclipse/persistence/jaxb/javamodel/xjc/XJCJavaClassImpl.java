/*
 * Copyright (c) 2011, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Rick Barkhouse - 2.1 - Initial implementation
package org.eclipse.persistence.jaxb.javamodel.xjc;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.ConversionManager;
import org.eclipse.persistence.jaxb.javamodel.*;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JPrimitiveType;
import com.sun.codemodel.JType;
import com.sun.codemodel.JTypeVar;

/**
 * INTERNAL:
 * <p>
 * <b>Purpose:</b> <code>JavaClass</code> implementation wrapping XJC's <code>JDefinedClass</code>.
 * Used when bootstrapping a <code>DynamicJAXBContext</code> from an XML Schema.
 * </p>
 *
 * <p>
 * <b>Responsibilities:</b>
 * </p>
 * <ul>
 *    <li>Provide Class information from the underlying <code>JDefinedClass</code>.</li>
 * </ul>
 *
 * @since EclipseLink 2.1
 *
 * @see org.eclipse.persistence.jaxb.javamodel.JavaClass
 */
public class XJCJavaClassImpl implements JavaClass {

    private JDefinedClass xjcClass;
    private JCodeModel jCodeModel;
    private JavaModel javaModel;
    private boolean isArray;
    private boolean isPrimitive;

    private JavaClass arg;

    private DynamicClassLoader dynamicClassLoader;

    private static final Map<String, JPrimitiveType> jPrimitiveTypes = new HashMap<String, JPrimitiveType>();
    static {
        JCodeModel tempCodeModel = new JCodeModel();
        jPrimitiveTypes.put("java.lang.Boolean", tempCodeModel.BOOLEAN);
        jPrimitiveTypes.put("java.lang.Byte", tempCodeModel.BYTE);
        jPrimitiveTypes.put("java.lang.Character", tempCodeModel.CHAR);
        jPrimitiveTypes.put("java.lang.Double", tempCodeModel.DOUBLE);
        jPrimitiveTypes.put("java.lang.Float", tempCodeModel.FLOAT);
        jPrimitiveTypes.put("java.lang.Integer", tempCodeModel.INT);
        jPrimitiveTypes.put("java.lang.Long", tempCodeModel.LONG);
        jPrimitiveTypes.put("java.lang.Short", tempCodeModel.SHORT);
    }

    /**
     * Construct a new instance of <code>XJCJavaClassImpl</code>.
     *
     * @param jDefinedClass - the XJC <code>JDefinedClass</code> to be wrapped.
     * @param codeModel - the XJC <code>JCodeModel</code> this class belongs to.
     * @param loader - the <code>ClassLoader</code> used to bootstrap the <code>DynamicJAXBContext</code>.
     */
    public XJCJavaClassImpl(JDefinedClass jDefinedClass, JCodeModel codeModel, DynamicClassLoader loader) {
        this(jDefinedClass, codeModel, loader, false, false);
    }

    /**
     * Construct a new instance of <code>XJCJavaClassImpl</code>.
     *
     * @param jDefinedClass - the XJC <code>JDefinedClass</code> to be wrapped.
     * @param codeModel - the XJC <code>JCodeModel</code> this class belongs to.
     * @param loader - the <code>ClassLoader</code> used to bootstrap the <code>DynamicJAXBContext</code>.
     * @param isArray - indicates that this class is an array type.
     * @param isPrimitive - indicates that this class is a primitive type.
     */
    public XJCJavaClassImpl(JDefinedClass jDefinedClass, JCodeModel codeModel, DynamicClassLoader loader, boolean isArray, boolean isPrimitive) {
        this.xjcClass = jDefinedClass;
        this.jCodeModel = codeModel;
        this.dynamicClassLoader = loader;
        this.isArray = isArray;
        this.isPrimitive = isPrimitive;
    }

    // ========================================================================

    public void setActualTypeArgument(JavaClass javaClass){
        arg = javaClass;
    }

    /**
     * Return the "actual" type from a parameterized type.  For example, if this
     * <code>JavaClass</code> represents <code>List&lt;Employee</code>, this method will return the
     * <code>Employee</code> <code>JavaClass</code>.
     *
     * @return a <code>Collection</code> containing the actual type's <code>JavaClass</code>.
     */
    @Override
    public Collection<JavaClass> getActualTypeArguments() {


        JTypeVar[] typeParams = xjcClass.typeParams();

        if (null == typeParams || 0 == typeParams.length ) {
            if(arg != null){
                java.util.List<JavaClass> theList = new ArrayList<>(1);
                theList.add(arg);
                return theList;
            }else{
                return new ArrayList<>(0);
            }
        }

        try {
            ArrayList<JavaClass> typeArguments = new ArrayList<>(1);
            JTypeVar var = typeParams[typeParams.length - 1];
            JClass xjcBoundClass = var.bound();

            JType basis = xjcBoundClass.erasure();
            JavaClass boundClass;

            if (basis != null) {
                boundClass = this.javaModel.getClass(basis.fullName());
            } else if (javaModel != null) {
                boundClass = this.javaModel.getClass(xjcBoundClass.fullName());
            } else {
                JDefinedClass c = jCodeModel._getClass(xjcBoundClass.fullName());
                boundClass = new XJCJavaClassImpl(c, jCodeModel, dynamicClassLoader);
            }

            typeArguments.add(boundClass);
            return typeArguments;
        } catch (Exception e) {
            return new ArrayList<>(0);
        }
    }

    /**
     * If this <code>JavaClass</code> is an array type, return the type of the
     * array components.
     *
     * @return <code>JavaClass</code> of this array's component type, or <code>null</code> if
     *         this is not an array type.
     */
    @Override
    public JavaClass getComponentType() {
        if (!isArray()) {
            return null;
        }

        return javaModel.getClass(this.xjcClass.fullName());
    }

    /**
     * Return the <code>JavaConstructor</code> for this <code>JavaClass</code> that has the
     * provided parameter types.
     *
     * @param parameterTypes the parameter list used to identify the constructor.
     *
     * @return the <code>JavaConstructor</code> with the signature matching parameterTypes.
     */
    @Override
    public JavaConstructor getConstructor(JavaClass[] parameterTypes) {
        JType[] xjcParameterTypes = new JType[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            JavaClass pType = parameterTypes[i];
            String className = pType.getQualifiedName();
            JType xjcType = null;

            if (pType.isPrimitive()) {
                xjcType = jPrimitiveTypes.get(className);
            } else {
                xjcType = jCodeModel._getClass(className);
            }
            xjcParameterTypes[i] = xjcType;
        }

        JMethod constructor = xjcClass.getConstructor(xjcParameterTypes);

        return new XJCJavaConstructorImpl(constructor, jCodeModel, dynamicClassLoader, this);
    }

    /**
     * Return all of the <code>JavaConstructors</code> for this <code>JavaClass</code>.
     *
     * @return A <code>Collection</code> containing this <code>JavaClass'</code> <code>JavaConstructors</code>.
     */
    @Override
    public Collection<JavaConstructor> getConstructors() {
        ArrayList<JavaConstructor> constructors = new ArrayList<>();
        Iterator<JMethod> it = xjcClass.constructors();

        while (it.hasNext()) {
            constructors.add(new XJCJavaConstructorImpl(it.next(), jCodeModel, dynamicClassLoader, this));
        }

        return constructors;
    }

    /**
     * Return this <code>JavaClass'</code> inner classes.
     *
     * @return A <code>Collection&lt;JavaClass&gt;</code> containing this <code>JavaClass'</code> inner classes.
     */
    @Override
    public Collection<JavaClass> getDeclaredClasses() {
        ArrayList<JavaClass> declaredClasses = new ArrayList<>();

        Iterator<JDefinedClass> it  = xjcClass.classes();

        while (it.hasNext()) {
            XJCJavaClassImpl dc;
            if (javaModel != null) {
                dc = (XJCJavaClassImpl) this.javaModel.getClass(it.next().fullName());
            } else {
                dc = new XJCJavaClassImpl(it.next(), jCodeModel, dynamicClassLoader);
            }
            declaredClasses.add(dc);
        }

        return declaredClasses;
    }

    /**
     * Return the declared <code>JavaConstructor</code> for this <code>JavaClass</code> that has the
     * provided parameter types.
     *
     * @param parameterTypes the parameter list used to identify the constructor.
     *
     * @return the <code>JavaConstructor</code> with the signature matching <code>parameterTypes</code>.
     */
    @Override
    public JavaConstructor getDeclaredConstructor(JavaClass[] parameterTypes) {
        return getConstructor(parameterTypes);
    }

    /**
     * Return all of the declared <code>JavaConstructors</code> for this <code>JavaClass</code>.
     *
     * @return A <code>Collection</code> containing this <code>JavaClass'</code> <code>JavaConstructors</code>.
     */
    @Override
    public Collection<JavaConstructor> getDeclaredConstructors() {
        return getConstructors();
    }

    /**
     * Return the declared <code>JavaField</code> for this <code>JavaClass</code>, identified
     * by <code>fieldName</code>.
     *
     * @param fieldName the name of the <code>JavaField</code> to return.
     *
     * @return the <code>JavaField</code> named <code>fieldName</code> from this <code>JavaClass</code>.
     */
    @Override
    public JavaField getDeclaredField(String fieldName) {
        JFieldVar xjcField = xjcClass.fields().get(fieldName);

        return new XJCJavaFieldImpl(xjcField, jCodeModel, dynamicClassLoader, this);
    }

    /**
     * Return all of the declared <code>JavaFields</code> for this <code>JavaClass</code>.
     *
     * @return A <code>Collection</code> containing this <code>JavaClass'</code> <code>JavaFields</code>.
     */
    @Override
    public Collection<JavaField> getDeclaredFields() {
        Collection<JFieldVar> xjcFields = xjcClass.fields().values();
        ArrayList<JavaField> fields = new ArrayList<>(xjcFields.size());

        for (JFieldVar jField : xjcFields) {
            fields.add(new XJCJavaFieldImpl(jField, jCodeModel, dynamicClassLoader, this));
        }

        return fields;
    }

    /**
     * Return the declared <code>JavaMethod</code> for this <code>JavaClass</code>,
     * identified by <code>name</code>, with the signature matching <code>args</code>.
     *
     * @param name the name of the <code>JavaMethod</code> to return.
     * @param args the parameter list used to identify the method.
     *
     * @return the matching <code>JavaMethod</code> from this <code>JavaClass</code>.
     */
    @Override
    public JavaMethod getDeclaredMethod(String name, JavaClass[] args) {
        return getMethod(name, args);
    }

    /**
     * Return all of the declared <code>JavaMethods</code> for this <code>JavaClass</code>.
     *
     * @return A <code>Collection</code> containing this <code>JavaClass'</code> <code>JavaMethods</code>.
     */
    @Override
    public Collection<JavaMethod> getDeclaredMethods() {
        return getMethods();
    }

    /**
     * Return the <code>JavaMethod</code> for this <code>JavaClass</code>, identified
     * by <code>name</code>, with the signature matching <code>args</code>.
     *
     * @param name the name of the <code>JavaMethod</code> to return.
     * @param args the parameter list used to identify the method.
     *
     * @return the matching <code>JavaMethod</code> from this <code>JavaClass</code>.
     */
    @Override
    public JavaMethod getMethod(String name, JavaClass[] args) {
        Collection<JMethod> xjcMethods = xjcClass.methods();

        for (JMethod xjcMethod : xjcMethods) {
            JType[] params = xjcMethod.listParamTypes();
            boolean argsAreEqual = argsAreEqual(args, params);

            if (xjcMethod.name().equals(name) && argsAreEqual) {
                return new XJCJavaMethodImpl(xjcMethod, jCodeModel, dynamicClassLoader, this);
            }
        }
        return null;
    }

    private boolean argsAreEqual(JavaClass[] elinkArgs, JType[] xjcArgs) {
        if (elinkArgs == null && xjcArgs == null) {
            return true;
        }
        if (elinkArgs != null && xjcArgs == null) {
            return false;
        }
        if (elinkArgs == null && xjcArgs != null) {
            return false;
        }
        if (elinkArgs.length != xjcArgs.length) {
            return false;
        }
        for (int i = 0; i < elinkArgs.length; i++) {
            JavaClass elinkClass = elinkArgs[i];
            JType xjcClass = xjcArgs[i];
            if (!elinkClass.getQualifiedName().equals(xjcClass.fullName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return all of the <code>JavaMethods</code> for this <code>JavaClass</code>.
     *
     * @return A <code>Collection</code> containing this <code>JavaClass'</code> <code>JavaMethods</code>.
     */
    @Override
    public Collection<JavaMethod> getMethods() {
        Collection<JMethod> xjcMethods = xjcClass.methods();
        ArrayList<JavaMethod> elinkMethods = new ArrayList<>(xjcMethods.size());

        for (JMethod xjcMethod : xjcMethods) {
            elinkMethods.add(new XJCJavaMethodImpl(xjcMethod, jCodeModel, dynamicClassLoader, this));
        }

        return elinkMethods;
    }

    /**
     * Returns the Java language modifiers for this <code>JavaClass</code>, encoded in an integer.
     *
     * @return the <code>int</code> representing the modifiers for this class.
     *
     * @see java.lang.reflect.Modifier
     */
    @Override
    public int getModifiers() {
        return xjcClass.mods().getValue();
    }

    /**
     * Returns the name of this <code>JavaClass</code>.
     *
     * @return the <code>String</code> name of this <code>JavaClass</code>.
     */
    @Override
    public String getName() {
        return getQualifiedName();
    }

    /**
     * Returns the <code>JavaPackage</code> that this <code>JavaClass</code> belongs to.
     *
     * @return the <code>JavaPackage</code> of this <code>JavaClass</code>.
     */
    @Override
    public JavaPackage getPackage() {
        return new XJCJavaPackageImpl(xjcClass.getPackage(), dynamicClassLoader);
    }

    /**
     * Returns the package name of this <code>JavaClass</code>.
     *
     * @return the <code>String</code> name of this <code>JavaClass'</code> <code>JavaPackage</code>.
     */
    @Override
    public String getPackageName() {
        return xjcClass._package().name();
    }

    /**
     * Returns the fully-qualified name of this <code>JavaClass</code>.
     *
     * @return the <code>String</code> name of this <code>JavaClass</code>.
     */
    @Override
    public String getQualifiedName() {
        if(isArray) {
            if(this.isPrimitive) {
                return getPrimitiveArrayNameFor(xjcClass.fullName());
            }
            return "[L" + xjcClass.fullName();
        }
        return xjcClass.fullName();
    }

    private String getPrimitiveArrayNameFor(String fullName) {
        Class<?> componentClass = ConversionManager.getPrimitiveClass(fullName);
        if(componentClass != null) {
            if(componentClass == ClassConstants.PBYTE) {
                return ClassConstants.APBYTE.getName();
            }
            if(componentClass == ClassConstants.PCHAR) {
                return ClassConstants.APCHAR.getName();
            }
            if(componentClass == ClassConstants.PBOOLEAN) {
                return boolean[].class.getName();
            }
            if(componentClass == ClassConstants.PDOUBLE) {
                return double[].class.getName();
            }
            if(componentClass == ClassConstants.PFLOAT) {
                return float[].class.getName();
            }
            if(componentClass == ClassConstants.PINT) {
                return int[].class.getName();
            }
            if(componentClass == ClassConstants.PLONG) {
                return long[].class.getName();
            }
            if(componentClass == ClassConstants.PSHORT) {
                return short[].class.getName();
            }
        }

        return fullName;
    }

    /**
     * Returns the raw name of this <code>JavaClass</code>.  Array types will
     * have "[]" appended to the name.
     *
     * @return the <code>String</code> raw name of this <code>JavaClass</code>.
     */
    @Override
    public String getRawName() {
        if(isArray) {
            return xjcClass.fullName() + "[]";
        }
        return xjcClass.fullName();
    }

    /**
     * Returns the super class of this <code>JavaClass</code>.
     *
     * @return <code>JavaClass</code> representing the super class of this <code>JavaClass</code>.
     */
    @Override
    public JavaClass getSuperclass() {
        try {
            JClass superClass = xjcClass.superClass();

            if (superClass == null) // if null -> no need to continue
                return null;

            if (superClass instanceof JDefinedClass) {
                if (javaModel != null) {
                    return this.javaModel.getClass(superClass.fullName());
                }
                return new XJCJavaClassImpl((JDefinedClass) superClass, jCodeModel, dynamicClassLoader);
            } else {
                if (javaModel != null) {
                    return this.javaModel.getClass(superClass.fullName());
                }
                JDefinedClass c = jCodeModel._getClass(superClass.fullName());
                return new XJCJavaClassImpl(c, jCodeModel, dynamicClassLoader);
            }

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Type[] getGenericInterfaces() {
        return new Type[0];
    }

    @Override
    public Type getGenericSuperclass() {
        return null;
    }

    /**
     * Indicates if this <code>JavaClass</code> has actual type arguments, i.e. is a
     * parameterized type (for example, <code>List&lt;Employee</code>).
     *
     * @return <code>true</code> if this <code>JavaClass</code> is parameterized, otherwise <code>false</code>.
     */
    @Override
    public boolean hasActualTypeArguments() {
        return xjcClass.typeParams().length > 0;
    }

    /**
     * Indicates if this <code>JavaClass</code> is <code>abstract</code>.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is <code>abstract</code>, otherwise <code>false</code>.
     */
    @Override
    public boolean isAbstract() {
        return xjcClass.isAbstract();
    }

    /**
     * Indicates if this <code>JavaClass</code> is an <code>Annotation</code>.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is an <code>Annotation</code>, otherwise <code>false</code>.
     */
    @Override
    public boolean isAnnotation() {
        return xjcClass.isAnnotationTypeDeclaration();
    }

    /**
     * Indicates if this <code>JavaClass</code> is an Array type.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is an Array type, otherwise <code>false</code>.
     */
    @Override
    public boolean isArray() {
        if (this.isArray) {
            return true;
        }
        return this.xjcClass.isArray();
    }

    /**
     * Indicates if this <code>JavaClass</code> is either the same as, or is a superclass of,
     * the <code>javaClass</code> argument.
     *
     * @param javaClass the <code>Class</code> to test.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is assignable from
     *         <code>javaClass</code>, otherwise <code>false</code>.
     *
     * @see java.lang.Class#isAssignableFrom(Class)
     */
    @Override
    public boolean isAssignableFrom(JavaClass javaClass) {
        if (javaClass == null) {
            return false;
        }

        XJCJavaClassImpl javaClassImpl = (XJCJavaClassImpl) javaClass;
        JClass someClass = javaClassImpl.xjcClass;

        return xjcClass.isAssignableFrom(someClass);
    }

    /**
     * Indicates if this <code>JavaClass</code> is an <code>enum</code>.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is an <code>enum</code>, otherwise <code>false</code>.
     */
    @Override
    public boolean isEnum() {
        return xjcClass.getClassType().equals(ClassType.ENUM);
    }

    /**
     * Indicates if this <code>JavaClass</code> is <code>final</code>.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is <code>final</code>, otherwise <code>false</code>.
     */
    @Override
    public boolean isFinal() {
        return Modifier.isFinal(getModifiers());
    }

    /**
     * Indicates if this <code>JavaClass</code> is an <code>interface</code>.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is an <code>interface</code>, otherwise <code>false</code>.
     */
    @Override
    public boolean isInterface() {
        return xjcClass.isInterface();
    }

    /**
     * Indicates if this <code>JavaClass</code> is an inner <code>Class</code>.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is an inner <code>Class</code>, otherwise <code>false</code>.
     */
    @Override
    public boolean isMemberClass() {
        return this.xjcClass.outer() != null;
    }

    /**
     * Indicates if this <code>JavaClass</code> represents a primitive type.
     *
     * @return <code>true</code> if this <code>JavaClass</code> represents a primitive type, otherwise <code>false</code>.
     */
    @Override
    public boolean isPrimitive() {
        return false;
    }

    /**
     * Indicates if this <code>JavaClass</code> is <code>private</code>.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is <code>private</code>, otherwise <code>false</code>.
     */
    @Override
    public boolean isPrivate() {
        return Modifier.isPrivate(getModifiers());
    }

    /**
     * Indicates if this <code>JavaClass</code> is <code>protected</code>.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is <code>protected</code>, otherwise <code>false</code>.
     */
    @Override
    public boolean isProtected() {
        return Modifier.isProtected(getModifiers());
    }

    /**
     * Indicates if this <code>JavaClass</code> is <code>public</code>.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is <code>public</code>, otherwise <code>false</code>.
     */
    @Override
    public boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    /**
     * Indicates if this <code>JavaClass</code> is <code>static</code>.
     *
     * @return <code>true</code> if this <code>JavaClass</code> is <code>static</code>, otherwise <code>false</code>.
     */
    @Override
    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    /**
     * Not supported.
     */
    @Override
    public boolean isSynthetic() {
        throw new UnsupportedOperationException("isSynthetic");
    }

    @Override
    public JavaClassInstanceOf instanceOf() {
        return JavaClassInstanceOf.XJC_JAVA_CLASS_IMPL;
    }

    /**
     * If this <code>JavaClass</code> is annotated with an <code>Annotation</code> matching <code>aClass</code>,
     * return its <code>JavaAnnotation</code> representation.
     *
     * @param aClass a <code>JavaClass</code> representing the <code>Annotation</code> to look for.
     *
     * @return the <code>JavaAnnotation</code> represented by <code>aClass</code>, if one exists, otherwise return <code>null</code>.
     */
    @Override
    @SuppressWarnings("unchecked")
    public JavaAnnotation getAnnotation(JavaClass aClass) {
        if (aClass != null) {
            Collection<JAnnotationUse> annotations = xjcClass.annotations();
            for (JAnnotationUse annotationUse : annotations) {
                XJCJavaAnnotationImpl xjcAnnotation = new XJCJavaAnnotationImpl(annotationUse, dynamicClassLoader);

                String myAnnotationClass = xjcAnnotation.getJavaAnnotationClass().getCanonicalName();
                String annotationClass = aClass.getQualifiedName();

                if (myAnnotationClass.equals(annotationClass)) {
                    return xjcAnnotation;
                }
            }
            // Didn't find annotation so return null
            return null;
        }
        // aClass was null so return null
        return null;
    }

    /**
     * Return all of the <code>Annotations</code> for this <code>JavaClass</code>.
     *
     * @return A <code>Collection</code> containing this <code>JavaClass'</code> <code>JavaAnnotations</code>.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<JavaAnnotation> getAnnotations() {
        ArrayList<JavaAnnotation> annotationsList = new ArrayList<>();
        Collection<JAnnotationUse> annotations = xjcClass.annotations();
        for (JAnnotationUse annotationUse : annotations) {
            XJCJavaAnnotationImpl xjcAnnotation = new XJCJavaAnnotationImpl(annotationUse, dynamicClassLoader);
            annotationsList.add(xjcAnnotation);
        }
        return annotationsList;
    }

    /**
     * Not supported.
     */
    @Override
    public JavaAnnotation getDeclaredAnnotation(JavaClass arg0) {
        return getAnnotation(arg0);
    }

    /**
     * Not supported.
     */
    @Override
    public Collection<JavaAnnotation> getDeclaredAnnotations() {
        return getAnnotations();
    }

    /**
     * Get this <code>JavaClass'</code> <code>JavaModel</code>.
     *
     * @return The <code>JavaModel</code> associated with this <code>JavaClass</code>.
     */
    public JavaModel getJavaModel() {
        return javaModel;
    }

    /**
     * Set this <code>JavaClass'</code> <code>JavaModel</code>.
     *
     * @param javaModel The <code>JavaModel</code> to set.
     */
    public void setJavaModel(JavaModel javaModel) {
        this.javaModel = javaModel;
    }

}
