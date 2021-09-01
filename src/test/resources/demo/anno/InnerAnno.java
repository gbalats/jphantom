package demo.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InnerAnno {
	@Retention(RetentionPolicy.RUNTIME)
	@interface TheInner {
		String value();
	}
}