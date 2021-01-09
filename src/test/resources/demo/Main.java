package demo;

import demo.anno.MyAnno;
import demo.anno.MyFieldAnno;
import demo.anno.MyMethodAnno;
import demo.itf.multi.Animal;
import demo.itf.multi.Dog;
import demo.itf.multi.Husky;
import demo.itf.multi.Person;
import demo.itf.single.MyInterface;
import demo.itf.single.MyInterfaceCopyImpl;
import demo.itf.single.MyInterfaceImpl;

import java.util.Arrays;
import java.util.List;

@MyAnno("Anywhere anno")
public class Main {
	@MyFieldAnno("Field anno")
	private static final Main instance = new Main();

	public static void main(String[] args) {
		instance.invokeMyInterfaceImpl();
		instance.invokeMyInterfaceCopyImpl();
		instance.invokeDog();
		instance.invokeStatic();
		instance.getStatic();
	}
	
	private void getStatic() {
		String message = StaticField.CONST;
		System.out.println(message);
	}

	private void invokeStatic() {
		StaticClass instance = StaticClass.get();
		System.out.println(instance);
	}

	private void invokeDog() {
		Dog dog = new Husky();
		dog.walk();

		Person person = new Person();
		person.jog();

		List<Animal> animals = Arrays.asList(dog, person);
		for (Animal animal : animals) {
			animal.speak();;
		}
	}

	@MyMethodAnno("Method anno")
	private void invokeMyInterfaceImpl() {
		MyInterface inter = new MyInterfaceImpl();
		inter.itfVoid();
		String text = inter.itfString();
		boolean status = inter.itfBool(110);
		if (status) {
			System.out.println(text);
		}
	}

	private void invokeMyInterfaceCopyImpl() {
		MyInterfaceCopyImpl inter = new MyInterfaceCopyImpl();
		inter.itfVoid();
		String text = inter.itfString();
		boolean status = inter.itfBool(110);
		if (status) {
			System.out.println(text);
		}
	}
}
