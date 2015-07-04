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
public class Transform4Test extends TestCase {

    @Test public void testBasics() throws Exception {
        TransDesc<Integer> listXform = TransDesc.fromList(Arrays.asList(1, 2, 3));
        TransDesc<Integer> iterXform = TransDesc.fromIterable(Arrays.asList(1, 2, 3));

        assertEquals(Arrays.asList(1, 2, 3),
                     listXform.foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                         accum.add(i);
                         return accum;
                     }));
        assertEquals(Arrays.asList(1, 2, 3),
                     iterXform.foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                         accum.add(i);
                         return accum;
                     }));

        assertEquals(Arrays.asList(2, 3, 4),
                     listXform.map(i -> i + 1)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(2, 3, 4),
                     iterXform.map(i -> i + 1)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));

        assertEquals(Arrays.asList(1, 3),
                     listXform.filter(i -> i != 2)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(1, 3),
                     iterXform.filter(i -> i != 2)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));

        assertEquals(Arrays.asList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                     listXform.flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                              .foldLeft(new ArrayList<>(), (accum, i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                     iterXform.flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                              .foldLeft(new ArrayList<>(), (accum, i) -> {
                                  accum.add(i);
                                  return accum;
                              }));

        assertEquals(Arrays.asList(2, 3),
                     listXform.drop(1)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(2, 3),
                     iterXform.drop(1)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
    }

    @Test public void longerCombinations() throws Exception {
        TransDesc<Integer> listXform = TransDesc.fromList(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        TransDesc<Integer> iterXform = TransDesc.fromIterable(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                     listXform.foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                         accum.add(i);
                         return accum;
                     }));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
                     iterXform.foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                         accum.add(i);
                         return accum;
                     }));

        assertEquals(Arrays.asList(2, 4, 6, 8),
                     listXform.filter(i -> i % 2 == 0)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(2, 4, 6, 8),
                     iterXform.filter(i -> i % 2 == 0)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));

        assertEquals(Arrays.asList(3, 5, 7, 9),
                     listXform.filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(3, 5, 7, 9),
                     iterXform.filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));

        assertEquals(Arrays.asList(3, 30, 300, 5, 50, 500, 7, 70, 700, 9, 90, 900),
                     listXform.filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(3, 30, 300, 5, 50, 500, 7, 70, 700, 9, 90, 900),
                     iterXform.filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));

        assertEquals(Arrays.asList(5, 50, 500, 7, 70, 700, 9, 90, 900),
                     listXform.drop(2)
                              .filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(5, 50, 500, 7, 70, 700, 9, 90, 900),
                     iterXform.drop(2)
                              .filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));

//        System.out.println("Testing separate drop.");
        assertEquals(Arrays.asList(7, 9),
                     listXform.filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .drop(2)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(7, 9),
                     iterXform.filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .drop(2)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
//        System.out.println("Done testing separate drop.");

        assertEquals(Arrays.asList(7, 70, 700, 9, 90, 900),
                     listXform.filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .drop(2)
                              .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(7, 70, 700, 9, 90, 900),
                     iterXform.filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .drop(2)
                              .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));

        assertEquals(Arrays.asList(500, 7, 70, 700, 9, 90, 900),
                     listXform.filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                              .drop(5)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));
        assertEquals(Arrays.asList(500, 7, 70, 700, 9, 90, 900),
                     iterXform.filter(i -> i % 2 == 0)
                              .map(i -> i + 1)
                              .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                              .drop(5)
                              .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                                  accum.add(i);
                                  return accum;
                              }));

    }

}