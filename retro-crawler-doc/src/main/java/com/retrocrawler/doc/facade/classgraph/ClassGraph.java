// file: src/main/java/io/github/classgraph/ClassGraph.java
package com.retrocrawler.doc.facade.classgraph;

public class ClassGraph {

	public ClassGraph acceptPackages(final String basePackage) {
		return this;
	}

	public ClassGraph enableAnnotationInfo() {
		return this;
	}

	public ScanResult scan() {
		return new ScanResult();
	}
}