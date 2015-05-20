package org.organicdesign.fp.experiments;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.organicdesign.fp.Option;

public class IteratorWrapperThoughts<T> {
    private final Iterator<T> iter;
    IteratorWrapperThoughts(Iterable<T> i) {
        iter = i.iterator();
    }

    public synchronized Option<T> nextBlocking() {
        return iter.hasNext() ? Option.of(iter.next())
                              : Option.none();
    }

    public Option<T> nextNonBlocking() {
        try {
             return Option.of(iter.next());
        } catch (NoSuchElementException nsee) {
            if(iter.hasNext()) {
                // Oops, exception was thrown for reason other than end-of-iterator.
                // Rethrow that exception.
                throw nsee;
            }
        }
        return Option.none();
    }
}
