package com.torrentservice.controllers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.torrentservice.aws.s3.S3Client;

import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.metainfo.TorrentFile;
import bt.runtime.BtClient;

@Controller
public class TorrentServiceController {

	@Autowired
	S3Client s3Client;

	/**
	 * executed with Bit-torrent client: https://github.com/atomashpolskiy/bt
	 */
	@RequestMapping(value = "/")
	public void torrentTask() {

		List<String> listAllFilesDownloaded = new ArrayList<>();
		StringBuilder s3keyName = new StringBuilder();
		String downloadId = RandomStringUtils.randomAlphabetic(10);
		String torrentDirectory = "/Users/archit/Documents/torrentstuff/" + downloadId;
		String magnet = "magnet:?xt=urn:btih:55e5fe0436cd60c2f0cfc210b5e124cc1505f38d&dn=Game.of.Thrones.S07E04.The.Spoils.of.War.360p.WEB-DL&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Fzer0day.ch%3A1337&tr=udp%3A%2F%2Fopen.demonii.com%3A1337&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Fexodus.desync.com%3A6969";
		Storage storage = new FileSystemStorage(new File(torrentDirectory).toPath());
		DHTModule dhtModule = new DHTModule(new DHTConfig() {
			@Override
			public boolean shouldUseRouterBootstrap() {
				return true;
			}
		});
		BtClient btClient = Bt.client().magnet(magnet).storage(storage).stopWhenDownloaded()
				.afterTorrentFetched(torrent -> {
					String fileName = torrent.getFiles().get(0).getPathElements().get(0);
					System.out.println("Using filename for downloadId: " + fileName);
					TorrentFile maxValueTorrentFile = torrent.getFiles().stream()
							.max(Comparator.comparing(TorrentFile::getSize)).get();
					s3keyName.append(maxValueTorrentFile.getPathElements().get(0));
				}).autoLoadModules().module(dhtModule).build();

		btClient.startAsync(state -> {
			int lastByte = 0;
			boolean filesProcessed = false;
			while (state.getPiecesRemaining() != 0) {
				if (lastByte != state.getPiecesRemaining()) {
					System.out.println("Downloading: "
							+ Math.abs(
									((double) state.getPiecesRemaining() / (double) state.getPiecesTotal()) * 100 - 100)
							+ "%");
					lastByte = state.getPiecesRemaining();

				}
			}
			if (state.getPiecesRemaining() == 0 && !filesProcessed) {
				System.out.println("Done!");
				btClient.stop();
				listFilesAndFilesSubDirectories(torrentDirectory, listAllFilesDownloaded);
				for (String filePath : listAllFilesDownloaded) {
					File file = new File(filePath);
					System.out.println("Uploading file: " + file.getName());
					s3Client.putFile(s3keyName.append("/").append(file.getName()).toString(), file);
					System.out.println("Uploaded file: " + file.getName());
				}
				filesProcessed = true;
			}

		}, 1000).join();
	}

	private static URL toUrl(File file) {
		try {
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Unexpected error", e);
		}
	}

	/**
	 * List all files from a directory and its subdirectories
	 * 
	 * @param directoryName
	 *            to be listed
	 */
	public void listFilesAndFilesSubDirectories(String directoryName, List<String> listAllFilesDownloaded) {
		File directory = new File(directoryName);
		// get all the files from a directory
		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (file.isFile()) {
				listAllFilesDownloaded.add(file.getAbsolutePath());
			} else if (file.isDirectory()) {
				listFilesAndFilesSubDirectories(file.getAbsolutePath(), listAllFilesDownloaded);
			}
		}
	}

}