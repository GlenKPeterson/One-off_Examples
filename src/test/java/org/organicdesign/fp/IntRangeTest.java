// Copyright (c) 2014-03-09 PlanBase Inc. & Glen Peterson
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class IntRangeTest {
    @Test(expected = IllegalArgumentException.class)
    public void factory1() {
        RangeOfLong.of(null, Long.valueOf(1));
    }
    @Test(expected = IllegalArgumentException.class)
    public void factory2() {
        RangeOfLong.of(Long.valueOf(1), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void factory3() {
        RangeOfLong.of(1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void subRange() {
        RangeOfLong.of(1, 8).getSubRanges(0);
    }

    @Test
    public void basics() {
        RangeOfLong ir1 = RangeOfLong.of(0, 0);
        assertEquals(ir1.contains(0), true);
        assertEquals(ir1.contains(1), false);
        assertEquals(ir1.contains(-1), false);
        assertEquals(ir1.size(), 1);
    }

    @Test
    public void exactSubRanges() {
        RangeOfLong ir2 = RangeOfLong.of(1, 8);
        List<RangeOfLong> l = ir2.getSubRanges(1);
        assertEquals(l.size(), 1);
        assertEquals(l.get(0), ir2);

        l = RangeOfLong.of(1, 1).getSubRanges(1);
        assertEquals(l.size(), 1);
        assertEquals(l.get(0), RangeOfLong.of(1, 1));

        l = RangeOfLong.of(1, 2).getSubRanges(2);
        assertEquals(l.size(), 2);
        assertEquals(l.get(0), RangeOfLong.of(1, 1));
        assertEquals(l.get(1), RangeOfLong.of(2, 2));

        l = RangeOfLong.of(1, 8).getSubRanges(2);
        assertEquals(l.size(), 2);
        assertEquals(l.get(0), RangeOfLong.of(1, 4));
        assertEquals(l.get(1), RangeOfLong.of(5, 8));

        l = RangeOfLong.of(1, 3).getSubRanges(3);
        assertEquals(l.size(), 3);
        assertEquals(l.get(0), RangeOfLong.of(1, 1));
        assertEquals(l.get(1), RangeOfLong.of(2, 2));
        assertEquals(l.get(2), RangeOfLong.of(3, 3));

        l = RangeOfLong.of(1, 9).getSubRanges(3);
        assertEquals(l.size(), 3);
        assertEquals(l.get(0), RangeOfLong.of(1, 3));
        assertEquals(l.get(1), RangeOfLong.of(4, 6));
        assertEquals(l.get(2), RangeOfLong.of(7, 9));

        l = RangeOfLong.of(1, 99).getSubRanges(3);
        assertEquals(l.size(), 3);
        assertEquals(l.get(0), RangeOfLong.of(1, 33));
        assertEquals(l.get(1), RangeOfLong.of(34, 66));
        assertEquals(l.get(2), RangeOfLong.of(67, 99));
    }

    @Test
    public void roundedSubRanges() {
        List<RangeOfLong> l = RangeOfLong.of(1, 100).getSubRanges(3);
        assertEquals(l.size(), 3);
        assertEquals(l.get(0), RangeOfLong.of(1, 34));
        assertEquals(l.get(1), RangeOfLong.of(35, 67));
        assertEquals(l.get(2), RangeOfLong.of(68, 100));
    }

    @Test public void iteratorTest() {
        Iterator<Long> a = Arrays.asList(-2L, -1L, 0L, 1L, 2L, 3L, 4L).iterator();
        Iterator<Long> b = RangeOfLong.of(-2, 5).iterator();

        while (a.hasNext()) {
            assertTrue(b.hasNext());
            assertEquals(a.next(), b.next());
        }
        assertFalse(b.hasNext());

    }
}
