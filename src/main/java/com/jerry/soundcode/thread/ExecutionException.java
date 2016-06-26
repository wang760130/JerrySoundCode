package com.jerry.soundcode.thread;

public class ExecutionException extends Exception {

	private static final long serialVersionUID = 1L;

	public ExecutionException() {}
	
	protected ExecutionException(String message) {
		super(message);
	}
	
	public ExecutionException(String message, java.lang.Throwable cause) {
        super(message, cause);
    }
	
	public ExecutionException(Throwable cause) {
        super();
    }

}
