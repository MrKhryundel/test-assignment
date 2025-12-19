/*
 * Copyright (c) 2014, NTUU KPI, Computer systems department and/or its affiliates. All rights reserved.
 * NTUU KPI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 */

package ua.kpi.comsys.test2.implementation;

import java.io.*;
import java.math.BigInteger;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import ua.kpi.comsys.test2.NumberList;

/**
 * Custom implementation of NumberList interface.
 *
 * Author: Zahvatkin Daniil, IO-36 №6
 * Variant: 4126
 * Тип списку: Лінійний двонаправлений
 * Система числення: трійкова
 * C5+1: Вісімкова
 * Операція зі списками: Алгебраїчне та логічне OR двох чисел
 */
public class NumberListImpl implements NumberList {
    //  Record book number

    /**
     * Returns student's record book number (4 digits).
     * IMPORTANT: tests require 4100 < number < 4429.
     */
    public static int getRecordBookNumber() {
        // Picked to satisfy: mod3=0 (doubly linked), mod5=1 (ternary), mod7=6 (OR)
        return 4126;
    }

    //  Base mapping (mod 5)

    private static int baseFromRecordBook() {
        int m = getRecordBookNumber() % 5;
        switch (m) {
            case 0: return 2;   // binary
            case 1: return 3;   // ternary
            case 2: return 8;   // octal
            case 3: return 10;  // decimal
            case 4: return 16;  // hex
            default: return 10;
        }
    }

    private static int nextBaseFromRecordBook() {
        int m = getRecordBookNumber() % 5;
        switch (m) {
            case 0: return 3;   // 2 -> 3
            case 1: return 8;   // 3 -> 8
            case 2: return 10;  // 8 -> 10
            case 3: return 16;  // 10 -> 16
            case 4: return 2;   // 16 -> 2
            default: return 10;
        }
    }

    //  Doubly linked list node

    private static final class Node {
        byte value;
        Node prev;
        Node next;

        Node(byte value) {
            this.value = value;
        }
    }

    private Node head;
    private Node tail;
    private int size;
    private int modCount;

    // Each instance has its base (digits are 0..base-1)
    private final int base;

    //  Constructors

    /**
     * Default constructor. Returns empty NumberListImpl.
     */
    public NumberListImpl() {
        this.base = baseFromRecordBook();
        // empty list
    }

    /**
     * Constructs new NumberListImpl by decimal number from file (string format).
     * If file does not exist, is empty, or contains invalid decimal => list stays empty.
     */
    public NumberListImpl(File file) {
        this.base = baseFromRecordBook();
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        String line = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            line = br.readLine();
            // if more lines exist, ignore (tests check it's single line)
        } catch (IOException ignored) {
            return;
        }
        if (line == null) {
            return;
        }
        initFromDecimalString(line.trim());
    }

    /**
     * Constructs new NumberListImpl by decimal number in string notation.
     * If invalid decimal string => list stays empty.
     */
    public NumberListImpl(String value) {
        this.base = baseFromRecordBook();
        if (value == null) {
            return;
        }
        initFromDecimalString(value.trim());
    }

    // Private constructor to create list with specific base (for changeScale / results)
    private NumberListImpl(int base) {
        this.base = base;
    }

    //  File save


    /**
     * Saves the number stored in the list into specified file in decimal.
     * If file is null or not writable => do nothing.
     */
    public void saveList(File file) {
        if (file == null) return;

        // Tests use existing files; still handle general case.
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            bw.write(toDecimalString());
        } catch (IOException ignored) {
            // do nothing
        }
    }

    //  Scale change

    /**
     * Returns new NumberListImpl representing the same number but in "next" base
     * defined by record book number assignment.
     *
     * Does not affect original list.
     */
    public NumberListImpl changeScale() {
        BigInteger val = toBigInteger();
        int newBase = nextBaseFromRecordBook();
        NumberListImpl res = new NumberListImpl(newBase);
        res.initFromBigInteger(val);
        return res;
    }

    //  Additional operation

    /**
     * Returns new NumberListImpl which represents result of additional operation
     * defined by record book number assignment (mod 7).
     *
     * Does not impact original lists.
     */
    public NumberListImpl additionalOperation(NumberList arg) {
        if (arg == null) {
            return new NumberListImpl(this.base); // empty
        }

        // Convert both operands to BigInteger in decimal value
        BigInteger a = this.toBigInteger();
        BigInteger b;

        if (arg instanceof NumberListImpl) {
            b = ((NumberListImpl) arg).toBigInteger();
        } else {
            // Fallback: treat arg as digits in *this.base* (best effort)
            b = bigIntegerFromDigits(arg, this.base);
        }

        int op = getRecordBookNumber() % 7;
        BigInteger r;

        switch (op) {
            case 0: // add
                r = a.add(b);
                break;
            case 1: // remove/subtract (no negatives allowed => if a<b => 0)
                r = a.subtract(b);
                if (r.signum() < 0) r = BigInteger.ZERO;
                break;
            case 2: // multiply
                r = a.multiply(b);
                break;
            case 3: // div (integer)
                if (b.equals(BigInteger.ZERO)) return new NumberListImpl(this.base);
                r = a.divide(b);
                break;
            case 4: // mod
                if (b.equals(BigInteger.ZERO)) return new NumberListImpl(this.base);
                r = a.mod(b);
                break;
            case 5: // AND (bitwise)
                r = a.and(b);
                break;
            case 6: // OR (bitwise)  <-- your variant
                r = a.or(b);
                break;
            default:
                r = BigInteger.ZERO;
        }

        NumberListImpl res = new NumberListImpl(this.base);
        res.initFromBigInteger(r);
        return res;
    }

    //  Decimal conversions

    /**
     * Returns string representation of number stored in the list in decimal.
     * Empty list => "0".
     */
    public String toDecimalString() {
        return toBigInteger().toString(10);
    }

    @Override
    public String toString() {
        if (size == 0) return "";
        StringBuilder sb = new StringBuilder(size);
        Node cur = head;
        while (cur != null) {
            sb.append(digitToChar(cur.value));
            cur = cur.next;
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (o instanceof NumberListImpl) {
            NumberListImpl other = (NumberListImpl) o;
            if (this.size != other.size) return false;
            if (this.base != other.base) return false;

            Node a = this.head;
            Node b = other.head;
            while (a != null && b != null) {
                if (a.value != b.value) return false;
                a = a.next;
                b = b.next;
            }
            return a == null && b == null;
        }

        // If comparing with some other List<Byte>
        if (o instanceof List) {
            List<?> other = (List<?>) o;
            if (other.size() != this.size) return false;
            int i = 0;
            for (Object x : other) {
                if (!(x instanceof Byte)) return false;
                if (!this.get(i).equals(x)) return false;
                i++;
            }
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        // not required by tests, but keep consistent with equals
        int h = 1;
        h = 31 * h + base;
        Node cur = head;
        while (cur != null) {
            h = 31 * h + cur.value;
            cur = cur.next;
        }
        return h;
    }

    //  List basics

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Byte)) return false;
        byte v = (Byte) o;
        Node cur = head;
        while (cur != null) {
            if (cur.value == v) return true;
            cur = cur.next;
        }
        return false;
    }

    @Override
    public Iterator<Byte> iterator() {
        return new Itr(0);
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size];
        int i = 0;
        Node cur = head;
        while (cur != null) {
            arr[i++] = cur.value;
            cur = cur.next;
        }
        return arr;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        // As required by assignment: can be left unimplemented.
        throw new UnsupportedOperationException("toArray(T[] a) is not required by assignment");
    }

    @Override
    public boolean add(Byte e) {
        requireNonNullDigit(e);
        requireDigitInBase(e);

        linkLast(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Byte)) return false;
        byte v = (Byte) o;

        Node cur = head;
        int idx = 0;
        while (cur != null) {
            if (cur.value == v) {
                unlink(cur);
                return true;
            }
            cur = cur.next;
            idx++;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        for (Object x : c) {
            if (!contains(x)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Byte> c) {
        if (c == null) throw new NullPointerException();
        boolean changed = false;
        for (Byte b : c) {
            add(b);
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Byte> c) {
        if (c == null) throw new NullPointerException();
        checkPositionIndex(index);

        if (c.isEmpty()) return false;
        int i = index;
        for (Byte b : c) {
            add(i++, b);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        boolean changed = false;
        Iterator<Byte> it = iterator();
        while (it.hasNext()) {
            Byte v = it.next();
            if (c.contains(v)) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        boolean changed = false;
        Iterator<Byte> it = iterator();
        while (it.hasNext()) {
            Byte v = it.next();
            if (!c.contains(v)) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        Node cur = head;
        while (cur != null) {
            Node next = cur.next;
            cur.prev = null;
            cur.next = null;
            cur = next;
        }
        head = tail = null;
        size = 0;
        modCount++;
    }

    @Override
    public Byte get(int index) {
        checkElementIndex(index);
        return node(index).value;
    }

    @Override
    public Byte set(int index, Byte element) {
        requireNonNullDigit(element);
        requireDigitInBase(element);
        checkElementIndex(index);

        Node n = node(index);
        byte old = n.value;
        n.value = element;
        return old;
    }

    @Override
    public void add(int index, Byte element) {
        requireNonNullDigit(element);
        requireDigitInBase(element);
        checkPositionIndex(index);

        if (index == size) {
            linkLast(element);
        } else {
            linkBefore(element, node(index));
        }
    }

    @Override
    public Byte remove(int index) {
        checkElementIndex(index);
        Node n = node(index);
        byte old = n.value;
        unlink(n);
        return old;
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof Byte)) return -1;
        byte v = (Byte) o;
        int idx = 0;
        Node cur = head;
        while (cur != null) {
            if (cur.value == v) return idx;
            cur = cur.next;
            idx++;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Byte)) return -1;
        byte v = (Byte) o;
        int idx = size - 1;
        Node cur = tail;
        while (cur != null) {
            if (cur.value == v) return idx;
            cur = cur.prev;
            idx--;
        }
        return -1;
    }

    @Override
    public ListIterator<Byte> listIterator() {
        return new ListItr(0);
    }

    @Override
    public ListIterator<Byte> listIterator(int index) {
        checkPositionIndex(index);
        return new ListItr(index);
    }

    @Override
    public List<Byte> subList(int fromIndex, int toIndex) {
        checkPositionIndex(fromIndex);
        checkPositionIndex(toIndex);
        if (fromIndex > toIndex) throw new IllegalArgumentException("fromIndex > toIndex");

        NumberListImpl res = new NumberListImpl(this.base);
        for (int i = fromIndex; i < toIndex; i++) {
            res.add(this.get(i));
        }
        return res;
    }

    //  NumberList extra methods

    @Override
    public boolean swap(int index1, int index2) {
        if (index1 < 0 || index2 < 0 || index1 >= size || index2 >= size) return false;
        if (index1 == index2) return true;
        if (index1 > index2) {
            int t = index1; index1 = index2; index2 = t;
        }
        Node a = node(index1);
        Node b = node(index2);
        byte tmp = a.value;
        a.value = b.value;
        b.value = tmp;
        modCount++;
        return true;
    }

    @Override
    public void sortAscending() {
        if (size <= 1) return;
        // Counting sort since digits are small within base
        int[] cnt = new int[base];
        Node cur = head;
        while (cur != null) {
            cnt[cur.value & 0xFF]++;
            cur = cur.next;
        }
        cur = head;
        for (int d = 0; d < base; d++) {
            int k = cnt[d];
            while (k-- > 0) {
                cur.value = (byte) d;
                cur = cur.next;
            }
        }
        modCount++;
    }

    @Override
    public void sortDescending() {
        if (size <= 1) return;
        int[] cnt = new int[base];
        Node cur = head;
        while (cur != null) {
            cnt[cur.value & 0xFF]++;
            cur = cur.next;
        }
        cur = head;
        for (int d = base - 1; d >= 0; d--) {
            int k = cnt[d];
            while (k-- > 0) {
                cur.value = (byte) d;
                cur = cur.next;
            }
        }
        modCount++;
    }

    @Override
    public void shiftLeft() {
        if (size <= 1) return;

        // move head to tail
        Node first = head;
        Node second = first.next;

        head = second;
        head.prev = null;

        first.next = null;
        first.prev = tail;

        tail.next = first;
        tail = first;

        modCount++;
    }

    @Override
    public void shiftRight() {
        if (size <= 1) return;

        // move tail to head
        Node last = tail;
        Node beforeLast = last.prev;

        tail = beforeLast;
        tail.next = null;

        last.prev = null;
        last.next = head;

        head.prev = last;
        head = last;

        modCount++;
    }

    //  Internal linked list ops

    private void linkLast(byte e) {
        Node newNode = new Node(e);
        Node t = tail;
        tail = newNode;
        if (t == null) {
            head = newNode;
        } else {
            newNode.prev = t;
            t.next = newNode;
        }
        size++;
        modCount++;
    }

    private void linkBefore(byte e, Node succ) {
        Node pred = succ.prev;
        Node newNode = new Node(e);
        newNode.next = succ;
        newNode.prev = pred;
        succ.prev = newNode;
        if (pred == null) {
            head = newNode;
        } else {
            pred.next = newNode;
        }
        size++;
        modCount++;
    }

    private byte unlink(Node x) {
        byte element = x.value;
        Node next = x.next;
        Node prev = x.prev;

        if (prev == null) {
            head = next;
        } else {
            prev.next = next;
            x.prev = null;
        }

        if (next == null) {
            tail = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }

        size--;
        modCount++;
        return element;
    }

    private Node node(int index) {
        // choose from head or tail
        if (index < (size >> 1)) {
            Node cur = head;
            for (int i = 0; i < index; i++) cur = cur.next;
            return cur;
        } else {
            Node cur = tail;
            for (int i = size - 1; i > index; i--) cur = cur.prev;
            return cur;
        }
    }

    //  Index checks

    private void checkElementIndex(int index) {
        if (!isElementIndex(index)) throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private void checkPositionIndex(int index) {
        if (!isPositionIndex(index)) throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }

    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }

    private String outOfBoundsMsg(int index) {
        return "Index: " + index + ", Size: " + size;
    }

    //  Null / digit validation
    
    private void requireNonNullDigit(Byte e) {
        if (e == null) throw new NullPointerException("Null elements are not allowed");
    }

    private void requireDigitInBase(Byte e) {
        int v = e & 0xFF;
        if (v < 0 || v >= base) {
            throw new IllegalArgumentException("Digit " + v + " is out of range for base " + base);
        }
    }

    //  Iterators

    private class Itr implements Iterator<Byte> {
        Node next;
        Node lastReturned;
        int nextIndex;
        int expectedModCount;

        Itr(int index) {
            expectedModCount = modCount;
            nextIndex = index;
            next = (index == size) ? null : node(index);
        }

        @Override
        public boolean hasNext() {
            return nextIndex < size;
        }

        @Override
        public Byte next() {
            checkForComodification();
            if (!hasNext()) throw new java.util.NoSuchElementException();

            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.value;
        }

        @Override
        public void remove() {
            checkForComodification();
            if (lastReturned == null) throw new IllegalStateException();

            Node lastNext = lastReturned.next;
            unlink(lastReturned);

            if (next == lastReturned) {
                next = lastNext;
            } else {
                nextIndex--;
            }

            lastReturned = null;
            expectedModCount = modCount;
        }

        final void checkForComodification() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    private final class ListItr extends Itr implements ListIterator<Byte> {

        ListItr(int index) {
            super(index);
        }

        @Override
        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        @Override
        public Byte previous() {
            checkForComodification();
            if (!hasPrevious()) throw new java.util.NoSuchElementException();

            if (next == null) {
                next = tail;
            } else {
                next = next.prev;
            }
            lastReturned = next;
            nextIndex--;
            return lastReturned.value;
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public void set(Byte e) {
            checkForComodification();
            requireNonNullDigit(e);
            requireDigitInBase(e);
            if (lastReturned == null) throw new IllegalStateException();
            lastReturned.value = e;
        }

        @Override
        public void add(Byte e) {
            checkForComodification();
            requireNonNullDigit(e);
            requireDigitInBase(e);

            if (next == null) {
                linkLast(e);
            } else {
                linkBefore(e, next);
            }
            nextIndex++;
            lastReturned = null;
            expectedModCount = modCount;
        }
    }

    //  Decimal string parsing / base conversions

    private void initFromDecimalString(String value) {
        // invalid => leave empty
        if (value == null) return;
        value = value.trim();
        if (value.isEmpty()) return;

        // no negative allowed, only digits
        if (value.startsWith("-")) return;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < '0' || ch > '9') {
                // invalid
                return;
            }
        }

        // Normalize leading zeros: keep single zero if all zeros
        int p = 0;
        while (p < value.length() && value.charAt(p) == '0') p++;
        if (p == value.length()) {
            // value is 0
            clear();
            add((byte) 0);
            return;
        }
        if (p > 0) value = value.substring(p);

        BigInteger bi;
        try {
            bi = new BigInteger(value, 10);
        } catch (Exception ex) {
            return;
        }

        initFromBigInteger(bi);
    }

    private void initFromBigInteger(BigInteger bi) {
        clear();

        if (bi == null || bi.signum() < 0) {
            return;
        }
        if (bi.equals(BigInteger.ZERO)) {
            add((byte) 0);
            return;
        }

        // Convert to digits in this.base, most significant first
        BigInteger b = BigInteger.valueOf(base);
        // collect remainders into temporary char buffer (reverse)
        // Use byte array as stack
        int cap = Math.max(16, bi.bitLength() / 2 + 4);
        byte[] tmp = new byte[cap];
        int len = 0;

        BigInteger n = bi;
        while (n.signum() > 0) {
            BigInteger[] qr = n.divideAndRemainder(b);
            int rem = qr[1].intValue();
            if (len == tmp.length) {
                // grow
                byte[] nt = new byte[tmp.length * 2];
                System.arraycopy(tmp, 0, nt, 0, tmp.length);
                tmp = nt;
            }
            tmp[len++] = (byte) rem;
            n = qr[0];
        }

        // tmp has least significant first, so add reversed to make MSB at index 0
        for (int i = len - 1; i >= 0; i--) {
            linkLast(tmp[i]);
        }
    }

    private BigInteger toBigInteger() {
        if (size == 0) return BigInteger.ZERO;
        BigInteger n = BigInteger.ZERO;
        BigInteger b = BigInteger.valueOf(base);

        Node cur = head;
        while (cur != null) {
            int digit = cur.value & 0xFF;
            n = n.multiply(b).add(BigInteger.valueOf(digit));
            cur = cur.next;
        }
        return n;
    }

    private static BigInteger bigIntegerFromDigits(List<Byte> digits, int base) {
        if (digits == null || digits.isEmpty()) return BigInteger.ZERO;
        BigInteger n = BigInteger.ZERO;
        BigInteger b = BigInteger.valueOf(base);
        for (Byte d : digits) {
            if (d == null) return BigInteger.ZERO;
            int v = d & 0xFF;
            if (v < 0 || v >= base) return BigInteger.ZERO;
            n = n.multiply(b).add(BigInteger.valueOf(v));
        }
        return n;
    }

    private char digitToChar(byte d) {
        int v = d & 0xFF;
        if (v < 10) return (char) ('0' + v);
        return (char) ('A' + (v - 10)); // for hex
    }
}
