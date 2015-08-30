package org.organicdesign.fp;

import org.junit.Test;
import org.organicdesign.fp.ephemeral.View;
import org.organicdesign.fp.function.Function0;
import org.organicdesign.fp.function.Function1;
import org.organicdesign.fp.permanent.Sequence;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.organicdesign.fp.StaticImports.unmodList;

public class IterableSpeedTest {

    public interface Option2<T> {
        /** Return the value wrapped in this Option.  Only safe to call this on Some. */
        T get();

        /** If this is Some, return the value wrapped in this Option.  Otherwise, return the given value. */
        T getOrElse(T t);

        /** Is this Some? */
        boolean isSome();

        /** Pass in a function to execute if its Some and another to execute if its None. */
        <U> U patMat(Function1<T,U> has, Function0<U> hasNot);

        // ==================================================== Static ====================================================
        /** None is a singleton and this is its only instance. */
        Option2 NONE = new None();

        /** Calling this instead of referring to NONE directly can make the type infrencer happy. */
        @SuppressWarnings("unchecked")
        static <T> Option2<T> none() { return NONE; }

        /** Public static factory method for contructing Option2s. */
        static <T> Option2<T> of(T t) {
            if (NONE.equals(t)) {
                return none();
            }
            return new Some<>(t);
        }

        /** Represents the absence of a value */
        final class None<T> implements Option2<T> {
            /** Private constructor for singleton. */
            private None() {}

            @Override public T get() { throw new IllegalStateException("Called get on None"); }

            @Override public T getOrElse(T t) { return t; }

            @Override public boolean isSome() { return false; }

            @Override public <U> U patMat(Function1<T,U> has, Function0<U> hasNot) { return hasNot.get(); }

            /** Valid, but deprecated because it's usually an error to call this in client code. */
            @Deprecated // Has no effect.  Darn!
            @Override public int hashCode() { return 0; }

            /** Valid, but deprecated because it's usually an error to call this in client code. */
            @Deprecated // Has no effect.  Darn!
            @Override public boolean equals(Object other) { return (this == other) || (other instanceof None); }

            // Defend our singleton property in the face of deserialization.  Not sure this is necessary, but probably
            // won't hurt.
            private Object readResolve() { return NONE; }
        }

        /** Represents the presence of a value, even if that value is null. */
        class Some<T> implements Option2<T> {
            private final T item;
            private Some(T t) { item = t; }

            //public static Some<T> of(T t) { return new Option2(t); }

            @Override public T get() { return item; }

            @Override public T getOrElse(T t) { return item; }

            @Override public boolean isSome() { return true; }

            @Override public <U> U patMat(Function1<T,U> has, Function0<U> hasNot) {
                return has.apply(item);
            }

            /** Valid, but deprecated because it's usually an error to call this in client code. */
            @Deprecated // Has no effect.  Darn!
            @Override public int hashCode() {
                // We return Integer.MIN_VALUE for null to make it different from None which always returns zero.
                return item == null ? Integer.MIN_VALUE : item.hashCode();
            }

            /** Valid, but deprecated because it's usually an error to call this in client code. */
            @Deprecated // Has no effect.  Darn!
            @Override public boolean equals(Object other) {
                if (this == other) { return true; }
                if ( !(other instanceof Option2) ) { return false; }

                final Option2 that = (Option2) other;
                return that.isSome() && Objects.equals(this.item, that.get());
            }
        }
    }


    public class SortedViewFromArray<T> {

        private final T[] items;
        private int idx;
        private int length;

        private SortedViewFromArray(int i, int l, T[] ts) {
            idx = i;
            length = l;
            items = ts;
        }

        public synchronized Option2<T> next() {
            if (idx == length) {
                return Option2.none();
            }
            return Option2.of(items[idx++]); // return current idx then increment idx
        }
    }

    public class SortedViewFromArray2<T> {

        private final T[] items;
        private int idx;
        private int length;

        private SortedViewFromArray2(int i, int l, T[] ts) {
            idx = i;
            length = l;
            items = ts;
        }

        public synchronized Option2<T> next() {
            if (idx == length) {
                return Option2.none();
            }
            // TODO: This is a clear 8% speed-up over the ++ operator.
            Option2<T> ret = Option2.of(items[idx]);
            idx = idx + 1;
            return ret;
            //return Option2.of(items[idx++]); // return current idx then increment idx
        }
    }

    public static class Option3<T> {
        private final T item;
        private Option3(T t) { item = t; }

        public T get() { return item; }

        public T getOrElse(T t) { return item; }

        public boolean isSome() { return true; }

        // ==================================================== Static ====================================================
        /** None is a singleton and this is its only instance. */
        public static final Option3<?> NONE = new Option3<Object>(null) {
            @Override public Object get() { throw new IllegalStateException("Called get on None"); }

            @Override public Object getOrElse(Object t) { return t; }

            @Override public boolean isSome() { return false; }

            @Override public <U> U patMat(Function1<Object,U> has, Function0<U> hasNot) {
                return hasNot.apply();
            }

            /** Valid, but deprecated because it's usually an error to call this in client code. */
            @Override public int hashCode() { return 0; }

            /** Valid, but deprecated because it's usually an error to call this in client code. */
            @Override public boolean equals(Object other) {
                if (this == other) { return true; }
                if ( !(other instanceof Option3) ) { return false; }

                final Option3 that = (Option3) other;
                return !that.isSome();
            }
        };

        /** Calling this instead of referring to NONE directly can make the type infrencer happy. */
        @SuppressWarnings("unchecked")
        public static <T> Option3<T> none() { return (Option3<T>) NONE; }

        /** Public static factory method for contructing Option3s. */
        public static <T> Option3<T> of(T t) {
            if (NONE.equals(t)) {
                return none();
            }
            return new Option3<>(t);
        }


        public <U> U patMat(Function1<T,U> has, Function0<U> hasNot) {
            return has.apply(item);
        }

        /** Valid, but deprecated because it's usually an error to call this in client code. */
        @Override public int hashCode() {
            // We return Integer.MIN_VALUE for null to make it different from None which always returns zero.
            return item == null ? Integer.MIN_VALUE : item.hashCode();
        }

        /** Valid, but deprecated because it's usually an error to call this in client code. */
        @Override public boolean equals(Object other) {
            if (this == other) { return true; }
            if ( !(other instanceof Option3) ) { return false; }

            final Option3 that = (Option3) other;
            return that.isSome() && Objects.equals(this.item, that.get());
        }
    }

    public class SortedViewFromArray3<T> {

        private final T[] items;
        private int idx;
        private int length;

        private SortedViewFromArray3(int i, int l, T[] ts) {
            idx = i;
            length = l;
            items = ts;
        }

        public synchronized Option3<T> next() {
            if (idx == length) {
                return Option3.none();
            }
            Option3<T> ret = Option3.of(items[idx]);
            idx = idx + 1;
            return ret;
            //return Option2.of(items[idx++]); // return current idx then increment idx
        }
    }

    public static class SortedSequenceFromArray2<T> {
        public static final SortedSequenceFromArray2<?> EMPTY_SEQUENCE = new SortedSequenceFromArray2<Object>(0, null) {
            @Override public Option<Object> head() { return Option.none(); }

            @Override public SortedSequenceFromArray2<Object> tail() {
                throw new UnsupportedOperationException("Can't tail emptySequence");
            }

            @Override public int hashCode() { return 0; }

            @Override public boolean equals(Object other) {
                // Cheapest operation first...
                if (this == other) { return true; }

                return (other instanceof SortedSequenceFromArray2) &&
                       !((SortedSequenceFromArray2) other).head().isSome();
            }

            @Override public String toString() { return "emptySequence()"; }
        };

        @SuppressWarnings("unchecked")
        public static <T> SortedSequenceFromArray2<T> emptySequence() {
            return (SortedSequenceFromArray2<T>) EMPTY_SEQUENCE;
        }

        private T[] items;
        private int idx;
        private SortedSequenceFromArray2<T> next;

        private SortedSequenceFromArray2(int i, T[] ts) { idx = i; items = ts; }

        @SafeVarargs
        static <T> SortedSequenceFromArray2<T> of(T... ts) {
            if ((ts == null) || (ts.length < 1)) { return emptySequence(); }
            return new SortedSequenceFromArray2<>(0, ts);
        }

        public Option<T> head() { return Option.of(items[idx]); }

        // This whole method is synchronized on the advice of Goetz2006 p. 347
        public synchronized SortedSequenceFromArray2<T> tail() {
            if (next == null) {
                int nextIdx = idx + 1;
                if (nextIdx < items.length) {
                    next = new SortedSequenceFromArray2<>(nextIdx, items);
                } else {
                    next = emptySequence();
                }
            }
            return next;
        }

    }


    public long benchmark(String prompt, Function0<Long> f) {
        System.out.print(prompt + ": ");
        int n = 15;
        long[] times = new long[n];
        for (int i = 0; i < n; i++) {
            long startTime = System.currentTimeMillis();
            f.apply();
            times[i] = System.currentTimeMillis() - startTime;
            System.out.print(times[i] + ", ");
        }
        Arrays.sort(times);
        double mean = 0;
        for (long l : times) {
            mean = mean + l;
        }
        mean = mean / times.length;

        System.out.println("\nMinimum: " + times[0] + "\nMean:    " + mean + "\nMedian:  " + times[n/2] + "\nMaximum: " + times[n - 1] + "\n");
        return times[0];
    }

    @Test public void speedTest() {
        // This is the real/better test.  Really should comment this whole thing out as it's just an experiment.
//      int MAX = 30000000;
        int MAX = 300000;
        long startTime;

        startTime = System.currentTimeMillis();
        System.out.println("Allocating large array...");
        final Long[] ls = new Long[MAX];
        System.out.println("Array allocat: " + (System.currentTimeMillis() - startTime));

        System.out.print("Array filling: ");
        startTime = System.currentTimeMillis();
        for (int i = 0; i < MAX; i++) {
            ls[i] = Long.valueOf(i);
        }
        System.out.println((System.currentTimeMillis() - startTime));

        System.out.print("List construction: ");
        startTime = System.currentTimeMillis();
        List<Long> lsList = Arrays.asList(ls);
        System.out.println((System.currentTimeMillis() - startTime));

        Function0<Long> forLoop = () -> {
            Long l = null;
            for (int i = 0; i < MAX; i++) {
                l = ls[i];
                if (l < -1) { break; }
            }
            return l;
        };

        benchmark("for (i=0;...)", forLoop);

        benchmark("SeqFromArray1", () -> {
            Sequence<Long> s1 = Sequence.ofArray(ls);
            while (s1.head().isSome()) {
                if (s1.head().get() < -1) { break; }
                s1 = s1.tail();
            }
            return s1.head().getOrElse(0L);
        });

//        benchmark("SeqFromArray2", () -> {
//            SortedSequenceFromArray2<Long> s1 = SortedSequenceFromArray2.of(ls);
//            while (s1.head().isSome()) {
//                if (s1.head().get() < -1) { break; }
//                s1 = s1.tail();
//            }
//            return s1.head().getOrElse(0L);
//        });

        benchmark("ViewFromArray", () -> {
            View<Long> s1 = View.ofArray(ls);
            Option<Long> item = s1.next();
            while (item.isSome()) {
                if (item.get() < -1) { break; }
                item = s1.next();
            }
            return item.getOrElse(0L);
        });

        benchmark("SortedViewArr", () -> {
            SortedViewFromArray<Long> s1 = new SortedViewFromArray<>(0, ls.length, ls);
            Option2<Long> item = s1.next();
            while (item.isSome()) {
                if (item.get() < -1) { break; }
                item = s1.next();
            }
            return item.getOrElse(0L);
        });

        benchmark("SortedViewAr2", () -> {
            SortedViewFromArray2<Long> s1 = new SortedViewFromArray2<>(0, ls.length, ls);
            Option2<Long> item = s1.next();
            while (item.isSome()) {
                if (item.get() < -1) {
                    break;
                }
                item = s1.next();
            }
            return item.getOrElse(0L);
        });

        benchmark("SortedViewAr3", () -> {
            SortedViewFromArray3<Long> s1 = new SortedViewFromArray3<>(0, ls.length, ls);
            Option3<Long> item = s1.next();
            while (item.isSome()) {
                if (item.get() < -1) { break; }
                item = s1.next();
            }
            return item.getOrElse(0L);
        });

        benchmark("List Iterator", () -> {
            Iterator<Long> iter = lsList.iterator();
            Long l = null;
            while (iter.hasNext()) {
                l = iter.next();
                if (l < -1) { break; }
            }
            return l;
        });

        benchmark("new  for loop", () -> {
            for (Long l : ls) {
                if (l < -1) {
                    break;
                }
            }
            return 0L;
        });

        benchmark("unmodArrayItr", () -> {
            Iterator<Long> iter = unmodList(lsList).iterator();
            Long l = null;
            while (iter.hasNext()) {
                l = iter.next();
                if (l < -1) {
                    break;
                }
            }
            return l;
        });

        benchmark("while (i<MAX)", () -> {
            Long l = null;
            int i = 0;
            while (i != MAX) {
                l = ls[i];
                i = i + 1;
                if (l < -1) {
                    break;
                }
            }
            return l;
        });

//        benchmark("SeqFromArray1", () -> {
//            Sequence<Long> s1 = Sequence.of(ls);
//            while (s1.head().isSome()) {
//                if (s1.head().get() < -1) { break; }
//                s1 = s1.tail();
//            }
//            return s1.head().getOrElse(0L);
//        });

        benchmark("Transform4List", () -> {
            Xform<Long> t = Xform.from(lsList);
            return t.foldLeft(0L, (accum, i) -> i < -1 ? i : accum);
        });

        benchmark("Transform4Array", () -> {
            Xform<Long> t = Xform.fromArray(ls);
            return t.foldLeft(0L, (accum, i) -> i < -1 ? i : accum);
        });
    }
}
