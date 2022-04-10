package com.example.sftp;

import com.example.sftp.config.SftpConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.sftp.session.SftpFileInfo;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@SpringBootTest
public class SftpExampleApplicationTests {

	@Autowired
	SftpConfig.UploadGateway gateway;

	@Autowired
	ResourceLoader resourceLoader;

	private static final String targetPath ="/Users/hyeongjong/aaaa/";

	@Test
	void uploadOneFileTest() {
		// given
		File tempFile = null;
		try {
			tempFile = new File(resourceLoader.getResource("classpath:static/MOCK_DATA1.json").getURI());
		} catch (IOException e) {
			Assertions.fail(e);
		}

		// when
		String now = OffsetDateTime.now().toString();
		String repliedPath = gateway.sendFileToSftp(tempFile, targetPath, now + ".json");

		// then
		Assertions.assertEquals(targetPath + now + ".json", repliedPath);
	}

	@Test
	void uploadFilesTest() {
		// given
		File tempFile1 = null;
		File tempFile2 = null;
		try {
			tempFile1 = new File(resourceLoader.getResource("classpath:static/MOCK_DATA1.json").getURI());
			tempFile2 = new File(resourceLoader.getResource("classpath:static/MOCK_DATA2.json").getURI());
		} catch (IOException e) {
			Assertions.fail(e);
		}

		// when
		String now1 = OffsetDateTime.now().toString();
		String repliedPath1 = gateway.sendFileToSftp(tempFile1, targetPath, now1 + ".json");
		String now2 = OffsetDateTime.now().toString();
		String repliedPath2 = gateway.sendFileToSftp(tempFile2, targetPath, now2 + ".json");

		List<SftpFileInfo> fileList = gateway.listFile(targetPath);

		// then
		Assertions.assertEquals(targetPath + now1 + ".json", repliedPath1);
		Assertions.assertEquals(targetPath + now2 + ".json", repliedPath2);
	}

	@Test
	void listTest() {
		List<SftpFileInfo> fileList = gateway.listFile(targetPath);
		for (SftpFileInfo file : fileList) {
			System.out.println("file = " + file.getFilename());
		}
	}

	@Test
	void uploadAndListTest() {
		// given
		File tempFile1 = null;
		File tempFile2 = null;
		try {
			tempFile1 = new File(resourceLoader.getResource("classpath:static/MOCK_DATA1.json").getURI());
			tempFile2 = new File(resourceLoader.getResource("classpath:static/MOCK_DATA2.json").getURI());
		} catch (IOException e) {
			Assertions.fail(e);
		}

		// when
		String now1 = OffsetDateTime.now().toString();
		gateway.sendFileToSftp(tempFile1, targetPath, now1 + ".json");
		String now2 = OffsetDateTime.now().toString();
		gateway.sendFileToSftp(tempFile2, targetPath, now2 + ".json");

		// then
		List<SftpFileInfo> fileList = gateway.listFile(targetPath);
		int count = 0;
		for (SftpFileInfo file : fileList) {
			if (file.getFilename().equals(now1 + ".json") || file.getFilename().equals(now2 + ".json")) {
				count++;
			}
		}

		Assertions.assertEquals(2, count);
	}
}