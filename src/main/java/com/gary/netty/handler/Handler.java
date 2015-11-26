package com.gary.netty.handler;

import com.gary.netty.event.Event;

public interface Handler<T> {

	public void handle(final Event<T> event) throws Exception;
}
