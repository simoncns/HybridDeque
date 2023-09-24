import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Doubly-linked-list implementation of the java.util.Deque interface. This implementation is more
 * space-efficient than Java's LinkedList class for large collections because each node contains a
 * block of elements instead of only one. This reduces the overhead required for next and previous
 * node references.
 *
 *<p>This implementation does not allow null's to be added to the collection. Adding a null will
 * result in a NullPointerException.
 *
 * @author Nick Simoncelli and Nathan Sprague
 * @version pa3
 * 
 */
public class HybridDeque<E> extends AbstractDeque<E> {

  /*
   * IMPLEMENTATION NOTES ----------------------------------
   *
   * The list of blocks is never empty, so leftBlock and rightBlock are never equal to null. The
   * list is not circular.
   *
   * A deque's first element is at leftBlock.elements[leftIndex]
   * 
   * and its last element is at rightBlock.elements[rightIndex].
   * 
   * The indices, leftIndex and rightIndex are always in the range:
   * 
   * 0 <= index < BLOCK_SIZE
   *
   * And their exact relationship is:
   * 
   * (leftIndex + size - 1) % BLOCK_SIZE == rightIndex
   *
   * Whenever leftBlock == rightBlock, then:
   * 
   * leftIndex + size - 1 == rightIndex
   *
   * However, when leftBlock != rightBlock, the leftIndex and rightIndex become indices into
   * distinct blocks and either may be larger than the other.
   *
   * Empty deques have:
   * 
   * size == 0
   * 
   * leftBlock == rightBlock
   * 
   * leftIndex == CENTER + 1
   * 
   * rightIndex == CENTER
   * 
   * Checking for size == 0 is the intended way to see whether the Deque is empty.
   * 
   * 
   * (Comments above are a lightly modified version of comments in Python's deque implementation:
   * https://github.com/python/cpython/blob/v3.11.2/Modules/_collectionsmodule.c
   * https://docs.python.org/3.11/license.html)
   * 
   */

  private static int BLOCK_SIZE = 64;
  private static int CENTER = (BLOCK_SIZE - 1) / 2;

  private Block leftBlock;
  private int leftIndex;
  private Block rightBlock;
  private int rightIndex;
  private int size;


  /**
   * Constructor for empty HybridDeque.
   */
  public HybridDeque() {
    this.size = 0;
    this.leftBlock = new Block(null, null);
    this.rightBlock = leftBlock;
    this.leftIndex = CENTER + 1;
    this.rightIndex = CENTER;
  }

  /**
   * DO NOT MODIFY THIS METHOD. This will be used in grading/testing to modify the default block
   * size..
   *
   * @param blockSize The new block size
   */
  protected static void setBlockSize(int blockSize) {
    HybridDeque.BLOCK_SIZE = blockSize;
    HybridDeque.CENTER = (blockSize - 1) / 2;
  }


  /**
   * Doubly linked list node (or block) containing an array with space for multiple elements.
   */
  private class Block {
    private E[] elements;
    private Block next;
    private Block prev;

    /**
     * Block Constructor.
     *
     * @param prev Reference to previous block, or null if this is the first
     * @param next Reference to next block, or null if this is the last
     */
    @SuppressWarnings("unchecked")
    public Block(Block prev, Block next) {
      this.elements = (E[]) (new Object[BLOCK_SIZE]);
      this.next = next;
      this.prev = prev;
    }

  }
  
  @Override
  public boolean offerFirst(E e) {
    if (e == null) {
      throw new NullPointerException();
    }
    if (leftIndex == 0) {
      Block newBlock = new Block(null, leftBlock);
      leftBlock.prev = newBlock;
      leftBlock = newBlock;
      leftIndex = BLOCK_SIZE - 1;
    } else {
      leftIndex--;
    }
    leftBlock.elements[leftIndex] = e;
    size++;
    return true;
  }

  @Override
  public boolean offerLast(E e) {
    if (e == null) {
      throw new NullPointerException();
    }
    if (rightIndex == BLOCK_SIZE - 1) {
      Block newBlock = new Block(rightBlock, null);
      rightBlock.next = newBlock;
      rightBlock = newBlock;
      rightIndex = 0;
    } else {
      rightIndex++;
    }
    rightBlock.elements[rightIndex] = e;
    size++;
    return true;
  }

  @Override
  public E pollFirst() {
    if (isEmpty()) {
      return null;
    }
    E tempE = leftBlock.elements[leftIndex];
    if (size == 1) {
      leftIndex = CENTER + 1;
      rightIndex = CENTER;
      leftBlock = rightBlock;
      size = 0;
    } else {
      if (leftIndex == BLOCK_SIZE - 1) {
        leftIndex = 0;
        leftBlock = leftBlock.next;
      } else {
        leftIndex++;
      }
      size--;
    }
    return tempE;
  }

  @Override
  public E pollLast() {
    if (isEmpty()) {
      return null;
    }
    E tempE = rightBlock.elements[rightIndex];
    if (size == 1) {
      leftIndex = CENTER + 1;
      rightIndex = CENTER;
      leftBlock = rightBlock;
      size = 0;
    } else {
      if (rightIndex == 0) {
        rightIndex = BLOCK_SIZE - 1;
        rightBlock = rightBlock.prev;
      } else {
        rightIndex--;
      }
      size--;
    }
    return tempE;
  }

  @Override
  public E peekFirst() {
    if (size == 0) {
      return null;
    }
    return leftBlock.elements[leftIndex];
  }

  @Override
  public E peekLast() {
    if (size == 0) {
      return null;
    }
    return rightBlock.elements[rightIndex];
  }

  @Override
  public boolean removeFirstOccurrence(Object o) {
    @SuppressWarnings("unchecked")
    E comp = (E) o;
    Iterator<E> dequeIterator = this.iterator();
    while (dequeIterator.hasNext()) {
      if (dequeIterator.next().equals(comp)) {
        dequeIterator.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean removeLastOccurrence(Object o) {
    @SuppressWarnings("unchecked")
    E comp = (E) o;
    Iterator<E> dequeIterator = this.descendingIterator();
    while (dequeIterator.hasNext()) {
      if (dequeIterator.next().equals(comp)) {
        dequeIterator.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public Iterator<E> descendingIterator() {
    return new HybridDequeDecendingIterator();
  }

  private class HybridDequeDecendingIterator implements Iterator<E> {

    private boolean removable;
    private int numLeft;
    private Block currBlock;
    private int currIndex;

    public HybridDequeDecendingIterator() {
      removable = false;
      numLeft = size();
      currBlock = rightBlock;
      currIndex = rightIndex;
    }

    @Override
    public boolean hasNext() {
      return numLeft > 0;
    }

    @Override
    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      E tempE = currBlock.elements[currIndex];
      if (currIndex == 0) {
        currBlock = currBlock.prev;
        currIndex = BLOCK_SIZE - 1;
      } else {
        currIndex--;
      }
      E result = tempE;
      numLeft--;
      removable = true;
      return result;
    }

    @Override
    public void remove() {
      if (!removable) {
        throw new NoSuchElementException();
      }
      Block tempBlock = currBlock;
      int tempIndex = currIndex;
      removeHelper(tempBlock, tempIndex, numLeft);
      currIndex--;
      removable = false;
    }
  }



  @Override
  public Iterator<E> iterator() {
    return new HybridDequeIterator();
  }

  private class HybridDequeIterator implements Iterator<E> {

    private boolean removable;
    private int numLeft;
    private Block currBlock;
    private int currIndex;


    public HybridDequeIterator() {
      removable = false;
      numLeft = size();
      currBlock = leftBlock;
      currIndex = leftIndex;
    }

    @Override
    public boolean hasNext() {
      return numLeft > 0;
    }

    @Override
    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      E tempE = currBlock.elements[currIndex];
      if (currIndex == BLOCK_SIZE - 1) {
        currBlock = currBlock.next;
        currIndex = 0;
      } else {
        currIndex++;
      }
      E result = tempE;
      numLeft--;
      removable = true;
      return result;
    }

    @Override
    public void remove() {
      if (!removable) {
        throw new NoSuchElementException();
      }
      Block tempBlock = currBlock;
      int tempIndex = currIndex;
      removeHelper(tempBlock, tempIndex, size() - numLeft);
      currIndex--;
      removable = false;
    }

  }

  /**
   * Helper for shifting elements after removal.
   *
   * @param tempBlock Starting block spot
   * @param tempIndex Starting index
   * @param numLeft number of elements to shift
   */
  public void removeHelper(Block tempBlock, int tempIndex, int numLeft) {
    for (int i = 0; i < numLeft - 1; i++) {
      if (tempIndex == BLOCK_SIZE - 1) {
        Block tempBlock2 = tempBlock.next;
        tempBlock.elements[BLOCK_SIZE - 1] = tempBlock2.elements[0];
        tempIndex = 0;
        tempBlock = tempBlock2;
      } else {
        tempBlock.elements[tempIndex - 1] = tempBlock.elements[tempIndex];
      }
      tempIndex++;
    }
  }

  @Override
  public int size() {
    return this.size;
  }

  /**
   * Clear method to make HybridDeque empty.
   */
  public void clear() {
    this.size = 0;
    this.leftBlock = new Block(null, null);
    this.rightBlock = leftBlock;
    this.leftIndex = CENTER + 1;
    this.rightIndex = CENTER;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Equals method to see if both HybridDeques are the same.
   *
   * @param other other HybridDeque being compared
   * @return boolean true for equal false for not
   */
  public boolean equals(Object other) {
    if (!(other instanceof HybridDeque)) {
      return false;
    }

    @SuppressWarnings("unchecked")
    HybridDeque<E> compDeque = (HybridDeque<E>) other;
    if (size() != compDeque.size) {
      return false;
    }
    Iterator<E> currIterator = this.descendingIterator();
    Iterator<E> otherIterator = compDeque.descendingIterator();
    while (currIterator.hasNext()) {
      if (!currIterator.next().equals(otherIterator.next())) {
        return false;
      }
    }
    return true;
  }

  // ----------------------------------------------------
  // ADD UNIMPLEMENTED DEQUE METHODS HERE.
  // (You Don't need to provide JavaDoc comments for inherited methods. They
  // will inherit appropriate comments from Deque.)

  // -------------------------------------------------
  // METHODS THAT NEED TO BE IMPLEMENTED FOR PART 1:
  //
  // constructor done
  // clear done
  // offerLast done
  // offerFirst done
  // peekFirst done
  // peekLast done
  // pollFirst done
  // pollLast done
  // equals done
  // iterator (without removal) done
  // descendingIterator (without removal) done

  // -------------------------------------------------
  // METHODS THAT NEED TO BE IMPLEMENTED FOR PART 2:
  //
  // removeFirstOccurrence
  // removeLastOccurrence
  // remove methods for iterators


}
