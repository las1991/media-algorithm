package com.sengled.media.jna.sengled_algorithm_base;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : sengled_algorithm_base.h</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class algorithm_base_result extends Structure {
	public int bresult;
	/** C type : char[10 * 1024] */
	public byte[] result = new byte[10 * 1024];
	public algorithm_base_result() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("bresult", "result");
	}
	/** @param result C type : char[10 * 1024] */
	public algorithm_base_result(int bresult, byte result[]) {
		super();
		this.bresult = bresult;
		if ((result.length != this.result.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.result = result;
	}
	public algorithm_base_result(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends algorithm_base_result implements Structure.ByReference {
		
	};
	public static class ByValue extends algorithm_base_result implements Structure.ByValue {
		
	};
}