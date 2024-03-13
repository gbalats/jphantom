package demo.itf.multi;

public abstract class Dog implements Animal, Quadruped {
	@Override
	public void walk() {
		System.out.println("Walking");
	}
}
