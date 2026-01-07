// file: src/main/java/io/github/classgraph/ScanResult.java
package com.retrocrawler.doc.facade.classgraph;

import java.util.List;

public class ScanResult implements AutoCloseable {

	public List<ClassInfo> getAllClasses() {
		return List.of();
	}

	@Override
	public void close() {
		// no-op
	}
}