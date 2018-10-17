package com.sengled.media.objectpool;


import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import com.sengled.media.jna.nal_decoder.YUVFrame2;

public class DecodeYUVFrame2ObjectPool extends AbsObjectPool<YUVFrame2> {

    private static DecodeYUVFrame2ObjectPool objectPool;

    public static DecodeYUVFrame2ObjectPool getInstance() {
        if (null == objectPool) {
            synchronized (DecodeYUVFrame2ObjectPool.class) {
                if (null == objectPool) {
                    objectPool = new DecodeYUVFrame2ObjectPool();
                }
            }
        }
        return objectPool;
    }

    @Override
    public PoolableObjectFactory<YUVFrame2> getFactory() {
        return new ObjectFactory();
    }
    
    public  class ObjectFactory extends BasePoolableObjectFactory<YUVFrame2>{
        @Override
        public YUVFrame2 makeObject() throws Exception {
            return new YUVFrame2();
        }
    }

    @Override
    public void clear(YUVFrame2 t) {
        t.clear();
    }
}
