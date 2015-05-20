package org.organicdesign.fp.experiments;

public class IteratorWrapperThoughtsTest {
//    // Time ImVectorImplementation vs. java.util.ArrayList to prove that performance does not degrade
//    // if changes are made.
//    @Test
//    public void speedTest() throws NoSuchAlgorithmException, InterruptedException {
//        final int maxItems = 1000000000;
//
//        System.out.println("Speed tests take time.  The more accurate, the more time.\n" +
//                                   "This may fail occasionally, then work when re-run, which is OK.\n" +
//                                   "Better that, than set the limit too high and miss a performance drop.");
//
//        // These are worst-case timings, indexed by number of items inserted in the test.
//        Map<Integer,Double> benchmarkRatios = unMap(
//                1, 1.4,
//                10, 2.7,
//                100, 6.5,
//                1000, 9.0,
//                10000, 18.0,
//                100000, 13.9,
//                1000000, 7.7);
//
//        // Remember the results of each insertion test to average them later.
//        List<Double> ratios = new ArrayList<>();
//
//        // Run tests for increasingly more inserts each time (powers of 10 should be fair since underlying
//        // implementations use powers of 2).
//        for (int numItems = 1; numItems <= maxItems; numItems *= 10) {
//
//            // Run the speed tests this many times (for better accuracy) testing ArrayList and ImVectorImpl alternately.
//            int testRepetitions = (numItems < 1000) ? 10000 :
//                                  (numItems < 10000) ? 1000 :
//                                  (numItems < 100000) ? 100 : 10;
//
//            long[] testTimes = new long[testRepetitions];
//            long[] benchTimes = new long[testRepetitions];
//            Long[] testLoad = new Long[testRepetitions];
//
//            for (int i = 0; i < testRepetitions; i++) {
//                testLoad[i] = Long.valueOf(i);
//            }
//            Iterable<Long> testIterable = Arrays.asList(testLoad);
//
//            IteratorWrapperThoughts<Long> iwt1 = new IteratorWrapperThoughts<>(testIterable);
////            IteratorWrapperThoughts<Long> iwt2 = new IteratorWrapperThoughts<>(testIterable);
//            Iterator<Long> testIterator = testIterable.iterator();
//
//            Thread.sleep(0); // GC and other processes, this is your chance.
//            Option<Long> o = null;
//            Long expected = 0L;
//            long startTime = 0;
//
//            for (int z = 0; z < testRepetitions; z++) {
//
//                Thread.sleep(0); // GC and other processes, this is your chance.
//                startTime = System.nanoTime();
//                expected = 0L;
//                while (testIterator.hasNext()) {
//                    Long item = testIterator.next();
//                    if (!expected.equals(item)) {
//                        throw new IllegalStateException("Expected: " + expected + " got: " + item);
//                    }
//                    expected++;
//                }
//                testTimes[z] = System.nanoTime() - startTime;
//
//                Thread.sleep(0); // GC and other processes, this is your chance.
//                startTime = System.nanoTime();
//                expected = 0L;
//                o = iwt1.nextBlocking();
//                while (o.isSome()) {
//                    if (!expected.equals(o.get())) {
//                        throw new IllegalStateException("Expected: " + expected + " got: " + o.get());
//                    }
//                    o = iwt1.nextBlocking();
//                    expected++;
//                }
//                benchTimes[z] = System.nanoTime() - startTime;
//
////                System.out.println("Blocking time: " + benchTimes[z]);
//
//
////                System.out.println("Non-Blocking time: " + testTimes[z]);
//            }
//
//            // We want the median time.  That discards all the unlucky (worst) and lucky (best) times.
//            // That makes it a fairer measurement for this than the mean time.
//            Arrays.sort(testTimes);
//            Arrays.sort(benchTimes);
//            long testTime = testTimes[testTimes.length / 2];
//            long benchTime = benchTimes[benchTimes.length / 2];
//
//            // Ratio of mean times of the tested collection vs. the benchmark.
//            double ratio = ((double) testTime) / ((double) benchTime);
//            System.out.println("Iterations: " + numItems + " non-blocking: " + testTime + " blocking: " + benchTime +
//                                       " test/benchmark: " + ratio);
//
//            // Verify that the median time is within established bounds for this test
////            assertTrue(ratio <= benchmarkRatios.get(numItems));
//
//            // Record these ratios to take an over-all mean later.
//            ratios.add(ratio);
//        }
//
//        // Compute mean ratio.
//        double sum = 0;
//        for (int i = 0; i < ratios.size(); i++) {
//            sum += ratios.get(i);
//        }
//        double meanRatio = sum / ratios.size();
//        System.out.println("meanRatio: " + meanRatio);
//
//        // Average-case timing over the range of number of inserts.
//        // This is typically 2.5, but max 3.8 for unit tests, max 5.3 for unitTests "with coverage" from IDEA.
//        // I think this means that PersistentVector performs worse with all the other work being done in the background
//        // than ArrayList does.
//        assertTrue(meanRatio < 10);
//    }
//
//    @Test
//    public void speedTest2() throws NoSuchAlgorithmException, InterruptedException {
//
//        // Remember the results of each insertion test to average them later.
//        List<Double> ratios = new ArrayList<>();
//
//        // Run the speed tests this many times (for better accuracy) testing ArrayList and ImVectorImpl alternately.
//        int testRepetitions = 10000000;
//
//        Long[] testLoad = new Long[testRepetitions];
//
//        for (int i = 0; i < testRepetitions; i++) {
//            testLoad[i] = Long.valueOf(i);
//        }
//        Iterable<Long> testIterable = Arrays.asList(testLoad);
//
//        IteratorWrapperThoughts<Long> iwt1 = new IteratorWrapperThoughts<>(testIterable);
////        IteratorWrapperThoughts<Long> iwt2 = new IteratorWrapperThoughts<>(testIterable);
////        Iterator<Long> testIterator = testIterable.iterator();
//
//        Mutable.IntRef numFoundBlocking = Mutable.IntRef.of(0);
//        Mutable.IntRef numFoundNonBlocking = Mutable.IntRef.of(0);
//
//        Thread tBlocking = new Thread(() -> {
//            Option<Long> o = iwt1.nextBlocking();
//            while (o.isSome()) {
//                numFoundBlocking.increment();
//                o = iwt1.nextBlocking();
//            }
//        });
//
//        Thread tNonBlocking = new Thread(() -> {
//            Option<Long> o = iwt1.nextNonBlocking();
//            while (o.isSome()) {
//                numFoundNonBlocking.increment();
//                o = iwt1.nextNonBlocking();
//            }
//        });
//
//        Thread.sleep(0); // GC and other processes, this is your chance.
//
//        tBlocking.start();
//        tNonBlocking.start();
//        tBlocking.join();
//        tNonBlocking.join();
//
//        // Ratio of mean times of the tested collection vs. the benchmark.
//        double ratio = ((double) numFoundNonBlocking.value()) / ((double) numFoundBlocking.value());
//        System.out.println("Iterations: " + testRepetitions + " non-blocking: " + numFoundNonBlocking.value() + " blocking: " + numFoundBlocking.value() +
//                           " test/benchmark: " + ratio);
//
//        // Verify that the median time is within established bounds for this test
////            assertTrue(ratio <= benchmarkRatios.get(numItems));
//
//        // Record these ratios to take an over-all mean later.
//        ratios.add(ratio);
//
//        // Compute mean ratio.
//        double sum = 0;
//        for (int i = 0; i < ratios.size(); i++) {
//            sum += ratios.get(i);
//        }
//        double meanRatio = sum / ratios.size();
//        System.out.println("meanRatio: " + meanRatio);
//
//        // Average-case timing over the range of number of inserts.
//        // This is typically 2.5, but max 3.8 for unit tests, max 5.3 for unitTests "with coverage" from IDEA.
//        // I think this means that PersistentVector performs worse with all the other work being done in the background
//        // than ArrayList does.
//        assertTrue(meanRatio < 10);
//    }
//
}
