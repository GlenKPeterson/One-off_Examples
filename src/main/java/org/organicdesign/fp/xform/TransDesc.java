// Copyright (c) 2015-08-20 PlanBase Inc. & Glen Peterson
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.organicdesign.fp.xform;

import org.organicdesign.fp.Transformable;
import org.organicdesign.fp.function.Function1;
import org.organicdesign.fp.function.Function2;

import java.util.List;
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

    enum OpStrategy { HANDLE_INTERNALLY, ASK_SUPPLIER, CANNOT_HANDLE; }

    static final Object TERMINATE = new Object();
    @SuppressWarnings("unchecked")
    private A terminate() { return (A) TERMINATE; }

    // This is just a sample usage to be sure it compiles.
//    Integer total = from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9))
//            .drop(1)
//            .filter(i -> i < 7)
//            .map(String::valueOf)
//            .flatMap(s -> Arrays.asList(s, s, s))
//            .foldLeft(0, (count, s) -> count + 1);

    /** Static factory method */
    public static <T> TransDesc<T> fromArray(T[] list) {
        return new SourceProviderArrayDesc<>(list);
    }

    /** Static factory method */
    public static <T> TransDesc<T> from(List<T> list) {
        return new SourceProviderListDesc<>(list);
    }

    /** Static factory method */
    public static <T> TransDesc<T> from(Iterable<T> list) {
        return new SourceProviderIterableDesc<>(list);
    }

    // ========================================= Instance =========================================

    // Fields
    final TransDesc prevOp;

    // Constructor
    TransDesc(TransDesc pre) { prevOp = pre; }

    // This is the main method of this whole package.  Everything else lives to serve this.
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
