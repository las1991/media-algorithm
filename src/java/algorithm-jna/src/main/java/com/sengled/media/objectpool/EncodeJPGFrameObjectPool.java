package com.sengled.media.objectpool;


import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import com.sengled.media.jna.jpg_encoder.JPGFrame;

public class EncodeJPGFrameObjectPool extends AbsObjectPool<JPGFrame> {

    private static EncodeJPGFrameObjectPool objectPool;

    public static EncodeJPGFrameObjectPool getInstance() {
        if (null == objectPool) {
            synchronized (EncodeJPGFrameObjectPool.class) {
                if (null == objectPool) {
                    objectPool = new EncodeJPGFrameObjectPool();
                }
            }
        }
        return objectPool;
    }

    @Override
    public PoolableObjectFactory<JPGFrame> getFactory() {
        return new AlgorithmResultObjectPoolFactory();
    }
    
    public  class AlgorithmResultObjectPoolFactory extends BasePoolableObjectFactory<JPGFrame>{
        @Override
        public JPGFrame makeObject() throws Exception {
            return new JPGFrame();
        }
    }

    @Override
    public void clear(JPGFrame t) {
        t.clear();
    }
}
