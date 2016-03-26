package fileOpe;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class writefile {
	
	public void writeobject(String filePath, Object o){
		double start3;
		double end3;
		start3 = System.currentTimeMillis();
		try{
			FileOutputStream fos = new FileOutputStream(filePath, true);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(o);
			oos.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		end3 = System.currentTimeMillis();
		System.out.println("storage time:"+ (end3-start3));
	}
}
