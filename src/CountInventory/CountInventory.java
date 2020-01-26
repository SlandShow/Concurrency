package CountInventory;

/**
 * Class for incrementing and decrementing items
 */
public class CountInventory {

    private int items;

    /**
     * Icrement items.
     * Non-atomic operation.
     */
    public void increment() {
        items++;
    }

    /**
     * Decrement items.
     * Non-atomic operation.
     */
    public void decrement() {
        items--;
    }

    /**
     * Return count of items for all threads.
     * @return items
     */
    public int getItems() {
        return items;
    }
}