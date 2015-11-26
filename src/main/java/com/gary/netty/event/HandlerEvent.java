package com.gary.netty.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HandlerEvent<T> {
	private int cmd;
	
	private boolean async;
	
	private T handler;
}
