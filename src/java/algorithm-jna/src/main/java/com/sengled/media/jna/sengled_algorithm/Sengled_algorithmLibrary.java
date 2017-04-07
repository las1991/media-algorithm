package com.sengled.media.jna.sengled_algorithm;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
/**
 * JNA Wrapper for library <b>sengled_algorithm</b><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public interface Sengled_algorithmLibrary extends Library {
	public static final String JNA_LIBRARY_NAME = "sengled_algorithm";
	public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(Sengled_algorithmLibrary.JNA_LIBRARY_NAME);
	public static final Sengled_algorithmLibrary INSTANCE = (Sengled_algorithmLibrary)Native.loadLibrary(Sengled_algorithmLibrary.JNA_LIBRARY_NAME, Sengled_algorithmLibrary.class);
	/** <i>native declaration : sengled_algorithm.h</i> */
	public static final int ALGORITHM_MAX_LENGTH = (int)1024;
	/** <i>native declaration : sengled_algorithm.h</i> */
	public static final int ALGORITHM_MAX_RESULT_LENGTH = (int)(10 * 1024);
	/** <i>native declaration : sengled_algorithm.h</i> */
	public static final int SLS_LOG_INFO = (int)0x0001;
	/** <i>native declaration : sengled_algorithm.h</i> */
	public static final int SLS_LOG_DEBUG = (int)0x0002;
	/** <i>native declaration : sengled_algorithm.h</i> */
	public static final int SLS_LOG_WARNING = (int)0x0003;
	/** <i>native declaration : sengled_algorithm.h</i> */
	public static final int SLS_LOG_ERROR = (int)0x0004;
	/** <i>native declaration : sengled_algorithm.h</i> */
	public static final int SLS_LOG_FATAL = (int)0x0005;
	/**
	 * create a algorithm instance<br>
	 * input<br>
	 * @param params is a common_params  struct ptr<br>
	 * ouput<br>
	 * @param Handle is inner pointer<br>
	 * Original signature : <code>void* create_algorithm_instance(common_params*)</code><br>
	 * <i>native declaration : sengled_algorithm.h:78</i>
	 */
	Pointer create_algorithm_instance(common_params params);
	/**
	 * feed a new frame <br>
	 * input<br>
	 * @param handle is the parameter which is created by create_algorithm_instance<br>
	 * @param frame is the YUV data<br>
	 * @param frame_width is the frame width<br>
	 * @param frame_height is the frame height<br>
	 * @param algorithm_params is (json or struct) parameters data which is required by algorithm<br>
	 * output<br>
	 * @param result is the algorithm detection result<br>
	 * Original signature : <code>void feed_frame(void*, void*, int, int, void*, algorithm_result*)</code><br>
	 * <i>native declaration : sengled_algorithm.h:91</i>
	 */
	void feed_frame(Pointer handle, Pointer frame, int frame_width, int frame_height, Pointer algorithm_params, algorithm_result result);
	/**
	 * delete a algorithm instance<br>
	 * input<br>
	 * @param handle is the parameter which is created by create_algorithm_instance<br>
	 * Original signature : <code>void delete_algorithm_instance(void*)</code><br>
	 * <i>native declaration : sengled_algorithm.h:98</i>
	 */
	void delete_algorithm_instance(Pointer handle);
}
