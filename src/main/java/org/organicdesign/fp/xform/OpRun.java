package org.organicdesign.fp.xform;

import org.organicdesign.fp.function.Function1;

/**
 OpRuns are mutable operations that the transform carries out when it is run.  This is in
 contrast to the TransDesc which are like the "source code" or transformation description.
 OpRuns are like compiled "op codes" of the transform.
 */
abstract class OpRun {
    // Time using a linked list of ops instead of array, so that we can easily remove ops from
    // the list when they are used up.
    Function1<Object,Boolean> filter = null;
    Function1 map = null;
    Function1<Object,MutableSourceProvider> flatMap = null;
//        Function1<Object,Boolean> keepGoing = null;

    public TransDesc.OpStrategy drop(long num) { return TransDesc.OpStrategy.CANNOT_HANDLE; }

    public TransDesc.OpStrategy take(long num) { return TransDesc.OpStrategy.CANNOT_HANDLE; }

//        public OpStrategy concatList(MutableSource nextSrc) { return OpStrategy.CANNOT_HANDLE; }

    /**
     We need to model this as a separate op for when the previous op is CANNOT_HANDLE.  It is
     coded as a filter, but still needs to be modeled separately so that subsequent drops can be
     combined into the earliest single explicit drop op.  Such combinations are additive,
     meaning that drop(3).drop(5) is equivalent to drop(8).
     */
    static class DropRun extends OpRun {
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
        @Override public TransDesc.OpStrategy drop(long num) {
            leftToDrop = leftToDrop + num;
            return TransDesc.OpStrategy.HANDLE_INTERNALLY;
        }
    }

    static class FilterRun extends OpRun {
        FilterRun(Function1<Object,Boolean> func) { filter = func; }
    }

    static class MapRun extends OpRun {
        MapRun(Function1 func) { map = func; }
        @Override public TransDesc.OpStrategy drop(long num) { return TransDesc.OpStrategy.ASK_SUPPLIER; }
        @Override public TransDesc.OpStrategy take(long num) { return TransDesc.OpStrategy.ASK_SUPPLIER; }
    }

    // TODO: FlatMap should drop and take internally using addition/subtraction on each output
    // TODO: list instead of testing each list item individually.
    static class FlatMapRun extends OpRun {
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
    static class TakeRun extends OpRun {
        private long numToTake;
        TakeRun(long take) {
            numToTake = take;
            map = a -> {
                if (numToTake > 0) {
                    numToTake = numToTake - 1;
                    return a;
                }
                return TransDesc.TERMINATE;
            };
        }

        @Override public TransDesc.OpStrategy take(long num) {
            if (num < 0) {
                throw new IllegalArgumentException("Can't take less than 0 items.");
            }
            if (num < numToTake) {
                numToTake = num;
            }
            return TransDesc.OpStrategy.HANDLE_INTERNALLY;
        }
    }
}
