/*
Copyright 2014 Glen Peterson

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SetInterface {
    static interface Foo {
        int phooey();

        public static final Comparator<Foo> COMPARATOR = (left, right) -> {
            if (left == right) { return 0; }
            if (left == null) { throw new IllegalArgumentException("Can't compare a null to a value"); }
            return left.phooey() - right.phooey(); // Not worrying about overflow right now...
        };
    }
    static class Aye implements Foo, Comparable<Foo> {
        private final int phooey;
        public Aye(int p) { phooey = p; }
        @Override public int phooey() { return phooey; }
        @Override public int hashCode() { return phooey; }
        @Override public boolean equals(Object other) {
            if (this == other) { return true; }
            if ( (other == null) ||
                 !(other instanceof Aye) || // Generally correct...
                 (this.hashCode() != other.hashCode()) ) {
                return false;
            }
            final Aye that = (Aye) other;
            return (this.phooey == that.phooey);
        }
        @Override public int compareTo(Foo that) { return Foo.COMPARATOR.compare(this, that); }
    }

    static class Boo implements Foo, Comparable<Foo> {
        private final int phooey;
        public Boo(int p) { phooey = p; }
        @Override public int phooey() { return phooey; }
        @Override public int hashCode() { return phooey; }
        @Override public boolean equals(Object other) {
            if (this == other) { return true; }
            if ( (other == null) ||
                 !(other instanceof Boo) || // Generally correct...
                 (this.hashCode() != other.hashCode()) ) {
                return false;
            }
            final Boo that = (Boo) other;
            return (this.phooey == that.phooey);
        }
        @Override public int compareTo(Foo that) { return Foo.COMPARATOR.compare(this, that); }
    }

    static void doTest(String msg, Set<Foo> fooset) {
        System.out.println(msg);
        fooset.add(new Aye(1));
        fooset.add(new Boo(1));
        for (Foo foo : fooset) {
            System.out.println(foo.phooey());
        }
    }

    public static void main(String[] args) {
        doTest("HashSet", new HashSet<>());
        doTest("TreeSet natural ordering", new TreeSet<>());
        doTest("TreeSet with comparator", new TreeSet<>(Foo.COMPARATOR));
        // Prints out:
        // HashSet
        // 1
        // 1
        // TreeSet natural ordering
        // 1
        // TreeSet with comparator
        // 1
    }   
}
