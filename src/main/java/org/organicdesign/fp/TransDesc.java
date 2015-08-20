package org.organicdesign.fp;

import org.organicdesign.fp.collections.UnmodIterable;
import org.organicdesign.fp.collections.UnmodSortedIterator;
import org.organicdesign.fp.function.Function1;
import org.organicdesign.fp.function.Function2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

// We model this as a linked list so that each transition can have it's own output type, building a
// type-safe bridge from first operation to the last.
/**
 A description of operations to be performed.  When foldLeft() is called, transformation definition
 is "compiled" into a mutable transformation which is then carried out.  This allows certain
 performance shortcuts (such as doing a drop with index addition instead of iteration) and also
 hides the mutability inherent in a transformation.
 */
public abstract class TransDesc<A> implements Transformable<A> {

    // TODO: Make an xform package and break all these out in there.  They can be package-scope instead of public, but all stuck together like this is a mess.

    enum OpStrategy { HANDLE_INTERNALLY, ASK_SUPPLIER, CANNOT_HANDLE; }

    private static final Object TERMINATE = new Object();
    @SuppressWarnings("unchecked")
    private A terminate() { return (A) TERMINATE; }

    interface MutableSourceProvider<T> extends UnmodIterable<T> {
        @Override MutableSource<T> iterator();
    }

    /**
     Like Iterator, this interface is inherently not thread-safe, so wrap it in something
     thread-safe before sharing across threads.
     */
    interface MutableSource<T> extends UnmodSortedIterator<T> {
//        public static final MutableSource<?> EMPTY = new MutableSource<Object>() {
//            @Override public boolean hasNext() { return false; }
//            @Override public Object next() { throw new NoSuchElementException("No more elements"); }
//            @Override public OpStrategy drop(long i) { return OpStrategy.HANDLE_INTERNALLY; }
//            @Override public int take(long i) { return i; }
//        };
//        @SuppressWarnings("unchecked")
//        static <X> MutableListSource<X> empty() { return (MutableListSource<X>) EMPTY; }

        /**
         Drops as many items as the source can handle.
         @param d the number of items to drop
         @return  whether the source can handle the take, or pass-through (ask-supplier), or can't
         do either.
         */
        OpStrategy drop(long d);

        /**
         Takes as many items as the source can handle.
         @param t the number of items to take.
         @return whether the source can handle the take, or pass-through (ask-supplier), or can't
         do either.
         */
        OpStrategy take(long t);

        // TODO: Mutable sources should record all drops, appends, (and takes?) then in a separate step right before processing, combine them together as appropriate.
        class MutableIterableSource<T> extends OpRun implements MutableSource<T> {
            private static final long IGNORE_TAKE = -1;
            final Iterator<T> items;
            long drop = 0;
            long numToTake = IGNORE_TAKE;

            MutableIterableSource(Iterable<T> ls) { items = ls.iterator(); }

            private void doDrop() {
                while ((drop > 0) && items.hasNext()) {
                    drop = drop - 1;
                    items.next();
                }
            }

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                if (numToTake == 0) { return false; }
                if (drop > 0) { doDrop(); }
                return items.hasNext();
            }

            /** {@inheritDoc} */
            @Override public T next() {
                if (drop > 0) { doDrop(); }
                if (numToTake > IGNORE_TAKE) {
                    if (numToTake == 0) {
                        throw new NoSuchElementException("Called next() without calling hasNext." +
                                                         " Completed specified take - no more" +
                                                         " elements left.");
                    }
                    numToTake = numToTake - 1;
                }
                return items.next();
            }

            /** {@inheritDoc} */
            @Override public OpStrategy drop(long d) {
                if (d < 1) { return OpStrategy.HANDLE_INTERNALLY; }
                drop = drop + d;
                return OpStrategy.HANDLE_INTERNALLY;
            }

            /** {@inheritDoc} */
            @Override public OpStrategy take(long take) {
                if (take < 0) {
                    throw new IllegalArgumentException("Can't take less than zero items.");
                }
                if (numToTake == IGNORE_TAKE) {
                    numToTake = take;
                } else if (take < numToTake) {
                    numToTake = take;
                }
                return OpStrategy.HANDLE_INTERNALLY;
            }
        } // end class MutableIterableSource

        class MutableListSource<T> extends OpRun implements MutableSource<T> {
            final List<T> items;
            int idx;
            int size;

            /** Do not use.  This is only so that MutableArraySource can inherit from this class. */
            MutableListSource() { items = null; };

            MutableListSource(List<T> ls, int i) { items = ls; idx = i; size = items.size(); }

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                return idx < size;
            }

            /** {@inheritDoc} */
            @Override public T next() {
                // Breaking into 3 statements was a clear 8% speed-up over the ++ operator in my
                // tests.
                T ret = items.get(idx);
                idx = idx + 1;
                return ret;
            }

            /** {@inheritDoc} */
            @Override public OpStrategy drop(long d) {
                if (d > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Can't drop more than " + Integer.MAX_VALUE +
                                                       " items");
                } else if (d < 0) {
                    throw new IllegalArgumentException("Makes no sense to drop less than 0 items");
                }

                if (d < 1) { return OpStrategy.HANDLE_INTERNALLY; }
                int numItems = size - idx;
                if (d > numItems) {
                    idx = size; // used up.
                } else {
                    idx = idx + (int) d;
                }
                return OpStrategy.HANDLE_INTERNALLY;
            }

            /** {@inheritDoc} */
            @Override public OpStrategy take(long take) {
                if (take < 0) {
                    throw new IllegalArgumentException("Makes no sense to take less than 0 items");
                }
                // Taking none is equivalent to an empty source.
                if (take < 1) {
                    idx = size;
                    return OpStrategy.HANDLE_INTERNALLY;
                }

                // Taking more items than we have is not possible.  Just take all in that case.
                int numItems = size - idx;
                if (take < numItems) {
                    size = idx + (int) take;
                }
                // If taking more than all the items, the take is meaningless.
                return OpStrategy.HANDLE_INTERNALLY;
            }

//            @Override public OpStrategy concatList(MutableListSource nextSrc) {
//                size = size + nextSrc.size;
//                return OpStrategy.HANDLE_INTERNALLY;
//            }

            @Override public String toString() {
                return "MutableListSource(idx:" + idx + ",size:" + size + ")";
            }
        } // end class MutableListSource

        // This was no faster.
        class MutableArraySource<T> extends MutableListSource<T> {
            final T[] itemArray;

            MutableArraySource(T[] ls, int i) {
                super();
                itemArray = ls; idx = i; size = itemArray.length;
            }

            /** {@inheritDoc} */
            @Override public T next() {
                // Breaking into 3 statements was a clear 8% speed-up over the ++ operator in my
                // tests.
                T ret = itemArray[idx];
                idx = idx + 1;
                return ret;
            }
        } // end class MutableArraySource
    } // end interface MutableSource

    /**
     OpRuns are mutable operations that the transform carries out when it is run.  This is in
     contrast to the TransDesc which are like the "source code" or transformation description.
     OpRuns are like compiled "op codes" of the transform.
     */
    static abstract class OpRun {
        // Time using a linked list of ops instead of array, so that we can easily remove ops from
        // the list when they are used up.
        Function1<Object,Boolean> filter = null;
        Function1 map = null;
        Function1<Object,MutableSourceProvider> flatMap = null;
//        Function1<Object,Boolean> keepGoing = null;

        public OpStrategy drop(long num) { return OpStrategy.CANNOT_HANDLE; }

        public OpStrategy take(long num) { return OpStrategy.CANNOT_HANDLE; }

//        public OpStrategy concatList(MutableSource nextSrc) { return OpStrategy.CANNOT_HANDLE; }

        /**
         We need to model this as a separate op for when the previous op is CANNOT_HANDLE.  It is
         coded as a filter, but still needs to be modeled separately so that subsequent drops can be
         combined into the earliest single explicit drop op.  Such combinations are additive,
         meaning that drop(3).drop(5) is equivalent to drop(8).
         */
        private static class DropRun extends OpRun {
            private long leftToDrop;
            DropRun(long drop) {
                leftToDrop = drop;
                filter = o -> {
                    if (leftToDrop > 0) {
                        leftToDrop = leftToDrop - 1;
                        return Boolean.FALSE;
                    }
                    return Boolean.TRUE;
                };
            }
            @Override public OpStrategy drop(long num) {
                leftToDrop = leftToDrop + num;
                return OpStrategy.HANDLE_INTERNALLY;
            }
        }

        private static class FilterRun extends OpRun {
            FilterRun(Function1<Object,Boolean> func) { filter = func; }
        }

        private static class MapRun extends OpRun {
            MapRun(Function1 func) { map = func; }
            @Override public OpStrategy drop(long num) { return OpStrategy.ASK_SUPPLIER; }
            @Override public OpStrategy take(long num) { return OpStrategy.ASK_SUPPLIER; }
        }

        // TODO: FlatMap should drop and take internally using addition/subtraction on each output
        // TODO: list instead of testing each list item individually.
        private static class FlatMapRun extends OpRun {
//            ListSourceDesc<U> cache = null;
//            int numToDrop = 0;

            FlatMapRun(Function1<Object,MutableSourceProvider> func) { flatMap = func; }
        }

        /**
         We need to model this as a separate op for when the previous op is CANNOT_HANDLE.  It is
         coded as a map, but still needs to be modeled separately so that subsequent takes can be
         combined into the earliest single explicit take op.  Such combination is a pick-least of
         all the takes, meaning that take(5).take(3) is equivalent to take(3).
         */
        private static class TakeRun extends OpRun {
            private long numToTake;
            TakeRun(long take) {
                numToTake = take;
                map = a -> {
                    if (numToTake > 0) {
                        numToTake = numToTake - 1;
                        return a;
                    }
                    return TERMINATE;
                };
            }

            @Override public OpStrategy take(long num) {
                if (num < 0) {
                    throw new IllegalArgumentException("Can't take less than 0 items.");
                }
                if (num < numToTake) {
                    numToTake = num;
                }
                return OpStrategy.HANDLE_INTERNALLY;
            }
        }
    }

    /**
     A RunList is like the compiled program from a Transform Description.  It contains a source
     and a list of OpRun op-codes.  Each of these is its own source provider, since the output
     of one transform can be the input to another.  FlatMap is implemented that way.  Notice that
     there are no types here: Since the input could be one type, and each map or flatmap operation
     could change that to another type, we ignore all that in the "compiled" version and just use
     Objects.  That lets us use the simplest iteration primitives (for speed).
     */
    private static class RunList implements MutableSourceProvider {
        MutableSource source;
        List<OpRun> list = new ArrayList<>();
        RunList next = null;
        RunList prev = null;

        private RunList(RunList prv, MutableSource src) { prev = prv; source = src; }
        public static RunList of(RunList prv, MutableSource src) {
            RunList ret = new RunList(prv, src);
            if (prv != null) { prv.next = ret; }
            return ret;
        }

        OpRun[] opArray() {
            return list.toArray(new OpRun[list.size()]);
        }
        @Override public MutableSource iterator() { return source; }
    }

    private static class AppendListDesc<T> extends TransDesc<T> {
        final SourceProviderListDesc<T> src;

        AppendListDesc(TransDesc<T> prev, SourceProviderListDesc<T> s) { super(prev); src = s; }

        @SuppressWarnings("unchecked")
        @Override RunList toRunList() {
            MutableSource ms = new MutableSource.MutableListSource<>(src.list, 0);
            RunList ret = prevOp.toRunList();
            int i = ret.list.size() - 1;
//              System.out.println("\tchecking previous items to see if they can handle a drop...");
            if (i > -1) {
                OpRun opRun = ret.list.get(i);
                if (opRun instanceof MutableSourceProvider) {
                    RunList ret2 = RunList.of(ret, ms);
                    ret2.list = ret.list;
                    return ret2;
                }
//                OpStrategy earlierA = opRun.concatList(ms);
//                if (earlierA == OpStrategy.HANDLE_INTERNALLY) {
//                    return ret;
//                }
            }
            return RunList.of(ret, ms);
        }
    }

    private static class AppendIterDesc<T> extends TransDesc<T> {
        final SourceProviderIterableDesc<T> src;

        AppendIterDesc(TransDesc<T> prev, SourceProviderIterableDesc<T> s) { super(prev); src = s; }

        @SuppressWarnings("unchecked")
        @Override RunList toRunList() {
            RunList ret = prevOp.toRunList();
            return RunList.of(ret, new MutableSource.MutableIterableSource<>(src.list));
        }
    }

    private static class AppendArrayDesc<T> extends TransDesc<T> {
        final SourceProviderArrayDesc<T> src;

        AppendArrayDesc(TransDesc<T> prev, SourceProviderArrayDesc<T> s) { super(prev); src = s; }

        @SuppressWarnings("unchecked")
        @Override RunList toRunList() {
            RunList ret = prevOp.toRunList();
            return RunList.of(ret, new MutableSource.MutableArraySource<>(src.list, 0));
        }
    }

    /**
     Describes a "drop" operation.  Drops will be pushed as early in the operation-list as possible,
     ideally being done using one-time pointer addition on the source.  When that is not possible,
     a Drop op-code is created (eventually implemented as a filter function).  Subsequent drop ops
     will be combined into the earliest drop (for speed).
     @param <T> the expected input type to drop.
     */
    private static class DropDesc<T> extends TransDesc<T> {
        private final long drop;
        DropDesc(TransDesc<T> prev, long d) { super(prev); drop = d; }

        @SuppressWarnings("unchecked")
        @Override RunList toRunList() {
//                System.out.println("in toRunList() for drop");
            RunList ret = prevOp.toRunList();
            int i = ret.list.size() - 1;
//              System.out.println("\tchecking previous items to see if they can handle a drop...");
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
            ret.list.add(new OpRun.DropRun(drop));
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

    /**
     Describes a "take" operation.  Takes will be pushed as early in the operation-list as possible,
     ideally being done using one-time pointer addition on the source.  When that is not possible,
     a Take op-code is created (eventually implemented as a filter function).  Subsequent take ops
     will be combined into the earliest take (for speed).
     @param <T> the expected input type to take.
     */
    private static class TakeDesc<T> extends TransDesc<T> {
        private final long take;
        TakeDesc(TransDesc<T> prev, long t) { super(prev); take = t; }

        @SuppressWarnings("unchecked")
        @Override RunList toRunList() {
//                System.out.println("in toRunList() for take");
            RunList ret = prevOp.toRunList();
            int i = ret.list.size() - 1;
//              System.out.println("\tchecking previous items to see if they can handle a take...");
            OpStrategy earlierTs = null;
            for (; i >= 0; i--) {
                OpRun opRun = ret.list.get(i);
                earlierTs = opRun.take(take);
                if (earlierTs == OpStrategy.CANNOT_HANDLE) {
//                        System.out.println("\tNone can handle a take...");
                    break;
                } else if (earlierTs == OpStrategy.HANDLE_INTERNALLY) {
//                        System.out.println("\tHandled internally by " + opRun);
                    return ret;
                }
            }
            if ( (earlierTs != OpStrategy.CANNOT_HANDLE) && (i <= 0) ) {
                OpStrategy srcDs = ret.source.take(take);
                if (srcDs == OpStrategy.HANDLE_INTERNALLY) {
//                        System.out.println("\tHandled internally by source: " + ret.source);
                    return ret;
                }
            }
//                System.out.println("\tSource could not handle take.");
//                System.out.println("\tMake a take for " + take + " items.");
            ret.list.add(new OpRun.TakeRun(take));
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
        private final List<? extends T> list;
        SourceProviderListDesc(List<? extends T> l) { super(null); list = l; }
        @Override RunList toRunList() {
            return RunList.of(null, new MutableSource.MutableListSource<>(list, 0));
        }
    }

    static class SourceProviderArrayDesc<T> extends TransDesc<T> {
        private final T[] list;
        SourceProviderArrayDesc(T[] l) { super(null); list = l; }
        @Override RunList toRunList() {
            // Why am I keeping this arround when it's no faster than using a List?  In theory, it
            // could be faster, so I keep it around just in case.
            return RunList.of(null, new MutableSource.MutableArraySource<>(list, 0));
        }
    }

    static class SourceProviderIterableDesc<T> extends TransDesc<T> {
        private final Iterable<? extends T> list;
        SourceProviderIterableDesc(Iterable<? extends T> l) { super(null); list = l; }
        @Override RunList toRunList() {
            return RunList.of(null, new MutableSource.MutableIterableSource<>(list));
        }
    }

    /** Static factory methods */
    public static <T> TransDesc<T> fromArray(T[] list) {
        return new SourceProviderArrayDesc<>(list);
    }

    /** Static factory methods */
    public static <T> TransDesc<T> from(List<T> list) {
        return new SourceProviderListDesc<>(list);
    }

    /** Static factory methods */
    public static <T> TransDesc<T> from(Iterable<T> list) {
        return new SourceProviderIterableDesc<>(list);
    }

    // ========================================= Instance =========================================

    // Fields
    final TransDesc prevOp;

    // Constructor
    TransDesc(TransDesc pre) { prevOp = pre; }

    // This is the main method of this whole file.  Everything else lives to serve this.
    // We used a linked-list to build the type-safe operations so if that code compiles, the types
    // should work out here too.  However, for performance, we don't want to be stuck creating and
    // passing Options around, nor do we want a telescoping stack of hasNext() and next() calls.
    // So abandon type safety, store all the intermediate results as Objects, and use loops and
    // sentinel values to break out or skip processing as appropriate.  Initial tests indicate this
    // is 2.6 times faster than wrapping items type-safely in Options and 10 to 100 times faster
    // than lazily evaluated and cached linked-list, Sequence model.
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
                    // This is how map can handle takeWhile, take, and other termination marker
                    // roles.  Remember, the fewer functions we have to check for, the faster this
                    // will execute.
                    if (o == TERMINATE) {
                        return (H) ret;
                    }
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

    // =============================================================================================
    // These will come from Transformable, but (will be) overridden to have a different return type.

    public TransDesc<A> concatList(List<? extends A> list) {
        return new AppendListDesc<>(this, new SourceProviderListDesc<>(list));
    }

    public TransDesc<A> concatIterable(Iterable<? extends A> list) {
        return new AppendIterDesc<>(this, new SourceProviderIterableDesc<>(list));
    }

    public TransDesc<A> concatArray(A[] list) {
        return new AppendArrayDesc<>(this, new SourceProviderArrayDesc<>(list));
    }

    /** The number of items to drop from the beginning of the output. */
    @Override public TransDesc<A> drop(long n) { return new DropDesc<>(this, n); }

    // Do we need a dropWhile???

    /** Provides a way to collect the results of the transformation. */
    @Override public <B> B foldLeft(B ident, Function2<B,? super A,B> reducer) {

        // Construct an optimized array of OpRuns (mutable operations for this run)
        RunList runList = toRunList();
        // Go back to the first runlist:
        while (runList.prev != null) { runList = runList.prev; }
//            System.out.println("this: " + this + " runList: " + runList);

        // Process the runlists in order.
        B ret = ident;
        while (runList != null) {
            // Actually do the fold.
            ret = _foldLeft(runList, runList.opArray(), 0, ret, reducer);
            runList = runList.next;
        }
        return ret;
    }

    // TODO: Test.
    @SuppressWarnings("unchecked")
    @Override
    public <B> B foldLeft(B ident, Function2<B,? super A,B> function2,
                          Function1<? super B,Boolean> function1) {
        // I'm coding this as a map operation that either returns the source, or a TERMINATE
        // sentinel value.
        return takeWhile((Function1<? super A,Boolean>) function1).foldLeft(ident, function2);
    }

    /** We will probably allow this some day, but for now, it's deprecated to avoid confusion. */
    @Override
    @Deprecated
    public void forEach(Consumer<? super A> action) {
        throw new UnsupportedOperationException("forEach() is a void method on Iterable.  " +
                                                "Use other forEach() which returns a Transform.");
    }

    // TODO: This conflicts with Iterable.forEach() which returns void.  Rename to forAll() or remove.
    @Override
    public TransDesc<A> forEach(Function1<? super A, ?> f) {
        return filter(a -> {
            f.apply(a);
            return Boolean.TRUE;
        });
    }

    @Override public TransDesc<A> filter(Function1<? super A,Boolean> f) {
        return new FilterDesc<>(this, f);
    }

    public <B> TransDesc<B> flatMap(Function1<? super A,Iterable<B>> f) {
        return new FlatMapDesc<>(this, f);
    }

    @Override public <B> TransDesc<B> map(Function1<? super A, ? extends B> f) {
        return new MapDesc<>(this, f);
    }

    abstract RunList toRunList();

    // TODO: Test.
    @Override
    public TransDesc<A> take(long l) { return new TakeDesc<>(this, l); }

    // TODO: Test.
    @Override
    public TransDesc<A> takeWhile(Function1<? super A,Boolean> function1) {
        // I'm coding this as a map operation that either returns the source, or a TERMINATE
        // sentinel value.
        return new MapDesc<>(this, a -> function1.apply(a) ? a : terminate());
    }
}
