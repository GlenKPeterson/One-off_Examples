/*
Copyright 2015 Glen K. Peterson

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
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
Sometimes you can find a decidable slice of an undecidable problem domain
https://twitter.com/djspiewak/status/555479647303892992

Function Equality is not decidable in general, but it is for
constant functions and identity functions.
https://twitter.com/djspiewak/status/555480203657375744

Regarding comparing identity functions see here:
https://github.com/GlenKPeterson/fp4java7/blob/master/src/main/java/org/organicdesign/fp/FunctionUtils.java#L102

Below are two examples of comparing constant functions.  At least one
of these examples works for an un-computable codomain.
*/
public class FunctionComparison {

    // Java 8 Functional Interface (using apply())
    // A constant function returns the same value no matter what it is passed.
    // This assumes that constant functions are meaningfully equal if the
    // constant return value is equal.
    // Maybe this would be clearer as a 0-argument "thunk" but I made 
    // this a one-argument function and it works.  Good enough for now...
    public static interface ConstantFunction<T,U> {
        // This is a way to get at the constant return value so
        // we can implement equals
        U constantValue();

        // The functional method
        default U apply(T t) { return constantValue(); }
    }

    // First example implementing class uses equals() for comparason
    public static class ConstFuncEqMeth<T,U> implements ConstantFunction<T,U> {

        // instance variable holding the constant return
        private final U u;

        // public constructors are poor style, but this is a simple example.
        // This is like Daniel's: a -> (b -> c)
        // Only it's actually c -> (b -> c)
        public ConstFuncEqMeth(U au) { u = au; }

        @Override public U constantValue() { return u; }

        @Override public int hashCode() { return u.hashCode(); }

        @Override public boolean equals(Object other) {
            if (this == other) { return true; }
            if ( (other == null) ||
                 !(other instanceof ConstFuncEqMeth) ||
                 (this.hashCode() != other.hashCode()) ) {
                return false;
            }
            ConstFuncEqMeth<?,?> that = (ConstFuncEqMeth<?,?>) other;
            return this.constantValue().equals(that.constantValue());
        }
    }

    // Second example implementing class uses controlled instances and
    // == for comparason
    public static class ConstFuncEqOp<T,U> implements ConstantFunction<T,U> {

        // Cache of all instances (can fill up memory eventually).
        // This may effectively be the codomain, but I'm considering it the
        // range of this function (*cough* functional object).
        private static final Map<Object,Object> instances = new HashMap<>();

        // instance variable holding the constant return
        private final U u;

        // private constructor
        private ConstFuncEqOp(U au) { u = au; }

        // public factory method
        // This is like Daniel's: a -> (b -> c)
        // Only it's effectively c -> (b -> c)
        @SuppressWarnings("unchecked")
        public static synchronized <A,B> ConstFuncEqOp<A,B> of(B b) {
            ConstFuncEqOp<A,B> ret = (ConstFuncEqOp<A,B>) instances.get(b);
            if (ret == null) {
                ret = new ConstFuncEqOp<A,B>(b);
                instances.put(b, ret);
            }
            return ret;
        }

        @Override public U constantValue() { return u; }
    }

    // Convenience method saves me some typing and you some reading.
    static void println(Object o) { System.out.println(o); }

    // Main
    // "The proof of the pudding is in the eating"
    public static void main(String... args) {
        println("Comparing different constant functions on a non computable " +
                "codomain using equals()");
        println(new ConstFuncEqMeth<>(BigInteger.valueOf(2938472394872398423L))
                       .equals(new ConstFuncEqMeth<>(BigInteger.valueOf(33))));

        println("Comparing 'equal' constant functions on a non computable " +
                "codomain using equals()");
        println(new ConstFuncEqMeth<>(BigInteger.valueOf(33))
                       .equals(new ConstFuncEqMeth<>(BigInteger.valueOf(33))));

        println("");
        println("Maybe you consider the instances a computable codomain, " +
                "but if we consider it the range...");
        println("Comparing different constant functions on a non computable " +
                "codomain using ==");
        println(ConstFuncEqOp.of(BigInteger.valueOf(2938472394872398423L)) ==
                ConstFuncEqOp.of(BigInteger.valueOf(33)));

        println("Comparing 'equal' constant functions on a non computable " +
                "codomain using ==");
        println(ConstFuncEqOp.of(BigInteger.valueOf(33)) ==
                ConstFuncEqOp.of(BigInteger.valueOf(33)));
    }
}
