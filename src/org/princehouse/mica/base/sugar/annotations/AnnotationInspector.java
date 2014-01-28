package org.princehouse.mica.base.sugar.annotations;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.princehouse.mica.base.simple.ConflictingSelectAnnotationsException;
import org.princehouse.mica.base.simple.SelectException;
import org.princehouse.mica.base.simple.Selector;
import org.princehouse.mica.base.simple.Selector.SelectorAnnotationDecider;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.FunctionalReflection;
import org.princehouse.mica.util.NotFoundException;
import org.princehouse.mica.util.TooManyException;

import fj.F;
import fj.P2;

public class AnnotationInspector {
    
    public static final boolean debugAnnotationInspector = false;
    public static void debug(String str) {
        if(debugAnnotationInspector) {
            System.out.println(str);
        }
    }
    
    public static Method locateUpdateMethod(Class<?> pclass) throws TooManyException, NotFoundException {
        // TODO sanity check that update has the right signature
        try {
            return Functional.findExactlyOne((Iterable<Method>) FunctionalReflection.getMethods(pclass),
                    FunctionalReflection.<Method> hasAnnotation(GossipUpdate.class));
        } catch (TooManyException e) {
            // If multiple options are found, see if one overrides the others by
            // sorting by declaring class subclass relation
            List<Method> options = Functional.mapcast(e.getOptions());
            HashMap<Class<?>, List<Method>> groups = Functional.groupBy(options,
                    FunctionalReflection.getOriginatingClassMethod);
            List<P2<Class<?>, List<Method>>> items = Functional.items(groups);
            Collections.sort(items, Functional.pcomparator(FunctionalReflection.subclassComparator));
            List<Method> methods = items.get(0)._2();
            if (methods.size() > 1)
                throw new TooManyException(Functional.mapcast(methods));
            else {
                return methods.get(0);
            }

        }
        // unreachable
    }

    public static Method locateFrequencyMethod(Class<?> pclass) throws TooManyException, NotFoundException {
        // TODO sanity check that freq has the right signature
        try {
            return Functional.findExactlyOne((Iterable<Method>) FunctionalReflection.getMethods(pclass),
                    FunctionalReflection.<Method> hasAnnotation(GossipRate.class));
        } catch (TooManyException e) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            List<Method> options = (List) e.getOptions();// Functional.mapcast(
            // e.getOptions().get(0));
            HashMap<Class<?>, List<Method>> groups = Functional.groupBy(options,
                    FunctionalReflection.getOriginatingClassMethod);
            List<P2<Class<?>, List<Method>>> items = Functional.items(groups);
            Collections.sort(items, Functional.pcomparator(FunctionalReflection.subclassComparator));
            List<Method> methods = items.get(0)._2();
            if (methods.size() > 1)
                throw new TooManyException(Functional.mapcast(methods));
            else {
                return methods.get(0);
            }
        }
        // unreachable
    }

    public static Selector locateSelectMethod(Class<?> klass) throws NotFoundException, TooManyException,
            SelectException {
        /*
         * Search through fields and functions looking for syntactic sugar
         * Select annotations
         */

        debug(String.format("locateSelectMethod: klass=%s",klass.getName()));
        
        // first class function that tells us if an AnnotatedElement has any of
        // the registered select annotations
        F<AnnotatedElement, Boolean> hasSelectAnnotation1 = Functional.<F<AnnotatedElement, Boolean>> foldl(
                Functional.<AnnotatedElement> or1(),
                Functional.map(Selector.registeredDeciders, SelectorAnnotationDecider.getAccept));

        AnnotatedElement selectElement = null;
        try {
            // search for annotated element in the given class (and its ancestor
            // classes)
            try {
                selectElement = Functional.findExactlyOne(
                        (Iterable<AnnotatedElement>) FunctionalReflection.getAnnotatedElements(klass),
                        hasSelectAnnotation1);
            } catch (NotFoundException nf) {
                Class<?> base = klass.getSuperclass();
                if (base == null)
                    throw nf;
                else
                    locateSelectMethod(base);
            }
        } catch (TooManyException e) {
            // we found more than one annotated element!
            // sort them by order of declaring class w.r.t.
            // inheritance hierarchy

            // get all of the annotated elements
            @SuppressWarnings({ "unchecked", "rawtypes" })
            List<AnnotatedElement> options = (List) e.getOptions();

            // group annotated elements by declaring class
            HashMap<Class<?>, List<AnnotatedElement>> groups = Functional.groupBy(options,
                    FunctionalReflection.<AnnotatedElement> getOriginatingClass());

            // rearrange grouping map as a list of key,value pairs
            List<P2<Class<?>, List<AnnotatedElement>>> items = Functional.items(groups);

            // sort by subclass relation
            Collections.sort(items, Functional.pcomparator(FunctionalReflection.subclassComparator));

            // get elements of the least class (i.e., the farthest descendant
            // subtype)
            List<AnnotatedElement> elements = items.get(0)._2();

            if (elements.size() > 1)
                // More than one declared select element in this class!
                throw new TooManyException(Functional.mapcast(elements));
            else {
                // Great, we decided on exactly one!
                selectElement = elements.get(0);
            }
        }

        final AnnotatedElement temp = selectElement;

        // Which Selectors accept this element?
        List<SelectorAnnotationDecider> acceptingDeciders = Functional.list(Functional.filter(
                Selector.registeredDeciders, new F<SelectorAnnotationDecider, Boolean>() {
                    @Override
                    public Boolean f(SelectorAnnotationDecider d) {
                        return d.accept(temp);
                    }
                }));

        switch (acceptingDeciders.size()) {
            case 0:
                throw new RuntimeException(
                        "Something has gone horribly wrong. Even though a decider chose this selectElement earlier, no deciders now accept it!");
            case 1:
                return acceptingDeciders.get(0).getSelector(selectElement);
            default:
                throw new ConflictingSelectAnnotationsException(selectElement);
        }

    }

}
