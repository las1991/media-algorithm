package com.sengled.media.worker.metrics;


public interface MetricsGraphics {

    Graphics getOrCreateGraphics(String name,
                   String type,
                   String colTemplates);
    

}
