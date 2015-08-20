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

import org.organicdesign.fp.function.Function1;

class FilterDesc<T> extends TransDesc<T> {
    final Function1<? super T,Boolean> f;

    FilterDesc(TransDesc<T> prev, Function1<? super T,Boolean> func) { super(prev); f = func; }

    @SuppressWarnings("unchecked")
    @Override RunList toRunList() {
        RunList ret = prevOp.toRunList();
        ret.list.add(new OpRun.FilterRun((Function1<Object,Boolean>) f));
        return ret;
    }
}
