// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.api;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AssertFullScanData {
    

    private static final Logger LOG = LoggerFactory.getLogger(AssertFullScanData.class);

	List<FullScanDataElement> fullDataScanElements;
	private File file;

	public AssertFullScanData(File file) {
		if (file==null) {
			throw new IllegalArgumentException("file may not be null");
		}
		if (!file.exists()) {
			throw new IllegalStateException("File does not exist:"+file);
		}
		this.file= file;
		this.fullDataScanElements=readZipfile(file);
	}

	public AssertFullScanData dumpDownloadFilePath() {
	    LOG.info("Full scan downloaded: {}", file.getAbsolutePath());
	    return this;
	}
	
	public AssertFullScanData containsFiles(int amount) {
		assertEquals(amount,fullDataScanElements.size());
		return this;
	}

	public AssertFullScanData containsFile(String name) {
		assertFile(name, fullDataScanElements,ScanMode.EQUAL);
		return this;
	}

	public FullScanDataElement resolveFile(String name) {
		return assertFile(name, fullDataScanElements,ScanMode.EQUAL);
	}

	public FullScanDataElement resolveFileStartingWith(String name) {
		return assertFile(name, fullDataScanElements,ScanMode.STARTSWITH);
	}

	private FullScanDataElement assertFile(String name,List<FullScanDataElement> list,ScanMode mode) {
		for (FullScanDataElement element: list) {
			if (mode==ScanMode.EQUAL) {
				if(name.contentEquals(element.fileName)) {
					return element;
				}
			}else if (mode==ScanMode.STARTSWITH) {
				if(element.fileName.startsWith(name)) {
					return element;
				}
			}
		}
		fail("Not found file name "+name+" inside list:\n"+list);
		return null;
	}

	private List<FullScanDataElement> readZipfile(File file){
		List<FullScanDataElement> list = new ArrayList<>();
		try(ZipFile zipFile = new ZipFile(file)){
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				InputStream stream = zipFile.getInputStream(entry);
				try(BufferedReader br = new BufferedReader(new InputStreamReader(stream))){
					String line = null;
					StringBuilder sb = new StringBuilder();
					while ( (line=br.readLine())!=null) {
						sb.append(line);
						sb.append("\n");
					}
					FullScanDataElement d = new FullScanDataElement();
					d.fileName = entry.getName();
					d.content=sb.toString();
					list.add(d);
				}
			}
		}catch(Exception e) {
			throw new AssertionError("Cannot read zip file:"+file,e);
		}
		return list;
	}

	public class FullScanDataElement{
		public String fileName;
		public String content;

		@Override
		public String toString() {
			return "FullScanDataElement [fileName=" + fileName + "]";
		}
	}

	@Override
	public String toString() {
		return "AssertFullScanData [file=" + file + ", fullDataScanElements=" + fullDataScanElements + "]";
	}

	private enum ScanMode{
		EQUAL,
		STARTSWITH
	}
}
