package fileOpe;

import graph.cg_graph;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class graphLoad {
	
	public cg_graph load(String filePath){		
		cg_graph g = new cg_graph();
		try{
			FileInputStream fis = new FileInputStream(filePath);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream ois = new ObjectInputStream(bis);
			g = (cg_graph) ois.readObject();
			ois.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}	
		return g;		
	}
}
