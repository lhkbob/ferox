#!BPY

# """
# Name: 'Idoneus Blender Dumper'
# Blender: 244
# Group: 'Export'
# Tooltip: 'Dump basic data to a text file for importing into the Idoneus engine.'
# """

import Blender;

def exportDump():
	print "Orco, Refl, Nor, UV, Win"
	print Blender.Texture.TexCo.ORCO
	print Blender.Texture.TexCo.REFL
	print Blender.Texture.TexCo.NOR
	print Blender.Texture.TexCo.UV
	print Blender.Texture.TexCo.WIN
	print "mix, multiply, add, subtract, divide, darken, difference, lighten, screen"
	print Blender.Texture.BlendModes.MIX
	print Blender.Texture.BlendModes.MULTIPLY
	print Blender.Texture.BlendModes.ADD
	print Blender.Texture.BlendModes.SUBTRACT
	print Blender.Texture.BlendModes.DIVIDE
	print Blender.Texture.BlendModes.DARKEN
	print Blender.Texture.BlendModes.DIFFERENCE
	print Blender.Texture.BlendModes.LIGHTEN
	print Blender.Texture.BlendModes.SCREEN
	print "extend, clip, clipcube, repeat, checker"
	print Blender.Texture.ExtendModes.EXTEND
	print Blender.Texture.ExtendModes.CLIP
	print Blender.Texture.ExtendModes.CLIPCUBE
	print Blender.Texture.ExtendModes.REPEAT

	tO = Blender.Object.GetSelected()
	if (len(tO) == 0):
		tO = Blender.Object.Get()
	objects = []
	for o in tO:
		if (o != None and o.getType() == 'Mesh'):
			objects += [o]
	if (len(objects) == 0):
		return
		
	strings = []
	for object in objects:
		mats = object.getData(False, True).materials #getMaterials()
		mat = mats[0]
		i = 1
		while (mat is None and i < len(mats)):
			mat = mats[i]
			i+=1

		strings += ["Object: %s" % (object.getName())]
		dumpTransform(strings, object.matrixWorld)
		dumpTextureData(strings, mat)
		dumpMaterialData(strings, mat)
		dumpMeshData(strings, object.getData(False, True))
		
	fout = open(objects[0].getName() + ".dump", "w")
	for s in strings:
		fout.write("%s\n" % (s))
	fout.flush()
	fout.close()
	
	
def dumpMeshData(s, mesh):
	mesh.calcNormals()
	verts = []
	normals = []
	textures = []
	
	s += ["Indices: %d %d" % (len(mesh.faces) * len(mesh.faces[0].verts), len(mesh.faces[0].verts))] 
	for face in mesh.faces:
		for i in range(len(face.verts)):
			if (face.smooth):
				n = face.verts[i].no
			else:
				n = face.no
			if (mesh.faceUV):
				t = face.uv[i]
			else:
				t = None
			s += ["%d" % (coord_index(face.verts[i].co, n, t, verts, normals, textures))] 

	s += ["Vertices: %d" % (len(verts))]
	for vert in verts:
		s +=["%.6f %.6f %.6f" % (vert[0], vert[1], vert[2])]

	s += ["Normals: %d" % (len(normals))]
	for norm in normals:
		s +=["%.6f %.6f %.6f" % (norm[0], norm[1], norm[2])]

	s += ["TexCoords: %d" % (len(textures))]
	for tex in textures:
		s +=["%.6f %.6f" % (tex[0], tex[1])]
		
		
def coord_index(v, n, t, verts, normals, textures):
	m_eps = pow(2, -23)
	for i in range(len(verts)):
		if ((verts[i][0]-m_eps < v[0] and verts[i][0]+m_eps > v[0]) and
			(verts[i][1]-m_eps < v[1] and verts[i][1]+m_eps > v[1]) and
			(verts[i][2]-m_eps < v[2] and verts[i][2]+m_eps > v[2])): # vertex the same, check normal and tc if they're not null
			if (n is not None and (normals[i][0]-m_eps < n[0] and normals[i][0]+m_eps > n[0]) and
								  (normals[i][1]-m_eps < n[1] and normals[i][1]+m_eps > n[1]) and
								  (normals[i][2]-m_eps < n[2] and normals[i][2]+m_eps > n[2])): # normal the same, check tc
				if (t is not None and (textures[i][0]-m_eps < t[0] and textures[i][0]+m_eps > t[0]) and
									  (textures[i][1]-m_eps < t[1] and textures[i][1]+m_eps > t[1])): # tc the same
					return i
				elif (t is None):
					return i
			elif (n is None and t is not None and  (textures[i][0]-m_eps < t[0] and textures[i][0]+m_eps > t[0]) and
												   (textures[i][1]-m_eps < t[1] and textures[i][1]+m_eps > t[1])): # tc the same
				return i
			elif (n is None and t is None):
				return i

	verts+=[v]
	if (n is not None):
		normals+=[n]
	if (t is not None):
		textures+=[t]
	return len(verts) - 1
		

def dumpTransform(s, matrix):
	s += ["Transform: "]
	for row in matrix:
		s += ["%.6f %.6f %.6f %.6f" % (row[0], row[1], row[2], row[3])]
		

def dumpMaterialData(s, mat):
	if (mat != None):
		s += ["Materials: 1"]
		s += ["Diffuse: %.6f %.6f %.6f %.6f" % (mat.rgbCol[0], mat.rgbCol[1], mat.rgbCol[2], mat.alpha)]
		s += ["Specular: %.6f %.6f %.6f %.6f" % (mat.specR, mat.specG, mat.specB, 1)]
		s += ["Shininess: %.6f" % (mat.hard / 2.0)]
	else:
		s += ["Materials: 0"]

def dumpTextureData(s, mat):
	if (mat != None):
		rT = []
		for t in mat.getTextures():
			if (valid_tex(t)):
				rT += [t]
		s += ["Textures: %d" % (min(4, len(rT)))]
		for t in rT:
			s += ["Image: %s" % (t.tex.getImage().getFilename())]
			s += ["Mipmap: %d" % (t.tex.mipmap)]
			s += ["NormalMap: %d" % (t.tex.normalMap)]
			s += ["EdgeMode: %d" % (t.tex.extend)]
			s += ["Interpolate: %d" % (t.tex.interpol)]
			s += ["Color: %.6f %.6f %.6f 1" % (t.col[0], t.col[1], t.col[2])]
			s += ["CoordSource: %d" % (t.texco)]
			s += ["BlendMode: %d" % (t.blendmode)] # blender: java -> {multiply,darken: MODULATE; add: ADD; subtract, difference: SUBTRACT; mix: BLEND}, others once I have shaders
			s += ["AffectsColor: %d" % (int(Blender.Texture.MapTo.COL | t.mapto == Blender.Texture.MapTo.COL))]
			s += ["AffectsNormals: %d" % (int(Blender.Texture.MapTo.NOR | t.mapto == Blender.Texture.MapTo.NOR))]
			s += ["AffectsReflection: %d" % (int(Blender.Texture.MapTo.REF | t.mapto == Blender.Texture.MapTo.REF))]
	else:
		s += ["Textures 0"]
				
def valid_tex(tex):
	if ((tex is None) or (tex.tex is None) or (not tex.tex.getType().__eq__('Image')) or (tex.tex.getImage() is None)
		or (tex.tex.getImage().getFilename() is None) or (tex.tex.getImage().getFilename().__eq__(""))):
		return False
	return True

exportDump();