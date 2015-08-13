// Copyright 2015-05-18 PlanBase Inc. & Glen Peterson
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.organicdesign.fp;

import org.organicdesign.fp.collections.UnmodMap;

import java.util.Comparator;

/**
 Based on "Purely Functional Data Structures" by Chris Okasaki, p. 24-28.  Mistakes are my own.
 This has never been tested AT ALL, or used in any way.  I thought I would use this for a Vector implementation,
 based on RRB-trees, but that turned out to be a very slightly non-standard B-Tree, and not very much like a Red Black
 Tree at all.
 */
public abstract class OkasakiRedBlackTree<K,V> implements UnmodMap.UnEntry<K,V> {
    final OkasakiRedBlackTree<K,V> left;
    final K key;
    final V value;
    final OkasakiRedBlackTree<K,V> right;

    OkasakiRedBlackTree(OkasakiRedBlackTree<K,V> l, K k, V v, OkasakiRedBlackTree<K,V> r) {
        left = l; key = k; value = v; right = r;
    }
    public abstract boolean isRed();
    @Override public K getKey() { return key; }
    @Override public V getValue() { return value; }

    private static class Red<K,V> extends OkasakiRedBlackTree<K,V> {
        Red(OkasakiRedBlackTree<K,V> l, K k, V v, OkasakiRedBlackTree<K,V> r) {
            super(l, k, v, r);
        }
        @Override public boolean isRed() { return true; }
        public String toString() { return "Red(" + left + "," + key + "," + right + ")"; }
    }
    private static class Black<K,V> extends OkasakiRedBlackTree<K,V> {

        Black(OkasakiRedBlackTree<K,V> l, K k, V v, OkasakiRedBlackTree<K,V> r) {
            super(l, k, v, r);
        }
        @Override public boolean isRed() { return false; }
        public String toString() { return "Black(" + left + "," + key + "," + right + ")"; }
    }

    private static <K,V> boolean member(Comparator<? super K> comp, UnmodMap.UnEntry<K,V> elem,
                                        OkasakiRedBlackTree<K,V> node) {
        if (node == null) { return false; }
        int c = comp.compare(elem.getKey(), node.key);
        return (c < 0) ? member(comp, elem, node.left) :
               (c > 0) ? member(comp, elem, node.right) :
               true;
    }

    // Okasaki p. 26:
    // The balance function detects and repairs each red-red violation when it processes the black parent of a red node
    // with a red child.  This black-red-red path can occur in any of four configurations, depending on whether each red
    // node is a left or right child.  However, the solution is the same in every case: rewrite the black-red-red path
    // as a red node with two black children.
    private static <K,V> OkasakiRedBlackTree<K,V> balance(OkasakiRedBlackTree<K,V> node) {
        if (node.isRed()) { return node; }

        OkasakiRedBlackTree<K,V> left = node.left;
        if (left.isRed()) {
            // First case: Left child is red and Left-Left grandchild is red:
            //               Black node
            //              /  Left child is red.                                  Return a red node
            //             /  /      Left-Left grandchild is red                  /   Left-Left grandchild blackened
            //            /  /      /                   Element                  /   /             Left child key
            //           /  /      /                   /   Some right-child     /   /             /       Left-Right grandchild
            //          /  /      /                   /  /                     /   /             /       /
            // balance(B, T(R, T(R, a, x, b), y, c), z, d)             =    T(R, T(B, a, x, b), y, T(B, c, z, d))
            //
            // Next case: Left child is red and Left-Right grandchild is red
            //               Black node
            //              /  Left child is red.                                  Return a red node
            //             /  /           Left-Right grandchild is red            /       Left-Left grandchild
            //            /  /           /              Element                  /       /  Left child key
            //           /  /           /              /   Some right-child     /       /  /  Left-Right-Left
            //          /  /           /              /  /                     /       /  /  /   Left-Right key
            // balance(B, T(R, a, x, T(R, b, y, c)), z, d)             =    T(R, T(B, a, x, b), y, T(B, c, z, d))
            if (left.left.isRed()) {
                return new Red<>(new Black<>(left.left.left, left.left.key, left.left.value, left.left.right),
                                     left.key, left.value,
                                     new Black<>(left.right, node.key, node.value, node.right));
            } else if (left.right.isRed()) {
                return new Red<>(new Black<>(left.left, left.key, left.value, left.right.left),
                                     left.right.key, left.right.value,
                                     new Black<>(left.right.right, node.key, node.value, node.right));
            }
            return node;
        }
            //
            // Next case: Right child is red and Right-Left grandchild is red
            // Next case: Right child is red and Right-Right grandchild is red
            // Any other configuration: return unchanged.
        OkasakiRedBlackTree<K,V> right = node.right;
        if (right.isRed()) {
            if (right.left.isRed()) {
                return new Red<>(new Black<>(left, node.key, node.value, right.left.left),
                                     right.left.key, right.left.value,
                                     new Black<>(right.left.right, right.key, right.value, right.right));
            } else if (right.right.isRed()) {
                return new Red<>(new Black<>(left, node.key, node.value, right.left),
                                     right.key, right.value,
                                     new Black<>(right.right.left, right.right.key, right.right.value, right.right.right));
            }
        }
        return node;
    }

    private static <K,V> OkasakiRedBlackTree<K,V> insert(Comparator<? super K> comp, UnmodMap.UnEntry<K,V> elem,
                                                         OkasakiRedBlackTree<K,V> node) {
        if (node == null) {
            return new Black<>(null, elem.getKey(), elem.getValue(), null); // Should this be Red?
        }
        int c = comp.compare(elem.getKey(), node.key);
        return (c < 0) ? balance(insert(comp, elem, node.left)) : // Let insert determine node color.
               (c > 0) ? balance(insert(comp, elem, node.right)) :
               node; // Node is already here.
    }

}
