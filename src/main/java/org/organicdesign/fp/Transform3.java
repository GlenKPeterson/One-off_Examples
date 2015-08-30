package org.organicdesign.fp;

import org.organicdesign.fp.collections.UnmodIterable;
import org.organicdesign.fp.collections.UnmodSortedIterator;
import org.organicdesign.fp.function.Function1;
import org.organicdesign.fp.function.Function2;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

// We model this as a linked list so that each transition can have it's own output type, building a type-safe bridge
// from first operation to the last.
public interface Transform3<A,B> { // extends Transformable<A> {

    // TODO: Need to ensure that take comes after drop!

    // enum OpStrategy { FINITE_DROPPABLE, INFINITE_NOT_DROPPABLE }

    interface MutableSourceProvider<T> extends UnmodIterable<T> {
        @Override MutableSource<T> iterator();
    }

    /**
     Like Iterator, this interface is inherently not thread-safe, so wrap it in something thread-safe before
     sharing across threads.
     */
    interface MutableSource<T> extends UnmodSortedIterator<T> {
        public static final MutableSource<?> EMPTY = new MutableSource<Object>() {
            @Override public boolean hasNext() { return false; }
            @Override public Object next() { throw new NoSuchElementException("No more elements"); }
            @Override public int drop(int i) { return i; }
            @Override public int take(int i) { return i; }
        };
        @SuppressWarnings("unchecked")
        static <X> MutableListSource<X> empty() { return (MutableListSource<X>) EMPTY; }

        /**
         Drops as many items as the source can handle.
         @param d the number of items to drop
         @return the number of drops left over when this source is exhausted (flatMap needs to know this).
         0 if d &lt; number of items left in source.  Otherwise, returns d - numItemsActuallyDropped.
         */
        int drop(int d);

        /**
         Takes as many items as the source can handle.
         @param t the number of items to take.
         @return the number of "takes" left over when this source is exhausted (flatMap needs to know this).
         0 if t &lt;= the number of items left in the source.  Otherwise returns t - numItemsActuallyTaken
         */
        int take(int t);

        class MutableListSource<T> extends OpRun implements MutableSource<T> {
            final List<T> items;
            int idx;
            int size;

            MutableListSource(List<T> ls, int i) { items = ls; idx = i; size = items.size(); }

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                return idx < size;
            }

            /** {@inheritDoc} */
            @Override public T next() {
                // Breaking into 3 statements was a clear 8% speed-up over the ++ operator in my tests.
                T ret = items.get(idx);
                idx = idx + 1;
                return ret;
            }

            /** {@inheritDoc} */
            @Override public int drop(int d) {
                if (d <= 0) { return 0; }
                int numItems = size - idx;
                if (d > numItems) {
                    int ret = d - numItems;
                    idx = size; // used up.
                    return ret;
                }
                idx = idx + d;
                return 0;
            }

            /** {@inheritDoc} */
            @Override public int take(int t) {
                // Taking none is equivalent to an empty source.
                if (t < 1) {
                    idx = size;
                    return 0;
                }

                int numItems = size - idx;
                if (t > numItems) {
                    // Taking more items doesn't affect us, it only affects the caller.  Just
                    // adjust their total by the number that we can handle.
                    return t - numItems;
                } // Can take them all.

                size = idx + t;
                return 0;
            }
        } // end class MutableListSource

        // TODO: Need MutableArraySource and MutableIterableSource
    } // end interface MutableSource

    abstract class OpRun {
        OpRun nextOp = null;
//        Function1<Object,Boolean> terminate = null;
        Function1<Object,Boolean> filter = null;
        Function1 map = null;
        Function1<Object,MutableSourceProvider> flatMap = null;

//        public abstract Operation drop(int num);

//        public abstract boolean doDrop(int num);

        private static class FilterRun extends OpRun {
            FilterRun(Function1<Object,Boolean> func, OpRun next) {
                filter = func; nextOp = next;
            }
        }

        private static class MapRun extends OpRun {
            MapRun(Function1 func, OpRun next) {
                map = func; nextOp = next;
            }
        }

        private static class FlatMapRun extends OpRun {
//            ListSourceDesc<U> cache = null;
//            int numToDrop = 0;

            FlatMapRun(Function1<Object,MutableSourceProvider> func, OpRun next) {
                flatMap = func; nextOp = next;
            }

//            @Override
//            public Option<U> next() {
//                while ((cache == null) || (cache.idx == cache.as.size())) {
//                    Option<T> next = prevOp.next();
//                    if (next.isSome()) {
//                        cache = new ListSourceDesc<>(f.apply(next.get()));
//                    } else {
//                        return Option.none();
//                    }
//                    if (numToDrop > 0) {
//                        if (numToDrop >= cache.as.size()) {
//                            numToDrop -= cache.as.size();
//                            cache = null;
//                        } else {
//                            cache.idx += numToDrop;
//                            numToDrop = 0;
//                        }
//                    }
//                }
//                return cache.next();
//            }
        }

    }

    class RunList implements MutableSourceProvider {
        MutableSource source;
        List<OpRun> list;
        OpRun[] opArray() {
            return list.toArray(new OpRun[list.size()]);
        }
        @Override public MutableSource iterator() { return source; }
    }

    // Using an abstract class here to limit the visibility of the next() method and present a less mutable
    // interface to the outside world.

    /**
     A description of an operation to be performed.

     @param <F> The input type to this OpDesc (if input and output type is the same, make this match G because the
               output type is dominant for this class.
     @param <G> the output type for this OpDesc.
     */
    static abstract class OpDesc<F,G> implements Transform3<F,G> {
        final OpDesc<?,F> prevOp;
//        final int drop;
//        final int take;
        OpDesc(OpDesc<?,F> pre) { prevOp = pre; }

        /** Gets the next item. */
//        abstract Option<F> next();
        abstract RunList toRunList(List<OpRun> ops);

        // Do I want to go back to modeling this as a separate step, then compress the steps
        // as much as possible to move the drop and take operations as early in the stream as possible.

        @SuppressWarnings("unchecked")
        @Override public OpDesc<F,F> drop(int i) {
            return new DropDesc<>((OpDesc<?,F>) this, i);
        }

//        public int dropAmt() { return drop; }

//        public int takeAmt() { return take; }

        @SuppressWarnings("unchecked")
        @Override public OpDesc<F,F> filter(Function1<? super F,Boolean> f) {
            return new FilterDesc<>((OpDesc<?,F>) this, f);
        }

        @SuppressWarnings("unchecked")
        @Override public OpDesc<F,MutableSourceProvider<G>> flatMap(Function1<? super F,MutableSourceProvider<G>> f) {
            return new FlatMapDesc<>((OpDesc<?,F>) this, f);
        }

        @SuppressWarnings("unchecked")
        @Override public OpDesc<F,G> map(Function1<? super F, ? extends G> f) {
            return new MapDesc<>((OpDesc<?,F>) this, f);
        }

        /** Provides a way to collect the results of the transformation. */
    //    @Override
        @Override public G foldLeft(G ident, Function2<G,? super F,G> reducer) {

            // Construct an optimized array of OpRuns (mutable operations for this run)
            RunList runList = toRunList(new ArrayList<>());

            // Actually do the fold.
            return _foldLeft(runList, runList.opArray(), 0, ident, reducer);
        }

        // We used a linked-list to build the type-safe operations so if that code compiles, the types should work out
        // here too.  However, for performance, we don't want to be stuck creating and passing Options around,
        // nor do we want a telescoping stack of hasNext() and next() calls.  So we're abandoning type safety
        // and calling all the intermediate results Objects.
        @SuppressWarnings("unchecked")
        private G _foldLeft(MutableSourceProvider source, OpRun[] ops, int opIdx, G ident, Function2 reducer) {
            Object ret = ident;
            for (Object o : source) {
                for (int j = opIdx; j < ops.length; j++) {
                    OpRun op = ops[j];
                    if ( (op.filter != null) && !op.filter.apply(o) ) {
                        break; // stop processing this soruce item and go to the next one.
                    }
                    if (op.map != null) {
                        o = op.map.apply(o);
                    } else if (op.flatMap != null) {
                        ret = _foldLeft(op.flatMap.apply(o), ops, j + 1, (G) ret, reducer);
                        break;
                    }
//                    if ( (op.terminate != null) && op.terminate.apply(o) ) {
//                        return (G) ret;
//                    }
                }
                ret = reducer.apply(ret, o);
            }
            return (G) ret;
        }

        private static class DropDesc<T> extends OpDesc<T,T> {
            private final int drop;
            DropDesc(OpDesc<?,T> prev, int d) { super(prev); drop = d; }

            @SuppressWarnings("unchecked")
            @Override RunList toRunList(List<OpRun> ops) {
                Mutable.IntRef leftToDrop = Mutable.IntRef.of(drop);
                RunList ret = prevOp.toRunList(ops);
                ret.list.add(new OpRun.FilterRun((t) -> {
                    if (leftToDrop.value() > 0) {
                        leftToDrop.set(leftToDrop.value() - 1);
                        return Boolean.FALSE;
                    }
                    return Boolean.TRUE; },
                                                 (ret.list.size() > 0) ? ret.list.get(ret.list.size() - 1) : null));
                for (int i = ret.list.size() - 1; i >= 0; i--) {
                    OpRun or = ret.list.get(i);
                    if (or instanceof MutableSource) {
                        MutableSource ms = (MutableSource) or;
                        int leftover = ms.drop(drop);
                        if (leftover != 0) {
                            // What's the best way to handle this?  I'm throwing an exception until I figure it out.
                            throw new IllegalStateException("Asked to drop " + drop + " items, but there were only " +
                                                                    (drop - leftover) + " to drop.");
                        }
                    }
                }
                return ret;
            }
        }

        private static class FilterDesc<T> extends OpDesc<T,T> {
            final Function1<? super T,Boolean> f;

            FilterDesc(OpDesc<?,T> prev, Function1<? super T,Boolean> func) { super(prev); f = func; }

            @SuppressWarnings("unchecked")
            @Override RunList toRunList(List<OpRun> ops) {
                RunList ret = prevOp.toRunList(ops);
                ret.list.add(new OpRun.FilterRun((Function1<Object,Boolean>) f,
                                                 (ret.list.size() > 0) ? ret.list.get(ret.list.size() - 1) : null));
                return ret;
            }
        }

        private static class MapDesc<T,U> extends OpDesc<T,U> {
            final Function1<? super T,? extends U> f;

            MapDesc(OpDesc<?,T> prev, Function1<? super T,? extends U> func) { super(prev); f = func; }

            @SuppressWarnings("unchecked")
            @Override RunList toRunList(List<OpRun> ops) {
                RunList ret = prevOp.toRunList(ops);
                ret.list.add(new OpRun.MapRun(f, (ret.list.size() > 0) ? ret.list.get(ret.list.size() - 1) : null));
                return ret;
            }
        }

        private static class FlatMapDesc<T,U> extends OpDesc<T,MutableSourceProvider<U>> {
            final Function1<? super T,MutableSourceProvider<U>> f;
            FlatMapDesc(OpDesc<?,T> prev, Function1<? super T,MutableSourceProvider<U>> func) {
                super(prev); f = func;
            }

            @SuppressWarnings("unchecked")
            @Override RunList toRunList(List<OpRun> ops) {
                RunList ret = prevOp.toRunList(ops);
                ret.list.add(new OpRun.FlatMapRun((Function1) f,
                                                  (ret.list.size() > 0) ? ret.list.get(ret.list.size() - 1) : null));
                return ret;
            }
        }
    } // end abstract class OpDesc

    // This is just a sample usage to be sure it compiles.
//    Integer total = from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9))
//            .drop(1)
//            .filter(i -> i < 7)
//            .map(String::valueOf)
//            .flatMap(s -> Arrays.asList(s, s, s))
//            .foldLeft(0, (count, s) -> count + 1);

    class SourceProviderDesc<T> extends OpDesc<T,T> {
        private final List<T> list;
        SourceProviderDesc(List<T> l) {
            super(null);
            list = l;
        }

        @Override RunList toRunList(List<OpRun> ops) {
            ops.add(new MutableSource.MutableListSource<>(list, 0));
            return null;
        }
    }


    /** Constructor.  Need to add an Iterable constructor and maybe some day even an array constructor. */
    static <T> Transform3<?,T> from(List<T> list) { return new SourceProviderDesc<>(list); }

    // ================================================================================================================
    // These will come from Transformable, but (will be) overridden to have a different return type.

    /** The number of items to drop from the beginning of the output.  The drop happens before take(). */
    Transform3<A,A> drop(int n);

//    @Override
    Transform3<A,A> filter(Function1<? super A,Boolean> f);

    Transform3<A,MutableSourceProvider<B>> flatMap(Function1<? super A,MutableSourceProvider<B>> f);

//    @Override
    Transform3<A,B> map(Function1<? super A, ? extends B> f);

    /** Provides a way to collect the results of the transformation. */
//    @Override
    B foldLeft(B ident, Function2<B,? super A,B> f);
}
