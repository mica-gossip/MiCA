package org.princehouse.mica.base.simple;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.annotations.SelectUniformRandom;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.simple.Selector.SelectorAnnotationDecider;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.FunctionalReflection;

import fj.F;

abstract class Selector<Q extends Protocol> {

	public abstract Distribution<Address> select(Runtime<?> rt, Q pinstance);




	// ----------------------- machinery for choosing a selector in the SimpleRuntimeAgent process() function -----------------
	public static abstract class SelectorAnnotationDecider {
		private Class<? extends Annotation> annotationClass = null;
		public SelectorAnnotationDecider(Class<? extends Annotation> annotationClass) {
			this.annotationClass = annotationClass;
		}
		public Class<? extends Annotation> getAnnotationClass() {
			return annotationClass;
		}
		public abstract <P extends Protocol> Selector<P> getSelector(AnnotatedElement element) throws InvalidSelectElement;
		
		// first class function 
		public F<AnnotatedElement,Boolean> accept1() {
			return FunctionalReflection.<AnnotatedElement>hasAnnotation(getAnnotationClass());
		}	
		
		public boolean accept(AnnotatedElement element) {
			return accept1().f(element);
		}
		public static F<SelectorAnnotationDecider, F<AnnotatedElement,Boolean>> getAccept = new F<SelectorAnnotationDecider,F<AnnotatedElement,Boolean>>() {
			@Override
			public F<AnnotatedElement, Boolean> f(SelectorAnnotationDecider arg0) {
				return arg0.accept1();
			}
		};
	}

	// Decider to delegate gossip select function for @Select annotation
	public static class SelectDecider extends SelectorAnnotationDecider {
		public SelectDecider() {
			super(Select.class);
		}
		public <P extends Protocol> Selector<P> getSelector(AnnotatedElement element) throws InvalidSelectElement {
			if (element instanceof Method) {
				return new SelectMethodSelector<P>((Method) element);
			} else if (element instanceof Field) {
				return new SelectFieldSelector<P>((Field) element);
			} else {
				throw new InvalidSelectElement(getAnnotationClass(), element);
			}
		}
	}

	// Decider to delegate gossip select function for @Select annotation
	public static class SelectUniformRandomDecider extends SelectorAnnotationDecider {
		public SelectUniformRandomDecider() {
			super(SelectUniformRandom.class);
		}
		public <P extends Protocol> Selector<P> getSelector(AnnotatedElement element) throws InvalidSelectElement {
			if (element instanceof Field) {
				return new UniformRandomCollectionFieldSelector<P>(
						(Field) element);
			} else {
				throw new InvalidSelectElement(getAnnotationClass(), element);

			}
		}
	}

	// used to associate Select* annotations with their Selector classes
	public static List<SelectorAnnotationDecider> registeredDeciders = Functional.list(new SelectorAnnotationDecider[] {
		new SelectDecider(),
		new SelectUniformRandomDecider()
	});
}