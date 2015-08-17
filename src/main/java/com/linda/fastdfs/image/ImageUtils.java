package com.linda.fastdfs.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;
import net.coobird.thumbnailator.geometry.Positions;

public class ImageUtils {
	
	private static boolean check(BufferedImage image,int resizeWidth,int resizeHeight){
		return (image.getHeight()>resizeHeight&&resizeHeight>0)||(image.getWidth()>resizeWidth&&resizeWidth>0);
	}
	
	public static boolean requireProcess(BufferedImage image,String fileId,int rotate,float quality,int resizeWidth,int resizeHeight,int cropWidth,int cropHeight,String fmt){
		fileId = fileId.toLowerCase();
		//格式
		if(fmt!=null&&!fileId.endsWith(fmt)&&(fileId.endsWith("jpeg")||fileId.endsWith("jpg")||fileId.endsWith("png")||fileId.endsWith("bmp"))){
			return true;
		}
		
		//图片质量
		if(quality>0&&quality<100){
			return true;
		}
		
		//输出尺寸
		if(resizeWidth<image.getWidth()&&resizeWidth>0){
			return true;
		}
		if(resizeHeight<image.getHeight()&&resizeHeight>0){
			return true;
		}
		
		//裁剪
		if(cropWidth<image.getWidth()&&cropWidth>0){
			return true;
		}
		if(cropHeight<image.getHeight()&&cropHeight>0){
			return true;
		}
		//旋转
		if(rotate>0){
			return true;
		}
		return false;
	}
	
	private static int fix(int width,int imgWidth){
		return width>imgWidth?imgWidth:width;
	}
	
	private static float fixQuality(int quality){
		Float f = new Float(quality);
		return f/100;
	}
	
	@SuppressWarnings("resource")
	public static byte[] processImage(String fileId,byte[] img,int rotate,int quality,int resizeWidth,int resizeHeight,int cropWidth,int cropHeight,String fmt) throws IOException{
		if(img!=null){
			ByteArrayInputStream bis = new ByteArrayInputStream(img);
			BufferedImage image = ImageIO.read(bis);
			try{
				if(!ImageUtils.requireProcess(image, fileId, rotate, quality, resizeWidth, resizeHeight, cropWidth, cropHeight, fmt)){
					return img;
				}
				
				resizeWidth = fix(image.getWidth(),resizeWidth);
				resizeHeight = fix(image.getHeight(),resizeHeight);
				cropWidth = fix(image.getWidth(),cropWidth);
				cropHeight = fix(image.getHeight(),cropHeight);
				
				Builder<BufferedImage> builder = Thumbnails.of(image);
				if(rotate>0){
					builder.rotate(rotate);
				}
				if(quality>0&&quality<100){
					float fixQuality = fixQuality(quality);
					builder.outputQuality(fixQuality);
					if(!check(image, resizeWidth, resizeHeight)){
						builder.size(image.getWidth(), image.getHeight());
					}
				}
				//裁剪加尺寸
				if(cropWidth>0&&cropHeight>0){
					if(check(image,resizeWidth,resizeHeight)){
						builder.sourceRegion(Positions.CENTER, cropWidth, cropHeight);
						builder.size(resizeWidth, resizeWidth);
					}else{
						builder.crop(Positions.CENTER);
						builder.size(cropWidth, cropHeight);
//						builder.scale(1.0);
					}
				}else{
					if(check(image,resizeWidth,resizeHeight)){
						builder.size(resizeWidth, resizeWidth);
					}
				}
				//格式转换
				if(fmt!=null){
					builder.outputFormat(fmt);
				}else{
					int idx = fileId.lastIndexOf('.');
					fmt = fileId.substring(idx+1);
					builder.outputFormat(fmt);
				}
				
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				builder.toOutputStream(bos);
				return bos.toByteArray();
			}finally{
				bis.close();
			}
		}
		return null;
	}

}
