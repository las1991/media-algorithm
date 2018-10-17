package com.sengled.media.objectpool;

import java.util.function.Function;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;

public abstract class AbsObjectPool<T> {
    
    private static Config config = new Config();
    static{
        config.maxActive = 1000;//并发
        config.maxIdle = 1000;
        config.minIdle = 1000;
    }
    private PoolableObjectFactory<T> factory;
    private GenericObjectPool<T> pool ;
    
    public abstract PoolableObjectFactory<T> getFactory();
    public GenericObjectPool<T> getPool(PoolableObjectFactory<T> factory){
        return new GenericObjectPool<>(factory);
    }
    
    
    public AbsObjectPool(){
        factory = getFactory();
        pool = getPool(factory);
    }
    
    public <R> R function(Function<T, R> function){
        T t = null;
        try {
            t = pool.borrowObject();
            return function.apply(t);
        } catch (Exception e) {
             
        }finally{
            if(null != t ){
                try {
                    clear(t);
                    pool.returnObject(t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    public abstract void clear(T t);
}
