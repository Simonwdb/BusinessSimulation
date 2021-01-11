package example_assignments;

public class TestingFile {

	int[] test = {8, 4, 6, 7, 9};

	boolean check() {
		for (int i = 0; i < 5; i++) {
			if (this.test[i] > 10) {
				return true;
			}
		}
		return false;
	}
	
	void start() {
		boolean check = check();
		System.out.println(check);
	}
	
	public static void main(String[] args) {
		
		new TestingFile().start();
		
		

	}

}
