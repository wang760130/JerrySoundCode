package com.jerry.soundcode.set;

import java.io.ObjectStreamField;
import java.io.Serializable;

public class BitSet implements Cloneable, Serializable {
	
	private final static int ADDRESS_BITS_PER_WORD = 6;
	private final static int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
	private final static int BIT_INDEX_MASK = BITS_PER_WORD - 1;
	
	private static final long WORD_MASK = 0xffffffffffffffffL;
	
	private static final ObjectStreamField[] serialPersistentFields = {
		new ObjectStreamField("bits", long[].class),
	};
	
	private long[] words;
	
	private transient int wordsInUse = 0;
	
	private transient boolean sizeIsSticky = false;
	
	private static final long serialVersionUID = -1542304988232054663L;
	
	private static int wordIndex(int bitIndex) {
		return bitIndex >> ADDRESS_BITS_PER_WORD;
	}
	
	private void checkInvariants() {
		
	}
}
