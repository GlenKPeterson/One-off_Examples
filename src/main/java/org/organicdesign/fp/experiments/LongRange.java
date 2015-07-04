package org.organicdesign.fp.experiments;

import org.organicdesign.fp.collections.UnmodSortedIterable;
import org.organicdesign.fp.collections.UnmodSortedIterator;

/**
 It would be super nice if Long implemented something beyond Comparable that would give a sense of
 what the *next* integer is.  In theory, this class could be used for anything that can provide it's
 next item.

 Does NOT handle an infinite range (yet).
 */
public class LongRange implements UnmodSortedIterable<Long> {
    private final long start;
    private final long end;
    private final long size;

    private LongRange(long s, long e) { start = s; end = e; size = (end - start) + 1; }

    public static LongRange of(long s, long e) {
        if (e < s) {
            throw new IllegalArgumentException("end of range must be >= start of range");
        }
        return new LongRange(s, e);
    }

    public static LongRange of(Number s, Number e) {
        if ((s == null) || (e == null)) {
            throw new IllegalArgumentException("Nulls not allowed");
        }
        return new LongRange(s.longValue(), e.longValue());
    }

//    public static LongRange of(int s, int e) { return of((long) s, (long) e); }

    public long start() { return start; }
    public long end() { return end; }

    /**
     For an infinite range could return one of: 1. Infinite, 2. Known (returns an integer), or
     it could return a range indicating known/unknown upper and lower bounds on the size.  It's at
     most 10M items, or at least 3 items, or similar.  Of course, any lower bound without an upper
     bound is still Infinite, so how is that different from Infinite?
     */
    public long size() { return size; }

    public boolean contains(long i) { return (i >= start) && (i <= end); }

    public long get(long idx) {
        if (idx < size) { return start + idx; }
        throw new IllegalArgumentException("Index " + idx + " was outside the size of this range: " + start + " to " + end);
    }

//    // You can ask for a given number of views, but what you get could be that number of fewer.
//    public ImList<LongRange> getSubRanges(int n) {
//        if (n < 1) {
//            throw new IllegalArgumentException("Must specify a positive number of ranges");
//        }
//        long numParts = (long) n;
//        ImList<LongRange> ranges = PersistentVector.empty();
//        if (numParts == 1) {
//            ranges = ranges.appendOne(this);
//        } else {
//            // TODO: Handle case where range is too small and also handle rounding error
//
////            System.out.println("sub-ranges for: " + this);
////            System.out.println("\tNum ranges: " + numParts);
////            System.out.println("\tsize: " + size());
//
//            Rational viewSize = Rational.of(size(), numParts);
////            System.out.println("\tviewSize: " + viewSize);
//
//            Rational partitionEnd = viewSize; // exact partition size - no rounding error.
//            long startIdx = 0;
//            for (long i = 0; partitionEnd.lte(size()); i++) {
////                System.out.println();
//                long endIdx = partitionEnd.ceiling() - 1;
////                System.out.println("\t\tstartIdx: " + startIdx);
////                System.out.println("\t\tendIdx: " + endIdx);
//                ranges = ranges.appendOne(LongRange.of(get(startIdx), get(endIdx)));
//                startIdx = endIdx + 1;
////                System.out.println("\t\tnext startIdx: " + startIdx);
//                partitionEnd = partitionEnd.plus(viewSize); // no rounding error
////                System.out.println("\t\tpartitionEnd: " + partitionEnd);
//            }
//        }
//        return ranges;
//    }

    @Override
    public int hashCode() { return (int) (start + end); }

    @Override
    public boolean equals(Object other) {
        // Cheapest operations first...
        if (this == other) { return true; }
        if ( !(other instanceof LongRange) ) { return false; }

        // Details...
        final LongRange that = (LongRange) other;
        // This is not a database object; compare "significant" fields here.
        return (this.start == that.start) &&
               (this.end == that.end);
    }

    /** {@inheritDoc} */
    @Override public UnmodSortedIterator<Long> iterator() {
        // TODO: this is exclusive of both endpoints.  I would think inclusive would be better, or subclasses RangeIncExc, RangeExcInc, RangeIncInc, RangeExcExc
        return new UnmodSortedIterator<Long>() {
            long s = start;
            @Override public boolean hasNext() { return s <= end; }
            @Override public Long next() {
                Long t = s;
                s = s + 1;
                return t;
            }
        };
    }
}