package org.princehouse.mica.util;

import static org.princehouse.mica.util.Functional.filter;
import static org.princehouse.mica.util.Functional.list;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

import fj.F;
import fj.F2;

public class FunctionalReflection {

	public static F<Method, Class<?>> getOriginatingClassMethod = new F<Method,Class<?>>() {
		@Override
		public Class<?> f(Method m) {
			return m.getDeclaringClass();
		}
	};

	public static F<Field, Class<?>> getOriginatingClassField = new F<Field,Class<?>>() {
		@Override
		public Class<?> f(Field m) {
			return m.getDeclaringClass();
		}
	};

	public static <T> F<T, Class<?>> getOriginatingClass() {
		return new F<T,Class<?>>() {
			@Override
			public Class<?> f(T m) {
				return ((Member) m).getDeclaringClass();
			}	
		};
	}

	public static List<Field> getFields(Class<?> k) {
		return Functional.list(k.getFields());
	}

	public static List<Method> getMethods(Class<?> k) {
		return Functional.list(k.getMethods());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<AnnotatedElement> getAnnotatedElements(Class<?> k) {
		return Functional.concatenate( (List) getFields(k), (List) getMethods(k));
	}


	public static <C extends AnnotatedElement> F<C,Boolean> hasAnnotation(final Class<? extends Annotation> annotationClass) {
		return new F<C,Boolean>() {
			@Override
			public Boolean f(C arg0) {
				for(Annotation a : arg0.getAnnotations()) {
					if(a.annotationType().equals(annotationClass))
						return true;

				}
				return false;
			}
		};
	}

	public static List<AnnotatedElement> getAnnotatedElements(Class<?> k, Class<? extends Annotation> annotationClass) {  
		return list(filter(getAnnotatedElements(k), hasAnnotation(annotationClass)));
	}

	public static F2<Class<?>,Class<?>,Boolean> isSubclassOf = new F2<Class<?>,Class<?>,Boolean>() {
		@Override
		public Boolean f(Class<?> k1, Class<?> k2) {
			if(k1 == null) 
				return false;
			else if(k1.equals(k2))
				return true;
			else {
				return f(k1.getSuperclass(),k2);
			}
		}

	};

	public static Comparator<Class<?>> subclassComparator = new Comparator<Class<?>>() {
		@Override
		public int compare(Class<?> k1, Class<?> k2) {
			if(k1.equals(k2)) {
				return 0;
			}
			else if(isSubclassOf.f(k1,k2)) {
				return -1;
			} else if(isSubclassOf.f(k2,k1)) {
				return 1;
			} else {
				// classes not related
				return 0;
			}
		}

	};
}
