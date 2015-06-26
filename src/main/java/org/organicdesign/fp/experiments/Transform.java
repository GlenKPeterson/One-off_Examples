package org.organicdesign.fp.experiments;

import org.organicdesign.fp.Option;
import org.organicdesign.fp.function.Function1;
import org.organicdesign.fp.function.Function2;

import java.util.List;

// We model this as a linked list so that each transition can have it's own output type, building a type-safe bridge
// from first operation to the last.
public interface Transform<A> { // extends Transformable<A> {
    class ListSource<T> implements Transform<T> {
        List<T> as;
        int idx = 0;

        ListSource(List<T> list) {
            as = list;
        }

        @Override
        public Transform<T> drop(int num) {
            idx = idx + num;
            return this;
        }

        @Override
        public Option<T> next() {
            return (idx < as.size()) ? Option.of(as.get(idx)) : Option.none();
        }
    }

    class Filter<T> implements Transform<T> {
        Transform<T> prevOp;
        final Function1<? super T,Boolean> f;

        Filter(Transform<T> prev, Function1<? super T,Boolean> func) {
            prevOp = prev;
            f = func;
        }

        @Override
        public Transform<T> drop(int num) {
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

    class Map<T, U> implements Transform<U> {
        Transform<T> prevOp;
        Function1<? super T,? extends U> f;

        Map(Transform<T> prev, Function1<? super T,? extends U> func) {
            prevOp = prev;
            f = func;
        }

        @Override
        public Transform<U> drop(int num) {
            return new Map<>(prevOp.drop(num), f);
        }

        @Override
        public Option<U> next() {
            Option<T> next = prevOp.next();
            return next.isSome() ? Option.of(f.apply(next.get())) : Option.none();
        }
    }

    class FlatMap<T, U> implements Transform<U> {
        Transform<T> prevOp;
        Function1<T,List<U>> f;
        ListSource<U> cache = null;
        int numToDrop = 0;

        FlatMap(Transform<T> prev, Function1<T,List<U>> func, int drop) {
            prevOp = prev;
            f = func;
            numToDrop = drop;
        }

        @Override
        public Transform<U> drop(int num) {
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

//    Transform<String> op = new ListSource<>(Arrays.asList(1,2,3,4,5,6,7,8,9))
//            .drop(1)
//            .filter(i -> i < 7)
//            .map(String::valueOf)
//            .flatMap(s -> Arrays.asList(s, s, s));

    /** Constructor.  Need to add an Iterable constructor and maybe some day even an array constructor. */
    static <T> Transform<T> source(List<T> list) { return new ListSource<>(list); }

    /** Gets the next item. */
    Option<A> next();

    /** The number of items to drop from the beginning of the output.  The drop happens before take(). */
    Transform<A> drop(int n);

//    @Override
    default Transform<A> filter(Function1<? super A,Boolean> f) {
        return new Filter<>(this, f);
    }

    default <B> Transform<B> flatMap(Function1<A,List<B>> f) {
        return new FlatMap<>(this, f, 0);
    }

//    @Override
    default <B> Transform<B> map(Function1<? super A, ? extends B> f) {
        return new Map<>(this, f);
    }

    /** Provides a way to collect the results of the transformation. */
//    @Override
    default <B> B foldLeft(B ident, Function2<B,? super A,B> f) {
        B ret = ident;
        Option<A> next = next();
        while (next.isSome()) {
            ret = f.apply(ret, next.get());
            next = next();
        }
        return ret;
    }
}
