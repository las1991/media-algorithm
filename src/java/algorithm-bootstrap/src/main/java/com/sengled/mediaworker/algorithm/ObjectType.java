package com.sengled.mediaworker.algorithm;

public enum ObjectType {

	PERSON(1,"person"),
    DOG(2,"dog"),
    CAR(3,"car"),
	UNKNOWN(0,"unknown");
    
	ObjectType(int value,String name) {
        this.value = value;
        this.name = name;
    }
    public final int value;
    public final String name;
    public static ObjectType findByName(String name){
    	for (ObjectType el : ObjectType.values()) {
			if(el.name.equals(name.trim())){
				return el;
			}
		}
    	return UNKNOWN;
    }
}
