class Test {
	public static void main(String[] args) {
		int x;
		int y;
		Foo f;
		Bar b;
		boolean q;
		boolean w;
		boolean e;
		int[] arr;
		int i;

		e = q && false;
		x = 5 + 4;
		y = x;
		q = false;
		x = x + 33;
		arr = new int[x];
		arr[44] = 4 + 3;
		f = new Bar();
		f = new Foo();
		b = new Bar();
		
		i = f.start(x, 2, b);
		System.out.println(44);
		i = b.start(1, 2, b);
	}
}

class Foo {
	int x;
	public int start(int a, int b, Bar x) {
		int c;
		c = a + b;
		c = 4;
		x = new Bar();
		c = this.start(0, 0, new Bar());
		return a;
	}

}
class Baz extends Bar {}
class Bar extends Foo {
	int g;
	public int myFunc(int jk) {
		return x;
	}

	public int start(int c, int qwer, Bar tt) {
		int hh;
		return hh;
	}

}

