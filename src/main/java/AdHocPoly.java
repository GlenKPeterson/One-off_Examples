// Notes on Erik Osheim's talk: http://plastic-idolatry.com/typcls/
// Particularly: http://plastic-idolatry.com/typcls/#howworks3
// Also inspired by Seth Tissue's talk: https://vimeo.com/39951109
public class AdHocPoly {

    // This would be the typeclass
    public interface Eq<T> {
        Boolean eq(T x, T y);
        default Boolean neq(T x, T y) { return !eq(x, y); }
    }

    // Below are three instances of the typeclass for various member types.  Notice that they don't
    // operate on things of type Eq, they operate on things of type T.
    //
    // In Java, it's more common to use static functions to process other Types of data.  But these
    // anonymous functions/classes need to implement the Eq interface so that we can pass them to
    // appropriate other functions.  That is why we are using instance methods instead.
    //
    // Each of these instances "glues" a member type to the typeclass interface.  Each one
    // implements the typeclass in terms of a member type.  Neither the typeclass interface (Eq),
    // nor the member types (Integer, Float, so far...) know  about these instances.
    static final Eq<Integer> intEq = (x, y) -> x.intValue() == y.intValue();
    static final Eq<Integer> intEqMod3 = (x, y) -> (x % 3) == (y % 3);
    static final Eq<Float> floatEq = (a, b) -> a.floatValue() == b.floatValue();

    // Let's make a little extendability matrix:
    //
    //  Data    ||    Functionality
    //  Type    || equality | eq Mod 3  |
    //  ========++==========+===========+
    //  Integer || intEq    | intEqMod3 |
    //  --------++----------+-----------+
    //  Float   || floatEq  |           |
    //  --------++----------+-----------+
    //
    // This is the old "expression problem," of how to write code so you can add new functionality
    // to old data, or add new data types to old functionality.  There is a blank square in the
    // above chart because modulo is an integer operation that makes no sense on a float.  But we
    // can easily add any box, or any new row or column, with appropriate implementations using the
    // above techniques.
    //
    // It may help to think of the implementations of Eq<T> as different contexts for working with
    // the Eq interface on different types of objects.

    public static void main(String[] args) {

        // Java has no syntactic sugar to make this look like anything other than what it is:
        // a separate class that handles specific operations for other classes.
        System.out.println("intEq.eq(3, 81): " + intEq.eq(3, 81));

        // This should be true.  This extends the original eq with new functionality
        System.out.println("intEqMod3.eq(3, 81): " + intEqMod3.eq(3, 81));

        // This extends the original eq to new data types.
        System.out.println("floatEq.neq(3.0f, 5.0f): " + floatEq.neq(3.0f, 5.0f));

        // And this is how you might use a type class (or similar - Equator defines 2 methods:
        // equals() and hashCode()):
        // https://github.com/GlenKPeterson/UncleJim/blob/master/src/main/java/org/organicdesign/fp/collections/PersistentHashMap.java#L83
        //
        // Here's Equator
        // https://github.com/GlenKPeterson/UncleJim/blob/master/src/main/java/org/organicdesign/fp/collections/Equator.java
    }
}
