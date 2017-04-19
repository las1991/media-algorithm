package com.sengled.media.interfaces;

public class YUVImage {
	private int width;
	private int height;
	private byte[] YUVData;
	
	public YUVImage(){
		
	}
	public YUVImage(int width, int hight, byte[]  YUVData) {
		this.width = width;
		this.height = hight;
		this.YUVData = YUVData;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHight(int hight) {
		this.height = hight;
	}
	public byte[]  getYUVData() {
		return YUVData;
	}
	public void setYUVData(byte[]  YUVData) {
		this.YUVData = YUVData;
	}
}
