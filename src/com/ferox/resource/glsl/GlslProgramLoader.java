package com.ferox.resource.glsl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/** This is a very simple utility that loads a GlslProgram from
 * two input text files that store the glsl code for the program.
 * 
 * @author Michael Ludwig
 *
 */
public class GlslProgramLoader {
	/** Load the two files as text files and return a GlslProgram that uses that
	 * text as its vertex and fragment shaders.  If vertexShader is null, then
	 * no vertex shader is used by the program.  Similarly if fragmentShader is null, no 
	 * fragment shader is used for the GlslProgram.
	 * 
	 * Throws an IOException if any I/O problem occurs while loading. */
	public static GlslProgram load(File vertexShader, File fragmentShader) throws IOException {
		String[] vCode = (vertexShader == null ? null : readAll(vertexShader));
		String[] fCode = (fragmentShader == null ? null : readAll(fragmentShader));
		
		return new GlslProgram(vCode, fCode);
	}
	
	/* Utility to read all lines of a file and convert it to an array of strings. */
	private static String[] readAll(File file) throws IOException {
		InputStreamReader stream = new InputStreamReader(new FileInputStream(file));
		BufferedReader reader = new BufferedReader(stream);
		
		List<String> lines = new ArrayList<String>();
		String line;
		while ((line = reader.readLine()) != null) {
			// read each line and add it to the list
			lines.add(line);
		}
		
		reader.close();
		return lines.toArray(new String[lines.size()]);
	}
}
