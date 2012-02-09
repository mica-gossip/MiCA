package org.princehouse.mica.util;

import java.util.Comparator;

public class FiniteFieldInteger32 implements FiniteFieldInteger<FiniteFieldInteger32> {

	private final int value;
	
	public FiniteFieldInteger32(int value) {
		assert(value >= 0);
		this.value = value;
	}
	
	@Override
	public Comparator<? super FiniteFieldInteger32> relativeDistanceComparator() {
		return new Comparator<FiniteFieldInteger32> () {

			@Override
			public int compare(FiniteFieldInteger32 a,
					FiniteFieldInteger32 b) {
				return forwardDistance(a).compareTo(forwardDistance(b));
			}
			
		};
	}
	
	private Integer forwardDistance(FiniteFieldInteger32 x) {
		if(x.value >= value) {
			return x.value - value;		
		} else 
			return Integer.MAX_VALUE - (value - x.value);
	}
	
	public boolean equals(Object o) {
		if(!(o instanceof FiniteFieldInteger32)) {
			return false;
		}
		FiniteFieldInteger32 oi = (FiniteFieldInteger32) o;
		return value == oi.value;
	}
	
	public int hashCode() { 
		return value;
	}

	public int getValue() {
		return value;
	}
	
	public FiniteFieldInteger32 add(FiniteFieldInteger32 other) {
		int y = value + other.value;
		if(y < 0) 
			y = Integer.MAX_VALUE + y;
		return new FiniteFieldInteger32(y);
	}

	public FiniteFieldInteger32 add(int other) {
		int y = value + other;
		if(y < 0) 
			y = Integer.MAX_VALUE + y;
		return new FiniteFieldInteger32(y);
	}
}
