package org.organicdesign.fp.experiments;

import org.organicdesign.fp.Option;
import org.organicdesign.fp.function.Function1;
import org.organicdesign.fp.function.Function2;

import java.util.List;

// We model this as a linked list so that each transition can have it's own output type, building a type-safe bridge
// from first operation to the last.
public interface Transform<A> { // extends Transformable<A> {

    // Using an abstract class here to limit the visibility of the next() method and present a less mutable
    // interface to the outside world.
    static abstract class Operation<F> implements Transform<F> {
        /** Gets the next item. */
        abstract Option<F> next();

        // Do I want to go back to modeling this as a separate step, then compress the steps
        // as much as possible to move the drop and take operations as early in the stream as possible.
        @Override public abstract Operation<F> drop(int n);

        @Override public Operation<F> filter(Function1<? super F,Boolean> f) {
            return new Filter<>(this, f);
        }

        @Override public <G> Operation<G> flatMap(Function1<F,List<G>> f) {
            return new FlatMap<>(this, f, 0);
        }

        @Override public <G> Operation<G> map(Function1<? super F, ? extends G> f) {
            return new Map<>(this, f);
        }

        /** Provides a way to collect the results of the transformation. */
    //    @Override
        @Override public <G> G foldLeft(G ident, Function2<G,? super F,G> f) {
            G ret = ident;
            Option<F> next = next();
            while (next.isSome()) {
                ret = f.apply(ret, next.get());
                next = next();
            }
            return ret;
        }

        private static class ListSource<T> extends Operation<T> {
            final List<T> as;
            // TODO: This is mutable state.  Figure out how to push it to a mutable action and keep the "Operation" immutable.
            int idx = 0;

            ListSource(List<T> list) {
                as = list;
            }

            @Override
            public Operation<T> drop(int num) {
                idx = idx + num;
                return this;
            }

            @Override
            public Option<T> next() {
                return (idx < as.size()) ? Option.of(as.get(idx)) : Option.none();
            }
        }

        private static class Filter<T> extends Operation<T> {
            final Operation<T> prevOp;
            final Function1<? super T,Boolean> f;

            Filter(Operation<T> prev, Function1<? super T,Boolean> func) {
                prevOp = prev;
                f = func;
            }

            @Override
            public Operation<T> drop(int num) {
                return new Filter<>(prevOp.drop(num), f);
            }

            @Override
            public Option<T> next() {
                Option<T> next = prevOp.next();
                while (next.isSome() && !f.apply(next.get())) {
                    next = prevOp.next();
                }
                return next;
            }
        }

        private static class Map<T, U> extends Operation<U> {
            final Operation<T> prevOp;
            final Function1<? super T,? extends U> f;

            Map(Operation<T> prev, Function1<? super T,? extends U> func) {
                prevOp = prev;
                f = func;
            }

            @Override
            public Operation<U> drop(int num) {
                return new Map<>(prevOp.drop(num), f);
            }

            @Override Option<U> next() {
                Option<T> next = prevOp.next();
                return next.isSome() ? Option.of(f.apply(next.get())) : Option.none();
            }
        }

        private static class FlatMap<T, U> extends Operation<U> {
            final Operation<T> prevOp;
            final Function1<T,List<U>> f;
            // TODO: This is mutable state.  Figure out how to push it to a mutable action and keep the "Operation" immutable.
            ListSource<U> cache = null;
            int numToDrop = 0;

            FlatMap(Operation<T> prev, Function1<T,List<U>> func, int drop) {
                prevOp = prev;
                f = func;
                numToDrop = drop;
            }

            @Override
            public Operation<U> drop(int num) {
                return new FlatMap<>(prevOp, f, numToDrop + num);
            }

            @Override
            public Option<U> next() {
                while ((cache == null) || (cache.idx == cache.as.size())) {
                    Option<T> next = prevOp.next();
                    if (next.isSome()) {
                        cache = new ListSource<>(f.apply(next.get()));
                    } else {
                        return Option.none();
                    }
                    if (numToDrop > 0) {
                        if (numToDrop >= cache.as.size()) {
                            numToDrop -= cache.as.size();
                            cache = null;
                        } else {
                            cache.idx += numToDrop;
                            numToDrop = 0;
                        }
                    }
                }
                return cache.next();
            }
        }
    } // end abstract class Operation

    // This is just a sample usage to be sure it compiles.
//    Integer total = from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9))
//            .drop(1)
//            .filter(i -> i < 7)
//            .map(String::valueOf)
//            .flatMap(s -> Arrays.asList(s, s, s))
//            .foldLeft(0, (count, s) -> count + 1);

    /** Constructor.  Need to add an Iterable constructor and maybe some day even an array constructor. */
    static <T> Transform<T> from(List<T> list) { return new Operation.ListSource<>(list); }

    // ================================================================================================================
    // These will come from Transformable, but (will be) overridden to have a different return type.

    /** The number of items to drop from the beginning of the output.  The drop happens before take(). */
    Transform<A> drop(int n);

//    @Override
    Transform<A> filter(Function1<? super A,Boolean> f);

    <B> Transform<B> flatMap(Function1<A,List<B>> f);

//    @Override
    <B> Transform<B> map(Function1<? super A, ? extends B> f);

    /** Provides a way to collect the results of the transformation. */
//    @Override
    <B> B foldLeft(B ident, Function2<B,? super A,B> f);
}
