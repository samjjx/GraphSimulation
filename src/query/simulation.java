package query;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import fileOpe.readfile;
import graph.edge;
import graph.cg_graph;


/**
 * this class computes simulation query between a pattern and a data graph
 * @author jiangxianlin
 *
 */
public class simulation {

	public HashMap<String, HashSet<String>> sim;			//	node -> match nodes
	public HashMap<String, HashSet<String>> premv;		//	node -> can not match parents
	private HashMap<String, HashSet<String>> pre;			//	node -> parents
	private HashMap<String, HashSet<String>> suc;		//	node -> children
	private HashSet<String> PSET;									//	set of all parent nodes

	
	/**
	 * Constructor
	 */
	public simulation(){
		this.sim = new HashMap<String, HashSet<String>>();		//	node -> match nodes
		this.premv = new HashMap<String, HashSet<String>>();	//	node -> can not match parents
		this.pre = new HashMap<String, HashSet<String>>();			//	node -> parents
		this.suc = new HashMap<String, HashSet<String>>();			//	node -> children
		this.PSET = new HashSet<String>();									//	set of all parent nodes
	}
	
	
	/**
	 * This procedure initialises the data structures which are used by EfficientSimilarity
	 * @param G
	 */
	public void initidx(cg_graph G){
		for(String v:G.vertexSet()){
			HashSet<String> pset = new HashSet<String>();	//	initialise pset
			for(edge e: G.incomingEdgesOf(v)){
				pset.add(G.getEdgeSource(e));
			}
			this.pre.put(v, pset);
			
			if(pset.size()>0){
				PSET.addAll(pset);
			}
			
			HashSet<String> cset = new HashSet<String>();	//	initialise cset
			for(edge e: G.outgoingEdgesOf(v)){
				cset.add(G.getEdgeTarget(e));
			}
			this.suc.put(v, cset);
		}
	}	

	/**
	 * @param P : pattern 
	 * @param G : graph
	 */
	public void EfficientSimilarity(cg_graph P, HashMap<String, String> attrP, cg_graph G, HashMap<String, String> attrG){
		// initialise sim set.
		for(String u:P.vertexSet()){

			HashSet<String> posmat = new HashSet<String>();			//	a node set which contains nodes that possibly match parents of v 
			HashSet<String> remove = new HashSet<String>();		//	a node set which contains nodes that can not match any parent node of v
			remove.addAll(this.PSET);

			HashSet<String> simset = this.sim.get(u);			//	sim(u)
			if(simset==null){
				simset = new HashSet<String>();					//	initialise simset
			}
			
			String lu = attrP.get(u);		//	label of u
			if(P.outDegreeOf(u)==0){
				for(String v:G.vertexSet()){
					String lv = attrG.get(v);
					if(lu.equals(lv)){
						simset.add(v);
						if(!this.pre.get(v).isEmpty()){
							posmat.addAll(this.pre.get(v));							
						}
					}
				}
			}
			else{
				for(String v:G.vertexSet()){
					String lv = attrG.get(v);
					if(lu.equals(lv) && G.outDegreeOf(v)!=0){
						simset.add(v);
						if(!this.pre.get(v).isEmpty()){
							posmat.addAll(this.pre.get(v));							
						}
					}
				}
			}
			this.sim.put(u, simset);
			remove.removeAll(posmat);
			this.premv.put(u, remove);
		}
		
		Queue<String> q = new LinkedList<String>();
		for(String n:this.premv.keySet()){
			HashSet<String> hs = this.premv.get(n);
			if(!hs.isEmpty()){
				q.add(n);
			}
		}
		
		while(!q.isEmpty()){
			String n = q.poll();
			for(edge e:P.incomingEdgesOf(n)){
				String u = P.getEdgeSource(e);
				HashSet<String> sim = this.sim.get(u);
				for(String w:this.premv.get(n)){
					if(sim.contains(w)){
						sim.remove(w);		// w in G can not match u in P
						for(edge ee:G.incomingEdgesOf(w)){
							String ww = G.getEdgeSource(ee);
							HashSet<String> cset = new HashSet<String>(); 
							cset.addAll(this.suc.get(ww));
							cset.retainAll(sim);
							if(cset.isEmpty()){
								this.premv.get(u).add(ww);
								if(!q.contains(u)){
									q.add(u);
								}
							}
						}
					}
				}
			}
			this.premv.get(n).clear();
		}
	}
	
	
	/**
	 * This procedure compares the search condition with the attribute values of the data node 
	 * @param operator
	 * @param searchvalue
	 * @param value
	 * @return
	 */
	public boolean check(String[] searchcondition, String[] value){
		boolean ans = false;
		
		//	0:attribute		1:operator		2:value
		int attribute = Integer.valueOf(searchcondition[0]);
		String operator = searchcondition[1];
		String searchvalue = searchcondition[2];
		
		if(value!=null){
			if(operator.equals("=")){
				if(value[attribute]!=null){
					if(searchvalue.contains(value[attribute])){
						ans = true;
					}
				}
			}
			else if(operator.equals("<=")){
				float searchu = Float.valueOf(searchvalue);
				if(value[attribute]!=null){
					float valuev = Float.valueOf(value[attribute].trim());
					if(valuev<=searchu){
						ans = true;
					}
				}
			}
			else if(operator.equals(">=")){
				float searchu = Float.valueOf(searchvalue);
				if(value[attribute]!=null){
					float valuev = Float.valueOf(value[attribute].trim());
					if(valuev>=searchu){
						ans = true;
					}
				}
			}
		}
		return ans;
	}
	
	
	/**
	 * 
	 * @param P : pattern 
	 * @param attrP : attribute of pattern
	 * @param G : graph
	 * @param attrG : attribute of the data graph
	 */
	public void EfficientSimilarityCitation(cg_graph P, HashMap<String, String[]> attrP, cg_graph G, HashMap<String, String[]> attrG){

		// initialise sim set.
		for(String u:P.vertexSet()){

			HashSet<String> posmat = new HashSet<String>();		//	a node set which contains nodes that possibly match parents of v 
			HashSet<String> remove = new HashSet<String>();		//	a node set which contains nodes that can not match any parent node of v
			remove.addAll(this.PSET);

			HashSet<String> simset = this.sim.get(u);			//	sim(u)
			if(simset==null){
				simset = new HashSet<String>();					//	initialise simset
			}
			
			String[] search = attrP.get(u);		//	search condition by u
			if(P.outDegreeOf(u)==0){
				for(String v:G.vertexSet()){
					if(this.check(search, attrG.get(v))){		//	0:attribute		1:operator		2:value
						simset.add(v);
						if(!this.pre.get(v).isEmpty()){
							posmat.addAll(this.pre.get(v));							
						}
					}
				}
			}
			else{
				for(String v:G.vertexSet()){
					if(this.check(search, attrG.get(v)) && G.outDegreeOf(v)!=0){
						simset.add(v);
						if(!this.pre.get(v).isEmpty()){
							posmat.addAll(this.pre.get(v));							
						}
					}
				}
			}
			this.sim.put(u, simset);
			remove.removeAll(posmat);
			this.premv.put(u, remove);
		}
		
		Queue<String> q = new LinkedList<String>();
		for(String n:this.premv.keySet()){
			HashSet<String> hs = this.premv.get(n);
			if(!hs.isEmpty()){
				q.add(n);
			}
		}
		
		while(!q.isEmpty()){
			String n = q.poll();
			for(edge e:P.incomingEdgesOf(n)){
				String u = P.getEdgeSource(e);
				HashSet<String> sim = this.sim.get(u);
				for(String w:this.premv.get(n)){
					if(sim.contains(w)){
						sim.remove(w);		// w in G can not match u in P
						for(edge ee:G.incomingEdgesOf(w)){
							String ww = G.getEdgeSource(ee);
							HashSet<String> cset = new HashSet<String>(); 
							cset.addAll(this.suc.get(ww));
							cset.retainAll(sim);
							if(cset.isEmpty()){
								this.premv.get(u).add(ww);
								if(!q.contains(u)){
									q.add(u);
								}
							}
						}
					}
				}
			}
			this.premv.get(n).clear();
		}
	}
	
	
	public void output(){
		
		for(String u:this.sim.keySet()){
			HashSet<String> vset = this.sim.get(u);
			String s = "";
			for(String v: vset){
				s = v +", "+ s;
			}
			System.out.println(u+": "+s);
		}
	}
	
	
	/**
	 * this procedure computes result graph based on the simulation relation
	 * @param P
	 * @param G
	 * @return
	 */
	
	public cg_graph genResultGraph(cg_graph P, cg_graph G){
		cg_graph resultgraph = new cg_graph();
		
		for(edge eu:P.edgeSet()){
			String fnu = (String) eu.getSource();
			String tnu = (String) eu.getTarget();
			
			HashSet<String> fnumat = this.sim.get(fnu);
			HashSet<String> tnumat = this.sim.get(tnu);
			
			for(String fv:fnumat){
				for(String tv:tnumat){
					if(G.containsEdge(fv, tv)){
						if(!resultgraph.containsVertex(fv)){
							resultgraph.addVertex(fv);
						}
						if(!resultgraph.containsVertex(tv)){
							resultgraph.addVertex(tv);
						}
						edge ev = new edge(fv,tv);
						resultgraph.addEdge(fv, tv, ev);
					}
				}
			}
		}
		return resultgraph;
	}
	
	
	public cg_graph patternGen(){
		cg_graph P = new cg_graph();
		P.addVertex("1");
		P.addVertex("2");
		P.addVertex("3");
//		P.addVertex("4");
//		P.addVertex("5");
		
//		edge e1 = new edge("1","4");
//		P.addEdge("1", "4", e1);
		edge e2 = new edge("2","1");
		P.addEdge("2", "1", e2);
		edge e3 = new edge("3","2");
		P.addEdge("3", "2", e3);
		edge e4 = new edge("3","1");
		P.addEdge("3", "1", e4);
//		edge e5 = new edge("3","5");
//		P.addEdge("3", "5", e5);
		return P;
	}
	

	public cg_graph graphGen(String file) throws IOException{
		BufferedReader reader = null;

		cg_graph g = new cg_graph();
		//		String filePath = "/disk/scratch/dataset/SourceData/WikiTalk.grp";
		//		Vector<String[]> graphVector = new Vector<String[]>();

		String[] opval = new String[2];
		try{
			reader = new BufferedReader(new FileReader(file));
			String text = null;
			while ((text = reader.readLine()) != null){
				System.out.println(g.edgeSet().size());
				if(text.length()>1 && !text.substring(0, 1).equals("#")){
					opval = text.split("	");
					String fn = opval[0];
					String tn = opval[1];
					if(!g.containsVertex(fn)){
						fn = opval[0];
						g.addVertex(fn);
					}
					if(!g.containsVertex(tn)){
						tn = opval[1];
						g.addVertex(tn);
					}					
					edge newedge = new edge(fn, tn);
					g.addEdge(fn, tn, newedge);
				}
			}
			//			System.out.println("edgeset size:"+graphVector.size());
		} catch (FileNotFoundException e){
			e.printStackTrace();
		} finally{
			try{
				if (reader != null){
					reader.close();
				}
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		return g;
	}
	
	
	public static void main(String[] args) throws IOException{		
		

		simulation sim = new simulation();
		cg_graph P = sim.patternGen();
		HashMap<String, String> attrP = new HashMap<String, String>();
		attrP.put("1", "B");
		attrP.put("2", "B");
		attrP.put("3", "D");
		
		readfile rf = new readfile();
		String graphPath = "D:/5-Ran-4-6.grp";
		cg_graph G = (cg_graph) rf.read(graphPath);
		
		String attrPath = "D:/5-Ran-4-6.atr";
		HashMap<String, String> attrG = (HashMap<String, String>) rf.read(attrPath);
		
		
		double start = System.currentTimeMillis();
		sim.initidx(G);
		sim.EfficientSimilarity(P, attrP, G, attrG);
		double end = System.currentTimeMillis();
		System.out.println("Time: "+(end-start));
	}
}
