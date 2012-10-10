/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * ChainedCollection is a utility collection that allows you to chain 1+
 * collections together of a common type to act as a single, ordered collection.
 * It is a view over its children collections and updates to them are reflected
 * in it. It's iterator supports removals only if the underlying collections'
 * iterators support removals. Similarly, it is fail-fast only if it's
 * underlying collections are fail-fast.
 * 
 * @author Michael Ludwig.
 * @param <T> The common type of a ChainedCollection's collections
 */
public class ChainedCollection<T> extends AbstractCollection<T> {
    private final Collection<? extends T>[] chain;

    /**
     * Create a ChainedCollection chaining <tt>c1</tt> and <tt>c2</tt>. The
     * elements in c1 will be before the elements in c2.
     * 
     * @param c1 The first child in the chain
     * @param c2 The second child in the chain
     * @throws NullPointerException if c1 or c2 are null
     */
    @SuppressWarnings("unchecked")
    public ChainedCollection(Collection<? extends T> c1, Collection<? extends T> c2) {
        if (c1 == null || c2 == null) {
            throw new NullPointerException("Collections cannot be null");
        }
        chain = new Collection[] {c1, c2};
    }

    /**
     * Create a ChainedCollection that chains together all collections currently
     * within <tt>toChain</tt>. The collections are ordered as they are returned
     * from <tt>toChain</tt>. This ordering will be consistent across the
     * ChainedCollection's iterators, although within-child iterators are not
     * guaranteed.
     * 
     * @param toChain The collection of collections to chain
     * @throws NullPointerException if toChain or any of its elements are null
     */
    @SuppressWarnings("unchecked")
    public ChainedCollection(Collection<Collection<? extends T>> toChain) {
        if (toChain == null) {
            throw new NullPointerException("Collections cannot be null");
        }

        chain = new Collection[toChain.size()];
        int i = 0;
        for (Collection<? extends T> c : toChain) {
            if (c == null) {
                throw new NullPointerException("Collections cannot be null");
            }
            chain[i++] = c;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new ChainIterator();
    }

    @Override
    public int size() {
        int size = 0;
        for (int i = 0; i < chain.length; i++) {
            size += chain[i].size();
        }
        return size;
    }

    private class ChainIterator implements Iterator<T> {
        private int index;
        private Iterator<? extends T> current;

        public ChainIterator() {
            index = 0;
            current = chain[0].iterator();
        }

        @Override
        public boolean hasNext() {
            if (current.hasNext()) {
                return true;
            }
            advance();
            return current.hasNext(); // rely on current to report correctly
        }

        @Override
        public T next() {
            if (current.hasNext()) {
                return current.next();
            }
            advance();
            return current.next(); // rely on current to throw NoSuchElementException()
        }

        private void advance() {
            while (!current.hasNext()) {
                if ((++index) < chain.length) {
                    current = chain[index].iterator();
                } else {
                    break;
                }
            }
        }

        @Override
        public void remove() {
            // remove element from underlying collection
            // - rely on it to correctly throw exceptions depending
            // - on its hasNext()/next() state
            current.remove();
        }
    }
}
