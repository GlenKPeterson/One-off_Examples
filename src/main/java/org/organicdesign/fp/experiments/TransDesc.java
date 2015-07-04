package org.organicdesign.fp.experiments;

import org.organicdesign.fp.Transformable;
import org.organicdesign.fp.collections.UnmodIterable;
import org.organicdesign.fp.collections.UnmodSortedIterator;
import org.organicdesign.fp.function.Function1;
import org.organicdesign.fp.function.Function2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// We model this as a linked list so that each transition can have it's own output type, building a type-safe bridge
// from first operation to the last.
/**
 A description of operations to be performed.  When foldLeft() is called, transformation definition
 is "compiled" into a mutable transformation which is then carried out.  This allows certain
 performance shortcuts (such as doing a drop with index addition instead of iteration) and also
 hides the mutability inherent in a transformation.
 */
public abstract class TransDesc<A> implements Transformable<A> {

    // TODO: Need to ensure that take comes after drop!

    enum OpStrategy { HANDLE_INTERNALLY, ASK_SUPPLIER, CANNOT_HANDLE; }

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
//            @Override public OpStrategy drop(int i) { return OpStrategy.HANDLE_INTERNALLY; }
//            @Override public int take(int i) { return i; }
//        };
//        @SuppressWarnings("unchecked")
//        static <X> MutableListSource<X> empty() { return (MutableListSource<X>) EMPTY; }

        /**
         Drops as many items as the source can handle.
         @param d the number of items to drop
         @return true if the source knows it can handle dropping all these items.
         */
        OpStrategy drop(int d);

//        /**
//         Takes as many items as the source can handle.
//         @param t the number of items to take.
//         @return the number of "takes" left over when this source is exhausted (flatMap needs to know this).
//         0 if t &lt;= the number of items left in the source.  Otherwise returns t - numItemsActuallyTaken
//         */
//        OpStrategy take(int t);

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
            @Override public OpStrategy drop(int d) {
                if (d < 1) { return OpStrategy.HANDLE_INTERNALLY; }
                int numItems = size - idx;
                if (d > numItems) {
                    idx = size; // used up.
                }
                idx = idx + d;
                return OpStrategy.HANDLE_INTERNALLY;
            }

//            /** {@inheritDoc} */
//            @Override public OpStrategy take(int t) {
//                // Taking none is equivalent to an empty source.
//                if (t < 1) {
//                    idx = size;
//                    return OpStrategy.HANDLE_INTERNALLY;
//                }
//
//                // Taking more items than we have is not possible.  Just take all in that case.
//                int numItems = size - idx;
//                if (t < numItems) {
//                    size = idx + t;
//                }
//                return OpStrategy.HANDLE_INTERNALLY;
//            }

            @Override public String toString() {
                return "MutableListSource(idx:" + idx + ",size:" + size + ")";
            }
        } // end class MutableListSource

        class MutableIterableSource<T> extends OpRun implements MutableSource<T> {
            final Iterator<T> items;
            int drop = 0;

            MutableIterableSource(Iterable<T> ls) { items = ls.iterator(); }

            private void doDrop() {
                while ((drop > 0) && items.hasNext()) {
                    drop = drop - 1;
                    items.next();
                }
            }

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                if (drop > 0) { doDrop(); }
                return items.hasNext();
            }

            /** {@inheritDoc} */
            @Override public T next() {
                if (drop > 0) { doDrop(); }
                return items.next();
            }

            /** {@inheritDoc} */
            @Override public OpStrategy drop(int d) {
                if (d < 1) { return OpStrategy.HANDLE_INTERNALLY; }
                drop = drop + d;
                return OpStrategy.HANDLE_INTERNALLY;
            }

//            /** {@inheritDoc} */
//            @Override public OpStrategy take(int t) {
//                return OpStrategy.CANNOT_HANDLE;
//            }
        } // end class MutableIterableSource

        // This was no faster.
        class MutableArraySource<T> extends OpRun implements MutableSource<T> {
            final T[] items;
            int idx;
            int size;

            MutableArraySource(T[] ls, int i) { items = ls; idx = i; size = items.length; }

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                return idx < size;
            }

            /** {@inheritDoc} */
            @Override public T next() {
                // Breaking into 3 statements was a clear 8% speed-up over the ++ operator in my tests.
                T ret = items[idx];
                idx = idx + 1;
                return ret;
            }

            /** {@inheritDoc} */
            @Override public OpStrategy drop(int d) {
                if (d < 1) { return OpStrategy.HANDLE_INTERNALLY; }
                int numItems = size - idx;
                if (d > numItems) {
                    idx = size; // used up.
                }
                idx = idx + d;
                return OpStrategy.HANDLE_INTERNALLY;
            }

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
        } // end class MutableArraySource

        // TODO: de-duplicate cut-and pasted code (if still fast) then make MutableIterableSource
    } // end interface MutableSource

    static abstract class OpRun {
        // Time using a linked list of ops instead of array, so that we can easily remove ops from
        // the list when they are used up.
        Function1<Object,Boolean> filter = null;
        Function1 map = null;
        Function1<Object,MutableSourceProvider> flatMap = null;
//        Function1<Object,Boolean> keepGoing = null;

        public abstract OpStrategy drop(int num);

//        public abstract boolean doDrop(int num);

        private static class FilterRun extends OpRun {
            FilterRun(Function1<Object,Boolean> func) { filter = func; }
            @Override public OpStrategy drop(int num) { return OpStrategy.CANNOT_HANDLE; }
        }

        private static class MapRun extends OpRun {
            MapRun(Function1 func) { map = func; }
            @Override public OpStrategy drop(int num) { return OpStrategy.ASK_SUPPLIER; }
        }

        private static class FlatMapRun extends OpRun {
//            ListSourceDesc<U> cache = null;
//            int numToDrop = 0;

            FlatMapRun(Function1<Object,MutableSourceProvider> func) { flatMap = func; }
            @Override public OpStrategy drop(int num) { return OpStrategy.CANNOT_HANDLE; }

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


    private static class DropDesc<T> extends TransDesc<T> {
        private final int drop;
        DropDesc(TransDesc<T> prev, int d) { super(prev); drop = d; }

        @SuppressWarnings("unchecked")
        @Override RunList toRunList() {
//                System.out.println("in toRunList() for drop");
            RunList ret = prevOp.toRunList();
            int i = ret.list.size() - 1;
//                System.out.println("\tchecking previous items to see if they can handle a drop...");
            OpStrategy earlierDs = null;
            for (; i >= 0; i--) {
                OpRun opRun = ret.list.get(i);
                earlierDs = opRun.drop(drop);
                if (earlierDs == OpStrategy.CANNOT_HANDLE) {
//                        System.out.println("\tNone can handle a drop...");
                    break;
                } else if (earlierDs == OpStrategy.HANDLE_INTERNALLY) {
//                        System.out.println("\tHandled internally by " + opRun);
                    return ret;
                }
            }
            if ( (earlierDs != OpStrategy.CANNOT_HANDLE) && (i <= 0) ) {
                OpStrategy srcDs = ret.source.drop(drop);
                if (srcDs == OpStrategy.HANDLE_INTERNALLY) {
//                        System.out.println("\tHandled internally by source: " + ret.source);
                    return ret;
                }
            }
//                System.out.println("\tSource could not handle drop.");
//                System.out.println("\tMake a drop for " + drop + " items.");
            ret.list.add(new OpRun.FilterRun(new Function1<Object,Boolean>() {
                private int leftToDrop = drop;
                @Override public Boolean applyEx(Object o) {
                    if (leftToDrop > 0) {
                        leftToDrop = leftToDrop - 1;
                        return Boolean.FALSE;
                    }
                    return Boolean.TRUE;
                }
            }));
            return ret;
        }
    }

    private static class FilterDesc<T> extends TransDesc<T> {
        final Function1<? super T,Boolean> f;

        FilterDesc(TransDesc<T> prev, Function1<? super T,Boolean> func) { super(prev); f = func; }

        @SuppressWarnings("unchecked")
        @Override RunList toRunList() {
            RunList ret = prevOp.toRunList();
            ret.list.add(new OpRun.FilterRun((Function1<Object,Boolean>) f));
            return ret;
        }
    }

    private static class MapDesc<T,U> extends TransDesc<U> {
        final Function1<? super T,? extends U> f;

        MapDesc(TransDesc<T> prev, Function1<? super T,? extends U> func) { super(prev); f = func; }

        @SuppressWarnings("unchecked")
        @Override RunList toRunList() {
            RunList ret = prevOp.toRunList();
            ret.list.add(new OpRun.MapRun(f));
            return ret;
        }
    }

    private static class FlatMapDesc<T,U> extends TransDesc<U> {
        final Function1<? super T,Iterable<U>> f;
        FlatMapDesc(TransDesc<T> prev, Function1<? super T,Iterable<U>> func) {
            super(prev); f = func;
        }

        @SuppressWarnings("unchecked")
        @Override RunList toRunList() {
            RunList ret = prevOp.toRunList();
            ret.list.add(new OpRun.FlatMapRun((Function1) f));
            return ret;
        }
    }

    // This is just a sample usage to be sure it compiles.
//    Integer total = from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9))
//            .drop(1)
//            .filter(i -> i < 7)
//            .map(String::valueOf)
//            .flatMap(s -> Arrays.asList(s, s, s))
//            .foldLeft(0, (count, s) -> count + 1);

    static class SourceProviderListDesc<T> extends TransDesc<T> {
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

    static class SourceProviderArrayDesc<T> extends TransDesc<T> {
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
            runList.source = new MutableSource.MutableArraySource<>(list, 0);
            return runList;
        }
    }

    static class SourceProviderIterableDesc<T> extends TransDesc<T> {
        private final Iterable<T> list;
        SourceProviderIterableDesc(Iterable<T> l) {
            super(null);
            list = l;
        }

        @Override RunList toRunList() {
//            ops.add(new MutableSource.MutableListSource<>(list, 0));
            RunList runList = new RunList();
            runList.list = new ArrayList<>();
            runList.source = new MutableSource.MutableIterableSource<>(list);
            return runList;
        }
    }

    /** Constructor.  Need to add an Iterable constructor and maybe some day even an array constructor. */
    static <T> TransDesc<T> fromList(List<T> list) { return new SourceProviderListDesc<>(list); }

    static <T> TransDesc<T> fromArray(T[] list) { return new SourceProviderArrayDesc<>(list); }

    static <T> TransDesc<T> fromIterable(Iterable<T> list) { return new SourceProviderIterableDesc<>(list); }

    // ================================================================================================================
    // These will come from Transformable, but (will be) overridden to have a different return type.

    /** The number of items to drop from the beginning of the output.  The drop happens before take(). */
//    TransDesc<A> drop(int n);
    @Override public TransDesc<A> drop(long n) { return drop((int) n); }

//    @Override
//    TransDesc<A> filter(Function1<? super A,Boolean> f);

    @Override
    public TransDesc<A> forEach(Function1<? super A, ?> f) {
        return filter(a -> {
            f.apply(a);
            return Boolean.TRUE;
        });
    }

    final TransDesc prevOp;
    TransDesc(TransDesc pre) { prevOp = pre; }

    abstract RunList toRunList();

// Do I want to go back to modeling this as a separate step, then compress the steps
// as much as possible to move the drop and take operations as early in the stream as possible.

    public TransDesc<A> drop(int i) { return new DropDesc<>(this, i); }

//        public int dropAmt() { return drop; }

//        public int takeAmt() { return take; }

    @Override public TransDesc<A> filter(Function1<? super A,Boolean> f) {
        return new FilterDesc<>(this, f);
    }

    public <B> TransDesc<B> flatMap(Function1<? super A,Iterable<B>> f) {
        return new FlatMapDesc<>(this, f);
    }

    @Override public <B> TransDesc<B> map(Function1<? super A, ? extends B> f) {
        return new MapDesc<>(this, f);
    }

    /** Provides a way to collect the results of the transformation. */
//    @Override
    @Override public <B> B foldLeft(B ident, Function2<B,? super A,B> reducer) {

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
                    ret = _foldLeft(op.flatMap.apply(o), ops, j + 1, (H) ret, reducer);
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
    } // end _foldLeft();


    @Override
    public TransDesc<A> take(long l) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public TransDesc<A> takeWhile(Function1<? super A,Boolean> function1) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public <B> B foldLeft(B ident, Function2<B,? super A,B> function2, Function1<? super B,Boolean> function1) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
