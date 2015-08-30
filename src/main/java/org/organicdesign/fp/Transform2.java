package org.organicdesign.fp;

// We model this as a linked list so that each transition can have it's own output type, building a type-safe bridge
// from first operation to the last.
public interface Transform2<A> { // extends Transformable<A> {
//
//    abstract class OpRun {
//        OpRun nextOp = null;
//        // TODO: Function1<Object,Boolean> preTerm = null;
//        public Function1<Object,Boolean> filter = null;
//        public Function1 map = null;
//        public Function1<Object,List> flatMap = null;
//        // TODO: Function1<Object,Boolean> postTerm = null;
////        public abstract OpRun drop(int num);
//        private static class ListSourceRun extends OpRun {
//            final List as;
//            int idx = 0;
//
//            ListSourceRun(List list, OpRun next) { as = list; nextOp = next; }
//
////            @Override
//            public OpRun drop(int num) {
//                idx = idx + num;
//                return this;
//            }
//        }
//
//        private static class FilterOp extends OpRun {
//            FilterOp(Function1<Object,Boolean> func, OpRun next) {
//                filter = func; nextOp = next;
//            }
//
////            @Override
////            public OpRun drop(int num) {
////                return new FilterOp(prevOp.drop(num), f);
////            }
//
////            @Override
////            public Option<T> next() {
////                Option<T> next = prevOp.next();
////                while (next.isSome() && !f.apply(next.get())) {
////                    next = prevOp.next();
////                }
////                return next;
////            }
//        }
//
//        private static class MapOp extends OpRun {
//            MapOp(Function1 func, OpRun next) {
//                map = func; nextOp = next;
//            }
//
////            @Override
////            public OpDesc<U> drop(int num) {
////                return new MapDesc<>(prevOp.drop(num), f);
////            }
////
////            @Override Option<U> next() {
////                Option<T> next = prevOp.next();
////                return next.isSome() ? Option.of(f.apply(next.get())) : Option.none();
////            }
//        }
//
//        private static class FlatMapOp extends OpRun {
//            // TODO: This is mutable state.  Figure out how to push it to a mutable action and keep the "OpDesc" immutable.
////            ListSourceDesc<U> cache = null;
//            int numToDrop = 0;
//
//            FlatMapOp(Function1<Object,List> func, int drop, OpRun next) {
//                flatMap = func; numToDrop = drop; nextOp = next;
//            }
//
////            @Override
//            public OpRun drop(int num) {
//                return new FlatMapOp(flatMap, numToDrop + num, nextOp);
//            }
//
////            @Override
////            public Option<U> next() {
////                while ((cache == null) || (cache.idx == cache.as.size())) {
////                    Option<T> next = prevOp.next();
////                    if (next.isSome()) {
////                        cache = new ListSourceDesc<>(f.apply(next.get()));
////                    } else {
////                        return Option.none();
////                    }
////                    if (numToDrop > 0) {
////                        if (numToDrop >= cache.as.size()) {
////                            numToDrop -= cache.as.size();
////                            cache = null;
////                        } else {
////                            cache.idx += numToDrop;
////                            numToDrop = 0;
////                        }
////                    }
////                }
////                return cache.next();
////            }
//        }
//
//    }
//
//    // Using an abstract class here to limit the visibility of the next() method and present a less mutable
//    // interface to the outside world.
//    static abstract class OpDesc<F> implements Transform2<F> {
//        OpDesc<F> prevOp;
//
//        /** Gets the next item. */
////        abstract Option<F> next();
//        abstract void addOpRunToList(List<OpRun> ops, int drop, int take);
//
//        // Do I want to go back to modeling this as a separate step, then compress the steps
//        // as much as possible to move the drop and take operations as early in the stream as possible.
//        @Override public abstract OpDesc<F> drop(int n);
//
//        @Override public OpDesc<F> filter(Function1<? super F,Boolean> f) {
//            // If the function is not supplied, or if it's a constant function that always returns true,
//            // Then this is a no-op and should be ignored.
//            if ( (f == null) ||
//                    (Function1.accept() == f) ) {
//                return this;
//            }
//            // Add this optimization...
////            if (Function1.reject() == f) {
////                return emptyTransform();
////            }
//            return new FilterDesc<>(this, f);
//        }
//
//        @Override public <G> OpDesc<G> flatMap(Function1<F,List<G>> f) {
//            return new FlatMapDesc<>(this, f, 0);
//        }
//
//        @Override public <G> OpDesc<G> map(Function1<? super F, ? extends G> f) {
//            return new MapDesc<>(this, f);
//        }
//
//        /** Provides a way to collect the results of the transformation. */
//    //    @Override
//        @Override public <G> G foldLeft(G ident, Function2<G,? super F,G> f) {
////            G ret = ident;
////            Option<F> next = next();
////            while (next.isSome()) {
////                ret = f.apply(ret, next.get());
////                next = next();
////            }
////            return ret;
//            List<OpRun> ops = new ArrayList<>();
//            OpDesc desc = this;
//            while (desc.prevOp != null) {
//
//            }
//            return _foldLeft(desc.as, ops, 0, ident, f);
//        }
//
//        @SuppressWarnings("unchecked")
//        private <G> G _foldLeft(Iterable source, OpRun[] ops, int opIdx, G ident, Function2 combiner) {
//            Object ret = ident;
//            for (Object o : source) {
//                for (int j = opIdx; j < ops.length; j++) {
//                    OpRun op = ops[j];
//                    if ( (op.filter != null) && !op.filter.apply(o) ) {
//                        break; // stop processing this soruce item and go to the next one.
//                    }
//                    if (op.map != null) {
//                        o = op.map.apply(o);
//                    } else if (op.flatMap != null) {
//                        ret = _foldLeft(op.flatMap.apply(o), ops, j + 1, ret, combiner);
//                        break;
//                    }
//                }
//                ret = combiner.apply(ret, o);
//            }
//            return (G) ret;
//        }
//
//        private static class ListSourceDesc<T> extends OpDesc<T> {
//            final List<T> as;
//            // TODO: This is mutable state.  Figure out how to push it to a mutable action and keep the "OpDesc" immutable.
//            final int idx;
//
//            ListSourceDesc(List<T> list, int i) { as = list; idx = i; }
//
////            @Override
////            public OpDesc<T> drop(int num) {
////                idx = idx + num;
////                return this;
////            }
//
////            @Override
////            public Option<T> next() {
////                return (idx < as.size()) ? Option.of(as.get(idx)) : Option.none();
////            }
//        }
//
//        private static class FilterDesc<T> extends OpDesc<T> {
//            final Function1<? super T,Boolean> f;
//
//            FilterDesc(OpDesc<T> prev, Function1<? super T,Boolean> func) {
//                prevOp = prev;
//                f = func;
//            }
//
//            @Override
//            public OpDesc<T> drop(int num) {
//                return new FilterDesc<>(prevOp.drop(num), f);
//            }
//
//            @Override
//            public Option<T> next() {
//                Option<T> next = prevOp.next();
//                while (next.isSome() && !f.apply(next.get())) {
//                    next = prevOp.next();
//                }
//                return next;
//            }
//        }
//
//        private static class MapDesc<T, U> extends OpDesc<U> {
//            final OpDesc<T> prevOp;
//            final Function1<? super T,? extends U> f;
//
//            MapDesc(OpDesc<T> prev, Function1<? super T,? extends U> func) {
//                prevOp = prev;
//                f = func;
//            }
//
//            @Override
//            public OpDesc<U> drop(int num) {
//                return new MapDesc<>(prevOp.drop(num), f);
//            }
//
//            @Override Option<U> next() {
//                Option<T> next = prevOp.next();
//                return next.isSome() ? Option.of(f.apply(next.get())) : Option.none();
//            }
//        }
//
//        private static class FlatMapDesc<T, U> extends OpDesc<U> {
//            final OpDesc<T> prevOp;
//            final Function1<T,List<U>> f;
//            // TODO: This is mutable state.  Figure out how to push it to a mutable action and keep the "OpDesc" immutable.
//            ListSourceDesc<U> cache = null;
//            int numToDrop = 0;
//
//            FlatMapDesc(OpDesc<T> prev, Function1<T,List<U>> func, int drop) {
//                prevOp = prev;
//                f = func;
//                numToDrop = drop;
//            }
//
//            @Override
//            public OpDesc<U> drop(int num) {
//                return new FlatMapDesc<>(prevOp, f, numToDrop + num);
//            }
//
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
//        }
//    } // end abstract class OpDesc
//
//    // This is just a sample usage to be sure it compiles.
////    Integer total = from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9))
////            .drop(1)
////            .filter(i -> i < 7)
////            .map(String::valueOf)
////            .flatMap(s -> Arrays.asList(s, s, s))
////            .foldLeft(0, (count, s) -> count + 1);
//
//    /** Constructor.  Need to add an Iterable constructor and maybe some day even an array constructor. */
//    static <T> Transform2<T> from(List<T> list) { return new OpDesc.ListSourceDesc<>(list); }
//
//    // ================================================================================================================
//    // These will come from Transformable, but (will be) overridden to have a different return type.
//
//    /** The number of items to drop from the beginning of the output.  The drop happens before take(). */
//    Transform2<A> drop(int n);
//
////    @Override
//    Transform2<A> filter(Function1<? super A,Boolean> f);
//
//    <B> Transform2<B> flatMap(Function1<A,List<B>> f);
//
////    @Override
//    <B> Transform2<B> map(Function1<? super A, ? extends B> f);
//
//    /** Provides a way to collect the results of the transformation. */
////    @Override
//    <B> B foldLeft(B ident, Function2<B,? super A,B> f);
}
