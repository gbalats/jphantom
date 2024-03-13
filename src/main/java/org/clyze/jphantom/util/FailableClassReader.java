package org.clyze.jphantom.util;

import org.clyze.jphantom.Options;
import org.clyze.jphantom.exc.IllegalBytecodeException;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Class reader implementation that allows muting of thrown {@link IllegalBytecodeException}
 * if {@link Options#isSoftFail()} is active.
 */
public class FailableClassReader extends ClassReader {
	public FailableClassReader(InputStream inputStream) throws IOException {
		super(inputStream);
	}

	public FailableClassReader(byte[] classFile) {
		super(classFile);
	}

	@Override
	public void accept(ClassVisitor classVisitor, Attribute[] attributePrototypes, int parsingOptions) {
		try {
			super.accept(classVisitor, attributePrototypes, parsingOptions);
		} catch (IllegalBytecodeException exception) {
			if (!Options.V().isSoftFail())
				throw exception;
		}
	}
}
