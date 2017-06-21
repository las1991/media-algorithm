package com.sengled.mediaworker.object;

import java.util.Map;

public interface ObjectRecognition {

	void sumbit(final String token,final byte[] nal,Map<String,Object> objectConfig);
}
