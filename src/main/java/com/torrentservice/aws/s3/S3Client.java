package com.torrentservice.aws.s3;

import java.io.File;
import java.util.List;

public interface S3Client {

	public void putFile(String key, File file);

	public File getFile(String key);
	
	public List<String> listAllFiles();

}
