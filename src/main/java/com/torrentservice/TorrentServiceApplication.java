package com.torrentservice;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.torrentservice.aws.s3.AWSProperties;

@SpringBootApplication
public class TorrentServiceApplication {

	@Autowired
	private AWSProperties awsProperties;

	public static void main(String[] args) {
		SpringApplication.run(TorrentServiceApplication.class, args);
	}

	@PostConstruct
	public void init() {
		System.out.println("Bucket: " + awsProperties.getBucket());
	}
}
