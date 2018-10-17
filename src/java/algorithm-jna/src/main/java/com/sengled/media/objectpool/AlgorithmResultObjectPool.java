package com.sengled.media.objectpool;


import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import com.sengled.media.jna.sengled_algorithm_base.algorithm_base_result2;

public class AlgorithmResultObjectPool extends AbsObjectPool<algorithm_base_result2> {

    private static AlgorithmResultObjectPool objectPool;

    public static AlgorithmResultObjectPool getInstance() {
        if (null == objectPool) {
            synchronized (AlgorithmResultObjectPool.class) {
                if (null == objectPool) {
                    objectPool = new AlgorithmResultObjectPool();
                }
            }
        }
        return objectPool;
    }

    @Override
    public PoolableObjectFactory<algorithm_base_result2> getFactory() {
        return new AlgorithmResultObjectPoolFactory();
    }
    
    public  class AlgorithmResultObjectPoolFactory extends BasePoolableObjectFactory<algorithm_base_result2>{
        @Override
        public algorithm_base_result2 makeObject() throws Exception {
            return new algorithm_base_result2();
        }
    }

    @Override
    public void clear(algorithm_base_result2 t) {
        t.clear();
    }
}
