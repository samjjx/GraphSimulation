package fileOpe;

import java.io.*;

public class readfile {
	
	public Object read(String filePath){		
		Object o = new Object();
		try{
			FileInputStream fis = new FileInputStream(filePath);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream ois = new ObjectInputStream(bis);
			o = ois.readObject();
			ois.close();			
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}	
		return o;		
	}
	
	
	public static void main(String args[]) {

		if (args.length != 1) {
			System.err.println("missing filename");
			System.exit(1);
		}
		try {
			FileInputStream fis = new FileInputStream(args[0]);
			BufferedInputStream bis =
				new BufferedInputStream(fis);
			int cnt = 0;
			int b;
			while ((b = bis.read()) != -1) {
				if (b == '\n')
					cnt++;
			}
			bis.close();
			System.out.println(cnt);
		}
		catch (IOException e) {
			System.err.println(e);
		}
		
	}
}
