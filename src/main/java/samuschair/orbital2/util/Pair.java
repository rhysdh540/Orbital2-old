package samuschair.orbital2.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor(staticName = "of")
public class Pair<F, S> {
	public F first;
	public S second;

	public static <T> Pair<T, T> of(T t) {
		return of(t, t);
	}
}
