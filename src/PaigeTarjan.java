
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import fileOpe.graphLoad;

import graph.edge;
import graph.cg_graph;
import graph.ranGraph.grpattr;


/**
 * this class carries out the Relational coarsest partition. 
 * @author jiangxianlin
 *
 */

public class PaigeTarjan {

	/**
	 * simulation check : check whether n1 is similar to n2.
	 * for each child n2' of n2, there should exist at least one child of n1 similar to n2'
	 * @param n1
	 * @param n2
	 * @param graph
	 * @return
	 */
	public boolean simCheck(String n1, String n2, grpattr G){
		boolean result = false;
		String n1Label = G.attr.get(n1);
		String n2Label = G.attr.get(n2);
		if(n1Label.equals(n2Label)){

			if(G.graph.getChildren(n1)==null && G.graph.getChildren(n2)==null){
				result = true;
			}
			else{
				for(edge e2:G.graph.outgoingEdgesOf(n2)){
					String child2 = G.graph.getEdgeTarget(e2);
					for(edge e1:G.graph.outgoingEdgesOf(n1)){
						String child1 = G.graph.getEdgeTarget(e1);
						if(this.simCheck(child1, child2, G)){
							result = true;
							break;
						}
					}
					if(result==false){
						break;
					}
				}
			}
		}
		return result;
	}	


	/**
	 *  compute E^-1
	 * @param S : subset of U
	 * @param nset : Universal elements
	 * @return pMap : mapping between node and its corresponding parent nodes
	 */
	public HashSet<String> cmpRelation(HashSet<String> S, HashSet<String> nset, HashMap<String, Vector<String>> pMap){
		HashSet<String> Ereverse = new HashSet<String>();
		for(String x:S){
			Vector<String> pset = pMap.get(x);
			if(pset!=null){
				Ereverse.addAll(pset);
			}
		}
		Ereverse.retainAll(nset);
		return Ereverse;
	}


	/**
	 * split the initial partition
	 * each block is split into two parts
	 * @param S : subset of U
	 * @param Q : initial partition
	 * @param nset : Universal elements
	 * @param pMap : mapping between node and its corresponding parent nodes
	 */
	public void split(HashSet<String> S, HashSet<HashSet<String>> Q, HashSet<String> nset, HashMap<String, Vector<String>> pMap){
		HashSet<String> Ereverse = this.cmpRelation(S, nset, pMap);
		HashSet<HashSet<String>> tmp = new HashSet<HashSet<String>>();
		for(HashSet<String> B:Q){
			HashSet<String> B1 = new HashSet<String>();
			HashSet<String> B2 = new HashSet<String>();
			B1.addAll(B);
			B2.addAll(B);
			B1.retainAll(Ereverse);
			B2.removeAll(Ereverse);
			if(!B1.isEmpty()){
				tmp.add(B1);
			}
			if(!B2.isEmpty()){
				tmp.add(B2);
			}
		}
		Q.clear();
		Q.addAll(tmp);
	}


	/**
	 * 
	 * @param Q : initial partition
	 * @param nset : Universal elements
	 * @param pMap : mapping between node and its corresponding parent nodes
	 *  
	 */
	public HashSet<HashSet<String>> Refine(HashSet<HashSet<String>> Q, 
			                                                              HashSet<String> nset, 
			                                                              HashMap<String, Vector<String>> pMap){
		HashSet<HashSet<String>> X = new HashSet<HashSet<String>>();		// another partition
		X.add(nset);		// initially X contains the whole nodes -- only one partition

		while(!Q.equals(X)){
//			System.out.println("Trace: "+Q.size()+", "+X.size());
			HashSet<String> S = new HashSet<String>();									// temporary block
			HashSet<String> B = new HashSet<String>();
			Search:		// find a block in X which is a subset of S and its size is less than 1/2 size of S.
			for(HashSet<String> tmp:X){
				if(!Q.contains(tmp)){
					S.addAll(tmp);
					for(HashSet<String> block:Q){
						if(S.containsAll(block) && block.size()<=S.size()/2){
							B.addAll(block);
							break Search;
						}
					}
				}
			}
			X.remove(S);
			S.removeAll(B);
			if(!B.isEmpty()){
				X.add(B);				
			}
			if(!S.isEmpty()){
				X.add(S);				
			}
			
			this.split(B, Q, nset, pMap);
			this.split(S, Q, nset, pMap);
		}
//		for(HashSet<String> element:Q){
//			String s = "";
//			for(String n:element){
//				s = s+", "+n.tag;
//			}
//			System.out.println(s.substring(2));
//		}
		return Q;
	}
	
	
	public static void main(String[] args){
		PaigeTarjan pt = new PaigeTarjan();
		graphLoad gl = new graphLoad();
		String path = "/disk/scratch/dataset/random/random-10-15.grp";
		cg_graph Graph = gl.load(path);

		Vector<HashSet<String>> P = new Vector<HashSet<String>>();	//	
		P.add(null);
		P.add(null);
		P.add(null);
		Vector<String> nset = new Vector<String>();
		nset.addAll(Graph.vertexSet());
		for(int i=0; i<nset.size(); i++){
			if(i<3){
				HashSet<String> p = P.get(0);
				if(p==null){
					p = new HashSet<String>();
				}
				p.add(nset.get(i));
				P.set(0, p);
			}
			else if(i>=3 && i<6){
				HashSet<String> p = P.get(1);
				if(p==null){
					p = new HashSet<String>();
				}
				p.add(nset.get(i));
				P.set(1, p);
			}
			else{
				HashSet<String> p = P.get(2);
				if(p==null){
					p = new HashSet<String>();
				}
				p.add(nset.get(i));
				P.set(2, p);
			}
		}
		
		System.out.println(P.get(0).size()+", "+P.get(1).size());
//		pt.Refine(P, Graph);
	}
	
}