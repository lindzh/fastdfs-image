package com.linda.fastdfs.image;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.mikesu.fastdfs.FastdfsClient;
import net.mikesu.fastdfs.FastdfsClientFactory;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class ImageServlet extends HttpServlet{

	private static final long serialVersionUID = -5414558574739357002L;

	private Logger logger = Logger.getLogger(ImageServlet.class);
	
	private FastdfsClient fastdfsClient = null;
	
	private Configuration contentTypeConf;
	
	@Override
	public void destroy() {
		fastdfsClient.close();
		super.destroy();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			fastdfsClient = FastdfsClientFactory.getFastdfsClient("FastdfsClient.properties");
		} catch (ConfigurationException e) {
			throw new ServletException(e);
		}
		try {
			contentTypeConf = new PropertiesConfiguration("ContentType.properties");
		} catch (ConfigurationException e) {
			throw new ServletException(e);
		}
	}
	
	private String getContentType(String fileId,String fmt,byte[] src,byte[] processImg){
		if(src!=processImg){//有处理
			if(fmt!=null){
				String ct = contentTypeConf.getString(fmt.toLowerCase());
				if(ct!=null){
					return ct;
				}else{
					return contentTypeConf.getString("default");
				}
			}
		}
		//默认的情况下
		int idx = fileId.lastIndexOf('.');
		if(idx>0){
			String type = fileId.substring(idx+1);
			String ct = contentTypeConf.getString(type.toLowerCase());
			if(ct!=null){
				return ct;
			}else{
				return contentTypeConf.getString("default");
			}
		}else{
			return contentTypeConf.getString("default");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		int rotateValue = 0;
		int resizeWidth = 0;
		int resizeHeight = 0;
		int cropWidth = 0;
		int cropHeight = 0;
		int qualityValue = 100;
		String fmt = null;
		
		String fileId = req.getRequestURI();
		String rotate = req.getParameter("rotate");
		String resize = req.getParameter("resize");
		String quality = req.getParameter("quality");
		String crop = req.getParameter("crop");
		fmt = req.getParameter("fmt");
		if(fmt!=null){
			int idx = fmt.lastIndexOf('.');
			if(idx>0){
				fmt=fmt.substring(idx);
			}
			fmt = fmt.toLowerCase();
		}
		
		if(rotate!=null){
			rotateValue = Integer.parseInt(rotate);
		}
		
		if(quality!=null){
			qualityValue = Integer.parseInt(quality);
		}
		
		if(resize!=null){
			String[] rewh = resize.split("x");
			if(rewh.length==2){//长宽都指定
				resizeWidth = Integer.parseInt(rewh[0]);
				resizeHeight = Integer.parseInt(rewh[1]);
			}else{//未指定时方形
				resizeWidth = Integer.parseInt(resize);
				resizeHeight = resizeWidth;
			}
		}
		
		if(crop!=null){
			String[] crwh = crop.split("x");
			if(crwh.length==2){
				cropWidth = Integer.parseInt(crwh[0]);
				cropHeight = Integer.parseInt(crwh[1]);
			}else{
				cropWidth = Integer.parseInt(crop);
				cropHeight = cropWidth;
			}
		}
		
		//下载文件
		byte[] img = null;
		try{
			img = fastdfsClient.download(fileId);
		}catch(Exception e){
			logger.error("download "+fileId,e);
		}
		
		byte[] resultImg = null;
		//处理图片
		if(img!=null){
			resultImg = ImageUtils.processImage(fileId,img, rotateValue, qualityValue, resizeWidth, resizeHeight, cropWidth, cropHeight,fmt);
		}
		
		//content type处理与返回
		if(resultImg!=null){
			String contentType = this.getContentType(fileId, fmt, img, resultImg);
			resp.setContentType(contentType);	
			resp.setContentLength(resultImg.length);
			ServletOutputStream sos = resp.getOutputStream();
			sos.write(resultImg);
		}else{
			resp.setStatus(404);
		}
		resp.flushBuffer();
	}
	
	
}
