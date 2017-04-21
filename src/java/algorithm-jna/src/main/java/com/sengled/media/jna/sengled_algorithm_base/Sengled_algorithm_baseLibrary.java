package com.sengled.media.jna.sengled_algorithm_base;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
/**
 * JNA Wrapper for library <b>sengled_algorithm_base</b><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public interface Sengled_algorithm_baseLibrary extends Library {
	public static final String JNA_LIBRARY_NAME = "sengled_algorithm_base";
	public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(Sengled_algorithm_baseLibrary.JNA_LIBRARY_NAME);
	public static final Sengled_algorithm_baseLibrary INSTANCE = (Sengled_algorithm_baseLibrary)Native.loadLibrary(Sengled_algorithm_baseLibrary.JNA_LIBRARY_NAME, Sengled_algorithm_baseLibrary.class);
	/** <i>native declaration : sengled_algorithm_base.h</i> */
	public static final int ALGORITHM_MAX_LENGTH = (int)1024;
	/** <i>native declaration : sengled_algorithm_base.h</i> */
	public static final int ALGORITHM_MAX_RESULT_LENGTH = (int)(10 * 1024);
	/**
	 * Original signature : <code>void SetLogCallback(void*)</code><br>
	 * <i>native declaration : sengled_algorithm_base.h:34</i>
	 */
	void SetLogCallback(Pointer callback);
	/**
	 * Original signature : <code>void* create_instance(const char*)</code><br>
	 * <i>native declaration : sengled_algorithm_base.h:36</i><br>
	 * @deprecated use the safer methods {@link #create_instance(java.lang.String)} and {@link #create_instance(com.sun.jna.Pointer)} instead
	 */
	@Deprecated 
	Pointer create_instance(Pointer token);
	/**
	 * Original signature : <code>void* create_instance(const char*)</code><br>
	 * <i>native declaration : sengled_algorithm_base.h:36</i>
	 */
	Pointer create_instance(String token);
	/**
	 * Original signature : <code>void feed(void*, void*, int, int, void*, algorithm_base_result*)</code><br>
	 * <i>native declaration : sengled_algorithm_base.h:38</i>
	 */
	void feed(Pointer handle, Pointer frame, int frame_width, int frame_height, Pointer algorithm_params, algorithm_base_result result);
	/**
	 * Original signature : <code>void delete_instance(void*)</code><br>
	 * <i>native declaration : sengled_algorithm_base.h:40</i>
	 */
	void delete_instance(Pointer handle);
}