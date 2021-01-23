package demo;

public class StaticField {
	public static final String CONST = internal();

	private static String internal() {
		return "Hello";
	}
}