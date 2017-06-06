package com.sengled.media.interfaces;

import com.sun.jna.Memory;

public class DisposeableMemory extends Memory{
	
    public DisposeableMemory(long size) {
        super(size);
    }
 
	public synchronized void dispose() {
		super.dispose();
	}
}
