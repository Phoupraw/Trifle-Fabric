package ph.mcmod.counter;

import sun.misc.Unsafe;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * @version 0.6.0
 */
@SuppressWarnings("unused")
public final class JavaUtil {
/**
 * @see #skip(Iterator, Predicate)
 */
public static <T> Iterable<T> skip(Iterable<T> delegate, Predicate<T> toSkip) {
	return () -> skip(delegate.iterator(), toSkip);
}

/**
 * 包装{@link Iterator}，使其跳过指定的元素
 *
 * @param delegate 要包装的{@link Iterator}
 * @param toSkip   如果元素满足这个条件，则跳过
 * @return 被包装的 {@link Iterator}
 */
public static <T> Iterator<T> skip(Iterator<T> delegate, Predicate<T> toSkip) {
	return new Iterator<>() {
		private T theNext;
		private boolean present;

		@Override
		public boolean hasNext() {
			if (present)
				return true;
			do {
				if (!delegate.hasNext())
					return false;
				theNext = delegate.next();
			} while (toSkip.test(theNext));
			present = true;
			return true;
		}

		@Override
		public T next() {
			if (hasNext()) {
				present = false;
				return theNext;
			}
			throw new NoSuchElementException();
		}
	};
}

public static float[] hsbFromRGB(int rgb, float[] hsb) {
	return Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsb);
}

/**
 * 判断一个字符串是否全部由数字组成
 *
 * @param s 字符串
 * @return 是全部由数字组成；如果是空字符串，仍然返回{@code true}
 */
public static boolean isDigits(String s) {
	for (int i = 0; i < s.length(); i++) {
		if (!Character.isDigit(s.charAt(i)))
			return false;
	}
	return true;
}

/**
 * 深度复制数组
 *
 * @param array 被复制的数组
 * @param <T>   数组元素类型，可以也是一个数组
 * @return 复制出的数组
 */
@SuppressWarnings("unchecked")
public static <T> T[] deepCopyOf(T[] array) {
	T[] newArray;
	if (array instanceof Object[][]) {
		newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length);
		for (int i = 0; i < newArray.length; i++) {
			newArray[i] = (T) deepCopyOf((Object[]) array[i]);
		}
	} else {
		newArray = Arrays.copyOf(array, array.length);
	}
	return newArray;
}

/**
 * 对非数组或列表的对象排序
 *
 * @param getter 相当于{@link List#get(int)}
 * @param setter 相当于{@link List#set(int, Object)}
 * @param size   相当于{@link List#size()}
 * @param <T>    元素类型
 */
public static <T extends Comparable<T>> void sort(IntFunction<? extends T> getter, BiConsumer<Integer, ? super T> setter, int size) {
	sort(getter, setter, size, Comparable::compareTo);
}

/**
 * 对非数组或列表的对象排序
 *
 * @param getter     相当于{@link List#get(int)}
 * @param setter     相当于{@link List#set(int, Object)}
 * @param size       相当于{@link List#size()}
 * @param comparator 比较器，为{@link List#sort(Comparator)}的参数
 * @param <T>        元素类型
 */
public static <T> void sort(IntFunction<? extends T> getter, BiConsumer<Integer, ? super T> setter, int size, Comparator<? super T> comparator) {
	List<T> list = new ArrayList<>(size);
	for (int i = 0; i < size; i++)
		list.add(getter.apply(i));
	list.sort(comparator);
	for (int i = 0; i < size; i++)
		setter.accept(i, list.get(i));
}

/**
 * @see #contact(Iterator)
 */
@SafeVarargs
public static <T> Iterator<T> contact(Iterator<T>... iterators) {
	return contact(Arrays.stream(iterators).iterator());
}

/**
 * 将多个迭代器连接成一个迭代器
 *
 * @return 连接的迭代器
 */
public static <T> Iterator<T> contact(Iterator<Iterator<T>> iteratorIterator) {
	if (!iteratorIterator.hasNext())
		return Collections.emptyIterator();
	return new Iterator<>() {
		Iterator<T> now = iteratorIterator.next();

		@Override
		public boolean hasNext() {
			while (!now.hasNext()) {
				if (iteratorIterator.hasNext()) {
					now = iteratorIterator.next();
				} else {
					return false;
				}
			}
			return true;
		}

		@Override
		public T next() {
			if (hasNext())
				return now.next();
			throw new NoSuchElementException();
		}
	};
}

/**
 * 考虑了数组和迭代器的toString，比{@link Objects#toString(Object)}考虑了更多可能
 */
public static String toString(Object o) {
	if (o instanceof Iterable<?> iterable)
		return toString(iterable.iterator());
	else if (o instanceof Iterator<?> iterator)
		return toString(iterator);
	else if (o instanceof Object[] ar)
		return Arrays.toString(ar);
	else if (o instanceof byte[] ar)
		return Arrays.toString(ar);
	else if (o instanceof short[] ar)
		return Arrays.toString(ar);
	else if (o instanceof int[] ar)
		return Arrays.toString(ar);
	else if (o instanceof long[] ar)
		return Arrays.toString(ar);
	else if (o instanceof float[] ar)
		return Arrays.toString(ar);
	else if (o instanceof double[] ar)
		return Arrays.toString(ar);
	else if (o instanceof boolean[] ar)
		return Arrays.toString(ar);
	else if (o instanceof char[] ar)
		return Arrays.toString(ar);
	else
		return String.valueOf(o);
}

/**
 * 将迭代器转化为{@link List}并调用{@link List#toString()}
 */
public static String toString(Iterator<?> iterator) {
	return collect(iterator).toString();
}

/**
 * 将{@link Iterator}转换为{@link Iterable}
 */
public static <T> List<T> collect(Iterator<T> iterator) {
	return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false).toList();
}

/**
 * 计算乘方
 *
 * @param a 底数
 * @param b 指数
 * @return 幂
 */
public static BigInteger pow(BigInteger a, BigInteger b) {
	try {
		return a.pow(b.intValueExact());//因为java自带的乘方函数更快，所以先尝试用java自带的乘方函数
	} catch (ArithmeticException e) {//如果b的范围超出了int，那就不能用java自带的乘方函数，改为自己循环
		BigInteger r = a;
		while (b.compareTo(BigInteger.ONE) > 0) {
			r = r.multiply(a);
			b = b.subtract(BigInteger.ONE);
		}
		return r;
	}
}

/**
 * 序列化对象
 *
 * @throws IOException 由{@link ObjectOutputStream}直接抛出
 */
public static byte[] serialize(Serializable serializable) throws IOException {
	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	new ObjectOutputStream(byteArrayOutputStream).writeObject(serializable);
	return byteArrayOutputStream.toByteArray();
}

/**
 * 反序列化对象
 *
 * @throws IOException            由{@link ObjectOutputStream}直接抛出
 * @throws ClassNotFoundException 由{@link ObjectOutputStream}直接抛出
 */
public static <T extends Serializable> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException, ClassCastException {
	return (T) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
}

/**
 * 利用反射和{@link Unsafe}强行浅复制一个对象。不要用来复制Java内置类的对象，否则一定复制失败！
 *
 * @param source 被复制的对象
 * @return 复制的对象。如果{@code source}为{@code null}或复制过程中捕获到异常，则返回{@code null}
 */
public static <T> T clone(T source) {
	T r = null;
	try {
		r = cloneNew(source);
	} catch (NoSuchFieldException | InstantiationException | IllegalAccessException | InaccessibleObjectException ignored) {
	}
	return r;
}

/**
 * 用{@link Unsafe}创建一个空对象，用反射把{@code source}的非静态字段赋值给它的，包括父类字段。
 *
 * @return 复制出的对象
 * @throws NoSuchFieldException        用反射获取{@link Unsafe#theUnsafe}时可能抛出。
 * @throws InstantiationException      见{@link Unsafe#allocateInstance(Class)}
 * @throws IllegalAccessException      用反射获取{@link Unsafe#theUnsafe}时可能抛出，以及见{@link #cloneTo(Object, Object, Class)}
 * @throws InaccessibleObjectException 见{@link #cloneTo(Object, Object, Class)}
 */
public static <T> T cloneNew(T source) throws NoSuchFieldException, InstantiationException, IllegalAccessException, InaccessibleObjectException {
	if (source == null)
		return null;
	Class<?> cls = source.getClass();
	Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
	unsafeField.setAccessible(true);
	Unsafe unsafe = (Unsafe) unsafeField.get(null);
	Object r = unsafe.allocateInstance(cls);
	cloneTo(source, r, cls);
	return (T) r;//FIXME 怎么消除警告？
}

/**
 * 用反射把{@code source}的非静态字段赋值给{@code target}的，包括父类字段。
 *
 * @param cls 复制这个类及其所有父类的字段
 * @throws IllegalAccessException      {@link Field#get(Object)}、{@link Field#set(Object, Object)}
 * @throws InaccessibleObjectException 通常在试图访问Java内部类的对象的非公开字段时时抛出，见{@link Field#setAccessible(boolean)}
 */
public static void cloneTo(Object source, Object target, Class<?> cls) throws IllegalAccessException, InaccessibleObjectException {
	Field[] fields = cls.getDeclaredFields();
	for (Field field : fields) {
		if (Modifier.isStatic(field.getModifiers()))
			continue;
		field.setAccessible(true);
		field.set(target, field.get(source));
	}
	if (cls.getSuperclass() != Object.class)
		cloneTo(source, target, cls.getSuperclass());
}

public static double rand(Random random, double limit) {
	return rand(random, -Math.abs(limit), Math.abs(limit));
}

public static double rand(Random random, double min, double max) {
	return min + random.nextDouble() * (max - min);
}

/**
 * 复制文件
 *
 * @param source 源文件
 * @param target 目标文件（直接写入目标文件，而不是当做文件夹在其下新创建文件）
 * @return 成功复制
 */
public static boolean copy(File source, File target) {
	try {
		FileChannel input = new FileInputStream(source).getChannel();
		FileChannel output = new FileOutputStream(target).getChannel();
		output.transferFrom(input, 0, input.size());
		return true;
	} catch (IOException e) {
		e.printStackTrace();
		return false;
	}
}

public interface CloneMethod {
	Object clone() throws CloneNotSupportedException;
}

public static <T extends Cloneable> T clone(CloneMethod cloneMethod) {
	try {
		return (T) cloneMethod.clone();
	} catch (CloneNotSupportedException e) {
		e.printStackTrace();
		return null;
	}
}

public static String decode(String coded) {
	StringBuilder sb = new StringBuilder();
	Matcher matcher = Pattern.compile("\\\\u[\\da-fA-F]{4}").matcher(coded);
	int p = 0;
	while (matcher.find()) {
		sb.append(coded, p, matcher.start());
		p = matcher.end();
		sb.append((char) Integer.parseInt(coded, matcher.start() + 2, matcher.end(), 16));
	}
	return sb.toString();
}

/**
 * 用反射比较两个对象的每个字段是否相等。
 * 如果在读取字段时发生了{@link IllegalAccessException}，则忽略对此字段的比较。
 * 不要用来比较Java内置类的对象，否则一定失败！
 *
 * @return 无论两个对象的类型如何，只要它们的字段的数量和类型完全相同，且每个字段都相等，则返回{@code true}；如果有任意字段不相等，则返回{@code false}；如果捕获到了{@link InaccessibleObjectException}，则返回{@code null}。
 */
public static Boolean equal(Object o1, Object o2) {//FIXME 没有比较父类字段
	if (o1 == o2)
		return true;
	if (o1 == null ^ o2 == null)
		return false;
	Field[] fields = o1.getClass().getDeclaredFields();
	if (!Arrays.equals(fields, o2.getClass().getDeclaredFields()))
		return false;
	for (Field field : fields) {
		try {
			field.setAccessible(true);
			Object v1 = field.get(o1);
			Object v2 = field.get(o2);
			if (!Objects.equals(v1, v2))
				return false;
		} catch (InaccessibleObjectException e) {
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	return true;
}

public static BigInteger factorial(BigInteger x) throws ArithmeticException {
	if (x.signum()<0)
		throw new ArithmeticException("x < 0");
	BigInteger r = BigInteger.ONE;
	for (BigInteger i = x; i.compareTo(BigInteger.TWO)>=0; i=i.subtract(BigInteger.ONE))
		r = r.multiply(i);
	System.out.println("x = " + x + ", r = "+r);
	return r;
}

public static BigInteger combinations(BigInteger n, BigInteger m) {
	return permutations(n, m).divide( permutations(m, m));
}
public static BigInteger C(BigInteger n, BigInteger m) {
	return combinations(n,m);
}
public static BigInteger permutations(BigInteger n, BigInteger m) {
	return factorial(n).divide(factorial(n.subtract(m)));
}
public static float rand(Random random, float limit) {
	return (float) rand(random, -Math.abs(limit), Math.abs(limit));
}
private JavaUtil() {
}

}
