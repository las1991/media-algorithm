package com.sengled.mediaworker.algorithm.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.algorithm.config.Actions.AlgorithmType;
import com.sengled.media.algorithm.config.AlgorithmConfig;
import com.sengled.media.algorithm.config.AlgorithmZone;
import com.sengled.media.algorithm.config.MotionSensitivity;

public class AlgorithmConfigWarpper {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlgorithmConfigWarpper.class);
    
    private int highMotionSensitivity;
    private int normalMotionSensitivity;
    private int lowMotionSensitivity;
    private AlgorithmConfig  algorithmConfig;
    
    public AlgorithmConfigWarpper(AlgorithmConfig algorithmConfig,int highSensitivity,int normalSensitivity,int lowSensitivity){
        this.algorithmConfig = algorithmConfig;
        this.highMotionSensitivity = highSensitivity;
        this.normalMotionSensitivity = normalSensitivity;
        this.lowMotionSensitivity = lowSensitivity;
    }

    public static class MotionConfig {
        private int sensitivity;

        private List<Data> dataList;

        public int getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(int sensitivity) {
            this.sensitivity = sensitivity;
        }

        public List<Data> getDataList() {
            return dataList;
        }

        public void setDataList(List<Data> dataList) {
            this.dataList = dataList;
        }

        @Override
        public String toString() {
            return "MotionConfig [sensitivity=" + sensitivity + ", dataList=" + dataList + "]";
        }

    }

    public static class ObjectConfig {
        private int sensitivity;

        private List<Data> dataList;

        public List<Data> getDataList() {
            return dataList;
        }

        public void setDataList(List<Data> dataList) {
            this.dataList = dataList;
        }

        public int getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(int sensitivity) {
            this.sensitivity = sensitivity;
        }

        @Override
        public String toString() {
            return "ObjectConfig [sensitivity=" + sensitivity + ", dataList=" + dataList + "]";
        }

    }

    public static class Data {
        private int id;

        private String pos;

        private String objectList;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public List<Integer> getPosList() {
            String[] poss = pos.split(",");
            if (4 != poss.length) {
                Collections.emptyList();
            }
            List<Integer> posList = new ArrayList<>();
            posList.add(Integer.valueOf(poss[0]));
            posList.add(Integer.valueOf(poss[1]));
            posList.add(Integer.valueOf(poss[2]));
            posList.add(Integer.valueOf(poss[3]));
            return posList;
        }

        public String getPos() {
            return pos;
        }

        public void setPosFromList(List<Integer> pos) {
            if (null == pos || pos.isEmpty()) {
                return;
            }
            this.pos = pos.get(0) + "," + pos.get(1) + "," + pos.get(2) + "," + pos.get(3);
        }

        public void setPos(String pos) {
            this.pos = pos;
        }

        public String getObjectList() {
            return objectList;
        }

        public void setObjectList(String objectList) {
            this.objectList = objectList;
        }

        @Override
        public String toString() {
            return "Data [id=" + id + ", pos=" + pos + ", objectList=" + objectList + "]";
        }
    }
    
    public  int getRealSensitivity(Integer showSensitivity) {
        int realSensitivity = normalMotionSensitivity;
        if( null == showSensitivity ) {
            return realSensitivity;
        }
        switch(showSensitivity) {
            case MotionSensitivity.HIGH:
                realSensitivity =  highMotionSensitivity;
                break;
            case MotionSensitivity.LOW:
                realSensitivity =  lowMotionSensitivity;
                break;
            default:
                realSensitivity =  normalMotionSensitivity;
        }
        LOGGER.debug("realSensitivity value:{}",realSensitivity);
        return realSensitivity;
    }
    
    public MotionConfig getBaseConfig() {
        if( ! algorithmConfig.getActions().isEnable() ){
            return null;
        }
        //使用MotionConfig 做为基础配置
        MotionConfig motion = new MotionConfig();
        motion.setSensitivity(getRealSensitivity(algorithmConfig.getMotionSensitivity()));
        motion.setDataList(getDataList());
        return motion;
    }
    
    public ObjectConfig getObjectConfig() {
        if( isEnable(AlgorithmType.PERSION) ){
            ObjectConfig oc = new ObjectConfig();
            oc.setDataList(getDataList());
            oc.setSensitivity(getRealSensitivity(algorithmConfig.getMotionSensitivity()));
            return oc;
        }
        return null;
    }

    public MotionConfig getMotionConfig() {
        
        if( isEnable(AlgorithmType.MOTION) ){
            MotionConfig motion = new MotionConfig();
            motion.setSensitivity(getRealSensitivity(algorithmConfig.getMotionSensitivity()));
            motion.setDataList(getDataList());
            return motion;
        }
        return null;
    }
    

    private boolean isEnable(AlgorithmType type){
        if( ! algorithmConfig.getActions().isEnable() ){
            return false;
        }
        
        List<AlgorithmType> list = algorithmConfig.getActions().getAlgorithms();
        for (AlgorithmType algorithmType : list) {
            if( algorithmType.equals(type)){
                return true;
            }
        }
        
        return false;
    }
    
    private List<Data> getDataList(){
        List<AlgorithmZone> zones = algorithmConfig.getActions().getZones();
        List<Data> dataList = new ArrayList<>();
        for (AlgorithmZone algorithmZone : zones) {
            Data data = new Data();
            data.setId(Long.valueOf(algorithmZone.getZoneId()).intValue());
            data.setObjectList("1");//1表示物体识别
            data.setPos(algorithmZone.getRoiAreaCoordinate());
            dataList.add(data);
        }
        return dataList;
    }

    @Override
    public String toString() {
        return "AlgorithmConfigWarpper [algorithmConfig=" + algorithmConfig + ", getBaseConfig()=" + getBaseConfig() + ", getObjectConfig()="
                + getObjectConfig() + ", getMotionConfig()=" + getMotionConfig() + ", getDataList()=" + getDataList() + "]";
    }

}
