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

import org.organicdesign.fp.collections.UnmodSortedIterator;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
    TransDesc.OpStrategy drop(long d);

    /**
     Takes as many items as the source can handle.
     @param t the number of items to take.
     @return whether the source can handle the take, or pass-through (ask-supplier), or can't
     do either.
     */
    TransDesc.OpStrategy take(long t);

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
        @Override public TransDesc.OpStrategy drop(long d) {
            if (d < 1) { return TransDesc.OpStrategy.HANDLE_INTERNALLY; }
            drop = drop + d;
            return TransDesc.OpStrategy.HANDLE_INTERNALLY;
        }

        /** {@inheritDoc} */
        @Override public TransDesc.OpStrategy take(long take) {
            if (take < 0) {
                throw new IllegalArgumentException("Can't take less than zero items.");
            }
            if (numToTake == IGNORE_TAKE) {
                numToTake = take;
            } else if (take < numToTake) {
                numToTake = take;
            }
            return TransDesc.OpStrategy.HANDLE_INTERNALLY;
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
        @Override public TransDesc.OpStrategy drop(long d) {
            if (d > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Can't drop more than " + Integer.MAX_VALUE +
                                                   " items");
            } else if (d < 0) {
                throw new IllegalArgumentException("Makes no sense to drop less than 0 items");
            }

            if (d < 1) { return TransDesc.OpStrategy.HANDLE_INTERNALLY; }
            int numItems = size - idx;
            if (d > numItems) {
                idx = size; // used up.
            } else {
                idx = idx + (int) d;
            }
            return TransDesc.OpStrategy.HANDLE_INTERNALLY;
        }

        /** {@inheritDoc} */
        @Override public TransDesc.OpStrategy take(long take) {
            if (take < 0) {
                throw new IllegalArgumentException("Makes no sense to take less than 0 items");
            }
            // Taking none is equivalent to an empty source.
            if (take < 1) {
                idx = size;
                return TransDesc.OpStrategy.HANDLE_INTERNALLY;
            }

            // Taking more items than we have is not possible.  Just take all in that case.
            int numItems = size - idx;
            if (take < numItems) {
                size = idx + (int) take;
            }
            // If taking more than all the items, the take is meaningless.
            return TransDesc.OpStrategy.HANDLE_INTERNALLY;
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
