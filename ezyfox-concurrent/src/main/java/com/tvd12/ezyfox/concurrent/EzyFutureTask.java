package com.tvd12.ezyfox.concurrent;

public class EzyFutureTask implements EzyFuture {

	protected boolean done;
	protected Object result;
	protected Exception exception;
	
	protected final static long NO_TIMEOUT = -1L;
	
	@Override
	public void setResult(Object result) {
		synchronized (this) {
			this.result = result;
			notify();
		}
	}
	
	@Override
	public void setException(Exception exception) {
		synchronized (this) {
			this.exception = exception;
			notify();
		}
	}
	
	@Override
	public <V> V get() throws Exception {
		V v = get(NO_TIMEOUT);
		return v;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <V> V get(long timeout) throws Exception {
		synchronized (this) {
			while(result == null && exception == null) {
				if(timeout > 0)
					wait(timeout);
				else 
					wait();
			}
			this.done = true;
			if(result != null)
				return (V)result;
			throw exception;
		}
	}
	
	@Override
	public boolean isDone() {
		synchronized (this) {
			return done;
		}
	}
	
}
