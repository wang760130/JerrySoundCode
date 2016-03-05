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
	
	public synchronized void load(Reader reader) throws IOException {
		load0(new LineReader(reader));
	}

	
	private void load0(LineReader lr) throws IOException {
		char[] convtBuf = new char[1024];
		int limit;
		int keyLen = 0;
		int valueStart = 0;
		char c;
		boolean hasSep = false;
		boolean precedingBacksLash = false;
		
		while((limit = lr.readLine()) >= 0) {
			c = lr.lineBuf[keyLen];
			if((c == '=' || c == ':') && !precedingBacksLash) {
				valueStart = keyLen + 1;
				hasSep = true;
				break;
			} else if((c == ' ' || c == '\t' || c == '\t' && !precedingBacksLash)) {
				valueStart = keyLen + 1;
				break;
			}
			
			if(c == '\\') {
				precedingBacksLash = !precedingBacksLash;
			} else {
				precedingBacksLash = false;
			}
			keyLen ++;
		}
		
		while(valueStart < limit) {
			c = lr.lineBuf[valueStart];
			if(c != ' ' && c != '\t' && c != '\f') {
				if(!hasSep && (c == '=' || c == ':')) {
					hasSep = true;
				} else {
					break;
				}
			}
			valueStart ++;
		}
		
		
		// TODO
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
				
				if(inStream != null) {
					c = (char) (0xff & inByteBuf[inOff++]);
				} else {
					c = inCharBuf[inOff++];
				}
				
				if(skipLF) {
					skipLF = false;
				}
				
				if(c == '\n') {
					continue;
				}
				
				if(skipWhiteSpace) {
					if(c == ' ' || c == '\t' || c == '\f') {
						continue;
					}
					if(!appendedLineBegin && (c == '\r' || c == '\n')) {
						continue;
					}
					skipWhiteSpace = false;
					appendedLineBegin = false;
				}
				
				if(c != '\n' && c != '\r') {
					lineBuf[len++] = c;
					if(len == lineBuf.length) {
						int newLength = lineBuf.length * 2;
						if(newLength < 0) {
							newLength = Integer.MAX_VALUE;
						}
						char[] buf = new char[newLength];
						System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
						lineBuf = buf;
					}
					if(c == '\\') {
						precedingBackslash = !precedingBackslash;
					} else {
						precedingBackslash = false;
					}
 				} else {
 					if(isCommentLine || len == 0) {
 						isCommentLine = false;
 						isNewLine = true;
 						skipWhiteSpace = true;
 						len = 0;
 						continue;
 					}
 					
 					if(inOff >= inLimit) {
 						inLimit = (inStream == null) ? reader.read(inCharBuf) 
 								: inStream.read(inByteBuf); 
 						inOff = 0;
 						if(inLimit <= 0) {
 							return len;
 						}
 					}
 					
 					if(precedingBackslash) {
 						len -= 1;
 						skipWhiteSpace = true;
 						appendedLineBegin = true;
 						precedingBackslash = false;
 						if(c == '\r') {
 							skipLF = true;
 						} else {
 							return len;
 						}
 					}
 				}
			}
		}
	}
	
	private String loadConvert(char[] in, int off, int len, char[] convtBuf) {
		if(convtBuf.length < len) {
			int newLen = len * 2;
			if(newLen < 0) {
				newLen = Integer.MAX_VALUE;
			}
			convtBuf = new char[newLen];
		}
		
		char aChar;
		char[] out = convtBuf;
		int outLen = 0;
		int end = off + len;
		
		while(off < end) {
			aChar = in[off++];
			if(aChar == '\\') {
				aChar = in[off++];
				if(aChar == 'u') {
					int value = 0;
					for(int i = 0; i < 4; i++) {
						aChar = in[off++];
						switch(aChar) {
//						case '0': case '1':
						}
					}
				}
			}
		}
		
		return null;
	}
}
