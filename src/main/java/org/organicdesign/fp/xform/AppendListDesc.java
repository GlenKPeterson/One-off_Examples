package org.organicdesign.fp.xform;

/**
 Represents the results of a previous transformation pre-pended to a list of additional items.
 @param <T>
 */
class AppendListDesc<T> extends TransDesc<T> {
    final SourceProviderListDesc<T> src;

    AppendListDesc(TransDesc<T> prev, SourceProviderListDesc<T> s) { super(prev); src = s; }

    @SuppressWarnings("unchecked")
    @Override RunList toRunList() {
        MutableSource ms = new MutableSource.MutableListSource<>(src.list, 0);
        RunList ret = prevOp.toRunList();
        int i = ret.list.size() - 1;
//              System.out.println("\tchecking previous items to see if they can handle a drop...");
        if (i > -1) {
            OpRun opRun = ret.list.get(i);
            if (opRun instanceof MutableSourceProvider) {
                RunList ret2 = RunList.of(ret, ms);
                ret2.list = ret.list;
                return ret2;
            }
//                OpStrategy earlierA = opRun.concatList(ms);
//                if (earlierA == OpStrategy.HANDLE_INTERNALLY) {
//                    return ret;
//                }
        }
        return RunList.of(ret, ms);
    }
}
