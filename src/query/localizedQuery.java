package query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

import fileOpe.readfile;
import graph.cg_graph;
import graph.edge;

class sort implements Comparator<String>{ // elements sorted in descending order
	
	private HashMap<String, Float> value;

	public sort(HashMap<String, Float> map){
		this.value = map;
	}
	
	public int compare(String a, String b){
		if (this.value.get(a)>this.value.get(b)){
			return -1;
		}
		else if (this.value.get(a)<this.value.get(b)){
			return 1;
		}
		else return 0;
	}	
}

public class localizedQuery {
	private cg_graph G; 
	private HashMap<String, String> attrG;
	private String up;	//	personalized node in Q
	private cg_graph Q;
	private HashMap<String, String> attrQ;
	private String vp;	//	personalized node in G
	
	/**
	 * this procedure checks whether Gq is larger than alpha*G
	 * @param Gq
	 * @param G
	 * @param alpha
	 * @return ans : true -- Gq<=alpha*G 	false -- Gq>alpha*G
	 */
	public boolean chkSize(cg_graph Gq, cg_graph G, float alpha){
		boolean ans = true;
		if((G.vertexSet().size()+G.edgeSet().size())*alpha<(Gq.vertexSet().size()+Gq.edgeSet().size())){
			ans = false;
		}
		return ans;
	}
	
	/**
	 * this procedure computes cost of a node pair (u, v)
	 * @param u
	 * @param Q
	 * @param attrQ
	 * @param v
	 * @param G : actually is Gq
	 * @param attrG
	 * @return
	 */
	public int cmpCost(String u, cg_graph Q, HashMap<String,String> attrQ, String v, cg_graph Gq, HashMap<String, String> attrG){
		HashSet<String> invalidSet = new HashSet<String>();
		for(edge eu:Q.outgoingEdgesOf(u)){
			String tu = (String) eu.getTarget();
			String tulabel = attrQ.get(tu);
			boolean flag = false;
			for(edge ev:Gq.outgoingEdgesOf(v)){
				String tv = (String) ev.getTarget();
				String tvlabel = attrG.get(tv);
				if(tulabel.equals(tvlabel)){
					flag = true;
					break;
				}
			}
			if(!flag){
				invalidSet.add(tu);
			}
		}
		for(edge eu:Q.incomingEdgesOf(u)){
			String fu = (String) eu.getSource();
			String fulabel = attrQ.get(fu);
			boolean flag = false;
			for(edge ev:Gq.incomingEdgesOf(v)){
				String fv = (String) ev.getSource();
				String fvlabel = attrG.get(fv);
				if(fulabel.equals(fvlabel)){
					flag = true;
					break;
				}
			}
			if(!flag){
				invalidSet.add(fu);
			}
		}
		return invalidSet.size();		
	}
	
	/**
	 * this procedure computes potential of a pair of nodes (u,v)
	 * @param u
	 * @param Q
	 * @param attrQ
	 * @param v
	 * @param G
	 * @param attrG
	 * @return
	 */
	public int cmpPotential(String u, cg_graph Q, HashMap<String, String> attrQ, String v, cg_graph G, HashMap<String, String> attrG){
		HashSet<String> validSet = new HashSet<String>();
		for(edge ev:G.outgoingEdgesOf(v)){
			String tv = (String) ev.getTarget();
			String tvlabel = attrG.get(tv);
			for(edge eu:Q.outgoingEdgesOf(u)){
				String tu = (String) eu.getTarget();
				String tulabel = attrQ.get(tu);
				if(tvlabel.equals(tulabel)){
					validSet.add(tv);
					break;
				}
			}
		}
		for(edge ev:G.incomingEdgesOf(v)){
			String fv = (String) ev.getSource();
			String fvlabel = attrG.get(fv);
			for(edge eu:Q.incomingEdgesOf(u)){
				String fu = (String) eu.getSource();
				String fulabel = attrQ.get(fu);
				if(fvlabel.equals(fulabel)){
					validSet.add(fv);
					break;
				}
			}
		}
		return validSet.size();
	}
	
	/**
	 * this procedure picks one parents of v and computes weights for N(v)
	 * @param u
	 * @param Q
	 * @param attrQ
	 * @param v
	 * @param G
	 * @param attrG
	 * @param Gq
	 * @return
	 */
	public HashMap<String, Float> pickup(String u, cg_graph Q, HashMap<String, String> attrQ, String v, cg_graph G, HashMap<String, String> attrG, cg_graph Gq){
//		ArrayList<String> ans = new ArrayList<String>();
		HashMap<String, Float> hm = new HashMap<String, Float>();	//	maintains weight for each candidate
		String ulabel = attrQ.get(u);
		boolean vflag = false, eflag = false;
		for(edge e: G.incomingEdgesOf(v)){
			String fv = (String) e.getSource();
			String vlabel = attrG.get(fv);
			if(ulabel.equals(vlabel)){
				if(!Gq.containsVertex(fv)){
					Gq.addVertex(fv);
					vflag = true;	//	indicates Gq added fv
				}
				edge ne = new edge(fv, v);
				if(!Gq.containsEdge(fv, v)){
					Gq.addEdge(fv, v, ne);
					eflag = true;
				}
				
				int cost = this.cmpCost(u, Q, attrQ, fv, Gq, attrG);	//	*important* : use Gq here
				int potential = this.cmpPotential(u, Q, attrQ, fv, G, attrG);  // *important* : use G here
				float weight = (float) potential/(cost + 1);
				hm.put(fv, weight);
				
				if(eflag){
					Gq.removeEdge(fv, v);
				}
				if(vflag){
					Gq.removeVertex(fv);	
				}
			}
		}		
		return hm;
	}
	
	/**
	 * this procedure picks children of v and computes weights for N(v)
	 * @param u : node in Q
	 * @param Q
	 * @param attrQ
	 * @param v : node in G
	 * @param G
	 * @param attrG
	 * @param Gq : initialised graph
	 * @return 
	 */
	public HashMap<String, Float> pickdw(String u, cg_graph Q, HashMap<String, String> attrQ, String v, cg_graph G, HashMap<String, String> attrG, cg_graph Gq){
		//ArrayList<String> ans = new ArrayList<String>();
		HashMap<String, Float> hm = new HashMap<String, Float>();
		String ulabel = attrQ.get(u);
		boolean vflag = false, eflag = false;
		for(edge e: G.outgoingEdgesOf(v)){
			String tv = (String) e.getTarget();
			String vlabel = attrG.get(tv);
			if(ulabel.equals(vlabel)){
				if(!Gq.containsVertex(tv)){
					Gq.addVertex(tv);
					vflag = true;
				}
				edge ne = new edge(v, tv);
				if(!Gq.containsEdge(v, tv)){
					Gq.addEdge(v, tv, ne);
					eflag = true;
				}
								
				int cost = this.cmpCost(u, Q, attrQ, tv, Gq, attrG);
				int potential = this.cmpPotential(u, Q, attrQ, tv, G, attrG);
				float weight = (float) potential/(cost + 1);
				hm.put(tv, weight);
				
				// recover Gq after the weight is computed
				if(eflag){
					Gq.removeEdge(v, tv);
				}
				if(vflag){
					Gq.removeVertex(tv);	
				}
			}
		}		
		return hm;
	}
	

	/**
	 * 
	 * @param up : personalized node
	 * @param Q : pattern graph
	 * @param attrQ : attributes of pattern graph
	 * @param vp : match candidates of personalized node
	 * @param G : data graph
	 * @param attrG : attributes of data graph
	 * @param alpha : resource ratio
	 * @return
	 */
	public cg_graph search(String up, cg_graph Q, HashMap<String, String> attrQ, String vp, cg_graph G, HashMap<String, String> attrG,  float alpha){
		int b = 2;
		HashSet<String> visited = new HashSet<String>();	// maintains a set of visited edges
		boolean terminate= false, changed = false;
		cg_graph Gq = new cg_graph();						//	subgraph Gq used for query
		Stack<String[]> S = new Stack<String[]>();			//	stack S used for maintaining a list of node pairs
		String[] start = {up,vp};
		S.push(start);
		Gq.addVertex(vp);
		while(!terminate){
			changed = false;
			String[] pair = S.pop();
//			if(!Gq.containsVertex(pair[1])){
//				Gq.addVertex(pair[1]);
//				changed = true;		//	when Gq is changed
//			}
			// update terminate;
			if(!this.chkSize(Gq, G, alpha)){
				terminate = true;
				return Gq;
			}
			for(edge e:Q.outgoingEdgesOf(pair[0])){
				String uu = e.getTarget().toString();
				String edge = pair[0]+"-"+uu;
				if(!visited.contains(edge)){
					HashMap<String, Float> hm = this.pickdw(uu, Q, attrQ, pair[1], G, attrG, Gq); // returns weights of a set of candidates for expanding
					ArrayList<String> list = new ArrayList<String>();	// used for sorting candidates based on their weights
					list.addAll(hm.keySet());
					Comparator<String> comparator = new sort(hm);
					Collections.sort(list, comparator);
					
					int num = 0;
					for(String vv: list){
						String[] uv = {uu,vv};
						S.push(uv);
						if(!Gq.containsVertex(vv)){
							Gq.addVertex(vv);
						}
						edge ee = new edge(pair[1],vv);
						if(!Gq.containsEdge(pair[1], vv)){
							Gq.addEdge(pair[1], vv, ee);
						}						
						changed = true;		//	when Gq is changed
						num++;
						if(num>=b){
							break;
						}
					}
					visited.add(edge);
				}
			}
			for(edge e:Q.incomingEdgesOf(pair[0])){
				String uu = e.getSource().toString();
				String edge = uu+"-"+pair[0];
				if(!visited.contains(edge)){
					HashMap<String, Float> hm = this.pickup(uu, Q, attrQ, pair[1], G, attrG, Gq);
					ArrayList<String> list = new ArrayList<String>();
//					for(String[] uv:S){
//						if(hm.keySet().contains(uv[1])){
//							hm.remove(uv[1]);
//						}
//					}
					list.addAll(hm.keySet());
					Comparator<String> comparator = new sort(hm);
					Collections.sort(list, comparator);
					
					int num = 0;
					for(String vv: list){
						String[] uv = {uu,vv};
						S.push(uv);
						if(!Gq.containsVertex(vv)){
							Gq.addVertex(vv);
						}
						edge ee = new edge(vv, pair[1]);
						if(!Gq.containsEdge(vv, pair[1])){
							Gq.addEdge(vv, pair[1], ee);
						}						
						changed = true;		//	when Gq is changed
						num++;
						if(num>=b){
							break;
						}
					}
					visited.add(edge);
				}
			}
			if(changed && S.isEmpty()){
				b = b+1;
				S.push(start);
				changed = false;
			}
			if(!changed && S.isEmpty()){
				terminate = true;
			}
		}
		return Gq;
	}
	
	/**
	 * this procedure computes a synthetic pattern graph
	 * @param vv
	 * @param ee
	 */
	public void patternGen(String vv, String ee){
		Vector<String> vSet = new Vector<String>();				//	vSet is used for randomly pick nodes
		HashSet<String> edgeSet = new HashSet<String>();		//	edgeset is used for recording edges, vSet and edgeset are for efficiently detecting duplicate info
		Vector<String> labelSet = new Vector<String>();
		labelSet.add("SA");			//	system architect
		labelSet.add("DBA");		//	database administrator
		labelSet.add("GD");		//	graphic designer
		labelSet.add("BA");		//	business analyst
		labelSet.add("ST");			//	system tester
		labelSet.add("PG");			//	programmer
		labelSet.add("Bio");		//	biologist
		labelSet.add("SE");			// system engineer
		labelSet.add("AI");			//	artificial intelligence
		labelSet.add("PM");		//	project manager
		labelSet.add("HR");		//	human resource management
		labelSet.add("MK");		//	marketing
		labelSet.add("FA");			//	finance analyst
		labelSet.add("UD");		//	user interface designer
		labelSet.add("SD");			//	software developer
		

		cg_graph Graph = new cg_graph();
		HashMap<String, String> attrG = new HashMap<String, String>(); 
		Random randomGenerator = new Random();
		for(int i=0; i<Integer.valueOf(vv); i++){
			String n = new String();
			n = String.valueOf(i);
			attrG.put(n, labelSet.elementAt(randomGenerator.nextInt(15)));
			Graph.addVertex(n);
			vSet.add(n);
			//System.out.println(Graph.vertexSet().size());
		}

		int vsize = vSet.size();
		String e = "";
		for(int j=0; j<Integer.valueOf(ee); j++){
			String fnode = vSet.elementAt((int)(Math.random()*vsize));
			String tnode = vSet.elementAt((int)(Math.random()*vsize));
			e = fnode+"."+tnode;
			while(edgeSet.contains(e) || fnode.equals(tnode)){
				fnode = vSet.elementAt((int)(Math.random()*vsize));
				e = fnode+"."+tnode;
			}
			edge edge = new edge(fnode, tnode);
			Graph.addEdge(fnode, tnode, edge);

			edgeSet.add(e);
		}
		
		this.up = vSet.get(randomGenerator.nextInt(Integer.valueOf(vv)));		// specify personalized node
		this.Q = Graph;
		this.attrQ = attrG;
	}
	
	/**
	 * @param vno : number of vertex
	 * @param eno : number of edge
	 * @param alpha : ratio
	 * @param Gpath : path of the data graph 
	 * @param AGpath : path of the attribute of the graph
	 */
	@SuppressWarnings("unchecked")
	public void test(String vno, String eno, float alpha, String Gpath, String AGpath){
//		this.patternGen(vno, eno);		// generate pattern graph
//		this.Q.Display(attrQ);
//		String uplabel = this.attrQ.get(this.up);

		readfile rf = new readfile();
		this.G = (cg_graph) rf.read(Gpath);
		this.attrG = (HashMap<String, String>) rf.read(AGpath);
		this.vp = "0";
//		Vector<String> vSet = new Vector<String>();
//		Random randomGenerator = new Random();
//		for(String v:this.G.vertexSet()){
//			String vlabel = this.attrG.get(v);
//			if(vlabel.equals(uplabel)){
//				vSet.add(v);
//			}
//		}
//		this.vp = vSet.get(randomGenerator.nextInt(Integer.valueOf(vSet.size())));
		System.out.println("personalized node u: " + this.vp);
		System.out.println("-----------------------");
//		for(String v : this.Q.vertexSet()){
//			String vlabel = this.attrQ.get(v);
//			if(vlabel.equals(uplabel)){
//				this.vp = v;
//				System.out.println("vp: "+this.vp);
//				break;
//			}
//		}
//		this.vp = this.up;
//		for(String v : this.G.vertexSet()){
//			String vlabel = this.attrG.get(v);
//			if(vlabel.equals(uplabel)){
//				this.vp = v;
//				System.out.println("vp: "+this.vp);
//				break;
//			}
//		}
		cg_graph Gq = this.search(this.vp, this.G, this.attrG, this.vp, this.G, this.attrG, alpha);
		Gq.Display(this.attrG);
	}
	
	public static void main(String[] args){
		localizedQuery lq = new localizedQuery();
		String vno = "2", eno = "1"; 
		float alpha = (float) 1;
		String path = "D:/Data/Synthetic/5-Ran-4-6";
		lq.test(vno, eno, alpha, path+".grp", path+".atr");
	}	
}
