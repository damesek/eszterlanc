package edu.northwestern.at.utils;

//#include "bottom.lic"

import java.util.*;

/**	A stack implemented using a single-ended queue.
 */

public class QueueStack<E>
{
	/**	The queue which is used to hold the stack entries. */

//	protected Queue<E> queue;
	protected Deque<E> queue;

	/**	Create an empty stack. */

	public QueueStack()
	{
//		queue	= new LinkedList<E>();
		queue	= new ArrayDeque<E>();
	}

	/**	Number of entries in stack.
	 *
	 *	@return		Number of entries in stack.
	 */

	public int size()
	{
		return queue.size();
	}

	/**	Check if stack is empty.
	 *
	 *	@return		true if stack is empty.
	 */

	public boolean isEmpty()
	{
//		return ( queue.size() == 0 );
		return queue.isEmpty();
	}

	/**	Add element to stack.
	 *
	 *	@param	e	Element to add to stack.
	 */

	public void push( E e )
	{
		queue.offer( e );
	}

	/**	Remove element from stack.
	 *
	 *	@return		Element returned.
	 *
	 *	@throws		EmptyQueueStackException when stack is empty.
	 */

	public E pop()
	{
		try
		{
			return queue.remove();
		}
		catch ( NoSuchElementException e )
		{
			throw new EmptyQueueStackException( "Empty stack." );
		}
	}

	/**	Peek at top element of stack without removing it.
	 *
	 *	@return		Element returned.
	 *
	 *	@throws		EmptyQueueStackException when stack is empty.
	 */

	public E peek()
	{
		try
		{
			return queue.element();
		}
		catch ( NoSuchElementException e )
		{
			throw new EmptyQueueStackException( "Empty stack." );
		}
	}
}

//#include "standard.lic"

