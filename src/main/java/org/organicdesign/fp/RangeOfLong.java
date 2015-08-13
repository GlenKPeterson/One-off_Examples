// Copyright (c) 2014-03-08 PlanBase Inc. & Glen Peterson
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

package org.organicdesign.fp;

import org.organicdesign.fp.collections.UnmodSortedIterable;
import org.organicdesign.fp.collections.UnmodSortedIterator;
import org.organicdesign.fp.math.Rational;

import java.util.ArrayList;
import java.util.List;

/**
 A special case of an indexed sequence of (long) integers.  It currently assumes a step of 1.
 Like everything in Java, the iterator it produces is inclusive of the the start endpoint but
 exclusive of the end.

 In theory, a class like this could be used for anything that can provide it's next item.  To do
 that, Integer/Long would need to implement something that defined what the next() and previous()
 values.

 Currently limited to Long.MIN_VALUE to Long.MAX_VALUE.
 */
public class RangeOfLong implements UnmodSortedIterable<Long> {
    private final long start;
    private final long end;
    private final long size;

    private RangeOfLong(long s, long e) { start = s; end = e; size = (end - start) + 1; }

    public static RangeOfLong of(long s, long e) {
        if (e < s) {
            throw new IllegalArgumentException("end of range must be >= start of range");
        }
        return new RangeOfLong(s, e);
    }

    public static RangeOfLong of(Number s, Number e) {
        if ((s == null) || (e == null)) {
            throw new IllegalArgumentException("Nulls not allowed");
        }
        return new RangeOfLong(s.longValue(), e.longValue());
    }

//    public static RangeOfLong of(int s, int e) { return of((long) s, (long) e); }

    public long start() { return start; }
    public long end() { return end; }

    public long size() { return size; }

    public boolean contains(long i) { return (i >= start) && (i <= end); }

    public long get(long idx) {
        if (idx < size) { return start + idx; }
        throw new IllegalArgumentException("Index " + idx + " was outside the size of this range: " + start + " to " + end);
    }

    // You can ask for a given number of views, but what you get could be that number of fewer.
    public List<RangeOfLong> getSubRanges(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Must specify a positive number of ranges");
        }
        long numParts = (long) n;
        List<RangeOfLong> ranges = new ArrayList<>();
        if (numParts == 1) {
            ranges.add(this);
        } else {
            // TODO: Handle case where range is too small and also handle rounding error

//            System.out.println("sub-ranges for: " + this);
//            System.out.println("\tNum ranges: " + numParts);
//            System.out.println("\tsize: " + size());

            Rational viewSize = Rational.of(size(), numParts);
//            System.out.println("\tviewSize: " + viewSize);

            Rational partitionEnd = viewSize; // exact partition size - no rounding error.
            long startIdx = 0;
            for (long i = 0; partitionEnd.lte(size()); i++) {
//                System.out.println();
                long endIdx = partitionEnd.ceiling() - 1;
//                System.out.println("\t\tstartIdx: " + startIdx);
//                System.out.println("\t\tendIdx: " + endIdx);
                ranges.add(RangeOfLong.of(get(startIdx), get(endIdx)));
                startIdx = endIdx + 1;
//                System.out.println("\t\tnext startIdx: " + startIdx);
                partitionEnd = partitionEnd.plus(viewSize); // no rounding error
//                System.out.println("\t\tpartitionEnd: " + partitionEnd);
            }
        }
        return ranges;
    }

    @Override
    public int hashCode() { return (int) (start + end); }

    @Override
    public boolean equals(Object other) {
        // Cheapest operations first...
        if (this == other) { return true; }
        if ( !(other instanceof RangeOfLong) ) { return false; }

        // Details...
        final RangeOfLong that = (RangeOfLong) other;
        // This is not a database object; compare "significant" fields here.
        return (this.start == that.start) &&
               (this.end == that.end);
    }

    /**
     {@inheritDoc}
     Iterates from start of range (inclusive) up-to, but excluding, the end of the range.
     I'm not sure this is a good idea, but Python, Clojure, Scala, and just about everything in
     Java expects similar behavior.
     */
    @Override
    public UnmodSortedIterator<Long> iterator() {
        return new UnmodSortedIterator<Long>() {
            long idx = start;
            @Override public boolean hasNext() { return idx < end; }
            @Override public Long next() {
                Long t = idx;
                idx = idx + 1;
                return t;
            }
        };
    }
}