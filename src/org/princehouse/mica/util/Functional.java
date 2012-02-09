package org.princehouse.mica.util;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import fj.Effect;
import fj.F;
import fj.F2;
import fj.P;
import fj.P2;
import fj.data.Option;

public class Functional {

	/**
	 * Concatenate one or more lists Always creates a new list
	 * 
	 * @param lists
	 * @return
	 */
	public static <T> List<T> concatenate(List<T>... lists) {
		List<T> temp = list();
		for (List<T> l : lists) {
			temp = extend(temp, l);
		}
		return temp;
	}

	/**
	 * Return and identity function
	 * 
	 * @param <T>
	 * @return
	 */
	public static <T> F<T, T> identity() {
		return new F<T, T>() {
			@Override
			public T f(T arg0) {
				return arg0;
			}
		};
	}

	/**
	 * Turn an iterator into an iterable This can only be iterated over once; it
	 * iterator() is called a second time, it throws an IteratorSpentException
	 * 
	 * @param <T>
	 * @param it
	 * @return
	 */
	public static <T> Iterable<T> iteratorAsIterable(final Iterator<T> it) {
		return new Iterable<T>() {
			private boolean spent = false;

			@Override
			public Iterator<T> iterator() {
				if (spent) {
					throw new IteratorSpentException();
				} else {
					spent = true;
					return it;
				}
			}

		};
	}

	/**
	 * Add everything from an iterable to a collection and then return the
	 * collection
	 * 
	 * @param collection
	 * @param iterable
	 * @return
	 */
	public static <C extends Collection<T>, T, S extends T> C extend(
			C collection, Iterable<S> iterable) {
		for (T q : iterable) {
			collection.add(q);
		}
		return collection;

	}

	@SuppressWarnings("unchecked")
	// stupid that it gives a warning for instanceof.... runtime generics are
	// erased, it's impossible to supply a type parameter
	/**
	 * Return the supplied iterable as a java.util.List.  If it is already a list, return it instead of creating a new one.
	 */
	public static <T> List<T> list(Iterable<T> iterable) {
		if (iterable instanceof List) {
			return (List<T>) iterable;
		}
		return extend(Functional.<T> list(), iterable);
	}

	@SuppressWarnings("unchecked")
	public static <T> Set<T> set(Iterable<T> iterable) {
		if (iterable instanceof Set) {
			return (Set<T>) iterable;
		}
		return extend(Functional.<T> set(), iterable);
	}

	public static <T> List<T> list(T[] tobj) {
		return extend(Functional.<T> list(), tobj);
	}

	public static <T> List<T> extend(List<T> l, T[] arr) {
		for (T x : arr) {
			l.add(x);
		}
		return l;
	}

	/**
	 * Return an empty java.util.LinkedList
	 * 
	 * @param <T>
	 * @return
	 */
	public static <T> List<T> list() {
		return new LinkedList<T>();
	}

	public static <T> Set<T> set() {
		return new HashSet<T>();
	}

	public static <T> Set<T> set(T item) {
		Set<T> s = set();
		s.add(item);
		return s;
	}

	/**
	 * Zip two iterables
	 * 
	 * @param <S>
	 * @param <T>
	 * @param s
	 * @param t
	 * @return
	 */
	public static <S, T> Iterable<P2<S, T>> zip(final Iterable<S> s,
			final Iterable<T> t) {
		return new Iterable<P2<S, T>>() {

			@Override
			public Iterator<P2<S, T>> iterator() {
				return new ImmutableIterator<P2<S, T>>() {
					private Iterator<S> sit = s.iterator();
					private Iterator<T> tit = t.iterator();

					@Override
					public boolean hasNext() {
						return sit.hasNext() && tit.hasNext();
					}

					@Override
					public P2<S, T> next() {
						return P.p(sit.next(), tit.next());
					}

				};
			}

		};
	}

	/**
	 * Given an iterable [a,b,c,d] returns an iterable of pairs [ (a,b), (b,c),
	 * (c,d) ]
	 * 
	 * @param <T>
	 * @param it
	 * @return
	 */
	public static <T> Iterable<P2<T, T>> segments(final Iterable<T> it) {
		return new Iterable<P2<T, T>>() {
			@Override
			public Iterator<P2<T, T>> iterator() {
				Iterator<T> i1 = it.iterator();
				Iterator<T> i2 = it.iterator();
				if (i1.hasNext())
					i1.next();
				return zip(iteratorAsIterable(i1), iteratorAsIterable(i2))
						.iterator();
			}
		};
	}

	/**
	 * Map an iterable using a supplied function
	 * 
	 * @param <S>
	 * @param <T>
	 * @param s
	 * @param f
	 * @return
	 */
	public static <S, T> Iterable<T> map(final Iterable<S> s, final F<S, T> f) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new ImmutableIterator<T>() {
					private Iterator<S> sit = s.iterator();

					@Override
					public boolean hasNext() {
						return sit.hasNext();
					}

					@Override
					public T next() {
						return f.f(sit.next());
					}
				};
			}
		};
	}

	/**
	 * Map the segments of an iterable using a function (calls segments() to get
	 * the segments)
	 * 
	 * @param <S>
	 * @param <T>
	 * @param s
	 * @param f
	 * @return
	 */
	public static <S, T> Iterable<T> mapSegments(final Iterable<S> s,
			final F2<S, S, T> f) {
		return map(segments(s), new F<P2<S, S>, T>() {
			@Override
			public T f(P2<S, S> p) {
				return f.f(p._1(), p._2());
			}
		});
	}

	/**
	 * Convert an effect to a Void function
	 * 
	 * @param <T>
	 * @param e
	 * @return
	 */
	public static <T> F<T, Void> effectToF(final Effect<T> e) {
		return new F<T, Void>() {
			@Override
			public Void f(T x) {
				e.e(x);
				return null;
			}
		};
	}

	/**
	 * Apply an effect in parallel to a list
	 * 
	 * @param <S>
	 * @param <T>
	 * @param input
	 * @param e
	 */
	public static <S, T> void applyParallel(List<S> input, Effect<S> e) {
		mapParallel(input, effectToF(e));
	}

	/**
	 * Number of threads to use for parallel operations
	 */
	private static final int N_THREADS = 7;

	/**
	 * Use parallel operations?
	 */
	public static final boolean MULTITHREADED = true;

	/**
	 * Defer the execution of f(s) until call() is called
	 * 
	 * @author lonnie
	 * 
	 * @param <S>
	 * @param <T>
	 */
	public static class DeferredF<S, T> implements Callable<T> {
		private S s;
		private F<S, T> f;

		public DeferredF(F<S, T> f, S s) {
			this.s = s;
			this.f = f;
		}

		@Override
		public T call() throws Exception {
			return f.f(s);
		}

		// returns a function that creates a deferredF when an input is given
		public static <S, T> F<S, DeferredF<S, T>> create(final F<S, T> f) {
			return new F<S, DeferredF<S, T>>() {
				@Override
				public DeferredF<S, T> f(S s) {
					return new DeferredF<S, T>(f, s);
				}
			};
		}
	};

	/**
	 * Return only elements x of the iterable for which prop(x) is true
	 * 
	 * @param <T>
	 * @param itr
	 * @param filt
	 * @return
	 */
	public static <T> Iterable<T> filter(Iterable<T> itr, F<T, Boolean> filt) {

		List<T> temp = Functional.list();
		for (T x : itr) {
			if (filt.f(x))
				temp.add(x);
		}
		return temp;
	}

	/**
	 * Map in parallel
	 * 
	 * @param <S>
	 * @param <T>
	 * @param input
	 * @param f
	 * @return
	 */
	public static <S, T> Iterable<T> mapParallel(Iterable<S> input,
			final F<S, T> f) {

		if (!MULTITHREADED) {
			return list(map(input, f));
		} else {
			List<S> inputl = list(input);
			int n = inputl.size();
			assert (n > 0);
			ExecutorService pool = Executors.newFixedThreadPool(Math.min(n,
					N_THREADS));

			List<DeferredF<S, T>> tasks = list(map(inputl, DeferredF.create(f)));

			// System.out.printf("map parallel %d tasks\n",tasks.size());

			try {
				List<Future<T>> futures = pool.invokeAll(tasks);
				pool.shutdown();
				return list(map(futures, new F<Future<T>, T>() {
					@Override
					public T f(Future<T> future) {
						try {
							assert (future != null);
							T val = future.get();
							// System.out.printf("GET FUTURE (done? %s) %s\n",future.isDone(),val);
							return val;
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							throw new RuntimeException(e);
						} catch (ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}
				}));
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				throw new RuntimeException(e1);
			}
		}
	}

	public static <T> Set<T> intersection(Set<T> a, Set<T> b) {
		Set<T> i = set();
		for (T o : a) {
			if (b.contains(o))
				i.add(o);
		}
		return i;
	}

	/**
	 * Return the first object from the list for which f evaluates to true
	 * Returns Option.none if none
	 * 
	 * @param l
	 * @param f
	 * @return
	 */
	public static <T> Option<T> find(List<T> l, F<T, Boolean> f) {
		for (T obj : l) {
			if (f.f(obj))
				return Option.some(obj);
		}
		return Option.none();
	}

	public static <T> Set<T> union(Set<T> a, Set<T> b) {
		return extend(extend(Functional.<T> set(), a), b);
	}

	@SuppressWarnings("unchecked")
	public static <T> T findExactlyOne(Iterable<T> objects, F<T, Boolean> f)
			throws TooManyException, NotFoundException {
		List<T> temp = list(filter(objects, f));
		if (temp.size() > 1) {
			throw new TooManyException((List<Object>) temp);
		} else if (temp.size() == 1) {
			return temp.get(0);
		} else {
			throw new NotFoundException();
		}
	}

	public static <A, B> List<B> mapcast(final List<A> options) {
		return (List<B>) map(options, new F<A, B>() {
			@SuppressWarnings("unchecked")
			@Override
			public B f(A a) {
				return (B) a;
			}
		});
	}

	public static <A, B> HashMap<A, B> hashMap() {
		return new HashMap<A, B>();
	}

	public static <A, B> HashMap<B, List<A>> groupBy(List<A> list,
			F<A, B> keyfunc) {
		HashMap<B, List<A>> h = Functional.hashMap();
		for (A obj : list) {
			B key = keyfunc.f(obj);
			if (!h.containsKey(key)) {
				h.put(key, Functional.<A> list());
			}
			h.get(key).add(obj);
		}
		return h;
	}

	public static <A, B> List<P2<A, B>> items(HashMap<A, B> h) {
		return list(map(h.entrySet(), new F<Entry<A, B>, P2<A, B>>() {
			@Override
			public P2<A, B> f(Entry<A, B> e) {
				return P.p(e.getKey(), e.getValue());
			}
		}));
	}

	public static <A> Comparator<P2<A, ?>> pcomparator(final Comparator<A> c) {
		return new Comparator<P2<A, ?>>() {
			@Override
			public int compare(P2<A, ?> o1, P2<A, ?> o2) {
				return c.compare(o1._1(), o2._1());
			}

		};
	}

	public static <A> F<A, Boolean> or(
			final F<A, Boolean> a,
			final F<A, Boolean> b) {
		return new F<A,Boolean>() {
			@Override
			public Boolean f(A arg) {
				return a.f(arg) || b.f(arg);
			}	
		};
	}
	
	public static <A> F<A, Boolean> and(
			final F<A, Boolean> a,
			final F<A, Boolean> b) {
		return new F<A,Boolean>() {
			@Override
			public Boolean f(A arg) {
				return a.f(arg) && b.f(arg);
			}	
		};
	}

	public static <K,V> Map<K,V> map() {
		return new HashMap<K,V>();
	}
}
