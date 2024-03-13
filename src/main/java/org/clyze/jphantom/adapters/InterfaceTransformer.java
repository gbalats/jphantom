package org.clyze.jphantom.adapters;

import org.clyze.jphantom.Options;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

public class InterfaceTransformer extends ClassVisitor implements Opcodes {
	public InterfaceTransformer(ClassVisitor cv) {
		super(Options.ASM_VER, cv);
	}

	@Override
	public void visit(int version, int access,
					  String name, String signature,
					  String superName, String[] interfaces) {

		super.visit(
				version,
				access | ACC_INTERFACE | ACC_ABSTRACT,
				name,
				signature,
				superName,
				interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access | ACC_ABSTRACT, name, descriptor, signature, exceptions);
		return new MethodRemover(mv);
	}

	public static class MethodRemover extends MethodVisitor {
		public MethodRemover(MethodVisitor mv) {
			super(Options.ASM_VER, mv);
		}

		@Override
		public void visitCode() {
			// Do not visit
		}

		@Override
		public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
			// Do not visit
		}

		@Override
		public void visitInsn(int opcode) {
			// Do not visit
		}

		@Override
		public void visitIntInsn(int opcode, int operand) {
			// Do not visittIntInsn(opcode, operand);
		}

		@Override
		public void visitVarInsn(int opcode, int var) {
			// Do not visit
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			// Do not visit
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
			// Do not visit
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
			// Do not visit
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			// Do not visit
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
			// Do not visit
		}

		@Override
		public void visitJumpInsn(int opcode, Label label) {
			// Do not visit
		}

		@Override
		public void visitLabel(Label label) {
			// Do not visit
		}

		@Override
		public void visitLdcInsn(Object value) {
			// Do not visit
		}

		@Override
		public void visitIincInsn(int var, int increment) {
			// Do not visit
		}

		@Override
		public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
			// Do not visit
		}

		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			// Do not visit
		}

		@Override
		public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
			// Do not visit
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			// Do not visit
			return null;
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
			// Do not visit
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			// Do not visit
			return null;
		}

		@Override
		public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
			// Do not visit
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
			// Do not visit
			return null;
		}

		@Override
		public void visitLineNumber(int line, Label start) {
			// Do not visit
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			// Do not visit
		}
	}
}
