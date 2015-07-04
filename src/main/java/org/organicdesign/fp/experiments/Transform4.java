package org.organicdesign.fp.experiments;

import org.organicdesign.fp.Mutable;
import org.organicdesign.fp.collections.UnmodIterable;
import org.organicdesign.fp.collections.UnmodSortedIterator;
import org.organicdesign.fp.function.Function1;
import org.organicdesign.fp.function.Function2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// We model this as a linked list so that each transition can have it's own output type, building a type-safe bridge
// from first operation to the last.
public interface Transform4<A> { // extends Transformable<A> {

    // TODO: Need to ensure that take comes after drop!

    enum DropStrategy { HANDLE_INTERNALLY, ASK_SUPPLIER, CANNOT_DROP; }

    interface MutableSourceProvider<T> extends UnmodIterable<T> {
        @Override MutableSource<T> iterator();
    }

    /**
     Like Iterator, this interface is inherently not thread-safe, so wrap it in something thread-safe before
     sharing across threads.
     */
    interface MutableSource<T> extends UnmodSortedIterator<T> {
//        public static final MutableSource<?> EMPTY = new MutableSource<Object>() {
//            @Override public boolean hasNext() { return false; }
//            @Override public Object next() { throw new NoSuchElementException("No more elements"); }
//            @Override public DropStrategy drop(int i) { return DropStrategy.HANDLE_INTERNALLY; }
//            @Override public int take(int i) { return i; }
//        };
//        @SuppressWarnings("unchecked")
//        static <X> MutableListSource<X> empty() { return (MutableListSource<X>) EMPTY; }

        /**
         Drops as many items as the source can handle.
         @param d the number of items to drop
         @return true if the source knows it can handle dropping all these items.
         */
        DropStrategy drop(int d);

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
            @Override public DropStrategy drop(int d) {
                if (d < 1) { return DropStrategy.HANDLE_INTERNALLY; }
                int numItems = size - idx;
                if (d > numItems) {
                    idx = size; // used up.
                }
                idx = idx + d;
                return DropStrategy.HANDLE_INTERNALLY;
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

        // This was no faster.
//        class MutableArraySource<T> extends OpRun implements MutableSource<T> {
//            final T[] items;
//            int idx;
//            int size;
//
//            MutableArraySource(T[] ls, int i) { items = ls; idx = i; size = items.length; }
//
//            /** {@inheritDoc} */
//            @Override public boolean hasNext() {
//                return idx < size;
//            }
//
//            /** {@inheritDoc} */
//            @Override public T next() {
//                // Breaking into 3 statements was a clear 8% speed-up over the ++ operator in my tests.
//                T ret = items[idx];
//                idx = idx + 1;
//                return ret;
//            }
//
//            /** {@inheritDoc} */
//            @Override public DropStrategy drop(int d) {
//                if (d < 1) { return DropStrategy.HANDLE_INTERNALLY; }
//                int numItems = size - idx;
//                if (d > numItems) {
//                    idx = size; // used up.
//                }
//                idx = idx + d;
//                return DropStrategy.HANDLE_INTERNALLY;
//            }
//
//            /** {@inheritDoc} */
//            @Override public int take(int t) {
//                // Taking none is equivalent to an empty source.
//                if (t < 1) {
//                    idx = size;
//                    return 0;
//                }
//
//                int numItems = size - idx;
//                if (t > numItems) {
//                    // Taking more items doesn't affect us, it only affects the caller.  Just
//                    // adjust their total by the number that we can handle.
//                    return t - numItems;
//                } // Can take them all.
//
//                size = idx + t;
//                return 0;
//            }
//        } // end class MutableArraySource

        // TODO: de-duplicate cut-and pasted code (if still fast) then make MutableIterableSource
    } // end interface MutableSource

    abstract class OpRun {
        // TODO: Try with a linked list of ops instead of array, that way we can remove ops from the list when they are used up.
//        OpRun nextOp = null;
//        Function1<Object,Boolean> terminate = null;
        Function1<Object,Boolean> filter = null;
        Function1 map = null;
        Function1<Object,MutableSourceProvider> flatMap = null;

        public abstract DropStrategy drop(int num);

//        public abstract boolean doDrop(int num);

        private static class FilterRun extends OpRun {
            FilterRun(Function1<Object,Boolean> func) { filter = func; }
            @Override public DropStrategy drop(int num) { return DropStrategy.CANNOT_DROP; }
        }

        private static class MapRun extends OpRun {
            MapRun(Function1 func) { map = func; }
            @Override public DropStrategy drop(int num) { return DropStrategy.ASK_SUPPLIER; }
        }

        private static class FlatMapRun extends OpRun {
//            ListSourceDesc<U> cache = null;
//            int numToDrop = 0;

            FlatMapRun(Function1<Object,MutableSourceProvider> func) { flatMap = func; }
            @Override public DropStrategy drop(int num) { return DropStrategy.CANNOT_DROP; }

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

     @param <G> the input type for this OpDesc.
     */
    static abstract class OpDesc<G> implements Transform4<G> {
        final OpDesc prevOp;
//        final int drop;
//        final int take;
        OpDesc(OpDesc pre) { prevOp = pre; }

        /** Gets the next item. */
//        abstract Option<F> next();
        abstract RunList toRunList();

        // Do I want to go back to modeling this as a separate step, then compress the steps
        // as much as possible to move the drop and take operations as early in the stream as possible.

        @Override public OpDesc<G> drop(int i) {
            return new DropDesc<>(this, i);
        }

//        public int dropAmt() { return drop; }

//        public int takeAmt() { return take; }

        @Override public OpDesc<G> filter(Function1<? super G,Boolean> f) {
            return new FilterDesc<>(this, f);
        }

//        @SuppressWarnings("unchecked")
        @Override public <H> OpDesc<H> flatMap(Function1<? super G,Iterable<H>> f) {
            return new FlatMapDesc<>(this, f);
        }

//        @SuppressWarnings("unchecked")
        @Override public <H> OpDesc<H> map(Function1<? super G, ? extends H> f) {
            return new MapDesc<>(this, f);
        }

        /** Provides a way to collect the results of the transformation. */
    //    @Override
        @Override public <H> H foldLeft(H ident, Function2<H,? super G,H> reducer) {

            // Construct an optimized array of OpRuns (mutable operations for this run)
            RunList runList = toRunList();
//            System.out.println("this: " + this + " runList: " + runList);

            // Actually do the fold.
            return _foldLeft(runList, runList.opArray(), 0, ident, reducer);
        }

        // We used a linked-list to build the type-safe operations so if that code compiles, the types should work out
        // here too.  However, for performance, we don't want to be stuck creating and passing Options around,
        // nor do we want a telescoping stack of hasNext() and next() calls.  So we're abandoning type safety
        // and calling all the intermediate results Objects.
        @SuppressWarnings("unchecked")
        private <H> H _foldLeft(Iterable source, OpRun[] ops, int opIdx, H ident, Function2 reducer) {
            Object ret = ident;

            // This is a label - the first one I have used in Java in years, or maybe ever.
            // I'm assuming this is fast, but will have to test to confirm it.
            sourceLoop:
            for (Object o : source) {
                for (int j = opIdx; j < ops.length; j++) {
                    OpRun op = ops[j];
                    if ( (op.filter != null) && !op.filter.apply(o) ) {
                        // stop processing this source item and go to the next one.
                        continue sourceLoop;
                    }
                    if (op.map != null) {
                        o = op.map.apply(o);
                    } else if (op.flatMap != null) {
                        ret = _foldLeft(op.flatMap.apply(o), ops, j + 1, (G) ret, reducer);
                        // stop processing this source item and go to the next one.
                        continue sourceLoop;
                    }
//                    if ( (op.terminate != null) && op.terminate.apply(o) ) {
//                        return (G) ret;
//                    }
                }
                // Here, the item made it through all the operations.  Combine it with the result.
                ret = reducer.apply(ret, o);
            }
            return (H) ret;
        }

        private static class DropDesc<T> extends OpDesc<T> {
            private final int drop;
            DropDesc(OpDesc<T> prev, int d) { super(prev); drop = d; }

//            @SuppressWarnings("unchecked")
            @Override RunList toRunList() {
                RunList ret = prevOp.toRunList();
                int i = ret.list.size() - 1;
                for (; i >= 0; i--) {
                    OpRun opRun = ret.list.get(i);
                    DropStrategy earlierDs = opRun.drop(drop);
                    if (earlierDs == DropStrategy.CANNOT_DROP) {
                        break;
                    } else if (earlierDs == DropStrategy.HANDLE_INTERNALLY) {
                        return ret;
                    }
                }
                if (i <= 0) {
                    DropStrategy srcDs = ret.source.drop(drop);
                    if (srcDs == DropStrategy.HANDLE_INTERNALLY) {
                        return ret;
                    }
                }
                Mutable.IntRef leftToDrop = Mutable.IntRef.of(drop);
                ret.list.add(new OpRun.FilterRun((t) -> {
                    if (leftToDrop.value() > 0) {
                        leftToDrop.set(leftToDrop.value() - 1);
                        return Boolean.FALSE;
                    }
                    return Boolean.TRUE; }));
                return ret;
            }
        }

        private static class FilterDesc<T> extends OpDesc<T> {
            final Function1<? super T,Boolean> f;

            FilterDesc(OpDesc<T> prev, Function1<? super T,Boolean> func) { super(prev); f = func; }

            @SuppressWarnings("unchecked")
            @Override RunList toRunList() {
                RunList ret = prevOp.toRunList();
                ret.list.add(new OpRun.FilterRun((Function1<Object,Boolean>) f));
                return ret;
            }
        }

        private static class MapDesc<T,U> extends OpDesc<U> {
            final Function1<? super T,? extends U> f;

            MapDesc(OpDesc<T> prev, Function1<? super T,? extends U> func) { super(prev); f = func; }

            @SuppressWarnings("unchecked")
            @Override RunList toRunList() {
                RunList ret = prevOp.toRunList();
                ret.list.add(new OpRun.MapRun(f));
                return ret;
            }
        }

        private static class FlatMapDesc<T,U> extends OpDesc<U> {
            final Function1<? super T,Iterable<U>> f;
            FlatMapDesc(OpDesc<T> prev, Function1<? super T,Iterable<U>> func) {
                super(prev); f = func;
            }

            @SuppressWarnings("unchecked")
            @Override RunList toRunList() {
                RunList ret = prevOp.toRunList();
                ret.list.add(new OpRun.FlatMapRun((Function1) f));
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

    class SourceProviderListDesc<T> extends OpDesc<T> {
        private final List<T> list;
        SourceProviderListDesc(List<T> l) {
            super(null);
            list = l;
        }

        @Override RunList toRunList() {
//            ops.add(new MutableSource.MutableListSource<>(list, 0));
            RunList runList = new RunList();
            runList.list = new ArrayList<>();
            runList.source = new MutableSource.MutableListSource<>(list, 0);
            return runList;
        }
    }

    class SourceProviderArrayDesc<T> extends OpDesc<T> {
        private final T[] list;
        SourceProviderArrayDesc(T[] l) {
            super(null);
            list = l;
        }

        @Override RunList toRunList() {
//            ops.add(new MutableSource.MutableListSource<>(list, 0));
            RunList runList = new RunList();
            runList.list = new ArrayList<>();
            // I made a MutableArraySource, but it was no faster than the list source, so no sense
            // in duplicating the code.  Just use the list.
            runList.source = new MutableSource.MutableListSource<>(Arrays.asList(list), 0);
            return runList;
        }
    }

    /** Constructor.  Need to add an Iterable constructor and maybe some day even an array constructor. */
    static <T> Transform4<T> fromList(List<T> list) { return new SourceProviderListDesc<>(list); }

    static <T> Transform4<T> fromArray(T[] list) { return new SourceProviderArrayDesc<>(list); }

    // ================================================================================================================
    // These will come from Transformable, but (will be) overridden to have a different return type.

    /** The number of items to drop from the beginning of the output.  The drop happens before take(). */
    Transform4<A> drop(int n);

//    @Override
    Transform4<A> filter(Function1<? super A,Boolean> f);

    <B> Transform4<B> flatMap(Function1<? super A,Iterable<B>> f);

//    @Override
    <B> Transform4<B> map(Function1<? super A, ? extends B> f);

    /** Provides a way to collect the results of the transformation. */
//    @Override
    <B> B foldLeft(B ident, Function2<B,? super A,B> f);

}
