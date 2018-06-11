package com.sengled.mediaworker.algorithm.service.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.TargetObject;

public class ObjectRecognitionResultWrapper {
    private Multimap<Integer, TargetObject> multiMap = ArrayListMultimap.create();

    public ObjectRecognitionResultWrapper(ObjectRecognitionResult orr) {
        for (  TargetObject to : orr.getObjects()) {
            multiMap.put(to.getFrame(), to);
        }
    }

    public Multimap<Integer, TargetObject> getMultiMap() {
        return multiMap;
    }
    
    public ObjectRecognitionResult getObjectRecognitionResult(int frameIndex){
        ObjectRecognitionResult  orr = new ObjectRecognitionResult();
        Collection<TargetObject> coList = multiMap.get(frameIndex);
        
        List<TargetObject> toList;
        if (coList instanceof List)
            toList = (List<TargetObject>)coList;
        else
            toList = new ArrayList<TargetObject>(coList);
        orr.setObjects(toList);
        return orr;
    }
    
}
