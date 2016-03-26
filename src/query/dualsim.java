package query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import graph.cg_graph;
import graph.edge;
import graph.ranGraph;
import graph.ranGraph.grpattr;

public class dualsim {
	
	/**
	 * compute longest shortest path in Q (treat Q as undirected graph)
	 * @param Q
	 */
	public int compute_diameter(cg_graph Q){
		int max = 0;
		HashMap<String, Integer> visited = new HashMap<String, Integer>();
		Queue<String> q = new LinkedList<String>();

		for(String vf:Q.vertexSet()){
			visited.clear();
			q.clear();
			q.add(vf);
			visited.put(vf, 0);
			while(!q.isEmpty()){
				String v = q.poll();
				int dist = visited.get(v);
				for(edge e:Q.outgoingEdgesOf(v)){
					String tv = (String) e.getTarget();
					if(!visited.keySet().contains(tv)){
						q.add(tv);
						visited.put(tv, dist+1);
					}
				}
				for(edge e:Q.incomingEdgesOf(v)){
					String fv = (String) e.getSource();
					if(!visited.keySet().contains(fv)){
						q.add(fv);
						visited.put(fv, dist+1);
					}
				}
			}
			
			for(String v:visited.keySet()){
				int dist = visited.get(v);
				if(dist>max){
					max = dist;
				}
			}
		}
		return max;
	}
	
	
	/**
	 * this procedure computes the ball of the node v
	 * @param G : graph G
	 * @param attrG : attribute of G
	 * @param v : centre node
	 * @param radius : radius of the ball
	 */
	public cg_graph compute_ball(cg_graph G, HashMap<String, String> attrG, String v, int radius){
		cg_graph ball = new cg_graph();
		Queue<String> q = new LinkedList<String>();
		HashMap<String, Integer> visited = new HashMap<String, Integer>();
		q.add(v);
		visited.put(v, 0);
		ball.addVertex(v);
		
		while(!q.isEmpty()){
			String current = q.poll();
			int dist = visited.get(current);
			if(dist>=radius){
				break;
			}
			for(edge e:G.outgoingEdgesOf(current)){
				String tv = (String) e.getTarget();
				if(!visited.keySet().contains(tv)){
					q.add(tv);
					visited.put(tv, dist+1);
					if(!ball.containsVertex(tv)){
						ball.addVertex(tv);	
					}
				}
				if(!ball.containsEdge(current, tv)){
					edge newedge = new edge(current, tv);
					ball.addEdge(current, tv, newedge);
				}
			}
			for(edge e:G.incomingEdgesOf(current)){
				String fv = (String) e.getSource();
				if(!visited.keySet().contains(fv)){
					q.add(fv);
					visited.put(fv, dist+1);
					if(!ball.containsVertex(fv)){
						ball.addVertex(fv);	
					}
				}
				if(!ball.containsEdge(fv, current)){
					edge newedge = new edge(fv, current);
					ball.addEdge(fv, current, newedge);
				}
			}
		}
		return ball;
	}
	
	
	/**
	 * this procedure computes dual-simulation relation for a given ball and a pattern Q
	 * @param ball : data graph
	 * @param attrBall
	 * @param Q : pattern graph
	 * @param attrQ
	 */
	public HashMap<String, HashSet<String>> compute_match(cg_graph ball, HashMap<String, String> attrBall, cg_graph Q, HashMap<String, String> attrQ){
		HashMap<String, HashSet<String>> sim = new HashMap<String, HashSet<String>>();	//	mapping between nodes and their matches
		for(String vq:Q.vertexSet()){
			String vqlabel = attrQ.get(vq);
			HashSet<String> matches = sim.get(vq);
			if(matches==null){
				matches = new HashSet<String>();
			}
			for(String vg:ball.vertexSet()	){
				String vglabel = attrBall.get(vg);
				if(vqlabel.equals(vglabel)){
					matches.add(vg);
				}
			}
			sim.put(vq, matches);
		}
		
		Queue<String> q = new LinkedList<String>();
		HashSet<String> rdtfumat = new  HashSet<String>();
		HashSet<String> rdttumat = new  HashSet<String>();
		/**
		 * detect those nodes whose match set varies, then initialises queue q
		 */
		for(edge e:Q.edgeSet()){
			String fu = (String) e.getSource();
			String tu = (String) e.getTarget();
			HashSet<String> fumat = sim.get(fu);
			HashSet<String> tumat = sim.get(tu);
			
			for(String fv:fumat){
				boolean ismatch = false;
				for(String tv:tumat){
					if(ball.containsEdge(fv, tv)){
						ismatch = true;
						break;
					}
				}
				if(!ismatch){
					rdtfumat.add(fv);
					if(!q.contains(fu)){	// can be improved
						q.add(fu);	
					}
				}
			}
			fumat.removeAll(rdtfumat);
			if(fumat.isEmpty()){
				sim.clear();
				return sim;
			}
			rdtfumat.clear();
			
			for(String tv:tumat){
				boolean ismatch = false;
				for(String fv:fumat){
					if(ball.containsEdge(fv, tv)){
						ismatch = true;
						break;
					}
				}
				if(!ismatch){
					rdttumat.add(tv);
					if(!q.contains(tu)){	//	can be improved
						q.add(tu);	
					}
				}
			}
			tumat.removeAll(rdttumat);
			if(tumat.isEmpty()){
				sim.clear();
				return sim;
			}
			rdttumat.clear();
		}
		
		HashSet<String> temp = new HashSet<String>();	// used to maintain invalid matches
		while(!q.isEmpty()){
			String u = q.poll();
			HashSet<String> umat = sim.get(u);
			temp.clear();
			for(String v:umat){
				boolean ismatch = true;
				for(edge e:Q.outgoingEdgesOf(u)){
					String tu = (String) e.getTarget();
					HashSet<String> tumat = sim.get(tu);
					boolean flag = false;
					for(String tv:tumat){
						if(ball.containsEdge(v, tv)){
							flag = true;
							break;
						}
					}
					if(!flag){
						ismatch = false;
						break;
					}
				}
				if(!ismatch){
					temp.add(v);
				}
				
				/** if v satisfies requirements in the downward direction, 
				 *   then we test whether it meets the conditions in the upward direction. 
				 * */
				if(ismatch){
					for(edge e:Q.incomingEdgesOf(u)){
						String fu = (String) e.getSource();
						HashSet<String> fumat = sim.get(fu);
						boolean flag = false;
						for(String fv:fumat){
							if(ball.containsEdge(fv, v)){
								flag = true;
								break;
							}
						}
						if(!flag){
							ismatch = false;
							break;
						}
					}
					if(!ismatch){
						temp.add(v);
					}
				}
			}
			
			/**
			 * if there are changes---some nodes should be removed from umat as they are invliad
			 * then we remove nodes in temp and push u's neighbours onto q.
			 */
			if(!temp.isEmpty()){
				umat.removeAll(temp);
				if(!umat.isEmpty()){
					for(edge e:Q.outgoingEdgesOf(u)){
						String tu = (String) e.getTarget();
						if(!q.contains(tu)){
							q.add(tu);
						}
					}
					for(edge e:Q.incomingEdgesOf(u)){
						String fu = (String) e.getSource();
						if(!q.contains(fu)){
							q.add(fu);
						}
					}
				}
				else{
					sim.clear();
					break;
				}
			}	
		}
		return sim;
	}
	
	
	public static void main(String[] args){
		dualsim ds = new dualsim();
		ranGraph rg = new ranGraph();
		grpattr ga = rg.new grpattr();
		grpattr qa = rg.new grpattr();
		ga = rg.test();
		qa = rg.test1();
		int diameter = ds.compute_diameter(qa.graph);
		System.out.println("diameter of Q: " + diameter);
		cg_graph ball = ds.compute_ball(ga.graph, ga.attr, "4", diameter);
		HashMap<String, HashSet<String>> sim = ds.compute_match(ball, ga.attr, qa.graph, qa.attr);
		for(String u:sim.keySet()){
			HashSet<String> umat = sim.get(u);
			String S = "";
			for(String v:umat){
				S = v+" , "+S;
			}
			System.out.println("pattern node "+u+" : "+S);
		}
//		System.out.println(diameter);
	}
}
