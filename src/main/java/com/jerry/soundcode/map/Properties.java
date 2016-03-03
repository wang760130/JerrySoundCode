package com.jerry.soundcode.map;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class Properties extends Hashtable<Object,Object>{

	private static final long serialVersionUID = 1L;

	protected Properties defaults;
	
	public Properties() {
		this(null);
	}
	
	public Properties(Properties defaults) {
		this.defaults = defaults;
	}

	public synchronized Object setProperty(String key, String value) {
		return put(key, value);
	}
	
	public synchronized void load(Reader reader) {
		load0(new LineReader(reader));
	}

	// TODO
	private void load0(LineReader lineReader) {
		// TODO Auto-generated method stub
		
	}
	
	class LineReader {
		public LineReader(InputStream inStream) {
			this.inStream = inStream;
			inByteBuf = new byte[8192];
		}
		
		public LineReader(Reader reader) {
			this.reader = reader;
			inCharBuf = new char[8192];
		}
		
		byte[] inByteBuf;
		char[] inCharBuf;
		char[] lineBuf = new char[1024];
		int inLimit = 0;
		int inOff = 0;
		InputStream inStream;
		Reader reader;
		
		int readLine() throws IOException {
			int len = 0;
			char c = 0;
			
			boolean skipWhiteSpace = true;
			boolean isCommentLine = false;
			boolean isNewLine = true;
			boolean appendedLineBegin = false;
			boolean precedingBackslash = false;
			boolean skipLF = false;
			
			while(true) {
				if(inOff >= inLimit) {
					inLimit = (inStream == null) ? reader.read(inCharBuf) 
							: inStream.read(inByteBuf);
				
					inOff = 0;
					if(inLimit <= 0) {
						if(len == 0 || isCommentLine) {
							return -1;
						}
						return len;
					}
				}
				
				// TODO
			}
			
		}
		
	}
}
