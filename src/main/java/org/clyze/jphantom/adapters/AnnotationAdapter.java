package org.clyze.jphantom.adapters;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

public class AnnotationAdapter extends InterfaceTransformer {
	private static final String ANNO_TYPE = "java/lang/annotation/Annotation";
	private static final String RETENTION = "Ljava/lang/annotation/Retention;";
	private static final String RETENTION_POLICY = "Ljava/lang/annotation/RetentionPolicy;";
	private boolean addRetention = true;

	public AnnotationAdapter(ClassVisitor cv) {
		super(cv);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (! Arrays.asList(interfaces).contains(ANNO_TYPE)) {
			String[] temp = new String[interfaces.length + 1];
			System.arraycopy(interfaces, 0, temp, 0, interfaces.length);
			temp[interfaces.length] = ANNO_TYPE;
			interfaces = temp;
		}
		super.visit(version, access | ACC_ANNOTATION, name, signature, superName, interfaces);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (RETENTION.equals(descriptor))
			addRetention = false;
		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public void visitEnd() {
		if (addRetention) {
			// Because this annotation is seen at the bytecode it must be persistent
			visitAnnotation(RETENTION, true).visitEnum("value", RETENTION_POLICY, "RUNTIME");
		}
		super.visitEnd();
	}
}
