package demo.itf.multi;

public class Person implements Animal, Biped {
	@Override
	public void speak() {
		System.out.println("Hello");
	}

	@Override
	public void jog() {
		System.out.println("Walk");
	}
}
