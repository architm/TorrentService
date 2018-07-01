package com.torrentservice.aws.s3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Component
public class S3ClientImpl implements S3Client {

	private AmazonS3 amazonS3;

	@Autowired
	private AWSProperties awsProperties;

	public S3ClientImpl() {
		this.amazonS3 = AmazonS3ClientBuilder.defaultClient();
	}

	@Override
	public void putFile(String key, File file) {
		try {
			amazonS3.putObject(awsProperties.getBucket(), key, file);
		} catch (AmazonServiceException e) {
			System.err.println(e.getErrorMessage());
		}

	}

	@Override
	public File getFile(String key) {
		try {
			S3Object o = amazonS3.getObject(awsProperties.getBucket(), key);
			S3ObjectInputStream s3is = o.getObjectContent();
			File file = new File(key);
			FileOutputStream fos = new FileOutputStream(file);
			byte[] read_buf = new byte[1024];
			int read_len = 0;
			while ((read_len = s3is.read(read_buf)) > 0) {
				fos.write(read_buf, 0, read_len);
			}
			s3is.close();
			fos.close();
			return file;
		} catch (AmazonServiceException e) {
			System.err.println(e.getErrorMessage());
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}

	@Override
	public List<String> listAllFiles() {
		List<String> listAllFiles = new ArrayList<>();
		ListObjectsV2Result result = amazonS3.listObjectsV2(awsProperties.getBucket());
		List<S3ObjectSummary> objects = result.getObjectSummaries();
		for (S3ObjectSummary os : objects) {
			listAllFiles.add(os.getKey());
		}
		return listAllFiles;
	}
}
