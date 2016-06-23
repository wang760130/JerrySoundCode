package com.jerry.soundcode.thread;

import java.util.concurrent.atomic.AtomicReference;

import com.jerry.soundcode.concurrent.atomic.AtomicInteger;

public class Exchanger<V> {

	private static final int NCPU = Runtime.getRuntime().availableProcessors();

	private static final int CAPACITY = 32;
	
	private static final int FULL = Math.max(0, Math.min(CAPACITY, NCPU / 2) - 1);
	
	private static final int SPINS = (NCPU == 1) ? 0 : 2000;
	
	private static final int TIME_SPINS = SPINS / 20;
	
	private static final Object CANCEL = new Object();
	
	private static final Object NULL_ITEM = new Object();
	
	private static final class Node extends AtomicReference<Object> {
		
		public final Object item;
		
		public volatile Thread waiter;
		
		public Node(Object item) {
			this.item = item;
		}
	}
	
	private static final class Slot extends AtomicReference<Object> {
		long q0, q1, q2, q3, q4, q5, q6, q7, q8, q9, qa, qb, qc, qd, qe;
	}
	
	private volatile Slot[] arena = new Slot[CAPACITY];
	
	private final AtomicInteger max = new AtomicInteger();
	
	
}
