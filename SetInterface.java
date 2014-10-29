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
import java.util.HashSet;
import java.util.Set;

public class SetInterface {
    static interface Foo {
        int phooey();
    }
    static class Aye implements Foo {
        private final int phooey;
        public Aye(int p) { phooey = p; }
        @Override public int phooey() { return phooey; }
        @Override public int hashCode() { return phooey; }
        @Override public boolean equals(Object other) {
            if (this == other) { return true; }
            if ( (other == null) ||
                 !(other instanceof Aye) || // Here's the culprit.
                 (this.hashCode() != other.hashCode()) ) {
                return false;
            }
            final Aye that = (Aye) other;
            return (this.phooey == that.phooey);
        }
    }

    static class Boo implements Foo {
        private final int phooey;
        public Boo(int p) { phooey = p; }
        @Override public int phooey() { return phooey; }
        @Override public int hashCode() { return phooey; }
        @Override public boolean equals(Object other) {
            if (this == other) { return true; }
            if ( (other == null) ||
                 !(other instanceof Boo) || // Here's the culprit.
                 (this.hashCode() != other.hashCode()) ) {
                return false;
            }
            final Boo that = (Boo) other;
            return (this.phooey == that.phooey);
        }
    }

    public static void main(String[] args) {
        Set<Foo> fooset = new HashSet<>();
        fooset.add(new Aye(1));
        fooset.add(new Boo(1));
        for (Foo foo : fooset) {
            System.out.println(foo.phooey());
        }
        // Prints:
        // 1
        // 1
        // Showing that there are two items which are equivalent "Foo"s
        // but their equals method prevents them from being equal.
        // Really, a set like this should use compareTo(a,b) and consider 0 to mean equals.
    }   
}
