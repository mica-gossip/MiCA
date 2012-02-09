package org.princehouse.mica.util;


import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.util.Files;


public class ClassUtils {
	public static boolean classImplements(Class<?> klass, Class<?> iface) {
		assert(iface.isInterface());
		for(Class<?> i : klass.getInterfaces()) {
			if(i.equals(iface))
				return true;
		}
		if(klass.equals(java.lang.Object.class))
			return false;
		return classImplements(klass.getSuperclass(),iface);
	}

	public static byte[] getClassBytecode(final Class<?> klass) throws IOException {
		String location = getClassLocation(klass);
		assert(location != null);
		return Files.readEntireFile(new File(location));
	}
	
	public static String getClassLocation(final Class<?> pClass) {
		final String location, name;
		name = pClass.getName().replaceAll("\\.", "/") + ".class";
		location = ClassLoader.getSystemResource(name).getPath();
		return location;
	}

	public static boolean classExtends(Class<?> k, Class<?> parentClass) {
		if(k.equals(parentClass)) 
			return true;
		if(k.equals(java.lang.Object.class))
			return false;
		return classExtends(k.getSuperclass(), parentClass);
	}


	/**
	 * Return all methods from a class that are annotated with Exchange
	 * Use getExchangeAnnotation(method) to get the annotation proxy.
	 * @param k A class.
	 * @return List of methods that have an Exchange annotation.
	 */
	public static List<Method> getExchangeMethods(Class<?> k) {
		List<Method> results = new LinkedList<Method>();
		for(Method m : k.getMethods()) {
			if(getExchangeAnnotation(m) != null)
				results.add(m);
		}
		return results;
	}
	
	/**
	 * Return the Exchange annotation proxy from a method, or null if none exists.
	 * @param m A method.
	 * @return Annotation proxy or null.
	 */
	public static Annotation getExchangeAnnotation(Method m) {
		for(Annotation a : m.getAnnotations()) {
			Class<?> k = a.annotationType();
			if(k.equals(GossipUpdate.class))
				return a;
		}
		return null;
	}
	
	
}
