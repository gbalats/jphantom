package org.clyze.jphantom.adapters;

import org.clyze.jphantom.Options;
import org.objectweb.asm.ClassVisitor;

public class InnerClassAdapter extends ClassVisitor {
	private final String innerType;
	private final int access;

	public InnerClassAdapter(ClassVisitor cv, String innerType, int access) {
		super(Options.ASM_VER, cv);
		this.innerType = innerType;
		this.access = access;
	}

	@Override
	public void visitEnd() {
		int i = innerType.lastIndexOf('$');
		String outerType = innerType.substring(0, i);
		String innerSimple = innerType.substring(i + 1);
		visitInnerClass(innerType, outerType, innerSimple, access);
		super.visitEnd();
	}
}
