package app.cache.entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntryListTest {

    private final EntryList<String, String> list = new EntryList<>();
    private final CacheEntry<String, String> a = new CacheEntry<>("A", "a");
    private final CacheEntry<String, String> b = new CacheEntry<>("B", "b");
    private final CacheEntry<String, String> c = new CacheEntry<>("C", "c");

    private List<String> drainKeys() {
        final List<String> keys = new ArrayList<>();
        CacheEntry<String, String> entry;
        while ((entry = list.removeFirst()) != null) {
            keys.add(entry.key());
        }
        return keys;
    }

    @Test
    void removeFirstOnEmptyListReturnsNull() {
        assertNull(list.removeFirst());
        assertTrue(list.isEmpty());
    }

    @Test
    void keepsInsertionOrder() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);

        assertEquals(List.of("A", "B", "C"), drainKeys());
        assertTrue(list.isEmpty());
    }

    @Test
    void removesHeadMiddleAndTail() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);

        list.remove(b);
        assertEquals(List.of("A", "C"), drainKeys());

        list.addLast(a);
        list.addLast(b);
        list.addLast(c);
        list.remove(a);
        list.remove(c);
        assertEquals(List.of("B"), drainKeys());
    }

    @Test
    void removingOnlyEntryLeavesListEmpty() {
        list.addLast(a);

        list.remove(a);

        assertTrue(list.isEmpty());
        list.addLast(b);
        assertEquals(List.of("B"), drainKeys());
    }

    @Test
    void moveToLastReordersEntry() {
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);

        list.moveToLast(a);
        list.moveToLast(a); // already last: no-op

        assertEquals(List.of("B", "C", "A"), drainKeys());
    }

    @Test
    void clearEmptiesList() {
        list.addLast(a);
        list.addLast(b);

        list.clear();

        assertTrue(list.isEmpty());
        assertNull(list.removeFirst());
    }
}
