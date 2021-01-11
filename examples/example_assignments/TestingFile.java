package example_assignments;

public class TestingFile {

	int[] test = {2, 4, 1, 0, 5};

	int check() {
		int index = 0;
		int lowest = 0;
		for (int i = 0; i < 5; i++) {
			if (this.test[i] > 10) {
				return 0;
			}
		}
		
		return index;
	}
	
	/*
	 * 
		    // add this
		    if (array.length == 0)
		        return -1;
		
		    int index = 0;
		    int min = array[index];
		
		    for (int i = 1; i < array.length; i++){
		        if (array[i] <= min){
		        min = array[i];
		        index = i;
		        }
		    }
		    return index;
		}
	 */
	
	
	
	void start() {
		int check = check();
		System.out.println(check);
	}

	
	public static void main(String[] args) {
		
		new TestingFile().start();
		
		

	}

}
