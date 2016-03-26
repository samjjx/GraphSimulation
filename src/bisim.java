import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.jgrapht.alg.StrongConnectivityInspector;

import fileOpe.graphLoad;
import fileOpe.readfile;
import graph.edge;
import graph.cg_graph;
import graph.ranGraph;
import graph.ranGraph.grpattr;


public class bisim {

	public HashMap<String, Integer> rankvalue;			// set rank value for each node in the DAG
	public HashMap<String, String> clusterMap;			// map the original node to the new node in the DAG
	public HashMap<String, HashSet<String>> Map;	// map the hyper node to its Strongly Connected Component in the original graph
	public HashSet<String> WF;										// well founded node set
	public int nodeid;														// new node id
	public HashSet<String> visited;								// visited nodes during a depth first search
	public HashMap<String, Vector<String>> pMap;		// mapping between node and its parent node set
	public HashMap<HashSet<String>, String> blockMap;		// map the block to the hyper node in the compressed graph
	public HashMap<String, HashSet<String>> hynodeMap;	// map the hyper node to the block
	public HashSet<String> sccvset;								// maintains scc node set (size>1) for the scc graph
	public HashMap<String, String> vmap;					//	map the original node to the collapsed node

	public bisim(){
		this.rankvalue = new HashMap<String, Integer>();
		this.clusterMap = new HashMap<String, String>();
		this.Map = new HashMap<String, HashSet<String>>();
		this.WF = new HashSet<String>();
		this.nodeid = 0;
		this.visited = new HashSet<String>();
		this.pMap = new HashMap<String, Vector<String>>();
		this.blockMap = new HashMap<HashSet<String>, String>();
		this.hynodeMap = new HashMap<String, HashSet<String>>();
		this.sccvset = new HashSet<String>();
		this.vmap =  new HashMap<String, String>();
	}
	
	/**
	 * this procedure computes a Directed Acyclic Graph --- shrink the Strongly Connected Component
	 * @param graph
	 * @return
	 */
	public cg_graph sccDAG(cg_graph graph){
		StrongConnectivityInspector<String, edge> sccIns = new StrongConnectivityInspector<String, edge>(graph);
		List<Set<String>> sccsets = sccIns.stronglyConnectedSets();
		cg_graph sccDAG = new cg_graph();
		
		int i = 0;
		for(Set<String> s:sccsets){
			String newnode = String.valueOf(i);			//  label the node with natural number
			sccDAG.addVertex(newnode);
			for(String n:s){
				this.clusterMap.put(n, newnode);
			}
			HashSet<String> scc = new HashSet<String>();
			scc.addAll(s);
			this.Map.put(newnode, scc);		//	map the scc node to a set of nodes which are shrinked to be scc node
			if(s.size()>1){
				this.sccvset.add(newnode);		//	put new node in the scc node set
			}
			i++;
		}
		for(Set<String> s:sccsets){
			for(String n:s){
				if(graph.outDegreeOf(n)>0){
					for(edge e:graph.outgoingEdgesOf(n)){
						String end = graph.getEdgeTarget(e);
						if(!s.contains(end)){	// get the node outside the current cluster
							String newstart = this.clusterMap.get(n);
							String newend = this.clusterMap.get(end);
							edge newedge = new edge(newstart, newend);
							if(!sccDAG.containsEdge(newedge)){
								sccDAG.addEdge(newstart, newend, newedge);
							}
						}
					}
				}
			}
		}
		return sccDAG;
	}
	

	/**
	 * check whether a node belongs to well founded part
	 * @param Gscc : DAG of G
	 */
	public void wellfounded(cg_graph Gscc){
		boolean flag = true;
		for(String Nscc:Gscc.vertexSet()){
			flag = true;
			HashSet<String> visited = new HashSet<String>();
			Queue<String> q = new LinkedList<String>();
			q.add(Nscc);
			visited.add(Nscc);
			while(!q.isEmpty()){
				String n = q.poll();
				if(this.sccvset.contains(n)){
					flag = false;
					break;
				}
				for(edge e:Gscc.outgoingEdgesOf(n)){
					String current = Gscc.getEdgeTarget(e);
					if(!visited.contains(current)){
						q.add(current);
						visited.add(current);
					}
				}
			}
			if(flag==true){
				this.WF.add(Nscc);
			}
		}
	}
	
	
	/**
	 * initialise the ranking value for each node in the DAG
	 * @param Gscc
	 */
	public void inirank(cg_graph Gscc){
		Vector<String> unvisited = new Vector<String>();
		unvisited.addAll(Gscc.vertexSet());
		
		while(!unvisited.isEmpty()){
			String n = unvisited.firstElement();
			this.visited.add(n);
			this.dfs(n, Gscc);
			unvisited.removeAll(this.visited);
		}
	}
	

	/**
	 * depth first search and ranking value computation
	 * @param n
	 * @param Gscc
	 */
	public void dfs(String n, cg_graph Gscc){
		int max = Integer.MIN_VALUE;
		if(Gscc.outDegreeOf(n)==0 && !this.sccvset.contains(n)){
			max = 0;
		}
		else if(Gscc.outDegreeOf(n)==0 && this.sccvset.contains(n)){
			max = Integer.MIN_VALUE;
		}
		for(edge e:Gscc.outgoingEdgesOf(n)){
			String target = Gscc.getEdgeTarget(e);
			if(!this.visited.contains(target)){
				this.visited.add(target);
				this.dfs(target, Gscc);
			}
			
			int r = this.rankvalue.get(target);
			if(this.WF.contains(target)){
				if((r+1)>max){
					max = r+1;
				}
			}
			else{
				if(r>max){
					max = r;
				}
			}
		}
		this.rankvalue.put(n, max);
	}
	
	/**
	 * find max rank value
	 * @return
	 */
	public int findmaxrank(){	// if all the nodes with rank = Integer.MIN_VALUE, the algorithm needs modify.
		int max = Integer.MIN_VALUE;
		for(Integer i:this.rankvalue.values()){
			if(i>max){
				max = i;
			}
		}
		return max;
	}

	/**
	 * this procedure collapse nodes in the graph
	 * @param graph : original graph
	 * @param attr : attribute of graph
	 * @param collapse : a set of nodes to be collapsed
	 * @param newGraph : new graph after collapse
	 * @param newattr : attribute of newGraph
	 * @return
	 */
	public HashSet<String> collapseII(cg_graph graph, HashMap<String, String> attr, HashSet<String> collapse, cg_graph newGraph, HashMap<String, String> newattr){
		HashMap<String, HashSet<String>> temp = new HashMap<String, HashSet<String>>();		// classify nodes with the label
		HashSet<String> newVSet = new HashSet<String>();		//	new node set which is formed by collapsing the node set
	
		for(String s:collapse){
			String label = attr.get(s);
			HashSet<String> vset = temp.get(label);
			if(vset==null){
				vset = new HashSet<String>();
			}
			vset.add(s);
			temp.put(label, vset);
		}
		
		for(String label:temp.keySet()){
			String newnode =  this.nodeid+"-"+label;
			newGraph.addVertex(newnode);
			newVSet.add(newnode);
			this.nodeid++;

			newattr.put(newnode, label);
			HashSet<String> proc = temp.get(label);	//	a set of nodes to be processed
			this.blockMap.put(proc, newnode);
			this.hynodeMap.put(newnode, proc);
			
			/**
			 * 	1. remove nodes in collapse
			 * 	2. connect parent and child nodes of collapse node set to new node
			 */
			newGraph.removeAllVertices(proc);
			for(String rmv:proc){
				this.vmap.put(rmv, newnode);
			}
			for(String remove:proc){
				Vector<String> pset = graph.getParents(remove);
				if(pset!=null){
					pset.removeAll(collapse);
					if(!pset.isEmpty()){
						for(String parent:pset){
							if(newGraph.containsVertex(parent)){
								edge ne = new edge(parent, newnode);
								newGraph.addEdge(parent, newnode, ne);
							}
							else{
								edge ne = new edge(vmap.get(parent), newnode);
								newGraph.addEdge(vmap.get(parent), newnode, ne);
							}
						}
					}
				}
				
				Vector<String> cset = graph.getChildren(remove);
				if(cset!=null){
					cset.removeAll(collapse);
					if(!cset.isEmpty()){
						for(String child:cset){
							if(newGraph.containsVertex(child)){
								edge ne = new edge(newnode, child);
								newGraph.addEdge(newnode, child, ne);
							}
							else{
								edge ne = new edge(newnode, vmap.get(child));
								newGraph.addEdge(newnode, vmap.get(child), ne);
							}
						}
					}
				}
			}
		}
		
//		ranGraph rg = new ranGraph();
//		grpattr grp = rg.new grpattr();
//		grp.graph = newGraph;
//		grp.attr = newattr;
		return newVSet;
	}
	
	
	/**
	 * this procedure collapse the node set
	 * @param graph : graph which is to be processed
	 * @param collapse : node set which is to be collapsed
	 * @return
	 */
	public String collapse(cg_graph graph, HashSet<String> collapse){
		HashSet<String> pnset = new HashSet<String>();
		HashSet<String> cnset = new HashSet<String>();
		
		String hn = new String();
//		hn.tag = "H-"+this.nodeid;
		hn = "H-"+this.nodeid;
		graph.addVertex(hn);
		this.blockMap.put(collapse, hn);
		this.hynodeMap.put(hn, collapse);
		
		for(String n:collapse){
			Vector<String> pset = new Vector<String>(); 
			if(this.pMap.get(n)!=null){
				pset.addAll(this.pMap.get(n));
				pnset.addAll(pset);
			}
			Vector<String>	cset = graph.getChildren(n);
			if(cset!=null){
				cnset.addAll(cset);
				for(String nn:cset){
					Vector<String> vec = this.pMap.get(nn);
					if(vec!=null){
						vec.removeAll(collapse);
						if(!vec.contains(hn)){
							vec.add(hn);
						}
						this.pMap.put(nn, vec);
					}
				}
			}
		}

		pnset.removeAll(collapse);
		cnset.removeAll(collapse);
		
		Vector<String> vec = new Vector<String>();
		if(!pnset.isEmpty()){
			vec.addAll(pnset);
		}
		this.pMap.put(hn, vec);

		for(String nn:pnset){
			if(!graph.containsVertex(nn)){
				graph.addVertex(nn);
			}
			edge ne = new edge(nn, hn);
			graph.addEdge(nn, hn, ne);
		}

		for(String nn:cnset){
			if(!graph.containsVertex(nn)){
				graph.addVertex(nn);
			}
			edge ne = new edge(hn, nn);
			graph.addEdge(hn, nn, ne);
		}

//		// adjust index-pmap
//		if(!pnset.isEmpty()){
//			Vector<String> vec = new Vector<String>();
//			vec.addAll(pnset);
//			this.pMap.put(hn, vec);
//			
//			
//			for(String n:pnset){
//				Vector<String> cset = graph.getChildren(n);
//				if(cset!=null){
//					for(String nn:cset){
//						Vector<String> pset = this.pMap.get(nn);
//						if(pset!=null){
//							pset.remove(index)
//						}
//					}
//				}
//			}
//			
//		}
		graph.removeAllVertices(collapse);
		this.nodeid++;
		return hn;
	}

	
	/**
	 * this procedure is for the graph with multiple labels
	 * @param graph : original graph
	 * @param	attr : attribute of the graph
	 * @param rankv : rank value of the nodes in original graph
	 * @return	:	graph and its attributes
	 */
	public grpattr bisimulation(cg_graph graph, HashMap<String, String> attr, HashMap<String, Integer> rankv){
		PaigeTarjan pt = new PaigeTarjan();
		int max = this.findmaxrank();
		System.out.println("Max: "+max);
		Vector<HashSet<String>> partition = new Vector<HashSet<String>>();		// original partition based on rank value
		Vector<HashSet<HashSet<String>>> partitionCopy = new Vector<HashSet<HashSet<String>>>();	//	another partition 
		
		/**
		 * 	Step 3, 4 : initialise the partition according to the node ranking.
		 */
		for(int i=0; i<=max+1; i++){
			partition.add(i, null);
		}
		HashSet<String> tmp;
		Set tempSet=rankv.keySet();
		
		for(String n:rankv.keySet()){
			int rank = rankv.get(n); 
			System.out.println("MIN_VALUE"+Integer.MIN_VALUE);

			if(rank==Integer.MIN_VALUE){
				tmp = partition.get(max+1);
			}
			else{
				tmp = partition.get(rank);
			}
			if(tmp==null){
				tmp = new HashSet<String>();
			}
			tmp.add(n);
			
			if(rank==Integer.MIN_VALUE){
				partition.set(max+1, tmp);				
			}
			else{
				partition.set(rank, tmp);
			}
		}

		/**
		 * 	Step 5 : Collapse the nodes with ranking = "-unlimited" 
		 *		Step 6 : Refine blocks at higher ranks
		 */
		HashSet<String> collapse = partition.get(max+1);
		cg_graph newGraph = (cg_graph) graph.clone();
		HashMap<String, String> newattr = new HashMap<String, String>();
		newattr.putAll(attr);
		
		if(collapse!=null){
//			String newhyn = this.collapse(graph, collapse);		// collapse B -unlimited
//			collapse.clear();
//			collapse.add(newhyn);
			HashSet<String> newVSet = this.collapseII(graph, attr, collapse, newGraph, newattr);
			for(int i=0; i<=max; i++){
				HashSet<HashSet<String>> vec = new HashSet<HashSet<String>>();
				for(String newhyn:newVSet){
					HashSet<String> nset1 = new HashSet<String>();
					HashSet<String> nset2 = new HashSet<String>();
					for(String nn:partition.get(i)){
						if(newGraph.containsEdge(nn, newhyn)){
							nset1.add(nn);
						}
						else{
							nset2.add(nn);
						}
					}
					if(!nset1.isEmpty()){
						vec.add(nset1);
					}
					if(!nset2.isEmpty()){
						vec.add(nset2);
					}
				}
				partitionCopy.add(i, vec);
			}
		}
		else{
			for(int i=0; i<=max; i++){
				HashSet<HashSet<String>> vec = new HashSet<HashSet<String>>();
				if(partition.get(i)!=null){
					vec.add(partition.get(i));
					partitionCopy.add(i, vec);
				}
			}
		}
	
		/**
		 * Step 7 : Iteratively refines the node sets with higher ranking
		 */
		for(int i=0; i<=max; i++){
			System.out.println("Step: " +i);
			// step (a)
			HashSet<HashSet<String>> B = partitionCopy.get(i);
			HashSet<String> nset = partition.get(i);
			
//			cg_graph Gi = graph.insubGraph(nset);
			HashSet<HashSet<String>> hs = pt.Refine(B, nset, this.pMap);
			partitionCopy.set(i, hs);

			// step (b)
			HashSet<String> newVec = new HashSet<String>();
			for(HashSet<String> X:hs){
//				String hn = this.collapse(graph, X);
//				newVec.add(hn);
				HashSet<String> newVSet = this.collapseII(graph, attr, X, newGraph, newattr);
				newVec.addAll(newVSet);
			}

			// step (c)
			for(String newhy:newVec){
				for(int j=i+1;j<=max;j++){
					HashSet<HashSet<String>> C = partitionCopy.get(j);
					HashSet<HashSet<String>> Ccopy = new HashSet<HashSet<String>>();
					for(HashSet<String> oldhs:C){
						HashSet<String> nset1 = new HashSet<String>();
						HashSet<String> nset2 = new HashSet<String>();
						
						for(String nn:oldhs){
							if(newGraph.containsEdge(nn, newhy)){
								nset1.add(nn);
							}
							else{
								nset2.add(nn);
							}
						}
						if(!nset1.isEmpty()){
							Ccopy.add(nset1);
						}
						if(!nset2.isEmpty()){
							Ccopy.add(nset2);
						}
					}
					partitionCopy.set(j, Ccopy);
				}
			}
		}
		ranGraph rg = new ranGraph();
		grpattr grp = rg.new grpattr();
		grp.graph = newGraph;
		grp.attr = newattr;
		return grp;
	}
	
	/**
	 * this procedure is for the graph with multiple labels
	 * @param graph
	 * @param Gscc
	 * @param rankv : node and its corresponding ranking
	 * @param attMap: node and its corresponding attributes
	 * @param para  : specify attribute of the node
	 * @return : node partition ranked by level 
	 */
	public Vector<HashSet<HashSet<String>>> bisimulationM(cg_graph graph, cg_graph Gscc, HashMap<String, Integer> rankv, HashMap<String, String[]> attMap, int para){
		PaigeTarjan pt = new PaigeTarjan();
		int max = this.findmaxrank();
		Vector<HashMap<String, HashSet<String>>> partition = new Vector<HashMap<String, HashSet<String>>>();	// original partition based on rank value
		Vector<HashSet<HashSet<String>>> partitionCopy = new Vector<HashSet<HashSet<String>>>();
		
		for(int i=0; i<=max+1; i++){
			partition.add(i, null);
			partitionCopy.add(i, null);
		}

		for(String n:rankv.keySet()){
			int rank = rankv.get(n);
			String[] att = attMap.get(n);
			HashMap<String, HashSet<String>> tmp;
			if(rank==Integer.MIN_VALUE){
				tmp = partition.get(max+1);		// label and its corresponding node set
			}
			else{
				tmp = partition.get(rank);
			}

			if(tmp==null){
				tmp = new HashMap<String, HashSet<String>>();
				HashSet<String> hs = new HashSet<String>();
				hs.add(n);
				if(att==null || att.length<para){
					tmp.put("Another", hs);		//	second attribute of Youtube = "Music"
				}
				else{
					tmp.put(att[para], hs);
				}
			}
			else{
				HashSet<String> hs;
				if(att==null || att.length<para){
					hs = tmp.get("Another");	//	second attribute of Youtube = "Music"
				}
				else{
					hs = tmp.get(att[para]);
				}
				
				if(hs==null){
					hs = new HashSet<String>();
				}
				hs.add(n);
				
				if(att==null || att.length<para){
					tmp.put("Another", hs);		//	second attribute of Youtube = "Music"
				}
				else{
					tmp.put(att[para], hs);
				}
			}
			
			if(rank==Integer.MIN_VALUE){
				partition.set(max+1, tmp);
			}
			else{
				partition.set(rank, tmp);
			}
		}

		// collapse node set with rank = -unlimited
		HashMap<String, HashSet<String>> collapse = partition.get(max+1);
		HashSet<String> nodeset = new HashSet<String>();
		if(collapse!=null){
			for(HashSet<String> nset:collapse.values()){
				String newhyn = this.collapse(graph, nset);		// collapse B -unlimited
				nodeset.add(newhyn);
			}
			HashSet<HashSet<String>> unlimited = new HashSet<HashSet<String>>();
			unlimited.addAll(collapse.values());
			partitionCopy.set(max+1, unlimited);							// store the unlimited layer in max+1 position
			// adjust partition on levels from 0--max
			for(int i=0; i<=max; i++){
				HashMap<String, HashSet<String>> hm = partition.get(i);
				HashSet<HashSet<String>> vec = new HashSet<HashSet<String>>();
				vec.addAll(hm.values());
				for(String nn:nodeset){
					Vector<HashSet<String>> vecCopy = new Vector<HashSet<String>>();
					for(HashSet<String> hs:vec){
						HashSet<String> nset1 = new HashSet<String>();
						HashSet<String> nset2 = new HashSet<String>();
						for(String n:hs){
							if(graph.containsEdge(n, nn)){
								nset1.add(n);
							}
							else{
								nset2.add(n);
							}
						}
						if(!nset1.isEmpty()){
							vecCopy.add(nset1);
						}
						if(!nset2.isEmpty()){
							vecCopy.add(nset2);
						}
					}
					vec.clear();
					vec.addAll(vecCopy);
				}
//				for(HashSet<String> hs:hm.values()){
//					for(String nn:nodeset){
//						for(String n:hs){
//							HashSet<String> nset1 = new HashSet<String>();
//							HashSet<String> nset2 = new HashSet<String>();
//							if(graph.containsEdge(n, nn)){
//								nset1.add(n);
//							}
//							else{
//								nset2.add(n);
//							}
//							if(!nset1.isEmpty()){
//								vec.add(nset1);
//							}
//							if(!nset2.isEmpty()){
//								vec.add(nset2);
//							}
//						}
//					}
//				}
				partitionCopy.set(i, vec);
			}
		}
		else{
			for(int i=0; i<=max; i++){
				HashSet<HashSet<String>> vec = new HashSet<HashSet<String>>();
				HashMap<String, HashSet<String>> hm = partition.get(i);
				vec.addAll(hm.values());
				partitionCopy.set(i, vec);
			}
		}
		
		for(int i=0; i<=max; i++){
			// step (a)
			HashSet<HashSet<String>> B = partitionCopy.get(i);
			HashSet<String> nset = new HashSet<String>();
			for(HashSet<String> hs:B){
				nset.addAll(hs);
			}
			System.out.println("Step: " +i+" , "+B.size()+" , "+nset.size());

			HashSet<HashSet<String>> hset = pt.Refine(B, nset, this.pMap);
			HashSet<HashSet<String>> newB = new HashSet<HashSet<String>>();
			newB.addAll(hset);
			partitionCopy.set(i, newB);

			// step (b)
			HashSet<String> newVec = new HashSet<String>();
			B = partitionCopy.get(i);
			
			for(HashSet<String> hs:B){
				System.out.print(hs.size()+",");
			}
			System.out.println();
			
			for(HashSet<String> X:B){
				String hn = this.collapse(graph, X);
				newVec.add(hn);
			}

			// step (c)
			for(int j=i+1; j<=max; j++){
				HashSet<HashSet<String>> vec = partitionCopy.get(j);
				for(String nn:newVec){
					Vector<HashSet<String>> vecCopy = new Vector<HashSet<String>>();
					for(HashSet<String> hs:vec){
						HashSet<String> nset1 = new HashSet<String>();
						HashSet<String> nset2 = new HashSet<String>();
						for(String n:hs){
							if(graph.containsEdge(n, nn)){
								nset1.add(n);
							}
							else{
								nset2.add(n);
							}
						}
						if(!nset1.isEmpty()){
							vecCopy.add(nset1);
						}
						if(!nset2.isEmpty()){
							vecCopy.add(nset2);
						}
					}
					vec.clear();
					vec.addAll(vecCopy);
				}
				partitionCopy.set(j, vec);
			}

//			for(String newhy:newVec){
//				for(int j=i+1;j<=max;j++){
//					Vector<HashSet<String>> C = partitionCopy.get(j);
//					Vector<HashSet<String>> Ccopy = new Vector<HashSet<String>>();
//					for(HashSet<String> oldhs:C){
//						HashSet<String> nset1 = new HashSet<String>();
//						HashSet<String> nset2 = new HashSet<String>();
//						
//						for(String nn:oldhs){
//							if(graph.containsEdge(nn, newhy)){
//								nset1.add(nn);
//							}
//							else{
//								nset2.add(nn);
//							}
//						}
//						if(!nset1.isEmpty()){
//							Ccopy.add(nset1);
//						}
//						if(!nset2.isEmpty()){
//							Ccopy.add(nset2);
//						}
//					}
//					partitionCopy.set(j, Ccopy);
//				}
//			}
		}
		return partitionCopy;
	}
	
	/***********************************************************************/
	/***********************************************************************/
	/********************below are for incremental bi-simulation*************/
	/***********************************************************************/
	/***********************************************************************/

	public boolean mergeCond(cg_graph gCom, HashSet<String> B1, HashSet<String> B2, Vector<Vector<HashSet<String>>> partitionCopy){
		boolean flag = true;
		int i1=Integer.MIN_VALUE, i2=Integer.MIN_VALUE;
		String s1 = this.blockMap.get(B1);
		String s2 = this.blockMap.get(B2);
		
		if(B1.equals(B2)){
			flag = false;
		}
		
		for(Vector<HashSet<String>> vec:partitionCopy){
			if(vec.contains(B1)){
				i1 = vec.indexOf(B1);
			}
			if(vec.contains(B2)){
				i2 = vec.indexOf(B2);
			}
			if(i1>Integer.MIN_VALUE && i2>Integer.MIN_VALUE){
				break;
			}
		}
		if(i1==i2){
			flag = false;
		}
		
		for(edge e:gCom.outgoingEdgesOf(s1)){
			String C = gCom.getEdgeTarget(e);
			if(gCom.containsEdge(s2, C)){
				flag = false;
				break;
			}
		}
		
		for(edge e:gCom.outgoingEdgesOf(s2)){
			String C = gCom.getEdgeTarget(e);
			if(gCom.containsEdge(s1, C)){
				flag = false;
				break;
			}
		}
		
		return flag;
	}
	
	
	public void rec_merge(cg_graph gCom, HashSet<String> B1, HashSet<String> B2, Vector<Vector<HashSet<String>>> partitionCopy){
		String s1 = this.blockMap.get(B1);
		String s2 = this.blockMap.get(B2);
		
		for(edge e:gCom.incomingEdgesOf(s1)){
			String fn = gCom.getEdgeSource(e);
			HashSet<String> blockfn = this.hynodeMap.get(fn);
			
			for(edge e1:gCom.incomingEdgesOf(s2)){
				String fn1 = gCom.getEdgeSource(e);
				HashSet<String> blockfn1 = this.hynodeMap.get(fn1);
				
				if(this.mergeCond(gCom, blockfn, blockfn1, partitionCopy)){
					this.rec_merge(gCom, blockfn, blockfn1, partitionCopy);
				}
			}
		}
	}
	
	public void mergePhase(HashSet<String> U, HashSet<String> V, cg_graph gCom, Vector<Vector<HashSet<String>>> partitionCopy){
		String node = this.blockMap.get(V);
		for(edge e:gCom.incomingEdgesOf(node)){
			String fn = gCom.getEdgeSource(e);
			HashSet<String> U1 = this.hynodeMap.get(fn);
			if(this.mergeCond(gCom, U1, U, partitionCopy)){
				this.rec_merge(gCom, U1, U, partitionCopy);
			}
		}
	}
	
	/**
	 * main part of the incremental bisimulation
	 * @param blocks : 
	 * @param fnode  : from node
	 * @param tnode  : to node
	 * @param gCom   : compressed graph
	 * @param P 	 : initial partition
	 * @param nset 	 : Universal elements
	 * @param pMap 	 : mapping between node and its corresponding parent nodes
	 */
	public void incAdd(Vector<Vector<HashSet<String>>> partitionCopy, cg_graph updatedG, HashMap<String, Integer> rankvalue, String fnode, String tnode, cg_graph gCom, HashSet<HashSet<String>> P, HashSet<String> nset){
		PaigeTarjan pt = new PaigeTarjan();
		
		HashSet<String> U = this.hynodeMap.get(fnode);
		HashSet<String> V = this.hynodeMap.get(tnode);
		
		String hn1 = this.blockMap.get(U);
		String hn2 = this.blockMap.get(V);
		
		if(gCom.containsEdge(hn1, hn2)){
			// add the edge u->v to original graph
			return;
		}
		
		pt.Refine(P, nset, this.pMap);
		
		if(this.WF.contains(fnode) && !this.WF.contains(tnode)){
			int previous = rankvalue.get(fnode);
			int rank = Math.max(previous, rankvalue.get(tnode)+1);
			rankvalue.put(fnode, rank);			
			
			if(previous!=rank){
				cg_graph dag = this.sccDAG(updatedG);
				this.WF.clear();
				this.rankvalue.clear();
				this.wellfounded(dag);
				this.inirank(dag);
			}
			this.mergePhase(U, V, gCom, partitionCopy);
		}
		else{
			 if(rankvalue.get(fnode)>rankvalue.get(tnode)){
				 this.mergePhase(U, V, gCom, partitionCopy);
			 }
			 else{
				 cg_graph dag = this.sccDAG(updatedG);
				 this.WF.clear();
				 this.rankvalue.clear();
				 this.wellfounded(dag);
				 this.inirank(dag);
			 }
		}
	}
	
	
	public void indexBuild(cg_graph Graph, String filePath){
		double s0 = System.currentTimeMillis();
		for(String n:Graph.vertexSet()){
			Vector<String> pset = Graph.getParents(n);
			this.pMap.put(n, pset);
		}
		double s01 = System.currentTimeMillis();
		System.out.println("Parent computation time: "+(s01-s0));
		
		try{
			FileOutputStream fos = new FileOutputStream(filePath, true);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(this.pMap);
			oos.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	
	@SuppressWarnings("unchecked")
	public HashMap<String, Vector<String>> indexLoad(String filePath){		
		HashMap<String, Vector<String>> pset = new HashMap<String, Vector<String>>();
		try{
			FileInputStream fis = new FileInputStream(filePath);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream ois = new ObjectInputStream(bis);
			pset = (HashMap<String, Vector<String>>) ois.readObject();
			ois.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}	
		return pset;		
	}
	
	public static void main(String[] args) throws IOException{
		bisim bs = new bisim();
		readfile rf = new readfile();
//		String path = "/disk/scratch/dataset/p2p-Gnutella08.grp";
//		String pathgrp = "/Users/jiangxianlin/dataset/random/7-Ran-200-500.grp";
//		String pathattr = "/Users/jiangxianlin/dataset/random/7-Ran-200-500.atr";
		String pathgrp="15-Ran-100-1000.grp";
		String pathattr="15-Ran-100-1000.atr";
		cg_graph Graph = (cg_graph) rf.read(pathgrp);
		HashMap<String, String> attr = (HashMap<String, String>) rf.read(pathattr);
		
		Graph.displaySize();
		
		for(String n:Graph.vertexSet()){
			Vector<String> pset = Graph.getParents(n);
			bs.pMap.put(n, pset);
		}
		
		cg_graph Gscc = bs.sccDAG(Graph);
		
		double s1 = System.currentTimeMillis();
		bs.wellfounded(Gscc);
		double s2 = System.currentTimeMillis();
		System.out.println("Time a: "+(s2-s1));
		bs.inirank(Gscc);
		double s3 = System.currentTimeMillis();
		System.out.println("Time b: "+(s3-s2));
		HashMap<String, Integer> rankvalue = new HashMap<String, Integer>();		// rank value in the original graph
		for(String n:bs.Map.keySet()){
			HashSet<String> hy = bs.Map.get(n);
			int value = bs.rankvalue.get(n);
			for(String m:hy){
				rankvalue.put(m, value);
			}
		}
		double s4 = System.currentTimeMillis();
		System.out.println("Time c: "+(s4-s3));
		
		ranGraph rg = new ranGraph();
		grpattr grp = rg.new grpattr();
		grp = bs.bisimulation(Graph, attr, rankvalue);
		grp.graph.displaySize();
		
		String path = "/Users/jiangxianlin/dataset/random/7-Ran-200-500-cmp";
		String p = path+".gml";
/**		Pattern query
 * 		bsimQuery bsq = new bsimQuery();
		String pathPattern = "/disk/scratch/dataset/pattern/randomTest/4-4-10.grp";
		cg_graph pattern = gl.load(pathPattern);
		pattern.displaySize();
	
		// initialise edge bound for pattern
		System.out.println("*************************************");
		bsq.nMapIni(Graph);
		
		bsq.eMapIni(pattern);
		short[][] disM = bsq.matrixCmp(Graph);
		
		bsq.inipatBound(pattern);
		double s21 = System.currentTimeMillis();
		bsq.canIni(pattern, Graph, bsq.eMap, disM);
		bsq.iniMatPre(pattern, Graph, bsq.eMap, disM);
		double s22 = System.currentTimeMillis();
		System.out.println("Total time: "+(s22-s21));
*/
	}
}