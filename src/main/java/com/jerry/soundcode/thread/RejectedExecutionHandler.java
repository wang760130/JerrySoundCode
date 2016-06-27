package com.jerry.soundcode.thread;

import com.jerry.soundcode.thread.ThreadPoolExecutor;

public interface RejectedExecutionHandler {

	void rejectedExecution(Runnable r, ThreadPoolExecutor executor);

}
