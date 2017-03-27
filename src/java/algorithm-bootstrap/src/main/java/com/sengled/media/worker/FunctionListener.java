package com.sengled.media.worker;

public interface FunctionListener {
    /**
     * 在Function处理结束后回调
     * @param e
     * @param record
     * @param result
     */
    void operationComplete(Exception e, String token,byte[] imageBytes,byte[] result);
}

