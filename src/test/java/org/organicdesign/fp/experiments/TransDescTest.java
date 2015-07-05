package org.organicdesign.fp.experiments;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.organicdesign.fp.collections.PersistentVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class TransDescTest extends TestCase {

    public static void basics(TransDesc<Integer> td) {
        assertEquals(Arrays.asList(1, 2, 3),
                     td.foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                         accum.add(i);
                         return accum;
                     }));
        assertEquals(Arrays.asList(2, 3, 4),
                     td.map(i -> i + 1)
                       .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                           accum.add(i);
                           return accum;
                       }));
        assertEquals(Arrays.asList(1, 3),
                     td.filter(i -> i != 2)
                       .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                           accum.add(i);
                           return accum;
                       }));
        assertEquals(Arrays.asList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                     td.flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                       .foldLeft(new ArrayList<>(), (accum, i) -> {
                           accum.add(i);
                           return accum;
                       }));
        assertEquals(Arrays.asList(2, 3),
                     td.drop(1)
                       .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                           accum.add(i);
                           return accum;
                       }));
    }

    @Test public void testBasics() {
        Integer[] src = new Integer[] {1, 2, 3};
        basics(TransDesc.fromList(Arrays.asList(src)));
        basics(TransDesc.fromIterable(Arrays.asList(src)));
        basics(TransDesc.fromArray(src));
    }

    public static void longerCombinations(TransDesc<Integer> td) {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                     td.foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                         accum.add(i);
                         return accum;
                     }));
        assertEquals(Arrays.asList(2, 4, 6, 8),
                     td.filter(i -> i % 2 == 0)
                       .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                           accum.add(i);
                           return accum;
                       }));
        assertEquals(Arrays.asList(3, 5, 7, 9),
                     td.filter(i -> i % 2 == 0)
                       .map(i -> i + 1)
                       .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                           accum.add(i);
                           return accum;
                       }));
        assertEquals(Arrays.asList(3, 30, 300, 5, 50, 500, 7, 70, 700, 9, 90, 900),
                     td.filter(i -> i % 2 == 0)
                       .map(i -> i + 1)
                       .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                       .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                           accum.add(i);
                           return accum;
                       }));
        assertEquals(Arrays.asList(5, 50, 500, 7, 70, 700, 9, 90, 900),
                     td.drop(2)
                       .filter(i -> i % 2 == 0)
                       .map(i -> i + 1)
                       .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                       .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                           accum.add(i);
                           return accum;
                       }));
//        System.out.println("Testing separate drop.");
        assertEquals(Arrays.asList(7, 9),
                     td.filter(i -> i % 2 == 0)
                       .map(i -> i + 1)
                       .drop(2)
                       .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                           accum.add(i);
                           return accum;
                       }));
//        System.out.println("Done testing separate drop.");
        assertEquals(Arrays.asList(7, 70, 700, 9, 90, 900),
                     td.filter(i -> i % 2 == 0)
                       .map(i -> i + 1)
                       .drop(2)
                       .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                       .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                           accum.add(i);
                           return accum;
                       }));
        assertEquals(Arrays.asList(500, 7, 70, 700, 9, 90, 900),
                     td.filter(i -> i % 2 == 0)
                       .map(i -> i + 1)
                       .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                       .drop(5)
                       .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                           accum.add(i);
                           return accum;
                       }));
    }

    @Test public void longerCombinations() {
        Integer[] src = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        longerCombinations(TransDesc.fromList(Arrays.asList(src)));
        longerCombinations(TransDesc.fromIterable(Arrays.asList(src)));
        longerCombinations(TransDesc.fromArray(src));
    }

}