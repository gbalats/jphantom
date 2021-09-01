package org.clyze.jphantom;

import org.clyze.jphantom.access.ClassAccessStateMachine;
import org.clyze.jphantom.access.FieldAccessStateMachine;
import org.clyze.jphantom.access.MethodAccessStateMachine;
import org.clyze.jphantom.adapters.ClassPhantomExtractor;
import org.clyze.jphantom.hier.ClassHierarchies;
import org.clyze.jphantom.hier.ClassHierarchy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class Tests {
	@AfterEach
	void cleanup() {
		// Clear static caches between tests.
		Phantoms.refresh();
		ClassAccessStateMachine.refresh();
		FieldAccessStateMachine.refresh();
		MethodAccessStateMachine.refresh();
	}

	@Test
	void testFullJarNoPhantoms() throws IOException {
		JPhantom jPhantom = get("demo-full.jar");

		assertNotNull(jPhantom);

		jPhantom.run();
		Map<Type, byte[]> generated = jPhantom.getGenerated();

		assertEquals(0, generated.size(), "No classes missing");
	}

	@Test
	void testGeneratePhantoms() throws IOException {
		JPhantom jPhantom = get("demo.jar"); // Only "Main.class" in the jar
		assertNotNull(jPhantom);

		jPhantom.run();
		Map<Type, byte[]> generated = jPhantom.getGenerated();

		assertNotEquals(0, generated.size(), "Classes should have been generated");

		// class MyInterfaceImpl implements MyInterface
		// - Usage points towards the interface via "invokeinterface"
		// - Methods will be declared in the interface
		assertExists(generated, "demo/itf/single/MyInterfaceImpl");
		assertExists(generated, "demo/itf/single/MyInterface");
		assertImplements(generated, "demo/itf/single/MyInterfaceImpl", "demo/itf/single/MyInterface");
		assertDefinesMethod(generated, "demo/itf/single/MyInterface", "itfVoid", "()V");
		assertDefinesMethod(generated, "demo/itf/single/MyInterface", "itfBool", "(I)Z");
		assertDefinesMethod(generated, "demo/itf/single/MyInterface", "itfString", "()Ljava/lang/String;");

		// class MyInterfaceCopyImpl ... Never directly references parent interface, thus we do not find the implementation
		// - Usage points towards the impl via "invokevirtual"
		// - Methods will be declared in the impl class
		assertExists(generated, "demo/itf/single/MyInterfaceCopyImpl");
		assertNotExists(generated, "demo/itf/single/MyInterfaceCopy");
		assertNotImplements(generated, "demo/itf/single/MyInterfaceCopyImpl", "demo/itf/single/MyInterfaceCopy");
		assertDefinesMethod(generated, "demo/itf/single/MyInterfaceCopyImpl", "itfVoid", "()V");
		assertDefinesMethod(generated, "demo/itf/single/MyInterfaceCopyImpl", "itfBool", "(I)Z");
		assertDefinesMethod(generated, "demo/itf/single/MyInterfaceCopyImpl", "itfString", "()Ljava/lang/String;");

		// class Husky extends Dog
		// - Interface Quadruped never directly referenced
		assertExists(generated, "demo/itf/multi/Husky");
		assertExists(generated, "demo/itf/multi/Dog");
		assertNotExists(generated, "demo/itf/multi/Quadruped");
		assertExtends(generated, "demo/itf/multi/Husky", "demo/itf/multi/Dog");

		// class Person
		// - Interface Biped never directly referenced
		// - Biped#jog will be defined in Person due to "invokevirtual" use on Person.
		assertExists(generated, "demo/itf/multi/Person");
		assertNotExists(generated, "demo/itf/multi/Biped");
		assertDefinesMethod(generated, "demo/itf/multi/Person", "jog", "()V");

		// From the array loop at the bottom, Dog and Person both call "speak" from Animal
		assertExists(generated, "demo/itf/multi/Animal");
		assertImplements(generated, "demo/itf/multi/Dog", "demo/itf/multi/Animal");
		assertImplements(generated, "demo/itf/multi/Person", "demo/itf/multi/Animal");
		assertDefinesMethod(generated, "demo/itf/multi/Animal", "speak", "()V");

		// From "invokeStatic" the static getter should be generated
		assertExists(generated, "demo/StaticClass");
		assertDefinesMethod(generated, "demo/StaticClass", "get", "()Ldemo/StaticClass;");

		// From "getStatic" the static string should be generated
		assertExists(generated, "demo/StaticField");
		assertDefinesField(generated, "demo/StaticField", "CONST", "Ljava/lang/String;");

		// Annotations
		assertExists(generated, "demo/anno/InnerAnno");
		assertExists(generated, "demo/anno/InnerAnno$TheInner");
		assertExists(generated, "demo/anno/MyAnno");
		assertExists(generated, "demo/anno/MyFieldAnno");
		assertExists(generated, "demo/anno/MyMethodAnno");
	}

	private void assertExists(Map<Type, byte[]> generated, String key) {
		assertNotNull(generated.get(Type.getObjectType(key)));
	}

	private void assertNotExists(Map<Type, byte[]> generated, String key) {
		assertNull(generated.get(Type.getObjectType(key)));
	}

	private void assertImplements(Map<Type, byte[]> generated, String subType, String interfaceType) {
		Type itf = Type.getObjectType(interfaceType);
		Type impl = Type.getObjectType(subType);
		assertEquals(itf.getInternalName(), new ClassReader(generated.get(impl)).getInterfaces()[0]);
	}

	private void assertExtends(Map<Type, byte[]> generated, String subType, String superType) {
		Type supr = Type.getObjectType(superType);
		Type impl = Type.getObjectType(subType);
		assertEquals(supr.getInternalName(), new ClassReader(generated.get(impl)).getSuperName());
	}

	private void assertNotImplements(Map<Type, byte[]> generated, String subType, String interfaceType) {
		Type itf = Type.getObjectType(interfaceType);
		Type impl = Type.getObjectType(subType);
		String[] itfs = new ClassReader(generated.get(impl)).getInterfaces();
		if (itfs.length > 0)
			assertNotEquals(itf.getInternalName(), itfs[0]);
	}

	private void assertNotExtends(Map<Type, byte[]> generated, String subType, String superType) {
		Type supr = Type.getObjectType(superType);
		Type impl = Type.getObjectType(subType);
		assertNotEquals(supr.getInternalName(), new ClassReader(generated.get(impl)).getSuperName());
	}

	private void assertDefinesField(Map<Type, byte[]> generated, String owner, String name, String desc) {
		boolean[] found = new boolean[1];
		Type ownerType = Type.getObjectType(owner);
		new ClassReader(generated.get(ownerType)).accept(new ClassVisitor(Options.ASM_VER) {
			@Override
			public FieldVisitor visitField(int access, String fName, String fDesc, String sig, Object value) {
				if (name.equals(fName) && desc.equals(fDesc)) {
					found[0] = true;
				}
				return null;
			}
		}, 0);
		assertTrue(found[0]);
	}

	private void assertDefinesMethod(Map<Type, byte[]> generated, String owner, String name, String desc) {
		boolean[] found = new boolean[1];
		Type ownerType = Type.getObjectType(owner);
		new ClassReader(generated.get(ownerType)).accept(new ClassVisitor(Options.ASM_VER) {
			@Override
			public MethodVisitor visitMethod(int access, String mName, String mDesc, String sig, String[] ex) {
				if (name.equals(mName) && desc.equals(mDesc)) {
					found[0] = true;
				}
				return null;
			}
		}, 0);
		assertTrue(found[0]);
	}

	private JPhantom get(String resourceName) throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resourceUrl = classLoader.getResource(resourceName);
		if (resourceUrl == null)
			throw new IOException("Test resource does not exist: " + resourceName);
		File file = new File(resourceUrl.getFile());
		ClassHierarchy hierarchy = ClassHierarchies.fromJar(new JarFile(file));
		ClassMembers members = ClassMembers.fromJar(new JarFile(file), hierarchy);
		Map<Type, ClassNode> nodes = new HashMap<>();
		JarFile jarFile = new JarFile(file);
		try (JarInputStream jin = new JarInputStream(new FileInputStream(file))) {
			JarEntry entry;
			while ((entry = jin.getNextJarEntry()) != null) {
				if (entry.isDirectory())
					continue;
				if (!entry.getName().endsWith(".class"))
					continue;
				ClassReader reader = new ClassReader(jarFile.getInputStream(entry));
				reader.accept(new ClassPhantomExtractor(hierarchy, members), 0);
				ClassNode node = new ClassNode();
				reader.accept(node, 0);
				nodes.put(Type.getObjectType(node.name), node);
			}
		} finally {
			jarFile.close();
		}
		return new JPhantom(nodes, hierarchy, members);
	}

	static void setConst(Field field, Object newValue) throws Exception {
		field.setAccessible(true);
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		field.set(null, newValue);
	}
}
