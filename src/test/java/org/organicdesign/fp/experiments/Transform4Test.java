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
        Transform4<Integer> t = Transform4.fromList(Arrays.asList(1, 2, 3));

        assertEquals(Arrays.asList(1,2,3),
                     t.foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                         accum.add(i);
                         return accum;
                     }));

        assertEquals(Arrays.asList(2,3,4),
                     t.map(i -> i + 1)
                      .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                          accum.add(i);
                          return accum;
                      }));

        assertEquals(Arrays.asList(1, 3),
                     t.filter(i -> i != 2)
                      .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                          accum.add(i);
                          return accum;
                      }));

        assertEquals(Arrays.asList(1, 10, 100, 2, 20, 200, 3, 30, 300),
                     t.flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                      .foldLeft(new ArrayList<>(), (accum, i) -> {
                          accum.add(i);
                          return accum;
                      }));

        assertEquals(Arrays.asList(2,3),
                     t.drop(1)
                      .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                          accum.add(i);
                          return accum;
                      }));
    }

    @Test public void longerCombinations() throws Exception {
        Transform4<Integer> t = Transform4.fromList(Arrays.asList(1,2,3,4,5,6,7,8,9));

        assertEquals(Arrays.asList(1,2,3,4,5,6,7,8,9),
                     t.foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                         accum.add(i);
                         return accum;
                     }));

        assertEquals(Arrays.asList(2,4,6,8),
                     t.filter(i -> i % 2 == 0)
                      .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                          accum.add(i);
                          return accum;
                      }));

        assertEquals(Arrays.asList(3,5,7,9),
                     t.filter(i -> i % 2 == 0)
                      .map(i -> i + 1)
                      .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                          accum.add(i);
                          return accum;
                      }));

        assertEquals(Arrays.asList(3,30,300, 5,50,500, 7,70,700, 9,90,900),
                     t.filter(i -> i % 2 == 0)
                      .map(i -> i + 1)
                      .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                      .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                          accum.add(i);
                          return accum;
                      }));

        assertEquals(Arrays.asList(5,50,500, 7,70,700, 9,90,900),
                     t.drop(2)
                      .filter(i -> i % 2 == 0)
                      .map(i -> i + 1)
                      .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                      .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                          accum.add(i);
                          return accum;
                      }));

//        System.out.println("Testing separate drop.");
        assertEquals(Arrays.asList(7, 9),
                     t.filter(i -> i % 2 == 0)
                      .map(i -> i + 1)
                      .drop(2)
                      .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                          accum.add(i);
                          return accum;
                      }));
//        System.out.println("Done testing separate drop.");

        assertEquals(Arrays.asList(7,70,700, 9,90,900),
                     t.filter(i -> i % 2 == 0)
                      .map(i -> i + 1)
                      .drop(2)
                      .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                      .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                          accum.add(i);
                          return accum;
                      }));

        assertEquals(Arrays.asList(500, 7, 70, 700, 9, 90, 900),
                     t.filter(i -> i % 2 == 0)
                      .map(i -> i + 1)
                      .flatMap(i -> PersistentVector.of(i, i * 10, i * 100))
                      .drop(5)
                      .foldLeft(new ArrayList<>(), (List<Integer> accum, Integer i) -> {
                          accum.add(i);
                          return accum;
                      }));

    }

}