public interface timestamp {
	/**
	 * A vector timestamp is owned by a process which has an id
	 * 
	 * @return the id of the owner process
	 */
	public int id();

	/**
	 * Get the current value of the logical clock for a process
	 *
	 * @param id of the process for which the value should
	 * 		     returned. id should be >= 0 and < size()
	 * 
	 * @throws IllegalAmount if id < 0 or id >= size
	 * 
	 * @return this[id] 
	 *  
	 */
	public int get(int id) throws IllegalArgumentException;
	
	/**
	 * Increments the logical clock for this process by 1
	 * All other clocks are left unchanged
	 * 
	 * E.g., if this vector timestamp is [1, 3, 4] and this.id() == 2,
	 * this vector timestamp is [1, 3, 5] after invoking increment
	 * 
	 */
	public void increment();
	
	/**
	 * Set the logical clock entries of this vector timestamp to the maximum
	 * of its logical clocks and other's logical clocks
	 * 
	 * E.g., if this vector timestamp is [2, 5] and other is [1, 6], then
	 * this vector timestamp is [2, 6] after invoking max
	 * 
	 * @param other : a vector timestamp to maximize this vector timestamp
	 *                with respect to

	 * @throws IllegalAmount if other.size() != size() 
	 * 
	 */
	public void max(timestamp other) throws IllegalArgumentException;

	/**
	 * Get the (constant) number of entries in this VectorTimestamp
	 * 
	 * E.g., if this vector timestamp is [3, 5, 7], size() returns 3 
	 * 
	 * @return number of entries
	 */
	public int size();
	
	/**
	 * A vector timestamp, other, can be delivered to this process if:
	 * 
	 *   for 0 <= i < size(), i != other.id(), i != id(): other[i] <= this[i], and
	 *   other[other.id()] == this[other.id()] + 1
	 * 
	 * E.g., if this vector timestamp is [3, 4, 7] and id() == 2 then 
	 * canDeliver will return true for ([4, 2, 5], 0) and false for
	 * ([5, 2, 5], 0)
	 * 
	 * @param other   : a VectorTimestamp to compare this vector timestamp to
	 *        
	 * @throws IllegalArgument if other is uncomparable to this vector timestamp
	 * 
	 * @return true if other is causally next
	 * 
	 */
	public boolean canDeliver(timestamp other)
		throws IllegalArgumentException;
}

/*The constructor of the implementing class should be of the form:

	public VectorTimestampImpl(int length, int id) throws IllegalArgumentException
*/
