package com.sengled.mediaworker.metrics;


public interface MetricsGraphics {

    Graphics getOrCreateGraphics(String name,
                   String type,
                   String colTemplates);
    

}
