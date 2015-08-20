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

/**
 Describes a "drop" operation.  Drops will be pushed as early in the operation-list as possible,
 ideally being done using one-time pointer addition on the source.  When that is not possible,
 a Drop op-code is created (eventually implemented as a filter function).  Subsequent drop ops
 will be combined into the earliest drop (for speed).
 @param <T> the expected input type to drop.
 */
class DropDesc<T> extends TransDesc<T> {
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
