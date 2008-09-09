import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.openmali.vecmath.Vector3f;

import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.atoms.BufferData.BufferTarget;
import com.ferox.core.states.atoms.BufferData.DataType;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.util.BufferUtil;
import com.ferox.core.util.io.IOManager;

public class PlyReader {
	public static void main(String[] args) {
		Geometry g = readPLYFile(new File("../../ply/dragon_vrip_res4.ply"));
		System.out.println("done reading + converting");
		try {
			IOManager.write(new File("src/data/models/dragon_4.ido2"), g);
			IOManager.read(new File("src/data/models/dragon_4.ido2"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Geometry readPLYFile(File file) {
		try {
			RandomAccessFile in = new RandomAccessFile(file, "r");
			String line = in.readLine();
			boolean startVerts = false;
			int maxVerts = 0;
			int maxInds = 0;
			int vertsRead = 0;
			int indsRead = 0;
			
			ArrayList<float[]> verts = new ArrayList<float[]>();
			ArrayList<int[]> ints = new ArrayList<int[]>();
			
			while (line != null) {
				String[] p = line.split(" ");
				
				if (p[0].equals("end_header")) {
					startVerts = true;
				} else if (p[0].equals("element")) {
					if (p[1].equals("vertex"))
						maxVerts = Integer.parseInt(p[2]);
					else if (p[1].equals("face"))
						maxInds = Integer.parseInt(p[2]);
				} else if (startVerts) {
					if (vertsRead < maxVerts) {
						float[] v = new float[3];
						v[0] = Float.parseFloat(p[0]) * 20;
						v[1] = Float.parseFloat(p[1]) * 20;
						v[2] = Float.parseFloat(p[2]) * 20;
						verts.add(v);
						vertsRead++;
					} else if (indsRead < maxInds) {
						int[] t = new int[3];
						t[0] = Integer.parseInt(p[1]);
						t[1] = Integer.parseInt(p[2]);
						t[2] = Integer.parseInt(p[3]);
						ints.add(t);
						indsRead++;
					} else
						break;
				}
				
				line = in.readLine();
			}
			in.close();
			System.out.println("done reading");
			System.out.println(maxInds + " " + maxVerts + " " + verts.size() + " " + ints.size());
			if (ints.size() != maxInds || verts.size() != maxVerts)
				throw new RuntimeException("error loading");
			
			FloatBuffer v = BufferUtil.newFloatBuffer(verts.size() * 3);
			ArrayList[] poly = new ArrayList[verts.size()];
			
			for (int i = 0; i < verts.size(); i++) {
				float[] vtx = verts.get(i);
				poly[i] = new ArrayList<Vector3f>();
				
				v.put(i * 3, vtx[0]);
				v.put(i * 3 + 1, vtx[1]);
				v.put(i * 3 + 2, vtx[2]);
			}
			
			IntBuffer ind = BufferUtil.newIntBuffer(ints.size() * 3);
			Vector3f e1 = new Vector3f();
			Vector3f e2 = new Vector3f();
			
			for (int i = 0; i < ints.size(); i++) {
				int[] ply = ints.get(i);
				
				ind.put(i * 3, ply[0]);
				ind.put(i * 3 + 1, ply[1]);
				ind.put(i * 3 + 2, ply[2]);
				
				Vector3f normal = new Vector3f();
				float[] v1 = verts.get(ply[0]);
				float[] v2 = verts.get(ply[1]);
				float[] v3 = verts.get(ply[2]);
				
				e1.set(v1);
				normal.set(v2);
				e1.sub(normal);
				e2.set(v1);
				normal.set(v3);
				e2.sub(normal);
				normal.cross(e1, e2);
				normal.normalize();
				
				poly[ply[0]].add(normal);
				poly[ply[1]].add(normal);
				poly[ply[2]].add(normal);
			}
			
			FloatBuffer n = BufferUtil.newFloatBuffer(verts.size() * 3);
			for (int i = 0; i < poly.length; i++) {
				ArrayList<Vector3f> ns = poly[i];
				
				if (ns.size() > 0) {
					e1.set(ns.get(0));
					for (int u = 1; u < ns.size(); u++)
						e1.add(ns.get(u));
					e1.normalize();
				} else {
					e1.set(0f, 0f, 1f);
					System.out.println("zero normal");
				}
				
				n.put(i * 3, e1.x);
				n.put(i * 3 + 1, e1.y);
				n.put(i * 3 + 2, e1.z);
			}
			
			BufferData vertices = new BufferData(v, DataType.FLOAT, v.capacity(), BufferTarget.ARRAY_BUFFER, true);
			BufferData normals = new BufferData(n, DataType.FLOAT, n.capacity(), BufferTarget.ARRAY_BUFFER, true);
			BufferData indices = new BufferData(ind, DataType.UNSIGNED_INT, ind.capacity(), BufferTarget.ELEMENT_BUFFER, true);
			
			return new Geometry(new VertexArray(vertices, 3), new VertexArray(normals, 3), new VertexArray(indices, 1), Geometry.PolygonType.TRIANGLES);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return null;
	}
}
