package org.princehouse.mica.base.simple;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.princehouse.mica.base.MalformedViewException;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeState;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.base.sugar.annotations.ViewUniformRandom;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Distribution;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.FunctionalReflection;

import fj.F;

public abstract class Selector {

    public abstract Distribution<Address> select(Protocol pinstance) throws SelectException;

    // Utility methods for selecting from various different data types
    @SuppressWarnings("unchecked")
    public static Distribution<Address> asDistribution(Object obj, RuntimeState rts) throws SelectException {
        if (obj instanceof Distribution) {
            return (Distribution<Address>) obj;
        } else if (obj instanceof Protocol) {
            return ((Protocol) obj).getView();
        } else if (obj instanceof Collection) {
            return Distribution.uniform((Collection<Address>) obj);
        } else if (obj instanceof Overlay) {

            Distribution<Address> view = ((Overlay) obj).getOverlay(rts);
            if (view != null && !view.isOne()) {
                throw new MalformedViewException(obj, view);
            }
            return view;
        } else if (obj instanceof Address) {
            return Distribution.singleton((Address) obj);
        } else if (obj == null) {
            return null;
        } else {
            throw new InvalidSelectValue(View.class, obj);
        }
    }

    @SuppressWarnings("unchecked")
    protected static Collection<Address> getCollectionFromValue(Object value, RuntimeState rts) throws SelectException {
        if (value instanceof Collection) {
            // TODO add sanity check for addresses
            return (Collection<Address>) value;
        } else if (value instanceof Overlay) {
            Distribution<Address> dist = ((Overlay) value).getOverlay(rts);
            return dist.keySet();
        } else {
            throw new RuntimeException(String.format("Don't know how to extract view from %s instance", value
                    .getClass().getName()));
        }
    }

    // ----------------------- machinery for choosing a selector in the
    // SimpleRuntimeAgent process() function -----------------
    public static abstract class SelectorAnnotationDecider {
        private Class<? extends Annotation> annotationClass = null;

        public SelectorAnnotationDecider(Class<? extends Annotation> annotationClass) {
            this.annotationClass = annotationClass;
        }

        public Class<? extends Annotation> getAnnotationClass() {
            return annotationClass;
        }

        public abstract <P extends Protocol> Selector getSelector(AnnotatedElement element) throws SelectException;

        // first class function
        public F<AnnotatedElement, Boolean> accept1() {
            return FunctionalReflection.<AnnotatedElement> hasAnnotation(getAnnotationClass());
        }

        public boolean accept(AnnotatedElement element) {
            return accept1().f(element);
        }

        public static F<SelectorAnnotationDecider, F<AnnotatedElement, Boolean>> getAccept = new F<SelectorAnnotationDecider, F<AnnotatedElement, Boolean>>() {
            @Override
            public F<AnnotatedElement, Boolean> f(SelectorAnnotationDecider arg0) {
                return arg0.accept1();
            }
        };
    }

    // Decider to delegate gossip select function for @Select annotation
    public static class SelectDecider extends SelectorAnnotationDecider {
        public SelectDecider() {
            super(View.class);
        }

        public <P extends Protocol> Selector getSelector(AnnotatedElement element) throws InvalidSelectElement {
            if (element instanceof Method) {
                return new SelectMethodSelector((Method) element);
            } else if (element instanceof Field) {
                return new SelectFieldSelector((Field) element);
            } else {
                throw new InvalidSelectElement(getAnnotationClass(), element);
            }
        }
    }

    // Decider to delegate gossip select function for @Select annotation
    public static class SelectUniformRandomDecider extends SelectorAnnotationDecider {
        public SelectUniformRandomDecider() {
            super(ViewUniformRandom.class);
        }

        public <P extends Protocol> Selector getSelector(AnnotatedElement element) throws InvalidSelectElement {
            if (element instanceof Field) {
                return new UniformRandomCollectionFieldSelector((Field) element);
            } else {
                throw new InvalidSelectElement(getAnnotationClass(), element);

            }
        }
    }

    // used to associate Select* annotations with their Selector classes
    public static List<SelectorAnnotationDecider> registeredDeciders = Functional.list(new SelectorAnnotationDecider[] {
            new SelectDecider(), new SelectUniformRandomDecider() });
}