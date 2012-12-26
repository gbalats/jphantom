package util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class Combinations<T extends List<E>, E> {
	
	/* Fields */
	
	private final T col;
	private final Factory<T> factory;
	
	/* Constructor */
	
    public Combinations(T col, Factory<T> f) {
    	this.col = col;
    	this.factory = f;
    }
    
    /* Methods */
    
    public T list() { return col; }
    
    public T minus(T subset) {
    	T result = factory.create();
    	//for(E e : col)
    	//	if (!subset.get().contains(e))
    	//		result.add(e);
    	
    	Iterator<E> it = col.iterator(), subit = subset.iterator();
    	
    	/* Advance pointers in parallel */
    	while (it.hasNext() && subit.hasNext()) {
    		E e = it.next(), sube = subit.next();
    		try {
    			/* Advance one pointer only until it matches the other */
                while (!e.equals(sube)) {
                    result.add(e);
                    e = it.next();     // May throw exception
                }
    		} catch (NoSuchElementException exc) { break; }
    	}
    	/* Append remaining */
    	while (it.hasNext())
    		result.add(it.next());
		return result;
    }
    
    /** Returns all the possible subsets of a collection */
    public Iterable<T> subsets() {
    	return subsets(false);
    }
    
    /** Returns all the possible strict subsets of a collection */
    public Iterable<T> strictSubsets() {
    	return subsets(true);
    }
    
    private Iterable<T> subsets(final boolean strict) {
    	return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					
					private long state;
					private final int offset; {
						offset = strict ? 1 : 0;
					}
					
					@Override
					public boolean hasNext() {
						return state + offset != 1 << col.size();
					}

					@Override
					public T next()
					{
						if (!hasNext())
							throw new NoSuchElementException();
						
						T result = factory.create();
						long bitmap = state++;
						
						/* Each bit determines if its corresponding element
						 * is included in this iteration's subset */
						for (E e : col) {
							if ((bitmap & 1) == 1)
								result.add(e);
							bitmap >>= 1;
							if (bitmap == 0) break;
						}
						return result;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
    	};
    }

    /** Returns all the possible subsets without left-right repetition */
    public Iterable<T> uniqueSubsets() {
    	return usubsets();
    }

    private Iterable<T> usubsets() {
    	return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					
					private long state;
					
					@Override
					public boolean hasNext() {
						return state + 1 != 1 << (col.size() - 1);
					}

					@Override
					public T next()
					{
						if (!hasNext())
							throw new NoSuchElementException();
						
						T result = factory.create();
						long bitmap = state++;
						
						/* Each bit determines if its corresponding element
						 * is included in this iteration's subset */
						boolean first = true;
						for (E e : col) {
							if (first) {
								result.add(e);
								first = false;
							} else {
								if ((bitmap & 1) == 1)
									result.add(e);
								bitmap >>= 1;
								if (bitmap == 0) break;
							}
						}
						return result;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
    	};
    }
	
	/* Index as an inner class */
	
    public class Index<S> {

    	private E[] from;
        private S[] to;

        @SuppressWarnings("unchecked")
		public Index(Class<S> clazz) {
        	from = (E[]) col.toArray();
            to = (S[]) Array.newInstance(clazz, 1 << col.size());
        }
    
        /** Getter for a set */
        public S get(Set<E> subset) {
			return get(ordered(subset));
        }
        
        private T ordered(Set<E> subset) {
        	T ordered = factory.create();
			// Enforce Order
			for (E old : col) {
				if (subset.contains(old))
                    ordered.add(old);
			}
			// All fields must have been found
			if (ordered.size() != subset.size())
				throw new IllegalArgumentException();
			return ordered;
        }
        
        public S get(T subset) {
            int bitmap = 0, i = 0;
            for (E e : subset) {
                while (!e.equals(from[i]))
                    i++;
                bitmap |= (1 << i);
            }
            return to[bitmap];
        }

        public void set(T subset, S value) {
            int bitmap = 0, i = 0;
            for (E e : subset) {
                while (!e.equals(from[i]))
                    i++;
                bitmap |= (1 << i);
            }
            to[bitmap] = value;
        }
        
        public void set(Set<E> subset, S value) {
            set(ordered(subset), value);
        }
    }
    
	public static void main(String[] args)
	{
		Combinations<List <String>, String> s = new Combinations<List<String>, String>(
				new ArrayList<String>(Arrays.asList("a", "b", "c", "d")),
				new Factory<List<String>>() {
					public List<String> create() {
						return new ArrayList<String>();
					}
				}
		);
		for (List <String> subset : s.uniqueSubsets()) {
			for (String str : subset)
				System.out.print(str + " ");
			System.out.println("");
		}
		
		Combinations<List <String>, String>.Index <Integer> d = s.new Index <Integer> (Integer.class);
		int i = 0;
		System.out.println("Setting...");
		for (List <String> sl : s.subsets()) {
			for (String n : sl)
				System.out.print(n + " ");
			System.out.println(" -> " + i);
			d.set(sl, new Integer(i++));
		}
		System.out.println("Getting...");
		for (List <String> sl : s.subsets()) {
			for (String n : sl)
				System.out.print(n + " ");
			System.out.print("-> " + d.get(sl).intValue() + " | ");
			List <String> nsl = s.minus(sl);
			for (String n : nsl)
				System.out.print(n + " ");
			System.out.println("-> " + d.get(nsl).intValue());
		}
	}
}
