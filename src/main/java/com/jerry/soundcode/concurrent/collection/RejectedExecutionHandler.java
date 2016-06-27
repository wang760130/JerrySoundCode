package com.jerry.soundcode.concurrent.collection;

import com.jerry.soundcode.thread.ThreadPoolExecutor;

public interface RejectedExecutionHandler {

	void rejectedExecution(Runnable r, ThreadPoolExecutor executor);

}
